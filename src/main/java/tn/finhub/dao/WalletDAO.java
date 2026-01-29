package tn.finhub.dao;

import tn.finhub.model.Wallet;
import tn.finhub.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;

public class WalletDAO {

    public Connection getConnection() {
        return DBConnection.getInstance();
    }

    // CREATE WALLET
    public void createWallet(int userId) {
        String sql = """
                    INSERT INTO wallets (user_id, currency, balance, escrow_balance)
                    VALUES (?, 'TND', 0, 0)
                """;

        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Wallet creation failed", e);
        }
    }

    // FIND BY ID
    public Wallet findById(int walletId) {
        String sql = "SELECT * FROM wallets WHERE id = ?";

        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
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

    // UPDATE STATUS
    public void updateStatus(int walletId, String status) {
        String sql = "UPDATE wallets SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update status failed", e);
        }
    }

    // UPDATE BALANCE (CREDIT / DEBIT)
    public void updateBalance(int walletId, BigDecimal amount) throws SQLException {
        String sql = """
                    UPDATE wallets
                    SET balance = balance + ?
                    WHERE id = ?
                """;

        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, walletId);
            ps.executeUpdate();
        }
    }

    // HOLD (MOVE TO ESCROW)
    public void hold(int walletId, BigDecimal amount) {
        String sql = """
                    UPDATE wallets
                    SET balance = balance - ?, escrow_balance = escrow_balance + ?
                    WHERE id = ?
                """;

        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setInt(3, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Escrow hold failed", e);
        }
    }

    // RELEASE ESCROW
    public void release(int walletId, BigDecimal amount) {
        String sql = """
                    UPDATE wallets
                    SET escrow_balance = escrow_balance - ?, balance = balance + ?
                    WHERE id = ?
                """;

        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setInt(3, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Escrow release failed", e);
        }
    }

    public Wallet findByUserId(int userId) {
        String sql = "SELECT * FROM wallets WHERE user_id = ?";

        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
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

    public void deleteById(int walletId) {
        String sql = "DELETE FROM wallets WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting wallet", e);
        }
    }

    public void deleteAll() {
        String sql = "DELETE FROM wallets";
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting all wallets", e);
        }
    }
}
