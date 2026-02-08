package tn.finhub.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Escrow {

    private int id;
    private int senderWalletId;
    private int receiverWalletId;
    private BigDecimal amount;
    private String conditionText;
    private String escrowType; // QR_CODE or ADMIN_APPROVAL
    private String secretCode; // For QR
    private String qrCodeImage;
    private Integer adminApproverId;
    private LocalDateTime expiryDate;
    private boolean isDisputed;
    private String status; // LOCKED, RELEASED, REFUNDED, DISPUTED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Escrow() {
    }

    public Escrow(int senderWalletId, int receiverWalletId, BigDecimal amount, String conditionText,
            String escrowType) {
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.amount = amount;
        this.conditionText = conditionText;
        this.escrowType = escrowType;
        this.status = "LOCKED";
        this.createdAt = LocalDateTime.now();
        this.isDisputed = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSenderWalletId() {
        return senderWalletId;
    }

    public void setSenderWalletId(int senderWalletId) {
        this.senderWalletId = senderWalletId;
    }

    public int getReceiverWalletId() {
        return receiverWalletId;
    }

    public void setReceiverWalletId(int receiverWalletId) {
        this.receiverWalletId = receiverWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getConditionText() {
        return conditionText;
    }

    public void setConditionText(String conditionText) {
        this.conditionText = conditionText;
    }

    public String getEscrowType() {
        return escrowType;
    }

    public void setEscrowType(String escrowType) {
        this.escrowType = escrowType;
    }

    public String getSecretCode() {
        return secretCode;
    }

    public void setSecretCode(String secretCode) {
        this.secretCode = secretCode;
    }

    public String getQrCodeImage() {
        return qrCodeImage;
    }

    public void setQrCodeImage(String qrCodeImage) {
        this.qrCodeImage = qrCodeImage;
    }

    public Integer getAdminApproverId() {
        return adminApproverId;
    }

    public void setAdminApproverId(Integer adminApproverId) {
        this.adminApproverId = adminApproverId;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isDisputed() {
        return isDisputed;
    }

    public void setDisputed(boolean disputed) {
        isDisputed = disputed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
