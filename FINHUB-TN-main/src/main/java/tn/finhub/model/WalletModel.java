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

        String prevHash = "0000000000000000000000000000000000000000000000000000000000000000";
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
        Wallet wallet = findById(walletId);
        if (wallet == null)
            return BigDecimal.ZERO;

        BigDecimal actualWalletBalance = wallet.getBalance();
        List<WalletTransaction> transactions = txDAO.findByWalletId(walletId);
        BigDecimal sumOthers = BigDecimal.ZERO;

        for (WalletTransaction tx : transactions) {
            if (tx.getId() == txId)
                continue; // Skip the target transaction being repaired

            BigDecimal amt = tx.getAmount();
            switch (tx.getType()) {
                case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS", "ESCROW_RCVD", "ESCROW_FEE",
                        "ESCROW_REFUND" ->
                    sumOthers = sumOthers.add(amt);
                case "DEBIT", "HOLD", "TRANSFER_SENT" -> sumOthers = sumOthers.subtract(amt);
            }
        }

        // Equation: Balance = Sum_Others + Target_Impact
        // Therefore: Target_Impact = Balance - Sum_Others
        BigDecimal targetImpact = actualWalletBalance.subtract(sumOthers);

        // Convert Impact to Absolute Amount based on Type
        // If Type is DEBIT (adds negative impact), then Amount = -Target_Impact
        // If Type is CREDIT (adds positive impact), then Amount = Target_Impact
        switch (type) {
            case "DEBIT", "HOLD", "TRANSFER_SENT" -> {
                return targetImpact.negate();
            }
            default -> {
                return targetImpact;
            }
        }
    }

    public void repairTransaction(int walletId, int txId, BigDecimal newAmount, String newRef) {
        List<WalletTransaction> transactions = txDAO.findByWalletId(walletId);
        java.util.Collections.reverse(transactions); // ASC order

        // Use the standard Genesis hash
        String prevHash = "0000000000000000000000000000000000000000000000000000000000000000";

        for (WalletTransaction tx : transactions) {
            if (tx.getId() == txId) {
                // Calculate what the hash WOULD be with the new values
                String calculatedHash = HashUtils.generateTransactionHash(
                        tx.getId(), tx.getWalletId(), tx.getType(), newAmount,
                        newRef, tx.getCreatedAt(), prevHash);

                // Compare with the IMMUTABLE stored hash
                if (!calculatedHash.equals(tx.getTxHash())) {
                    throw new RuntimeException(
                            "Invalid repair data: Hash mismatch. You must restore the exact original values.");
                }

                // If valid, update the record
                // We only update Amount and Ref. The Hash remains the same (because it
                // matches!).
                txDAO.update(tx.getId(), newAmount, tx.getType(), newRef, tx.getTxHash(), prevHash);

                // Recalculate Wallet Balance
                recalculateWalletBalance(walletId);

                // Automatically unfreeze and restore status
                unfreezeWallet(walletId);
                return;
            }
            prevHash = tx.getTxHash();
        }
        throw new RuntimeException("Transaction not found.");
    }

    public void recalculateWalletBalance(int walletId) {
        List<WalletTransaction> transactions = txDAO.findByWalletId(walletId);
        BigDecimal newBalance = BigDecimal.ZERO;
        for (WalletTransaction tx : transactions) {
            BigDecimal amt = tx.getAmount();
            switch (tx.getType()) {
                case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS", "ESCROW_RCVD", "ESCROW_FEE",
                        "ESCROW_REFUND" ->
                    newBalance = newBalance.add(amt);
                case "DEBIT", "HOLD", "TRANSFER_SENT" -> newBalance = newBalance.subtract(amt);
            }
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
                int rows = ps.executeUpdate();
                if (rows == 0)
                    throw new RuntimeException("Wallet " + walletId + " not found or hold update failed");
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
                int rows = ps.executeUpdate();
                if (rows == 0)
                    throw new RuntimeException("Wallet " + walletId + " not found or release update failed");
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

        // Fetch Sender
        User sender = null;
        String senderName = "Unknown";

        Wallet senderWallet = findById(senderWalletId);
        if (senderWallet != null) {
            sender = userModel.findById(senderWallet.getUserId());
            if (sender != null) {
                senderName = sender.getFullName();
            }
        }

        // Format: "Transfer to [Name] (Wallet [ID])"
        // This preserves the Name for display but keeps the ID for robust photo lookup
        String senderRef = "Transfer to " + recipient.getFullName() + " (Wallet " + recipientWallet.getId() + ")";
        String receiverRef = "Transfer from " + senderName + " (Wallet " + senderWalletId + ")";

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
            int rows = ps.executeUpdate();
            if (rows == 0)
                throw new SQLException("Wallet " + walletId + " not found or update failed");
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
                System.out.println("[DEBUG] TAMPERED TX DETECTED: ID=" + tx.getId() + " Expected=" + expectedHash
                        + " Actual=" + tx.getTxHash());

                BigDecimal debugCalcBalance = BigDecimal.ZERO;
                for (WalletTransaction t : txs) {
                    BigDecimal amt = t.getAmount();
                    switch (t.getType()) {
                        case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS", "ESCROW_RCVD", "ESCROW_FEE",
                                "ESCROW_REFUND" ->
                            debugCalcBalance = debugCalcBalance.add(amt);
                        case "DEBIT", "HOLD", "TRANSFER_SENT" -> debugCalcBalance = debugCalcBalance.subtract(amt);
                    }
                }
                System.out.println("[DEBUG] Total Calculated Balance from Transactions: " + debugCalcBalance);

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
        System.out.println("[DEBUG] Wallet " + walletId + " - Calculated Balance: " + calcBalance
                + " | Actual Balance: " + w.getBalance());
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
                int rows = ps.executeUpdate();
                if (rows == 0)
                    throw new RuntimeException("Sender wallet " + senderId + " not found or update failed");
            }
            // Use existing helper
            recordTransaction(senderId, "ESCROW_SENT", totalAmount, "Released to Wallet " + receiverId);

            // 2. Credit Receiver Main Balance
            updateBalanceInTx(receiverId, netAmount, conn);
            recordTransaction(receiverId, "ESCROW_RCVD", netAmount, "Received from Escrow Wallet " + senderId);

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
                int rows = ps.executeUpdate();
                if (rows == 0)
                    throw new RuntimeException("Sender wallet " + senderId + " not found or update failed");
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

    // ========================
    // LOOKUP HELPERS
    // ========================

    public java.util.Map<Integer, String> findProfilePhotosByWalletIds(java.util.Set<Integer> walletIds) {
        java.util.Map<Integer, String> photoMap = new java.util.HashMap<>();
        if (walletIds == null || walletIds.isEmpty()) {
            return photoMap;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT w.id as wallet_id, u.profile_photo_url FROM wallets w JOIN users_local u ON w.user_id = u.user_id WHERE w.id IN (");
        for (int i = 0; i < walletIds.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            int index = 1;
            for (Integer id : walletIds) {
                ps.setInt(index++, id);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int wId = rs.getInt("wallet_id");
                String photoUrl = rs.getString("profile_photo_url");
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    photoMap.put(wId, photoUrl);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return photoMap;
    }

    public java.util.Map<Integer, String> findOwnerNamesByWalletIds(java.util.Set<Integer> walletIds) {
        java.util.Map<Integer, String> nameMap = new java.util.HashMap<>();
        if (walletIds == null || walletIds.isEmpty()) {
            return nameMap;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT w.id as wallet_id, u.full_name FROM wallets w JOIN users_local u ON w.user_id = u.user_id WHERE w.id IN (");
        for (int i = 0; i < walletIds.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            int index = 1;
            for (Integer id : walletIds) {
                ps.setInt(index++, id);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int wId = rs.getInt("wallet_id");
                String fullName = rs.getString("full_name");
                if (fullName != null && !fullName.isEmpty()) {
                    nameMap.put(wId, fullName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nameMap;
    }
}
