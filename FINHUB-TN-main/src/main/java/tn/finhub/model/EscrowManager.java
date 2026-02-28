package tn.finhub.model;

import tn.finhub.util.DBConnection;
import tn.finhub.util.TokenGenerator;
import tn.finhub.util.DatabaseFixer;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EscrowManager {

    private final WalletModel walletModel = new WalletModel();
    private final UserModel userModel = new UserModel();
    private final BlockchainManager blockchainManager = new BlockchainManager();
    private final SupportModel supportModel = new SupportModel();

    public EscrowManager() {
        // Temporary fix for schema
        DatabaseFixer.fixEscrowTable();
    }

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    // 1. Create Escrow
    public void createEscrow(int senderWalletId, int receiverWalletId, BigDecimal amount, String condition,
            String type) {
        if (senderWalletId == receiverWalletId) {
            throw new RuntimeException("Cannot create escrow with self");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        // Check Verification Status (Risk Limit)
        Wallet senderWallet = walletModel.findById(senderWalletId);
        User sender = userModel.findById(senderWallet.getUserId());

        if (!sender.isEmailVerified()) {
            // Unverified Limit: 500 TND
            if (amount.compareTo(new BigDecimal("500")) > 0) {
                throw new RuntimeException(
                        "Unverified users cannot escrow more than 500 TND. Please verify your email/identity.");
            }
        }

        // 1. Hold Funds (Moves to Escrow Balance)
        String conditionSummary = condition.length() > 30 ? condition.substring(0, 30) + "..." : condition;
        walletModel.hold(senderWalletId, amount, "Escrow: " + conditionSummary);

        // 2. Generate Secret & QR Code
        String secretCode = null;
        String qrCodeImage = null;
        if ("QR_CODE".equals(type)) {
            // Simple random string
            secretCode = TokenGenerator.generateToken().substring(0, 10).toUpperCase();
            // Generate QR Code
            qrCodeImage = tn.finhub.util.QRCodeGenerator.generateQRCodeImage(secretCode, 300, 300);
        }

        // 3. Insert Escrow
        String sql = """
                    INSERT INTO escrow
                    (sender_wallet_id, receiver_wallet_id, amount, condition_text, escrow_type, secret_code, qr_code_image, status, expiry_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'LOCKED', ?)
                """;

        int escrowId = -1;
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderWalletId);
            ps.setInt(2, receiverWalletId);
            ps.setBigDecimal(3, amount);
            ps.setString(4, condition);
            ps.setString(5, type);
            ps.setString(6, secretCode); // Can be null if ADMIN
            ps.setString(7, qrCodeImage);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now().plusDays(7))); // 7 days expiry

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    escrowId = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Escrow DB Insert Failed", e);
        }

        // 4. Log to Blockchain
        blockchainManager.addBlock("ESCROW_CREATE",
                "Created Escrow " + escrowId + " Amount: " + amount,
                null, escrowId);
    }

    // 2. Release Escrow (QR / Auto)
    public void releaseEscrow(int escrowId, String inputCode) {
        Escrow escrow = findById(escrowId);
        if (escrow == null)
            throw new RuntimeException("Escrow not found");
        if (!"LOCKED".equals(escrow.getStatus()))
            throw new RuntimeException("Escrow is not LOCKED");
        if (escrow.isDisputed())
            throw new RuntimeException("Escrow is DISPUTED. Contact Admin.");

        // Verify Code
        if ("QR_CODE".equals(escrow.getEscrowType())) {
            if (escrow.getSecretCode() == null || !escrow.getSecretCode().equals(inputCode)) {
                throw new RuntimeException("Invalid Secret Code");
            }
        }

        processRelease(escrow);
    }

    // 3. Release by Admin
    public void releaseEscrowByAdmin(int escrowId, int adminUserId) {
        Escrow escrow = findById(escrowId);
        if (escrow == null)
            throw new RuntimeException("Escrow not found");
        if (!"LOCKED".equals(escrow.getStatus()) && !"DISPUTED".equals(escrow.getStatus())) {
            throw new RuntimeException("Escrow status is " + escrow.getStatus());
        }

        processRelease(escrow);

        // Log Admin Action (Update admin_approver_id)
        updateAdminApprover(escrowId, adminUserId);
    }

    // 3.1 Release by Sender (Voluntary)
    public void releaseEscrowBySender(int escrowId, int userId) {
        Escrow escrow = findById(escrowId);
        if (escrow == null)
            throw new RuntimeException("Escrow not found");

        Wallet senderWallet = walletModel.findByUserId(userId);
        if (senderWallet == null || senderWallet.getId() != escrow.getSenderWalletId()) {
            throw new RuntimeException("Unauthorized: Only sender can release funds manually.");
        }

        if (!"LOCKED".equals(escrow.getStatus())) {
            throw new RuntimeException("Escrow is not LOCKED");
        }

        processRelease(escrow);
    }

    private void processRelease(Escrow escrow) {
        // Calculate Fee (1%)
        BigDecimal fee = escrow.getAmount().multiply(new BigDecimal("0.01"));
        int adminWalletId = walletModel.getBankWalletId();

        // Transfer Funds
        walletModel.processEscrowRelease(
                escrow.getSenderWalletId(),
                escrow.getReceiverWalletId(),
                adminWalletId,
                escrow.getAmount(),
                fee);

        // Update Status
        updateStatus(escrow.getId(), "RELEASED");

        // Blockchain
        blockchainManager.addBlock("ESCROW_RELEASE",
                "Released Escrow " + escrow.getId(),
                null, escrow.getId());

        // Increase Trust Score for Sender (Seller? Wait, Sender is BUYER in escrow
        // usually?)
        // Escrow: Sender = Buyer (puts money in). Receiver = Seller (gets money).
        // Trust Score should go to SELLER (Receiver) for fulfilling the order.
        // User requesting improvement said: "trust of the seller". Seller is the
        // Receiver.
        Wallet receiverWallet = walletModel.findById(escrow.getReceiverWalletId());
        userModel.updateTrustScore(receiverWallet.getUserId(), 10);
    }

    // 4. Refund (Cancellation)
    public void refundEscrow(int escrowId) {
        Escrow escrow = findById(escrowId);
        if (escrow == null)
            throw new RuntimeException("Escrow not found");
        if ("RELEASED".equals(escrow.getStatus()))
            throw new RuntimeException("Already Released");

        walletModel.refundEscrow(escrow.getSenderWalletId(), escrow.getAmount());

        updateStatus(escrowId, "REFUNDED");

        blockchainManager.addBlock("ESCROW_REFUND",
                "Refunded Escrow " + escrowId,
                null, escrowId);
    }

    // 4.1 Claim Refund (Expiry)
    public void claimRefund(int escrowId, int userId) {
        Escrow escrow = findById(escrowId);
        if (escrow == null)
            throw new RuntimeException("Escrow not found");

        Wallet senderWallet = walletModel.findByUserId(userId);
        if (senderWallet == null || senderWallet.getId() != escrow.getSenderWalletId()) {
            throw new RuntimeException("Unauthorized: Only sender can claim refund.");
        }

        if (!"LOCKED".equals(escrow.getStatus())) {
            throw new RuntimeException("Escrow is not LOCKED");
        }

        if (escrow.getExpiryDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Escrow has not expired yet. Expiry: " + escrow.getExpiryDate());
        }

        refundEscrow(escrowId);
    }

    // 5. Dispute
    public void raiseDispute(int escrowId) {
        Escrow escrow = findById(escrowId);
        if (escrow == null)
            throw new RuntimeException("Escrow not found");
        if (!"LOCKED".equals(escrow.getStatus()))
            throw new RuntimeException("Cannot dispute non-locked escrow");

        // Update DB
        String sql = "UPDATE escrow SET is_disputed = TRUE, status = 'DISPUTED' WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, escrowId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Blockchain
        blockchainManager.addBlock("ESCROW_DISPUTE",
                "Dispute raised for Escrow " + escrowId,
                null, escrowId);

        // Create Support Ticket
        Wallet senderWallet = walletModel.findById(escrow.getSenderWalletId());
        supportModel.createTicketWithInitialMessage(
                senderWallet.getUserId(),
                "Dispute Escrow #" + escrowId,
                "Escrow Dispute",
                "I am disputing this transaction. Please investigate.");
    }

    // Helpers
    public Escrow findById(int id) {
        String sql = "SELECT * FROM escrow WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Escrow e = new Escrow();
                    e.setId(rs.getInt("id"));
                    e.setSenderWalletId(rs.getInt("sender_wallet_id"));
                    e.setReceiverWalletId(rs.getInt("receiver_wallet_id"));
                    e.setAmount(rs.getBigDecimal("amount"));
                    e.setConditionText(rs.getString("condition_text"));
                    e.setEscrowType(rs.getString("escrow_type"));
                    e.setSecretCode(rs.getString("secret_code"));
                    e.setStatus(rs.getString("status"));
                    e.setExpiryDate(rs.getTimestamp("expiry_date").toLocalDateTime());
                    e.setDisputed(rs.getBoolean("is_disputed"));
                    return e;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateStatus(int id, String status) {
        String sql = "UPDATE escrow SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateAdminApprover(int id, int adminId) {
        String sql = "UPDATE escrow SET admin_approver_id = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasActiveEscrows(int walletId) {
        String sql = "SELECT COUNT(*) FROM escrow WHERE (sender_wallet_id = ? OR receiver_wallet_id = ?) AND status IN ('LOCKED', 'DISPUTED')";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setInt(2, walletId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void deleteByWalletId(int walletId) {
        String sql = "DELETE FROM escrow WHERE sender_wallet_id = ? OR receiver_wallet_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setInt(2, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete escrows for wallet " + walletId, e);
        }
    }

    public List<Escrow> getEscrowsByWalletId(int walletId) {
        List<Escrow> list = new ArrayList<>();
        String sql = "SELECT * FROM escrow WHERE sender_wallet_id = ? OR receiver_wallet_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setInt(2, walletId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Escrow e = mapResultSetToEscrow(rs);
                list.add(e);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Escrow> getEscrowsForAdmin() {
        List<Escrow> list = new ArrayList<>();
        // Fetch all escrows, ordered by status priority (DISPUTED > LOCKED > OTHERS)
        String sql = "SELECT * FROM escrow ORDER BY CASE WHEN status = 'DISPUTED' THEN 1 WHEN status = 'LOCKED' THEN 2 ELSE 3 END, created_at DESC";
        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Escrow e = mapResultSetToEscrow(rs);
                list.add(e);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public BigDecimal getTotalActiveEscrowAmount() {
        String sql = "SELECT SUM(amount) FROM escrow WHERE status IN ('LOCKED', 'DISPUTED')";
        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal(1);
                return total != null ? total : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    private Escrow mapResultSetToEscrow(ResultSet rs) throws SQLException {
        Escrow e = new Escrow();
        e.setId(rs.getInt("id"));
        e.setSenderWalletId(rs.getInt("sender_wallet_id"));
        e.setReceiverWalletId(rs.getInt("receiver_wallet_id"));
        e.setAmount(rs.getBigDecimal("amount"));
        e.setConditionText(rs.getString("condition_text"));
        e.setEscrowType(rs.getString("escrow_type"));
        e.setSecretCode(rs.getString("secret_code"));
        e.setQrCodeImage(rs.getString("qr_code_image"));
        e.setStatus(rs.getString("status"));
        if (rs.getTimestamp("expiry_date") != null)
            e.setExpiryDate(rs.getTimestamp("expiry_date").toLocalDateTime());
        e.setDisputed(rs.getBoolean("is_disputed"));
        if (rs.getTimestamp("created_at") != null)
            e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return e;
    }
}
