package tn.finhub.service;

import tn.finhub.dao.UserDAO;
import tn.finhub.dao.impl.UserDAOImpl;
import tn.finhub.util.TokenGenerator;

import java.sql.*;
import java.time.LocalDateTime;

public class VerificationService {

    private Connection connection = tn.finhub.util.DBConnection.getInstance();
    private UserDAO userDAO = new UserDAOImpl();
    private EmailService emailService = new EmailService();

    public void createAndSendToken(int userId, String email) {

        String token = TokenGenerator.generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        String sql = """
                    INSERT INTO email_verification (user_id, token, expires_at)
                    VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();

            emailService.sendVerificationEmail(email, token);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verifyToken(String token) {

        String sql = """
                    SELECT * FROM email_verification
                    WHERE token = ? AND expires_at > NOW() AND verified = FALSE
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

        connection.prepareStatement(
                "UPDATE email_verification SET verified = TRUE WHERE token = '" + token + "'").executeUpdate();

        connection.prepareStatement(
                "UPDATE user SET email_verified = TRUE WHERE id = " + userId).executeUpdate();
    }
}
