package tn.finhub.model;

import java.sql.Timestamp;

public class SupportMessage {
    private int id;
    private int ticketId;
    private String senderRole; // USER, ADMIN, SYSTEM, AI
    private String message;
    private Timestamp createdAt;

    private String attachmentPath;

    public SupportMessage() {
    }

    public SupportMessage(int ticketId, String senderRole, String message) {
        this.ticketId = ticketId;
        this.senderRole = senderRole;
        this.message = message;
    }

    public SupportMessage(int ticketId, String senderRole, String message, String attachmentPath) {
        this.ticketId = ticketId;
        this.senderRole = senderRole;
        this.message = message;
        this.attachmentPath = attachmentPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }
}
