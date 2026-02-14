package tn.finhub.model;

import tn.finhub.util.DBConnection;

import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LedgerManager {

    public void insertAuditLog(LedgerAuditLog log) {
        String sql = "INSERT INTO ledger_audit_log (wallet_id, verified, checked_at, message) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, log.getWalletId());
            stmt.setBoolean(2, log.isVerified());
            stmt.setTimestamp(3, Timestamp.valueOf(log.getCheckedAt()));
            stmt.setString(4, log.getMessage());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error inserting audit log: " + e.getMessage());
        }
    }

    public void insertFlag(LedgerFlag flag) {
        String sql = "INSERT INTO ledger_flags (wallet_id, reason, flagged_at) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, flag.getWalletId());
            stmt.setString(2, flag.getReason());
            stmt.setTimestamp(3, Timestamp.valueOf(flag.getFlaggedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error inserting ledger flag: " + e.getMessage());
        }
    }

    public boolean hasActiveFlags(int walletId) {
        String sql = "SELECT count(*) FROM ledger_flags WHERE wallet_id = ?";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, walletId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<LedgerAuditLog> getAuditLogs(int walletId) {
        List<LedgerAuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM ledger_audit_log WHERE wallet_id = ? ORDER BY checked_at DESC";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, walletId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(new LedgerAuditLog(
                            rs.getInt("id"),
                            rs.getInt("wallet_id"),
                            rs.getBoolean("verified"),
                            rs.getTimestamp("checked_at").toLocalDateTime(),
                            rs.getString("message")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public LedgerFlag getLatestFlag(int walletId) {
        String sql = "SELECT * FROM ledger_flags WHERE wallet_id = ? ORDER BY flagged_at DESC LIMIT 1";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, walletId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new LedgerFlag(
                            rs.getInt("id"),
                            rs.getInt("wallet_id"),
                            rs.getString("reason"),
                            rs.getTimestamp("flagged_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteLogsByWalletId(int walletId) {
        String sql = "DELETE FROM ledger_audit_log WHERE wallet_id = ?";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, walletId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting audit logs: " + e.getMessage());
        }
    }

    public void deleteFlagsByWalletId(int walletId) {
        String sql = "DELETE FROM ledger_flags WHERE wallet_id = ?";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, walletId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting flags: " + e.getMessage());
        }
    }

    public void deleteAllLogs() {
        String sql = "DELETE FROM ledger_audit_log";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting all audit logs: " + e.getMessage());
        }
    }

    public void deleteAllFlags() {
        String sql = "DELETE FROM ledger_flags";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting all flags: " + e.getMessage());
        }
    }
}
