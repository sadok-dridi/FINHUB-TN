package tn.finhub.model;

import java.time.LocalDateTime;

public class SavedContact {
    private int id;
    private int userId;
    private String contactEmail;
    private String contactName;
    private LocalDateTime createdAt;

    public SavedContact() {
    }

    public SavedContact(int userId, String contactEmail, String contactName) {
        this.userId = userId;
        this.contactEmail = contactEmail;
        this.contactName = contactName;
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

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return contactName + " (" + contactEmail + ")";
    }
}
