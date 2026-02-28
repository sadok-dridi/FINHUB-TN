package tn.finhub.model;

import java.sql.Timestamp;

public class SupportTicket {
    private int id;
    private int userId;
    private String subject;
    private String category; // WALLET, TRANSACTION, AUTH, SYSTEM, OTHER
    private String status; // OPEN, CLOSED, RESOLVED
    private String priority; // NORMAL, HIGH, URGENT
    private Timestamp createdAt;
    private Timestamp resolvedAt;

    // User display helper (not in DB)
    private String userFullName;

    public SupportTicket() {
    }

    public SupportTicket(int userId, String subject, String category, String priority) {
        this.userId = userId;
        this.subject = subject;
        this.category = category;
        this.priority = priority;
        this.status = "OPEN";
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }
}
