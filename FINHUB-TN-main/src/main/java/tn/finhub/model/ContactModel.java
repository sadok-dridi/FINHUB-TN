package tn.finhub.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tn.finhub.util.DBConnection;

public class ContactModel {

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    public void ensureAliasColumn() {
        String sql = "ALTER TABLE trusted_contacts ADD COLUMN IF NOT EXISTS alias VARCHAR(255)";
        try (java.sql.Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addContact(int userId, int contactId) {
        String sql = "INSERT INTO trusted_contacts (user_id, contact_id) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, contactId);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<User> getContacts(int userId) {
        List<User> contacts = new ArrayList<>();
        // Updated to fetch alias
        String sql = "SELECT u.*, tc.alias FROM users_local u " +
                "JOIN trusted_contacts tc ON u.user_id = tc.contact_id " +
                "WHERE tc.user_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Fix: User constructor uses 'user_id' from result set
                    User user = new User(
                            rs.getInt("user_id"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("full_name"),
                            rs.getString("phone_number"),
                            rs.getString("profile_photo_url"));
                    user.setTrustScore(rs.getInt("trust_score"));
                    user.setContactAlias(rs.getString("alias"));
                    contacts.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    public boolean updateAlias(int userId, int contactId, String alias) {
        String sql = "UPDATE trusted_contacts SET alias = ? WHERE user_id = ? AND contact_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, alias);
            ps.setInt(2, userId);
            ps.setInt(3, contactId);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeContact(int userId, int contactId) {
        String sql = "DELETE FROM trusted_contacts WHERE user_id = ? AND contact_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, contactId);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
