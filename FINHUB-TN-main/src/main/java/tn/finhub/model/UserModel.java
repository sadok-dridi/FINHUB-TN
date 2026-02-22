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
        String sql = "SELECT user_id, email, role, full_name, phone_number, profile_photo_url, trust_score FROM users_local";

        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                User u = new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("full_name"));
                u.setPhoneNumber(rs.getString("phone_number"));
                u.setProfilePhotoUrl(rs.getString("profile_photo_url"));
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
        String sql = "SELECT user_id, email, role, full_name, phone_number, profile_photo_url, trust_score FROM users_local WHERE LOWER(email) = LOWER(?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User u = new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("full_name"));
                u.setPhoneNumber(rs.getString("phone_number"));
                u.setProfilePhotoUrl(rs.getString("profile_photo_url"));
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
        String sql = "SELECT user_id, email, role, full_name, phone_number, profile_photo_url, trust_score FROM users_local WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User(rs.getInt("user_id"), rs.getString("email"), rs.getString("role"),
                        rs.getString("full_name"));
                u.setPhoneNumber(rs.getString("phone_number"));
                u.setProfilePhotoUrl(rs.getString("profile_photo_url"));
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
                    INSERT INTO users_local (user_id, email, role, full_name, phone_number, profile_photo_url)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                    email = VALUES(email),
                    role = VALUES(role),
                    full_name = VALUES(full_name),
                    phone_number = COALESCE(VALUES(phone_number), phone_number),
                    profile_photo_url = COALESCE(VALUES(profile_photo_url), profile_photo_url)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, u.getId());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getRole());
            ps.setString(4, u.getFullName());
            ps.setString(5, u.getPhoneNumber());
            ps.setString(6, u.getProfilePhotoUrl());
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
        // 0. Fetch User Details for Email-based deletion (Contacts)
        User user = findById(id);
        if (user == null) {
            logger.warn("User {} not found for deletion", id);
            return;
        }

        // 1. Safety Check: Active Escrows
        WalletModel walletModel = new WalletModel();
        Wallet wallet = walletModel.findByUserId(id);

        if (wallet != null) {
            EscrowManager escrowManager = new EscrowManager();
            if (escrowManager.hasActiveEscrows(wallet.getId())) {
                throw new RuntimeException("Cannot delete user: Active Escrow transactions pending.");
            }

            // 2. Asset Reclaiming (Transfer to Bank)
            try {
                // Portfolio
                MarketModel marketModel = new MarketModel();
                marketModel.liquidatePortfolioToWallet(id);

                // Wallet Balance
                walletModel.transferEntireBalanceToBank(id);

            } catch (Exception e) {
                logger.error("Failed to reclaim assets for user {}", id, e);
                // Proceed? Or abort?
                // User wants "if account deleted... it goes to bank".
                // If transfer fails, we probably shouldn't just burn the money.
                // But typically we log and proceed or throw.
                // Throwing stops delete, which is safe.
                throw new RuntimeException("Asset reclamation failed. Deletion aborted.", e);
            }
        }

        // 3. Server Delete
        try {
            deleteUserOnServer(id);
        } catch (Exception e) {
            logger.error("Server delete failed for user {}, proceeding with local cleanup", id, e);
        }

        // 4. Local Cascade Delete
        try {
            // A. Wallet-based Cleanup
            if (wallet != null) {
                // Virtual Cards
                VirtualCardModel virtualCardModel = new VirtualCardModel();
                virtualCardModel.deleteByWalletId(wallet.getId());

                // Escrow (History/Inactive only due to check above)
                EscrowManager escrowManager = new EscrowManager();
                escrowManager.deleteByWalletId(wallet.getId());

                // Wallet Transactions & Ledger (handled by deleteWalletRecursive)
                walletModel.deleteWalletRecursive(wallet.getId());
            }

            // B. User-based Cleanup
            SavedContactModel savedContactModel = new SavedContactModel();
            savedContactModel.deleteByUserId(id); // Contacts saved BY this user
            if (user.getEmail() != null) {
                savedContactModel.deleteByContactEmail(user.getEmail()); // This user saved IN others' lists
            }

            // Trusted Contacts (Escrow contacts)
            deleteFromTable("trusted_contacts", "user_id", id);
            deleteFromTable("trusted_contacts", "contact_id", id);

            // Financial Profile
            deleteFromTable("financial_profiles_local", "user_id", id);

            // KYC Requests
            deleteFromTable("kyc_requests", "user_id", id);

            // System Alerts
            deleteFromTable("system_alerts", "user_id", id);

            MarketModel marketModel = new MarketModel();
            marketModel.deletePortfolioByUserId(id);
            marketModel.deleteTradesByUserId(id);

            SupportModel supportModel = new SupportModel();
            supportModel.deleteTicketsByUserId(id);

        } catch (Exception e) {
            logger.error("Error during local cascade delete for user {}", id, e);
            throw new RuntimeException("Partially failed to delete local user data", e);
        }

        // 5. Delete User Local
        delete(id);
    }

    private void deleteFromTable(String table, String column, int value) {
        String sql = "DELETE FROM " + table + " WHERE " + column + " = ?";
        try (java.sql.PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, value);
            ps.executeUpdate();
        } catch (java.sql.SQLException e) {
            logger.warn("Failed to delete from {} where {} = {}", table, column, value, e);
        }
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

    public void updateProfile(User user) {
        String sql = "UPDATE users_local SET phone_number = ?, profile_photo_url = ? WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getPhoneNumber());
            ps.setString(2, user.getProfilePhotoUrl());
            ps.setInt(3, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating profile for user {}", user.getId(), e);
        }
    }

    public java.util.Map<String, String> findProfilePhotosByNames(java.util.Set<String> names) {
        java.util.Map<String, String> photoMap = new java.util.HashMap<>();
        if (names == null || names.isEmpty()) {
            return photoMap;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT full_name, profile_photo_url FROM users_local WHERE full_name IN (");
        for (int i = 0; i < names.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            int index = 1;
            for (String name : names) {
                ps.setString(index++, name);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String fullName = rs.getString("full_name");
                String photoUrl = rs.getString("profile_photo_url");
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    photoMap.put(fullName, photoUrl);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching profile photos by names", e);
        }
        return photoMap;
    }

    public java.util.Map<String, String> findProfilePhotosByEmails(java.util.Set<String> emails) {
        java.util.Map<String, String> photoMap = new java.util.HashMap<>();
        if (emails == null || emails.isEmpty()) {
            return photoMap;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT email, profile_photo_url FROM users_local WHERE email IN (");
        for (int i = 0; i < emails.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            int index = 1;
            for (String email : emails) {
                ps.setString(index++, email);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String email = rs.getString("email");
                String photoUrl = rs.getString("profile_photo_url");
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    photoMap.put(email, photoUrl);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching profile photos by emails", e);
        }
        return photoMap;
    }
}
