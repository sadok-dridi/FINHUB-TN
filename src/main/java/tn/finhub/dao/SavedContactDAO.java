package tn.finhub.dao;

import tn.finhub.model.SavedContact;
import tn.finhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavedContactDAO {

    public void addContact(SavedContact contact) {
        String sql = "INSERT INTO saved_contacts (user_id, contact_email, contact_name) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getInstance();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, contact.getUserId());
            pstmt.setString(2, contact.getContactEmail());
            pstmt.setString(3, contact.getContactName());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error adding contact: " + e.getMessage());
        }
    }

    public List<SavedContact> getContactsByUserId(int userId) {
        List<SavedContact> contacts = new ArrayList<>();
        String sql = "SELECT * FROM saved_contacts WHERE user_id = ? ORDER BY contact_name ASC";

        try (Connection conn = DBConnection.getInstance();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SavedContact contact = new SavedContact();
                    contact.setId(rs.getInt("id"));
                    contact.setUserId(rs.getInt("user_id"));
                    contact.setContactEmail(rs.getString("contact_email"));
                    contact.setContactName(rs.getString("contact_name"));
                    contact.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    contacts.add(contact);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    public void deleteContact(int contactId) {
        String sql = "DELETE FROM saved_contacts WHERE id = ?";
        try (Connection conn = DBConnection.getInstance();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, contactId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting contact: " + e.getMessage());
        }
    }

    public boolean exists(int userId, String email) {
        String sql = "SELECT COUNT(*) FROM saved_contacts WHERE user_id = ? AND contact_email = ?";
        try (Connection conn = DBConnection.getInstance();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
