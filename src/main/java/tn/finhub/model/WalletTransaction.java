package tn.finhub.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransaction {

    private int id;
    private int walletId;
<<<<<<< HEAD
    private String type; // CREDIT | DEBIT | HOLD | RELEASE
=======
    private String type;       // CREDIT | DEBIT | HOLD | RELEASE
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
    private BigDecimal amount;
    private String reference;
    private LocalDateTime createdAt;

<<<<<<< HEAD
    private String prevHash;
    private String txHash;

    public WalletTransaction() {
    }

    public WalletTransaction(int id, int walletId, String type,
            BigDecimal amount, String reference,
            String prevHash, String txHash,
            LocalDateTime createdAt) {
=======
    public WalletTransaction() {}

    public WalletTransaction(int id, int walletId, String type,
                             BigDecimal  amount, String reference,
                             LocalDateTime createdAt) {
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
        this.id = id;
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.reference = reference;
<<<<<<< HEAD
        this.prevHash = prevHash;
        this.txHash = txHash;
=======
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
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

<<<<<<< HEAD
    public BigDecimal getAmount() {
=======
    public BigDecimal  getAmount() {
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
        return amount;
    }

    public String getReference() {
        return reference;
    }

<<<<<<< HEAD
    public String getPrevHash() {
        return prevHash;
    }

    public String getTxHash() {
        return txHash;
    }

=======
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
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

<<<<<<< HEAD
    public void setAmount(BigDecimal amount) {
=======
    public void setAmount(BigDecimal  amount) {
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
        this.amount = amount;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

<<<<<<< HEAD
    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

=======
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
