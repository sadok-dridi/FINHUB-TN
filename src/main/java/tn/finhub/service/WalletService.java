package tn.finhub.service;

import tn.finhub.dao.WalletDAO;
import tn.finhub.dao.WalletTransactionDAO;
import tn.finhub.model.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;

public class WalletService {

    private WalletDAO walletDAO = new WalletDAO();
    private WalletTransactionDAO txDAO = new WalletTransactionDAO();
    private tn.finhub.dao.LedgerDAO ledgerDAO = new tn.finhub.dao.LedgerDAO();

    public void createWalletIfNotExists(int userId) {
        Wallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null) {
            walletDAO.createWallet(userId);
        }
    }

    // ✅ CREDIT
    public void credit(int walletId, BigDecimal amount, String ref) {
        checkStatus(walletId);
        executeAtomic(walletId, amount, "CREDIT", ref);
    }

    // ✅ DEBIT
    public void debit(int walletId, BigDecimal amount, String ref) {
        checkStatus(walletId);
        Wallet wallet = walletDAO.findById(walletId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        executeAtomic(walletId, amount.negate(), "DEBIT", ref);
    }

    // ✅ HOLD (ESCROW)
    public void hold(int walletId, BigDecimal amount, String ref) {
        checkStatus(walletId);
        Wallet wallet = walletDAO.findById(walletId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        walletDAO.hold(walletId, amount);
        recordTransaction(walletId, "HOLD", amount, ref);
    }

    // ✅ RELEASE
    public void release(int walletId, BigDecimal amount, String ref) {
        checkStatus(walletId);
        walletDAO.release(walletId, amount);
        recordTransaction(walletId, "RELEASE", amount, ref);
    }

    // 💸 TRANSFER (Wallet → Wallet)
    public void transfer(int fromWalletId, int toWalletId, BigDecimal amount, String ref) {
        // Legacy support: append ID for uniqueness if generic
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

        Wallet fromWallet = walletDAO.findById(fromWalletId);
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        Connection conn = walletDAO.getConnection();
        try {
            conn.setAutoCommit(false);

            // 1. Debit Sender
            walletDAO.updateBalance(fromWalletId, amount.negate());
            recordTransaction(fromWalletId, "TRANSFER_SENT", amount, senderRef);

            // 2. Credit Receiver
            walletDAO.updateBalance(toWalletId, amount);
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

    // 🔐 ATOMIC OPERATION
    private void executeAtomic(
            int walletId,
            BigDecimal amount,
            String type,
            String ref) {
        Connection conn = walletDAO.getConnection();

        try {
            conn.setAutoCommit(false);
            walletDAO.updateBalance(walletId, amount);
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

    public Wallet getWallet(int userId) {
        return walletDAO.findByUserId(userId);
    }

    public java.util.List<tn.finhub.model.WalletTransaction> getTransactionHistory(int walletId) {
        return txDAO.findByWalletId(walletId);
    }

    private void recordTransaction(int walletId, String type, BigDecimal amount, String ref) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String prevHash = getLastHash(walletId);

        String data = formatForHash(prevHash, walletId, type, amount, ref, now);

        String txHash = tn.finhub.util.HashUtils.sha256(data);
        txDAO.insert(walletId, type, amount, ref, prevHash, txHash, now);
    }

    private String getLastHash(int walletId) {
        java.util.List<tn.finhub.model.WalletTransaction> list = txDAO.findByWalletId(walletId);
        if (list.isEmpty()) {
            return "0000000000000000000000000000000000000000000000000000000000000000";
        }
        return list.get(0).getTxHash();
    }

    // 🛡️ LEDGER VERIFICATION
    public boolean verifyLedger(int walletId) {
        java.util.List<tn.finhub.model.WalletTransaction> txs = txDAO.findByWalletId(walletId);

        // Ensure we process from Genesis to Latest
        // Assuming findByWalletId returns [Latest ... Genesis] (DESC), we reverse to
        // [Genesis ... Latest]
        // If it returns [Genesis ... Latest] (ASC), we shouldn't reverse.
        // Based on existing code iterating with "000...000" start, we need Genesis
        // first.
        // Existing code: Collections.reverse(txs).
        java.util.Collections.reverse(txs);

        String previousHash = "0000000000000000000000000000000000000000000000000000000000000000";

        for (tn.finhub.model.WalletTransaction tx : txs) {
            String data = formatForHash(
                    previousHash,
                    tx.getWalletId(),
                    tx.getType(),
                    tx.getAmount(),
                    tx.getReference(),
                    tx.getCreatedAt());
            String expectedHash = tn.finhub.util.HashUtils.sha256(data);

            if (!expectedHash.equals(tx.getTxHash())) {
                String msg = "Hash mismatch detected at transaction " + tx.getId();
                System.err.println("❌ " + msg);
                System.err.println("Expected: " + expectedHash);
                System.err.println("Actual:   " + tx.getTxHash());

                // Log failure
                ledgerDAO.insertAuditLog(new tn.finhub.model.LedgerAuditLog(walletId, false, msg));
                return false;
            }
            previousHash = tx.getTxHash();
        }

        // Log success
        ledgerDAO.insertAuditLog(new tn.finhub.model.LedgerAuditLog(walletId, true, "Ledger integrity verified"));
        return true;
    }

    public int getTamperedTransactionId(int walletId) {
        java.util.List<tn.finhub.model.WalletTransaction> txs = txDAO.findByWalletId(walletId);
        java.util.Collections.reverse(txs);

        String previousHash = "0000000000000000000000000000000000000000000000000000000000000000";

        for (tn.finhub.model.WalletTransaction tx : txs) {
            String data = formatForHash(
                    previousHash,
                    tx.getWalletId(),
                    tx.getType(),
                    tx.getAmount(),
                    tx.getReference(),
                    tx.getCreatedAt());
            String expectedHash = tn.finhub.util.HashUtils.sha256(data);

            if (!expectedHash.equals(tx.getTxHash())) {
                return tx.getId();
            }
            previousHash = tx.getTxHash();
        }
        return -1;
    }

    // ⚖️ BALANCE VERIFICATION
    public boolean verifyBalance(int walletId) {
        java.util.List<tn.finhub.model.WalletTransaction> txs = txDAO.findByWalletId(walletId);

        BigDecimal calculatedBalance = BigDecimal.ZERO;
        BigDecimal calculatedEscrow = BigDecimal.ZERO;

        for (tn.finhub.model.WalletTransaction tx : txs) {
            BigDecimal amt = tx.getAmount();
            switch (tx.getType()) {
                case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS" ->
                    calculatedBalance = calculatedBalance.add(amt);
                case "DEBIT", "HOLD", "TRANSFER_SENT" -> calculatedBalance = calculatedBalance.subtract(amt);
            }

            if ("HOLD".equals(tx.getType()))
                calculatedEscrow = calculatedEscrow.add(amt);
            if ("RELEASE".equals(tx.getType()))
                calculatedEscrow = calculatedEscrow.subtract(amt);
        }

        Wallet wallet = walletDAO.findById(walletId);

        // Use compareTo for BigDecimal equality to ignore scale differences
        boolean balanceMatch = wallet.getBalance().compareTo(calculatedBalance) == 0;
        boolean escrowMatch = wallet.getEscrowBalance().compareTo(calculatedEscrow) == 0;

        if (!balanceMatch || !escrowMatch) {
            System.err.println("❌ Balance Mismatch!");
            System.err.println("DB Balance: " + wallet.getBalance() + " | Calc: " + calculatedBalance);
            System.err.println("DB Escrow: " + wallet.getEscrowBalance() + " | Calc: " + calculatedEscrow);
            return false;
        }
        return true;
    }

    private String formatForHash(String prevHash, int walletId, String type, BigDecimal amount, String ref,
            java.time.LocalDateTime createdAt) {
        // Enforce 3 decimal places for consistency with DB DECIMAL(15,3)
        String amountStr = amount.setScale(3, java.math.RoundingMode.HALF_UP).toString();
        // Truncate timestamp to seconds to avoid nanosecond mismatches
        java.time.LocalDateTime truncatedTime = createdAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        return prevHash + walletId + type + amountStr + ref + truncatedTime;
    }

    // 🧊 FREEZE WALLET
    public void freezeWallet(int walletId) {
        // EXEMPTION: Never freeze the Central Bank Wallet
        try {
            if (walletId == getBankWalletId()) {
                System.err.println("⚠️ Attempted to freeze Central Bank Wallet (ID " + walletId + "). Action blocked.");
                return;
            }
        } catch (Exception ignored) {
        }

        walletDAO.updateStatus(walletId, "FROZEN");
    }

    public void unfreezeWallet(int walletId) {
        // Only ACTIVE is supported as alternative for now, assuming unfreeze -> active
        walletDAO.updateStatus(walletId, "ACTIVE");
    }

    public boolean isFrozen(int walletId) {
        Wallet w = walletDAO.findById(walletId);
        return "FROZEN".equals(w.getStatus());
    }

    public java.util.List<tn.finhub.model.LedgerAuditLog> getAuditLogs(int walletId) {
        return ledgerDAO.getAuditLogs(walletId);
    }

    public tn.finhub.model.LedgerFlag getLatestFlag(int walletId) {
        return ledgerDAO.getLatestFlag(walletId);
    }

    private void checkStatus(int walletId) {
        // EXEMPTION: Do not verify the Central Bank Wallet (ID 999999 or lookup)
        // This is necessary because it was created with an artificial balance.
        try {
            if (walletId == getBankWalletId()) {
                // Auto-Repair 1: If Bank Wallet is somehow FROZEN, unfreeze it immediately.
                if (isFrozen(walletId)) {
                    System.out.println("🔧 Auto-Repairing Central Bank Wallet: Unfreezing...");
                    unfreezeWallet(walletId);
                }

                // Auto-Repair 2: Ensure Genesis Transaction exists to fix balance mismatch
                ensureGenesisTransaction(walletId);

                return;
            }
        } catch (Exception ignored) {
            // If bank wallet isn't configured, proceed with normal checks
        }

        // Check Ledger Flags (Enforcement)
        if (ledgerDAO.hasActiveFlags(walletId)) {
            throw new RuntimeException("Wallet is frozen due to integrity violation");
        }

        if (isFrozen(walletId)) {
            throw new RuntimeException("Wallet is FROZEN. Contact support.");
        }

        if (!verifyLedger(walletId)) {
            String reason = "Ledger integrity violation – wallet frozen";
            ledgerDAO.insertFlag(new tn.finhub.model.LedgerFlag(walletId, reason));
            freezeWallet(walletId);
            throw new RuntimeException("Security Alert: " + reason);
        }

        if (!verifyBalance(walletId)) {
            String reason = "Balance mismatch detected – wallet frozen";
            ledgerDAO.insertFlag(new tn.finhub.model.LedgerFlag(walletId, reason));
            freezeWallet(walletId);
            throw new RuntimeException("Security Alert: " + reason);
        }
    }

    public boolean hasWallet(int userId) {
        return walletDAO.findByUserId(userId) != null;
    }

    public void transferWalletToUser(int currentUserId, int newUserId) {
        Wallet wallet = walletDAO.findByUserId(currentUserId);
        if (wallet == null) {
            throw new RuntimeException("No wallet found to transfer.");
        }

        // Ensure new user doesn't already have one
        if (walletDAO.findByUserId(newUserId) != null) {
            throw new RuntimeException("Target user already has a wallet.");
        }

        // Transfer
        walletDAO.updateUserId(wallet.getId(), newUserId);
    }

    public void transferByEmail(int senderWalletId, String recipientEmail, BigDecimal amount) {
        UserService userService = new UserService();
        tn.finhub.model.User recipient = userService.getUserByEmail(recipientEmail);

        if (recipient == null) {
            throw new RuntimeException("User with email " + recipientEmail + " not found.");
        }

        Wallet recipientWallet = walletDAO.findByUserId(recipient.getId());
        if (recipientWallet == null) {
            throw new RuntimeException("Recipient does not have an active wallet.");
        }

        if ("FROZEN".equals(recipientWallet.getStatus())) {
            throw new RuntimeException("Recipient wallet is frozen. Cannot receive funds.");
        }

        // Fetch Sender Name for nicer reference
        String senderName = "Unknown";
        Wallet senderWallet = walletDAO.findById(senderWalletId);
        if (senderWallet != null) {
            tn.finhub.model.User sender = userService.getUserById(senderWallet.getUserId());
            if (sender != null)
                senderName = sender.getFullName();
        }

        // Use Names in Reference
        String senderRef = "Transfer to " + recipient.getFullName();
        String receiverRef = "Transfer from " + senderName;

        transferInternal(senderWalletId, recipientWallet.getId(), amount, senderRef, receiverRef);
    }

    public int getBankWalletId() {
        UserService userService = new UserService();
        tn.finhub.model.User bankUser = userService.getUserByEmail("bank@finhub.tn");
        if (bankUser == null) {
            throw new RuntimeException("Bank configuration missing: 'bank@finhub.tn' user not found.");
        }
        Wallet bankWallet = walletDAO.findByUserId(bankUser.getId());
        if (bankWallet == null) {
            throw new RuntimeException("Bank configuration missing: Bank wallet not found.");
        }
        return bankWallet.getId();
    }

    private void ensureGenesisTransaction(int walletId) {
        try {
            java.util.List<tn.finhub.model.WalletTransaction> history = txDAO.findByWalletId(walletId);
            if (history.isEmpty()) {
                Wallet w = walletDAO.findById(walletId);
                if (w.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    System.out.println("🔧 Injecting Genesis Transaction for Wallet " + walletId);
                    recordTransaction(walletId, "GENESIS", w.getBalance(), "Initial Bank Balance");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to ensure genesis transaction: " + e.getMessage());
        }
    }

    // 🔧 REPAIR TRANSACTION (Data Restoration)
    public void repairTransaction(int walletId, int txId, BigDecimal newAmount, String newRef) {
        // Fetch the specific transaction to get its existing hashes
        // We use the DAO's list method for now as we don't have findById
        java.util.List<tn.finhub.model.WalletTransaction> txs = txDAO.findByWalletId(walletId);
        tn.finhub.model.WalletTransaction targetTx = null;

        for (tn.finhub.model.WalletTransaction tx : txs) {
            if (tx.getId() == txId) {
                targetTx = tx;
                break;
            }
        }

        if (targetTx == null) {
            throw new RuntimeException("Transaction ID " + txId + " not found in wallet " + walletId);
        }

        // Update ONLY the data fields (Amount, Reference)
        // We preserve the existing Hashes (TxHash, PrevHash) exactly as they are.
        // The goal is to restore the data so it matches these hashes again.
        txDAO.update(
                targetTx.getId(),
                newAmount,
                targetTx.getType(),
                newRef,
                targetTx.getTxHash(), // Keep existing hash
                targetTx.getPrevHash() // Keep existing prev hash
        );

        // Verify if this fix restored integrity
        tryResolveFlags(walletId);
    }

    // 🕵️ AUDIT HEURISTIC: Calculate expected value based on balance discrepancy
    public BigDecimal calculateDiscrepancy(int walletId, int txId, BigDecimal currentWrongAmount, String type) {
        Wallet wallet = walletDAO.findById(walletId);
        java.util.List<tn.finhub.model.WalletTransaction> txs = txDAO.findByWalletId(walletId);

        BigDecimal calculatedBalance = BigDecimal.ZERO;

        // 1. Calculate sum of all transactions as they currently are (including the
        // error)
        for (tn.finhub.model.WalletTransaction tx : txs) {
            BigDecimal amt = tx.getAmount();
            switch (tx.getType()) {
                case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS" ->
                    calculatedBalance = calculatedBalance.add(amt);
                case "DEBIT", "HOLD", "TRANSFER_SENT" ->
                    calculatedBalance = calculatedBalance.subtract(amt);
            }
        }

        // 2. The difference between Calc and Real Balance is the error magnitude
        // Error = Calc - Real
        // If Error is Positive, the Ledger thinks we have MORE than we do -> One
        // 'Credit' is too high or 'Debit' too low
        BigDecimal error = calculatedBalance.subtract(wallet.getBalance());

        // 3. Apply error correction to the specific transaction type
        // If type adds to balance (Credit), we subtract the error to fix it.
        // If type subtracts from balance (Debit), we add the error to fix it.
        switch (type) {
            case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS":
                return currentWrongAmount.subtract(error);
            case "DEBIT", "HOLD", "TRANSFER_SENT":
                return currentWrongAmount.add(error); // (Debit is negative impact, so adding error reduces magnitude)
            default:
                return BigDecimal.ZERO;
        }
    }

    public void tryResolveFlags(int walletId) {
        if (verifyLedger(walletId) && verifyBalance(walletId)) {
            ledgerDAO.deleteFlagsByWalletId(walletId);
            unfreezeWallet(walletId);
            System.out.println("✅ Wallet " + walletId + " repaired and unfrozen.");
        } else {
            System.err.println("⚠️ Repair attempted but validation still failed for Wallet " + walletId);
        }
    }
}
