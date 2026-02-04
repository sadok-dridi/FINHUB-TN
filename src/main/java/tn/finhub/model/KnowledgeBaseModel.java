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
        String sql = "SELECT * FROM knowledge_base ORDER BY category, created_at DESC";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                articles.add(mapResultSetToArticle(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articles;
    }

    public List<KnowledgeBase> searchArticles(String query) {
        List<KnowledgeBase> articles = new ArrayList<>();
        // Simple search query, could be improved with Full Text Search if DB supports
        // it
        String sql = "SELECT * FROM knowledge_base WHERE question LIKE ? OR answer LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            String likeQuery = "%" + query + "%";
            stmt.setString(1, likeQuery);
            stmt.setString(2, likeQuery);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    articles.add(mapResultSetToArticle(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articles;
    }

    public List<KnowledgeBase> getArticlesByCategory(String category) {
        List<KnowledgeBase> articles = new ArrayList<>();
        String sql = "SELECT * FROM knowledge_base WHERE category = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    articles.add(mapResultSetToArticle(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articles;
    }

    private KnowledgeBase mapResultSetToArticle(ResultSet rs) throws SQLException {
        KnowledgeBase article = new KnowledgeBase();
        article.setId(rs.getInt("id"));
        article.setCategory(rs.getString("category"));
        article.setQuestion(rs.getString("question"));
        article.setAnswer(rs.getString("answer"));
        article.setCreatedAt(rs.getTimestamp("created_at"));
        return article;
    }
}
