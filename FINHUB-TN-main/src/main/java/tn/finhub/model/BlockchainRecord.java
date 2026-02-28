package tn.finhub.model;

import java.time.LocalDateTime;

public class BlockchainRecord {

    private int id;
    private String previousHash;
    private String dataHash;
    private String type; // TRANSACTION, ESCROW_CREATE, etc.
    private int nonce;
    private LocalDateTime timestamp;
    private String currentHash;
    private Integer walletTransactionId; // Nullable
    private Integer escrowId; // Nullable

    public BlockchainRecord() {
    }

    public BlockchainRecord(String previousHash, String dataHash, String type, int nonce, String currentHash) {
        this.previousHash = previousHash;
        this.dataHash = dataHash;
        this.type = type;
        this.nonce = nonce;
        this.currentHash = currentHash;
        this.timestamp = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getDataHash() {
        return dataHash;
    }

    public void setDataHash(String dataHash) {
        this.dataHash = dataHash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public Integer getWalletTransactionId() {
        return walletTransactionId;
    }

    public void setWalletTransactionId(Integer walletTransactionId) {
        this.walletTransactionId = walletTransactionId;
    }

    public Integer getEscrowId() {
        return escrowId;
    }

    public void setEscrowId(Integer escrowId) {
        this.escrowId = escrowId;
    }
}
