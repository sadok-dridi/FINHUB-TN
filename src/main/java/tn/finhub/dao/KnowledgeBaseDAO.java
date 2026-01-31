package tn.finhub.dao;

import tn.finhub.model.KnowledgeBase;
import tn.finhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeBaseDAO {

    public List<KnowledgeBase> getAllArticles() {
        List<KnowledgeBase> articles = new ArrayList<>();
        String sql = "SELECT * FROM knowledge_base ORDER BY category, created_at DESC";
        try (Statement stmt = DBConnection.getInstance().createStatement();
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
        String sql = "SELECT * FROM knowledge_base WHERE question LIKE ? OR answer LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
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
        try (PreparedStatement stmt = DBConnection.getInstance().prepareStatement(sql)) {
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
