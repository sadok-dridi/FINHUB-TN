package tn.finhub.model;

import java.sql.Timestamp;

public class SystemAlert {
    private int id;
    private int userId;
    private String severity; // INFO, WARNING, CRITICAL
    private String message;
    private String source; // LEDGER, WALLET, AUTH
    private Timestamp createdAt;

    public SystemAlert() {
    }

    public SystemAlert(int userId, String severity, String message, String source) {
        this.userId = userId;
        this.severity = severity;
        this.message = message;
        this.source = source;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
