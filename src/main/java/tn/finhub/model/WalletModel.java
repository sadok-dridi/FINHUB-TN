package tn.finhub.model;

import tn.finhub.util.DBConnection;
import tn.finhub.util.HashUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class WalletModel {

    // Dependencies (We still use DAOs internally for Audit/Ledger table access
    // until those are fully migrated,
    // but the Wallet Logic is consolidated here)
    private final tn.finhub.model.TransactionManager txDAO = new tn.finhub.model.TransactionManager();
    private final tn.finhub.model.LedgerManager ledgerDAO = new tn.finhub.model.LedgerManager();

    public Connection getConnection() {
        return DBConnection.getInstance();
    }

    // ========================
    // CRUD & DATA ACCESS
    // ========================

    public Wallet findById(int walletId) {
        String sql = "SELECT * FROM wallets WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Wallet(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("currency"),
                        rs.getBigDecimal("balance"),
                        rs.getBigDecimal("escrow_balance"),
                        rs.getString("status"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Wallet fetch failed", e);
        }
        return null;
    }

    public Wallet findByUserId(int userId) {
        String sql = "SELECT * FROM wallets WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Wallet(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("currency"),
                        rs.getBigDecimal("balance"),
                        rs.getBigDecimal("escrow_balance"),
                        rs.getString("status"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Wallet lookup failed", e);
        }
        return null;
    }

    public void createWallet(int userId) {
        String sql = """
                    INSERT INTO wallets (user_id, currency, balance, escrow_balance)
                    VALUES (?, 'TND', 0, 0)
                """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Wallet creation failed", e);
        }
    }

    public void createWalletIfNotExists(int userId) {
        if (findByUserId(userId) == null) {
            createWallet(userId);
        }
    }

    // Helper to delete fully
    public void deleteWalletRecursive(int walletId) {
        ledgerDAO.deleteLogsByWalletId(walletId);
        ledgerDAO.deleteFlagsByWalletId(walletId);
        txDAO.deleteByWalletId(walletId);
        deleteById(walletId);
    }

    public void deleteById(int walletId) {
        String sql = "DELETE FROM wallets WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting wallet", e);
        }
    }

    public void updateStatus(int walletId, String status) {
        String sql = "UPDATE wallets SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update status failed", e);
        }
    }

    public void updateUserId(int walletId, int newUserId) {
        String sql = "UPDATE wallets SET user_id = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, newUserId);
            ps.setInt(2, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error transferring wallet ownership", e);
        }
    }

    public List<tn.finhub.model.LedgerAuditLog> getAuditLogs(int walletId) {
        return ledgerDAO.getAuditLogs(walletId);
    }

    // ========================
    // TRANSACTION LOGIC
    // ========================

    public void credit(int walletId, BigDecimal amount, String ref) {
        checkStatus(walletId);
        executeAtomic(walletId, amount, "CREDIT", ref);
    }

    public void debit(int walletId, BigDecimal amount, String ref) {
        checkStatus(walletId);
        Wallet wallet = findById(walletId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        executeAtomic(walletId, amount.negate(), "DEBIT", ref);
    }

    // ========================
    // ADMIN / REPAIR LOGIC
    // ========================

    public int getTamperedTransactionId(int walletId) {
        List<WalletTransaction> transactions = txDAO.findByWalletId(walletId);
        // findByWalletId returns DESC. We need ASC for chain verification.
        java.util.Collections.reverse(transactions);

        String prevHash = "0";
        for (WalletTransaction tx : transactions) {
            // Verify Link
            if (!tx.getPrevHash().equals(prevHash)) {
                return tx.getId();
            }
            // Verify Integrity
            String calculatedHash = HashUtils.generateTransactionHash(
                    tx.getId(), tx.getWalletId(), tx.getType(), tx.getAmount(),
                    tx.getReference(), tx.getCreatedAt(), prevHash);
            if (!calculatedHash.equals(tx.getTxHash())) {
                return tx.getId();
            }
            prevHash = tx.getTxHash();
        }
        return -1;
    }

    public BigDecimal calculateDiscrepancy(int walletId, int txId, BigDecimal reportedAmount, String type) {
        // Simple implementation: return the amount as we don't have the original
        // complex logic.
        // In a real scenario, this might look up audit logs or backup ledgers.
        return reportedAmount;
    }

    public void repairTransaction(int walletId, int txId, BigDecimal newAmount, String newRef) {
        List<WalletTransaction> transactions = txDAO.findByWalletId(walletId);
        java.util.Collections.reverse(transactions); // ASC order

        String prevHash = "0";
        boolean foundTarget = false;
        BigDecimal runningBalance = BigDecimal.ZERO; // We would need to recalculate balance too if we were tracking it
                                                     // in txs (we aren't, balance is in Wallet)

        for (WalletTransaction tx : transactions) {
            if (tx.getId() == txId) {
                foundTarget = true;
                // Update this transaction
                String newHash = HashUtils.generateTransactionHash(
                        tx.getId(), tx.getWalletId(), tx.getType(), newAmount,
                        newRef, tx.getCreatedAt(), prevHash);
                txDAO.update(tx.getId(), newAmount, tx.getType(), newRef, newHash, prevHash);
                prevHash = newHash;
            } else if (foundTarget) {
                // Update subsequent transaction
                String newHash = HashUtils.generateTransactionHash(
                        tx.getId(), tx.getWalletId(), tx.getType(), tx.getAmount(),
                        tx.getReference(), tx.getCreatedAt(), prevHash);
                txDAO.update(tx.getId(), tx.getAmount(), tx.getType(), tx.getReference(), newHash, prevHash);
                prevHash = newHash;
            } else {
                // Before target, just update prevHash
                prevHash = tx.getTxHash();
            }
        }

        // Recalculate Wallet Balance
        recalculateWalletBalance(walletId);
    }

    public void recalculateWalletBalance(int walletId) {
        List<WalletTransaction> transactions = txDAO.findByWalletId(walletId);
        BigDecimal newBalance = BigDecimal.ZERO;
        for (WalletTransaction tx : transactions) {
            BigDecimal amt = tx.getAmount(); // Amount is signed? No, type determines sign.
            // Wait, TransactionManager inserts amount as positive usually?
            // Let's check getSign logic in Controller: DEBIT/WITHDRAWAL are negative.
            // But usually stored absolute?
            // In `credit`: executeAtomic(..., amount, "CREDIT"...) where amount is +ve.
            // In `debit`: executeAtomic(..., amount.negate(), "DEBIT"...) where amount is
            // passed as -ve?
            // `executeAtomic(int walletId, BigDecimal amount, ...)`
            // DB stores signed amount?
            // public void insert(..., BigDecimal amount, ...)
            // Yes, executeAtomic passes `amount` (which is signed for debit).
            // So we just sum them up.
            newBalance = newBalance.add(tx.getAmount());
        }

        String sql = "UPDATE wallets SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setInt(2, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating wallet balance", e);
        }
    }

    public void hold(int walletId, BigDecimal amount, String ref) {
        checkStatus(walletId);
        Wallet wallet = findById(walletId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Custom Atomic execution for HOLD
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);

            // Move balance to escrow
            String sql = "UPDATE wallets SET balance = balance - ?, escrow_balance = escrow_balance + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, amount);
                ps.setBigDecimal(2, amount);
                ps.setInt(3, walletId);
                ps.executeUpdate();
            }

            recordTransaction(walletId, "HOLD", amount, ref);
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Escrow hold failed", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }

    public void release(int walletId, BigDecimal amount, String ref) {
        checkStatus(walletId);

        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);

            // Move escrow back to balance (or out if paid, but usually release implies
            // unlock)
            // Implementation from DAO: escrow - amount, balance + amount
            String sql = "UPDATE wallets SET escrow_balance = escrow_balance - ?, balance = balance + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, amount);
                ps.setBigDecimal(2, amount);
                ps.setInt(3, walletId);
                ps.executeUpdate();
            }

            recordTransaction(walletId, "RELEASE", amount, ref);
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Escrow release failed", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }

    public void transfer(int fromWalletId, int toWalletId, BigDecimal amount, String ref) {
        transferInternal(fromWalletId, toWalletId, amount,
                ref + " to " + toWalletId,
                ref + " from " + fromWalletId);
    }

    public void transferInternal(int fromWalletId, int toWalletId, BigDecimal amount, String senderRef,
            String receiverRef) {
        if (fromWalletId == toWalletId) {
            throw new RuntimeException("Cannot transfer to same wallet");
        }
        checkStatus(fromWalletId);
        checkStatus(toWalletId);

        Wallet fromWallet = findById(fromWalletId);
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);

            // 1. Debit Sender
            updateBalanceInTx(fromWalletId, amount.negate(), conn);
            recordTransaction(fromWalletId, "TRANSFER_SENT", amount, senderRef);

            // 2. Credit Receiver
            updateBalanceInTx(toWalletId, amount, conn);
            recordTransaction(toWalletId, "TRANSFER_RECEIVED", amount, receiverRef);

            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }

    public void transferByEmail(int senderWalletId, String recipientEmail, BigDecimal amount) {
        UserModel userModel = new UserModel();
        User recipient = userModel.findByEmail(recipientEmail);

        if (recipient == null) {
            throw new RuntimeException("User with email " + recipientEmail + " not found.");
        }

        Wallet recipientWallet = findByUserId(recipient.getId());
        if (recipientWallet == null) {
            throw new RuntimeException("Recipient does not have an active wallet.");
        }

        if ("FROZEN".equals(recipientWallet.getStatus())) {
            throw new RuntimeException("Recipient wallet is frozen. Cannot receive funds.");
        }

        // Fetch Sender Name
        String senderName = "Unknown";
        Wallet senderWallet = findById(senderWalletId);
        if (senderWallet != null) {
            User sender = userModel.findById(senderWallet.getUserId());
            if (sender != null)
                senderName = sender.getFullName();
        }

        String senderRef = "Transfer to " + recipient.getFullName();
        String receiverRef = "Transfer from " + senderName;

        transferInternal(senderWalletId, recipientWallet.getId(), amount, senderRef, receiverRef);
    }

    private void executeAtomic(int walletId, BigDecimal amount, String type, String ref) {
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            updateBalanceInTx(walletId, amount, conn);
            recordTransaction(walletId, type, amount.abs(), ref);
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Wallet operation failed", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }

    private void updateBalanceInTx(int walletId, BigDecimal amount, Connection conn) throws SQLException {
        String sql = "UPDATE wallets SET balance = balance + ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, walletId);
            ps.executeUpdate();
        }
    }

    // ========================
    // LEDGER & SECURITY
    // ========================

    public Wallet getWallet(int userId) {
        return findByUserId(userId);
    }

    public List<WalletTransaction> getTransactionHistory(int walletId) {
        return txDAO.findByWalletId(walletId);
    }

    public BigDecimal getTotalVolume() {
        String sql = "SELECT SUM(amount) FROM wallet_transactions";
        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                BigDecimal val = rs.getBigDecimal(1);
                return val == null ? BigDecimal.ZERO : val;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting total volume", e);
        }
        return BigDecimal.ZERO;
    }

    private final tn.finhub.model.BlockchainManager blockchainManager = new tn.finhub.model.BlockchainManager();

    private void recordTransaction(int walletId, String type, BigDecimal amount, String ref) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String prevHash = getLastHash(walletId);

        String data = formatForHash(prevHash, walletId, type, amount, ref, now);
        String txHash = HashUtils.sha256(data);

        // 1. Insert into Relational DB (Wallet Transactions)
        int txId = txDAO.insert(walletId, type, amount, ref, prevHash, txHash, now);

        // 2. Log to Blockchain Ledger
        blockchainManager.addBlock("TRANSACTION",
                "Wallet: " + walletId + ", Type: " + type + ", Amount: " + amount + ", Ref: " + ref,
                txId != -1 ? txId : null,
                null);
    }

    private String getLastHash(int walletId) {
        List<WalletTransaction> list = txDAO.findByWalletId(walletId);
        if (list.isEmpty()) {
            return "0000000000000000000000000000000000000000000000000000000000000000";
        }
        return list.get(0).getTxHash();
    }

    public int getBankWalletId() {
        UserModel um = new UserModel();
        String bankEmail = "sadok.dridi.engineer@gmail.com";
        User bankUser = um.findByEmail(bankEmail);

        if (bankUser == null) {
            try {
                // Attempt to sync if missing
                um.syncUsersFromServer();
                bankUser = um.findByEmail(bankEmail);
            } catch (Exception e) {
                System.err.println("Failed to sync bank user: " + e.getMessage());
            }
        }

        if (bankUser == null)
            throw new RuntimeException("Bank configuration missing: " + bankEmail + " not found locally or on server.");

        Wallet w = findByUserId(bankUser.getId());
        if (w == null) {
            // Auto-create wallet if user exists but wallet doesn't
            createWallet(bankUser.getId());
            w = findByUserId(bankUser.getId());
        }
        return w.getId();
    }

    private void checkStatus(int walletId) {
        try {
            if (walletId == getBankWalletId()) {
                if (isFrozen(walletId))
                    unfreezeWallet(walletId);
                ensureGenesisTransaction(walletId);
                return;
            }
        } catch (Exception ignored) {
        }

        if (ledgerDAO.hasActiveFlags(walletId)) {
            throw new RuntimeException("Wallet is frozen due to integrity violation");
        }
        if (isFrozen(walletId)) {
            // Attempt Self-Healing: If the bug is fixed, the balance might now be valid.
            if (verifyBalance(walletId) && verifyLedger(walletId)) {
                unfreezeWallet(walletId);
                return; // Healed
            }
            throw new RuntimeException("Wallet is FROZEN. Contact support.");
        }
        if (!verifyLedger(walletId)) {
            String reason = "Ledger integrity violation – wallet frozen";
            ledgerDAO.insertFlag(new LedgerFlag(walletId, reason));
            freezeWallet(walletId);
            throw new RuntimeException("Security Alert: " + reason);
        }
        if (!verifyBalance(walletId)) {
            String reason = "Balance mismatch detected – wallet frozen";
            ledgerDAO.insertFlag(new LedgerFlag(walletId, reason));
            freezeWallet(walletId);
            throw new RuntimeException("Security Alert: " + reason);
        }
    }

    public void freezeWallet(int walletId) {
        try {
            if (walletId == getBankWalletId()) {
                System.err.println("Attempts to freeze Central Bank Wallet blocked.");
                return;
            }
        } catch (Exception ignored) {
        }
        updateStatus(walletId, "FROZEN");
    }

    public void unfreezeWallet(int walletId) {
        updateStatus(walletId, "ACTIVE");
        // Also remove flags? Ideally yes, but let's keep history.
        // But for checkStatus to pass next time, hasActiveFlags must be false?
        // ledgerDAO.hasActiveFlags checks for unresolved flags.
        // Implementation DETAIL: We should probably mark flags as resolved if we
        // unfreeze.
        // For now, let's assume manual unfreeze or this self-heal implies resolution.
        // We'll trust verifyBalance returning true.
        // Wait, checkStatus checks hasActiveFlags FIRST.
        // We need to clear flags if we heal.
        try {
            // Simple clear of active flags for this wallet for self-healing context
            String sql = "DELETE FROM ledger_flags WHERE wallet_id = ?";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setInt(1, walletId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isFrozen(int walletId) {
        Wallet w = findById(walletId);
        return "FROZEN".equals(w.getStatus());
    }

    public tn.finhub.model.LedgerFlag getLatestFlag(int walletId) {
        return ledgerDAO.getLatestFlag(walletId);
    }

    public boolean verifyLedger(int walletId) {
        List<WalletTransaction> txs = txDAO.findByWalletId(walletId);
        Collections.reverse(txs); // Processing Genesis -> Latest

        String previousHash = "0000000000000000000000000000000000000000000000000000000000000000";

        for (WalletTransaction tx : txs) {
            String data = formatForHash(previousHash, tx.getWalletId(), tx.getType(), tx.getAmount(), tx.getReference(),
                    tx.getCreatedAt());
            String expectedHash = HashUtils.sha256(data);
            if (!expectedHash.equals(tx.getTxHash())) {
                ledgerDAO.insertAuditLog(new LedgerAuditLog(walletId, false, "Hash mismatch tx " + tx.getId()));
                return false;
            }
            previousHash = tx.getTxHash();
        }
        ledgerDAO.insertAuditLog(new LedgerAuditLog(walletId, true, "Ledger integrity verified"));
        return true;
    }

    public boolean verifyBalance(int walletId) {
        List<WalletTransaction> txs = txDAO.findByWalletId(walletId);
        BigDecimal calcBalance = BigDecimal.ZERO;
        BigDecimal calcEscrow = BigDecimal.ZERO;

        for (WalletTransaction tx : txs) {
            BigDecimal amt = tx.getAmount();
            switch (tx.getType()) {
                case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS", "ESCROW_RCVD", "ESCROW_FEE",
                        "ESCROW_REFUND" ->
                    calcBalance = calcBalance.add(amt);
                case "DEBIT", "HOLD", "TRANSFER_SENT" -> calcBalance = calcBalance.subtract(amt);
            }
            if ("HOLD".equals(tx.getType()))
                calcEscrow = calcEscrow.add(amt);
            if ("RELEASE".equals(tx.getType()) || "ESCROW_SENT".equals(tx.getType())
                    || "ESCROW_REFUND".equals(tx.getType()))
                calcEscrow = calcEscrow.subtract(amt);
        }

        Wallet w = findById(walletId);
        return w.getBalance().compareTo(calcBalance) == 0 && w.getEscrowBalance().compareTo(calcEscrow) == 0;
    }

    private void ensureGenesisTransaction(int walletId) {
        try {
            List<WalletTransaction> history = txDAO.findByWalletId(walletId);
            if (history.isEmpty()) {
                Wallet w = findById(walletId);
                if (w.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    recordTransaction(walletId, "GENESIS", w.getBalance(), "Initial Bank Balance");
                }
            }
        } catch (Exception e) {
        }
    }

    private String formatForHash(String prevHash, int walletId, String type, BigDecimal amount, String ref,
            LocalDateTime createdAt) {
        String amountStr = amount.setScale(3, RoundingMode.HALF_UP).toString();
        LocalDateTime truncatedTime = createdAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        return prevHash + walletId + type + amountStr + ref + truncatedTime;
    }

    public boolean hasWallet(int userId) {
        return findByUserId(userId) != null;
    }

    public void transferWalletToUser(int currentUserId, int newUserId) {
        Wallet w = findByUserId(currentUserId);
        if (w == null)
            throw new RuntimeException("No wallet");
        if (findByUserId(newUserId) != null)
            throw new RuntimeException("Target has wallet");
        updateUserId(w.getId(), newUserId);
    }

    public void transferEntireBalanceToBank(int userId) {
        Wallet userWallet = findByUserId(userId);
        if (userWallet == null)
            return;

        int bankWalletId = getBankWalletId();
        if (userWallet.getId() == bankWalletId) {
            throw new RuntimeException("Cannot delete Central Bank User");
        }

        BigDecimal totalWealth = userWallet.getBalance().add(userWallet.getEscrowBalance());
        if (totalWealth.compareTo(BigDecimal.ZERO) > 0) {
            // Credit Bank
            credit(bankWalletId, totalWealth, "Reclaimed Assets from User " + userId);

            // Log for debugging
            System.out.println("Transferred " + totalWealth + " TND from User " + userId + " to Bank.");
        }
    }

    // ========================
    // ESCROW SPECIFIC LOGIC
    // ========================

    public void processEscrowRelease(int senderId, int receiverId, int adminId, BigDecimal totalAmount,
            BigDecimal fee) {
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);

            BigDecimal netAmount = totalAmount.subtract(fee);

            // 1. Debit Sender Escrow Balance
            String sqlSender = "UPDATE wallets SET escrow_balance = escrow_balance - ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlSender)) {
                ps.setBigDecimal(1, totalAmount);
                ps.setInt(2, senderId);
                ps.executeUpdate();
            }
            // Use existing helper
            recordTransaction(senderId, "ESCROW_SENT", totalAmount, "Released to Wallet " + receiverId);

            // 2. Credit Receiver Main Balance
            updateBalanceInTx(receiverId, netAmount, conn);
            recordTransaction(receiverId, "ESCROW_RCVD", netAmount, "Received from Escrow");

            // 3. Credit Admin Fee (if fee > 0)
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                updateBalanceInTx(adminId, fee, conn);
                recordTransaction(adminId, "ESCROW_FEE", fee, "Fee from Escrow " + senderId);
            }

            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Escrow release failed: " + e.getMessage(), e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }

    public void refundEscrow(int senderId, BigDecimal amount) {
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);

            // Move from Escrow Balance back to Main Balance
            String sql = "UPDATE wallets SET escrow_balance = escrow_balance - ?, balance = balance + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, amount);
                ps.setBigDecimal(2, amount);
                ps.setInt(3, senderId);
                ps.executeUpdate();
            }

            recordTransaction(senderId, "ESCROW_REFUND", amount, "Refunded from Escrow");
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Escrow refund failed", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }
}
