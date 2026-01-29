package tn.finhub.dao;

import tn.finhub.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class WalletTransactionDAO {

    private Connection connection = DBConnection.getInstance();

    public void insert(int walletId, String type,
            BigDecimal amount, String reference) {

        String sql = """
                    INSERT INTO wallet_transactions
                    (wallet_id, type, amount, reference)
                    VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.setString(4, reference);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Transaction insert failed", e);
        }
    }

    public java.util.List<tn.finhub.model.WalletTransaction> findByWalletId(int walletId) {
        java.util.List<tn.finhub.model.WalletTransaction> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM wallet_transactions WHERE wallet_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tn.finhub.model.WalletTransaction tx = new tn.finhub.model.WalletTransaction(
                            rs.getInt("id"),
                            rs.getInt("wallet_id"),
                            rs.getString("type"),
                            rs.getBigDecimal("amount"),
                            rs.getString("reference"),
                            rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(tx);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching transactions", e);
        }
        return list;
    }
}
