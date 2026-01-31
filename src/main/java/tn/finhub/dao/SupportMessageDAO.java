package tn.finhub.dao;

import tn.finhub.model.SupportMessage;
import tn.finhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupportMessageDAO {

    public void createMessage(SupportMessage message) {
        String sql = "INSERT INTO support_messages (ticket_id, sender_role, message) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, message.getTicketId());
            stmt.setString(2, message.getSenderRole());
            stmt.setString(3, message.getMessage());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating support message: " + e.getMessage());
        }
    }

    public List<SupportMessage> getMessagesByTicketId(int ticketId) {
        List<SupportMessage> messages = new ArrayList<>();
        String sql = "SELECT * FROM support_messages WHERE ticket_id = ? ORDER BY created_at ASC";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SupportMessage msg = new SupportMessage();
                    msg.setId(rs.getInt("id"));
                    msg.setTicketId(rs.getInt("ticket_id"));
                    msg.setSenderRole(rs.getString("sender_role"));
                    msg.setMessage(rs.getString("message"));
                    msg.setCreatedAt(rs.getTimestamp("created_at"));
                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
}
