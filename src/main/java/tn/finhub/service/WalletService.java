package tn.finhub.service;

import tn.finhub.dao.WalletDAO;
import tn.finhub.dao.WalletTransactionDAO;
import tn.finhub.model.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;

public class WalletService {

    private WalletDAO walletDAO = new WalletDAO();
    private WalletTransactionDAO txDAO = new WalletTransactionDAO();

    public void createWalletIfNotExists(int userId) {
        Wallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null) {
            walletDAO.createWallet(userId);
        }
    }

    // ✅ CREDIT
    public void credit(int walletId, BigDecimal amount, String ref) {
        executeAtomic(walletId, amount, "CREDIT", ref);
    }

    // ✅ DEBIT
    public void debit(int walletId, BigDecimal amount, String ref) {
        Wallet wallet = walletDAO.findById(walletId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        executeAtomic(walletId, amount.negate(), "DEBIT", ref);
    }

    // ✅ HOLD (ESCROW)
    public void hold(int walletId, BigDecimal amount, String ref) {
        Wallet wallet = walletDAO.findById(walletId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        walletDAO.hold(walletId, amount);
        txDAO.insert(walletId, "HOLD", amount, ref);
    }

    // ✅ RELEASE
    public void release(int walletId, BigDecimal amount, String ref) {
        walletDAO.release(walletId, amount);
        txDAO.insert(walletId, "RELEASE", amount, ref);
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
            txDAO.insert(walletId, type, amount.abs(), ref);
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
}
