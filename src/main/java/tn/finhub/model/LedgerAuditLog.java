package tn.finhub.model;

import java.time.LocalDateTime;

public class LedgerAuditLog {
    private int id;
    private int walletId;
    private boolean verified;
    private LocalDateTime checkedAt;
    private String message;

    public LedgerAuditLog() {
    }

    public LedgerAuditLog(int walletId, boolean verified, String message) {
        this.walletId = walletId;
        this.verified = verified;
        this.message = message;
        this.checkedAt = LocalDateTime.now();
    }

    public LedgerAuditLog(int id, int walletId, boolean verified, LocalDateTime checkedAt, String message) {
        this.id = id;
        this.walletId = walletId;
        this.verified = verified;
        this.checkedAt = checkedAt;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWalletId() {
        return walletId;
    }

    public void setWalletId(int walletId) {
        this.walletId = walletId;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
