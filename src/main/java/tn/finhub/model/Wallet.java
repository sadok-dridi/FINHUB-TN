package tn.finhub.model;

import java.math.BigDecimal;

public class Wallet {

    private int id;
    private int userId;
    private String currency; // "TND"
    private BigDecimal balance;
    private BigDecimal escrowBalance;
    private String status; // ACTIVE | FROZEN

    public Wallet() {
    }

    public Wallet(int id, int userId, String currency,
            BigDecimal balance, BigDecimal escrowBalance, String status) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balance = balance;
        this.escrowBalance = escrowBalance;
        this.status = status;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getEscrowBalance() {
        return escrowBalance;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setEscrowBalance(BigDecimal escrowBalance) {
        this.escrowBalance = escrowBalance;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
