package tn.finhub.model;

import tn.finhub.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupportModel {

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    // ==========================================
    // TICKET MANAGEMENT
    // ==========================================

    public void createTicket(SupportTicket ticket) {
        String sql = "INSERT INTO support_tickets (user_id, subject, category, status, priority) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, ticket.getUserId());
            stmt.setString(2, ticket.getSubject());
            stmt.setString(3, ticket.getCategory());
            stmt.setString(4, ticket.getStatus());
            stmt.setString(5, ticket.getPriority());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    ticket.setId(generatedKeys.getInt(1));
                }
            }

            // Add initial message automatically if needed, but usually handled by
            // controller or service logic.
            // In strict MVC, controller calls createTicket then createMessage.
            // Or we can have a helper method here.
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating support ticket: " + e.getMessage());
        }
    }

    public void createTicketWithInitialMessage(int userId, String subject, String category, String initialMessage) {
        SupportTicket ticket = new SupportTicket(userId, subject, category, "NORMAL");
        createTicket(ticket);
        createMessage(new SupportMessage(ticket.getId(), "USER", initialMessage));
    }

    public List<SupportTicket> getTicketsByUserId(int userId) {
        List<SupportTicket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM support_tickets WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapResultSetToTicket(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }

    public SupportTicket getTicketById(int ticketId) {
        String sql = "SELECT * FROM support_tickets WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTicket(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateTicketStatus(int ticketId, String status) {
        String sql = "UPDATE support_tickets SET status = ?, resolved_at = ? WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
                stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            } else {
                stmt.setTimestamp(2, null);
            }
            stmt.setInt(3, ticketId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error updating ticket status: " + e.getMessage());
        }
    }

    public void resolveTicket(int ticketId) {
        updateTicketStatus(ticketId, "RESOLVED");
        createMessage(new SupportMessage(ticketId, "SYSTEM", "Ticket marked as resolved by user."));
    }

    private SupportTicket mapResultSetToTicket(ResultSet rs) throws SQLException {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(rs.getInt("id"));
        ticket.setUserId(rs.getInt("user_id"));
        ticket.setSubject(rs.getString("subject"));
        ticket.setCategory(rs.getString("category"));
        ticket.setStatus(rs.getString("status"));
        ticket.setPriority(rs.getString("priority"));
        ticket.setCreatedAt(rs.getTimestamp("created_at"));
        ticket.setResolvedAt(rs.getTimestamp("resolved_at"));
        return ticket;
    }

    // ==========================================
    // MESSAGE MANAGEMENT
    // ==========================================

    public void createMessage(SupportMessage message) {
        String sql = "INSERT INTO support_messages (ticket_id, sender_role, message) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    public List<SupportMessage> getTicketMessages(int ticketId) {
        List<SupportMessage> messages = new ArrayList<>();
        String sql = "SELECT * FROM support_messages WHERE ticket_id = ? ORDER BY created_at ASC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
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

    public void addUserMessage(int ticketId, String content) {
        createMessage(new SupportMessage(ticketId, "USER", content));
    }

    public void addSystemMessage(int ticketId, String content) {
        createMessage(new SupportMessage(ticketId, "SYSTEM", content));
    }
}
