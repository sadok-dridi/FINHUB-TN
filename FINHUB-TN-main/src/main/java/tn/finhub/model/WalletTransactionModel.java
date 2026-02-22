package tn.finhub.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class WalletTransactionModel {

    public void insert(int walletId, String type,
            BigDecimal amount, String reference, String prevHash, String txHash, java.time.LocalDateTime createdAt) {

        String sql = """
                    INSERT INTO wallet_transactions
                    (wallet_id, type, amount, reference, prev_hash, tx_hash, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.setString(4, reference);
            ps.setString(5, prevHash);
            ps.setString(6, txHash);
            ps.setTimestamp(7, Timestamp.valueOf(createdAt));
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Transaction insert failed", e);
        }
    }

    public java.util.List<tn.finhub.model.WalletTransaction> findByWalletId(int walletId) {
        java.util.List<tn.finhub.model.WalletTransaction> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM wallet_transactions WHERE wallet_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tn.finhub.model.WalletTransaction tx = new tn.finhub.model.WalletTransaction(
                            rs.getInt("id"),
                            rs.getInt("wallet_id"),
                            rs.getString("type"),
                            rs.getBigDecimal("amount"),
                            rs.getString("reference"),
                            rs.getString("prev_hash"),
                            rs.getString("tx_hash"),
                            rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(tx);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching transactions", e);
        }
        return list;
    }

    public void deleteByWalletId(int walletId) {
        String sql = "DELETE FROM wallet_transactions WHERE wallet_id = ?";
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting transactions", e);
        }
    }

    public void deleteAll() {
        String sql = "DELETE FROM wallet_transactions";
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting all transactions", e);
        }
    }

    public tn.finhub.model.WalletTransaction getLastTransaction(int walletId) {
        String sql = "SELECT * FROM wallet_transactions WHERE wallet_id = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new tn.finhub.model.WalletTransaction(
                            rs.getInt("id"),
                            rs.getInt("wallet_id"),
                            rs.getString("type"),
                            rs.getBigDecimal("amount"),
                            rs.getString("reference"),
                            rs.getString("prev_hash"),
                            rs.getString("tx_hash"),
                            rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching last transaction", e);
        }
        return null; // Genesis
    }

    public void update(int txId, BigDecimal amount, String type, String reference, String txHash, String prevHash) {
        String sql = "UPDATE wallet_transactions SET amount = ?, type = ?, reference = ?, tx_hash = ?, prev_hash = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, type);
            ps.setString(3, reference);
            ps.setString(4, txHash);
            ps.setString(5, prevHash);
            ps.setInt(6, txId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating transaction", e);
        }
    }
}
