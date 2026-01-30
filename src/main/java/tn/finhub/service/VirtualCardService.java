package tn.finhub.service;

import tn.finhub.dao.VirtualCardDAO;
import tn.finhub.dao.WalletDAO;
import tn.finhub.model.VirtualCard;
import tn.finhub.model.Wallet;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class VirtualCardService {

    private final VirtualCardDAO cardDAO = new VirtualCardDAO();
    private final WalletDAO walletDAO = new WalletDAO();
    private final SecureRandom random = new SecureRandom();

    public VirtualCard createCardForWallet(int walletId) {
        String cardNumber = generateLuhnCardNumber();
        String cvv = String.format("%03d", random.nextInt(1000));
        Date expiryDate = Date.valueOf(LocalDate.now().plusYears(3));

        VirtualCard card = new VirtualCard(walletId, cardNumber, cvv, expiryDate);
        cardDAO.save(card);
        return card;
    }

    public List<VirtualCard> getCardsByWallet(int walletId) {
        return cardDAO.findByWalletId(walletId);
    }

    /**
     * Simulates a transaction.
     * Checks if the card exists, is active, and if the LINKED WALLET has enough
     * funds.
     */
    public boolean simulateTransaction(String cardNumber, String cvv, double amount) {
        VirtualCard card = cardDAO.findByCardNumber(cardNumber);
        if (card == null || !"ACTIVE".equals(card.getStatus())) {
            System.out.println("Card invalid or inactive.");
            return false;
        }

        if (!card.getCvv().equals(cvv)) {
            System.out.println("Invalid CVV.");
            return false;
        }

        Wallet wallet = walletDAO.findById(card.getWalletId());
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
        VirtualCard card = cardDAO.findByCardNumber(cardNumber);
        if (card == null) {
            return null;
        }
        return walletDAO.findById(card.getWalletId());
    }

    public VirtualCard findCard(String cardNumber) {
        return cardDAO.findByCardNumber(cardNumber);
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
        int nDigits = cardNo.length();
        int nSum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--) {
            int d = cardNo.charAt(i) - '0';
            if (isSecond == true)
                d = d * 2;
            nSum += d / 10;
            nSum += d % 10;
            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }
}
