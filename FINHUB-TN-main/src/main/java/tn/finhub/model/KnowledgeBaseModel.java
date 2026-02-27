package tn.finhub.model;

import tn.finhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeBaseModel {

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    public List<KnowledgeBase> getAllArticles() {
        List<KnowledgeBase> articles = new ArrayList<>();
        String sql = "SELECT * FROM knowledge_base ORDER BY created_at DESC";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                articles.add(new KnowledgeBase(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articles;
    }

    public void addArticle(String title, String category, String content) {
        String sql = "INSERT INTO knowledge_base (title, category, content) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setString(2, category);
            stmt.setString(3, content);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error adding article: " + e.getMessage());
        }
    }

    public void updateArticle(int id, String title, String category, String content) {
        String sql = "UPDATE knowledge_base SET title = ?, category = ?, content = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setString(2, category);
            stmt.setString(3, content);
            stmt.setInt(4, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error updating article: " + e.getMessage());
        }
    }

    public void deleteArticle(int id) {
        String sql = "DELETE FROM knowledge_base WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting article: " + e.getMessage());
        }
    }

    public List<KnowledgeBase> searchArticles(String query) {
        List<KnowledgeBase> articles = new ArrayList<>();
        String sql = "SELECT * FROM knowledge_base WHERE title LIKE ? OR content LIKE ? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            String likeQuery = "%" + query + "%";
            stmt.setString(1, likeQuery);
            stmt.setString(2, likeQuery);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    articles.add(new KnowledgeBase(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at"),
                            rs.getTimestamp("updated_at")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articles;
    }
}
