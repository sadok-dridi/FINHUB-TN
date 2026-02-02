package tn.finhub.dao;

import tn.finhub.model.VirtualCard;
import tn.finhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VirtualCardDAO {

    public VirtualCardDAO() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
                    CREATE TABLE IF NOT EXISTS virtual_cards (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        wallet_id INT NOT NULL,
                        card_number VARCHAR(16) NOT NULL UNIQUE,
                        cvv VARCHAR(3) NOT NULL,
                        expiry_date DATE NOT NULL,
                        status VARCHAR(20) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
                    )
                """;
        try (Connection connection = DBConnection.getInstance();
                Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create virtual_cards table", e);
        }
    }

    public void save(VirtualCard card) {
        String sql = "INSERT INTO virtual_cards (wallet_id, card_number, cvv, expiry_date, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getInstance();
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, card.getWalletId());
            ps.setString(2, card.getCardNumber());
            ps.setString(3, card.getCvv());
            ps.setDate(4, card.getExpiryDate());
            ps.setString(5, card.getStatus());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                card.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save virtual card", e);
        }
    }

    public List<VirtualCard> findByWalletId(int walletId) {
        List<VirtualCard> cards = new ArrayList<>();
        String sql = "SELECT * FROM virtual_cards WHERE wallet_id = ?";
        try (Connection connection = DBConnection.getInstance();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cards.add(new VirtualCard(
                        rs.getInt("id"),
                        rs.getInt("wallet_id"),
                        rs.getString("card_number"),
                        rs.getString("cvv"),
                        rs.getDate("expiry_date"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find virtual cards", e);
        }
        return cards;
    }

    public VirtualCard findByCardNumber(String cardNumber) {
        String sql = "SELECT * FROM virtual_cards WHERE card_number = ?";
        try (Connection connection = DBConnection.getInstance();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cardNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new VirtualCard(
                        rs.getInt("id"),
                        rs.getInt("wallet_id"),
                        rs.getString("card_number"),
                        rs.getString("cvv"),
                        rs.getDate("expiry_date"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find virtual card by number", e);
        }
        return null;
    }
}
