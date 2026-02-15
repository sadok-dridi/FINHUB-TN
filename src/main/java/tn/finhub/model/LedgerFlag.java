package tn.finhub.model;

import java.time.LocalDateTime;

public class LedgerFlag {
    private int id;
    private int walletId;
    private String reason;
    private LocalDateTime flaggedAt;

    public LedgerFlag() {
    }

    public LedgerFlag(int walletId, String reason) {
        this.walletId = walletId;
        this.reason = reason;
        this.flaggedAt = LocalDateTime.now();
    }

    public LedgerFlag(int id, int walletId, String reason, LocalDateTime flaggedAt) {
        this.id = id;
        this.walletId = walletId;
        this.reason = reason;
        this.flaggedAt = flaggedAt;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getFlaggedAt() {
        return flaggedAt;
    }

    public void setFlaggedAt(LocalDateTime flaggedAt) {
        this.flaggedAt = flaggedAt;
    }
}
