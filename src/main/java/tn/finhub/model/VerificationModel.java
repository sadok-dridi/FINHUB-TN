package tn.finhub.model;

import tn.finhub.util.DBConnection;
import tn.finhub.util.MailClient;
import tn.finhub.util.TokenGenerator;

import java.sql.*;
import java.time.LocalDateTime;

public class VerificationModel {

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    public void createAndSendToken(int userId, String email) {

        String token = TokenGenerator.generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        String sql = """
                    INSERT INTO email_verification (user_id, token, expires_at)
                    VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();

            MailClient.sendVerificationEmail(email, token); // Use MailClient

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verifyToken(String token) {

        String sql = """
                    SELECT * FROM email_verification
                    WHERE token = ? AND expires_at > NOW() AND verified = FALSE
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");

                markVerified(userId, token);
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void markVerified(int userId, String token) throws SQLException {

        getConnection().prepareStatement(
                "UPDATE email_verification SET verified = TRUE WHERE token = '" + token + "'").executeUpdate();

        getConnection().prepareStatement(
                "UPDATE users_local SET email_verified = TRUE WHERE user_id = " + userId).executeUpdate();
    }
}
