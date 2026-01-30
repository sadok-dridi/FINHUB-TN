package tn.finhub.dao.impl;

import tn.finhub.dao.UserDAO;
import tn.finhub.model.User;
import tn.finhub.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);

    Connection connection = DBConnection.getInstance();

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, email, role, full_name FROM users_local";

        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("full_name")));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving users", e);
        }
        return users;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM users_local WHERE user_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            logger.info("User deleted with id {}", id);
        } catch (SQLException e) {
            logger.error("Error deleting user", e);
        }
    }

    public void deleteAll() {
        String sql = "DELETE FROM users_local";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
            logger.info("All local users deleted (sync reset)");
        } catch (SQLException e) {
            logger.error("Error deleting all users", e);
        }
    }

    // ========================
    // INSERT (SYNC FROM SERVER)
    // ========================

    public void insert(User u) {
        String sql = """
                    INSERT INTO users_local (user_id, email, role, full_name)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                    email = VALUES(email),
                    role = VALUES(role),
                    full_name = VALUES(full_name)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, u.getId());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getRole());
            ps.setString(4, u.getFullName());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error upserting user {}", u.getEmail(), e);
        }
    }

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT user_id, email, role, full_name FROM users_local WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("full_name"));
            }
        } catch (SQLException e) {
            logger.error("Error finding user", e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT user_id, email, role, full_name FROM users_local WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("user_id"), rs.getString("email"), rs.getString("role"),
                        rs.getString("full_name"));
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id", e);
        }
        return null;
    }
}
