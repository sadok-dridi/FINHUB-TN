package tn.finhub.model;

import java.sql.Timestamp;

public class KYCRequest {
    private int requestId;
    private int userId;
    private String documentType; // "ID_CARD" or "VIDEO"
    private String documentUrl;
    private String status; // "PENDING", "APPROVED", "REJECTED"
    private Timestamp submissionDate;

    // Additional fields for display if needed (e.g., user name)
    private String userEmail;

    public KYCRequest() {
    }

    public KYCRequest(int requestId, int userId, String documentType, String documentUrl, String status,
            Timestamp submissionDate) {
        this.requestId = requestId;
        this.userId = userId;
        this.documentType = documentType;
        this.documentUrl = documentUrl;
        this.status = status;
        this.submissionDate = submissionDate;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(Timestamp submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
