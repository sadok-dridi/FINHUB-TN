package tn.finhub.model;

import tn.finhub.util.DBConnection;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VirtualCardModel {

    private final SecureRandom random = new SecureRandom();

    public VirtualCardModel() {
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

    // ========================
    // DATA ACCESS
    // ========================

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

    // ========================
    // BUSINESS LOGIC
    // ========================

    public VirtualCard createCardForWallet(int walletId) {
        String cardNumber = generateLuhnCardNumber();
        String cvv = String.format("%03d", random.nextInt(1000));
        Date expiryDate = Date.valueOf(LocalDate.now().plusYears(3));

        VirtualCard card = new VirtualCard(walletId, cardNumber, cvv, expiryDate);
        save(card);
        return card;
    }

    public List<VirtualCard> getCardsByWallet(int walletId) {
        return findByWalletId(walletId);
    }

    /**
     * Simulates a transaction.
     * Checks if the card exists, is active, and if the LINKED WALLET has enough
     * funds.
     */
    public boolean simulateTransaction(String cardNumber, String cvv, double amount) {
        VirtualCard card = findByCardNumber(cardNumber);
        if (card == null || !"ACTIVE".equals(card.getStatus())) {
            System.out.println("Card invalid or inactive.");
            return false;
        }

        if (!card.getCvv().equals(cvv)) {
            System.out.println("Invalid CVV.");
            return false;
        }

        // Dependent on WalletModel to check balance
        WalletModel walletModel = new WalletModel();
        Wallet wallet = walletModel.findById(card.getWalletId());

        if (wallet == null) {
            System.out.println("Wallet not found.");
            return false;
        }

        BigDecimal txAmount = BigDecimal.valueOf(amount);
        if (wallet.getBalance().compareTo(txAmount) >= 0) {
            // Success! In a real scenario, we would deduct here or authorize.
            // For simulation, we just return true.
            System.out.println("Transaction Approved: Sufficient Wallet Funds.");
            return true;
        } else {
            System.out.println("Transaction Declined: Insufficient Wallet Funds.");
            return false;
        }
    }

    public Wallet findCardOwner(String cardNumber) {
        VirtualCard card = findByCardNumber(cardNumber);
        if (card == null) {
            return null;
        }
        WalletModel walletModel = new WalletModel();
        return walletModel.findById(card.getWalletId());
    }

    public VirtualCard findCard(String cardNumber) {
        return findByCardNumber(cardNumber);
    }

    private String generateLuhnCardNumber() {
        // Start with 4 (Visa)
        StringBuilder builder = new StringBuilder("4");

        // Generate next 14 digits random
        for (int i = 0; i < 14; i++) {
            builder.append(random.nextInt(10));
        }

        // Calculate Check Digit
        String temp = builder.toString();
        int checkDigit = calculateLuhnCheckDigit(temp);

        return temp + checkDigit;
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;

        // Loop from right to left
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));

            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }

    // Validate Luhn (Helper for verification)
    public static boolean checkLuhn(String cardNo) {
        return tn.finhub.util.CardUtils.checkLuhn(cardNo);
    }
}
