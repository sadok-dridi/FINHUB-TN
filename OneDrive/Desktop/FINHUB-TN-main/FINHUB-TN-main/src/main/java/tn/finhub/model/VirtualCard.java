package tn.finhub.model;

import java.sql.Date;
import java.sql.Timestamp;

public class VirtualCard {
    private int id;
    private int walletId;
    private String cardNumber;
    private String cvv;
    private Date expiryDate;
    private String status; // "ACTIVE", "BLOCKED"
    private Timestamp createdAt;

    public VirtualCard() {
    }

    public VirtualCard(int id, int walletId, String cardNumber, String cvv, Date expiryDate, String status,
            Timestamp createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public VirtualCard(int walletId, String cardNumber, String cvv, Date expiryDate) {
        this.walletId = walletId;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
        this.status = "ACTIVE";
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getWalletId() {
        return walletId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setWalletId(int walletId) {
        this.walletId = walletId;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
