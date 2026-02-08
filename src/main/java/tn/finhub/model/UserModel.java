package tn.finhub.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.finhub.util.ApiClient;
import tn.finhub.util.SessionManager;
import tn.finhub.util.DBConnection;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserModel {

    private static final Logger logger = LoggerFactory.getLogger(UserModel.class);

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    // ========================
    // DATA ACCESS METHODS
    // ========================

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, email, role, full_name, trust_score FROM users_local";

        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                User u = new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("full_name"));
                // u.setEmailVerified(rs.getBoolean("email_verified")); // Not in local DB
                u.setTrustScore(rs.getInt("trust_score"));
                users.add(u);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving users", e);
        }
        return users;
    }

    public int countUsers() {
        String sql = "SELECT COUNT(*) FROM users_local";
        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting users", e);
        }
        return 0;
    }

    public User findByEmail(String email) {
        String sql = "SELECT user_id, email, role, full_name, trust_score FROM users_local WHERE LOWER(email) = LOWER(?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User u = new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("full_name"));
                // u.setEmailVerified(rs.getBoolean("email_verified")); // Not in local DB
                u.setTrustScore(rs.getInt("trust_score"));
                return u;
            }
        } catch (SQLException e) {
            logger.error("Error finding user", e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT user_id, email, role, full_name, trust_score FROM users_local WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User(rs.getInt("user_id"), rs.getString("email"), rs.getString("role"),
                        rs.getString("full_name"));
                // u.setEmailVerified(rs.getBoolean("email_verified")); // Not in local DB
                u.setTrustScore(rs.getInt("trust_score"));
                return u;
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id", e);
        }
        return null;
    }

    public void insert(User u) {
        String sql = """
                    INSERT INTO users_local (user_id, email, role, full_name)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                    email = VALUES(email),
                    role = VALUES(role),
                    full_name = VALUES(full_name)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, u.getId());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getRole());
            ps.setString(4, u.getFullName());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error upserting user {}", u.getEmail(), e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM users_local WHERE user_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            logger.info("User deleted with id {}", id);
        } catch (SQLException e) {
            logger.error("Error deleting user", e);
        }
    }

    public void deleteAll() {
        String sql = "DELETE FROM users_local";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.executeUpdate();
            logger.info("All local users deleted (sync reset)");
        } catch (SQLException e) {
            logger.error("Error deleting all users", e);
        }
    }

    // ========================
    // BUSINESS LOGIC METHODS
    // ========================

    public void syncUsersFromServer() {
        List<User> serverUsers = ApiClient.fetchUsersFromServer();
        for (User u : serverUsers) {
            insert(u);
        }
    }

    public void deleteUser(int id) {
        // 1. Server Delete
        deleteUserOnServer(id);

        // 2. Local Cascade Delete
        WalletModel walletModel = new WalletModel();
        Wallet wallet = walletModel.findByUserId(id);

        if (wallet != null) {
            walletModel.deleteWalletRecursive(wallet.getId());
        }

        // 3. Delete User Local
        delete(id);
    }

    private void deleteUserOnServer(int userId) {
        String token = SessionManager.getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing JWT token in session");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiClient.BASE_URL + "/admin/users/" + userId))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .DELETE()
                .build();

        try {
            HttpResponse<String> response = ApiClient.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            if (code < 200 || code >= 300) {
                String body = response.body();
                throw new RuntimeException(
                        "Server delete failed, status=" + code + (body != null ? (", body=" + body) : ""));
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Server delete request failed", e);
        }
    }

    public void updateTrustScore(int userId, int pointsDelta) {
        String sql = "UPDATE users_local SET trust_score = trust_score + ? WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, pointsDelta);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating trust score for user {}", userId, e);
        }
    }
}
