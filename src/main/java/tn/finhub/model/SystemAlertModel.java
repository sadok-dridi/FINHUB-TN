package tn.finhub.model;

import tn.finhub.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SystemAlertModel {

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    public void createAlert(SystemAlert alert) {
        String sql = "INSERT INTO system_alerts (user_id, severity, message, source) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, alert.getUserId());
            stmt.setString(2, alert.getSeverity());
            stmt.setString(3, alert.getMessage());
            stmt.setString(4, alert.getSource());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating system alert: " + e.getMessage());
        }
    }

    public void createAlert(int userId, String severity, String message, String source) {
        createAlert(new SystemAlert(userId, severity, message, source));
    }

    public List<SystemAlert> getAlertsByUserId(int userId) {
        List<SystemAlert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM system_alerts WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SystemAlert alert = new SystemAlert();
                    alert.setId(rs.getInt("id"));
                    alert.setUserId(rs.getInt("user_id"));
                    alert.setSeverity(rs.getString("severity"));
                    alert.setMessage(rs.getString("message"));
                    alert.setSource(rs.getString("source"));
                    alert.setCreatedAt(rs.getTimestamp("created_at"));
                    alerts.add(alert);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    public void broadcastAlert(String severity, String message, String source) {
        UserModel userModel = new UserModel();
        List<tn.finhub.model.User> allUsers = userModel.findAll();

        String sql = "INSERT INTO system_alerts (user_id, severity, message, source) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            for (tn.finhub.model.User user : allUsers) {
                stmt.setInt(1, user.getId());
                stmt.setString(2, severity);
                stmt.setString(3, message);
                stmt.setString(4, source);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error broadcasting alert: " + e.getMessage());
        }
    }
}
