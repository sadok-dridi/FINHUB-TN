package tn.finhub.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransaction {

    private int id;
    private int walletId;
    private String type; // CREDIT | DEBIT | HOLD | RELEASE
    private BigDecimal amount;
    private String reference;
    private LocalDateTime createdAt;

    private String prevHash;
    private String txHash;

    public WalletTransaction() {
    }

    public WalletTransaction(int id, int walletId, String type,
            BigDecimal amount, String reference,
            String prevHash, String txHash,
            LocalDateTime createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.reference = reference;
        this.prevHash = prevHash;
        this.txHash = txHash;
        this.createdAt = createdAt;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getWalletId() {
        return walletId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReference() {
        return reference;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getTxHash() {
        return txHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setWalletId(int walletId) {
        this.walletId = walletId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
