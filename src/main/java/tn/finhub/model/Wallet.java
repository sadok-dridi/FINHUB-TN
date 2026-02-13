package tn.finhub.model;

import java.math.BigDecimal;

public class Wallet {

    private int id;
    private int userId;
<<<<<<< HEAD
    private String currency; // "TND"
    private BigDecimal balance;
    private BigDecimal escrowBalance;
    private String status; // ACTIVE | FROZEN

    public Wallet() {
    }

    public Wallet(int id, int userId, String currency,
            BigDecimal balance, BigDecimal escrowBalance, String status) {
=======
    private String currency;      // "TND"
    private BigDecimal   balance;
    private BigDecimal  escrowBalance;


    public Wallet() {}

    public Wallet(int id, int userId, String currency,
                  BigDecimal  balance, BigDecimal  escrowBalance ) {
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balance = balance;
        this.escrowBalance = escrowBalance;
<<<<<<< HEAD
        this.status = status;
=======

>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
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

<<<<<<< HEAD
    public BigDecimal getEscrowBalance() {
        return escrowBalance;
    }

    public String getStatus() {
        return status;
    }
=======
    public BigDecimal  getEscrowBalance() {
        return escrowBalance;
    }


>>>>>>> 3239865d261585c607c2f3379522c60b1fede853

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

<<<<<<< HEAD
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setEscrowBalance(BigDecimal escrowBalance) {
        this.escrowBalance = escrowBalance;
    }

    public void setStatus(String status) {
        this.status = status;
    }
=======
    public void setBalance(BigDecimal  balance) {
        this.balance = balance;
    }

    public void setEscrowBalance(BigDecimal  escrowBalance) {
        this.escrowBalance = escrowBalance;
    }


>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
}
