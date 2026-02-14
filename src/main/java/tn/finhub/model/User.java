package tn.finhub.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    private int id;
    private String email;
    private String role;
    @JsonProperty("full_name")
    private String fullName;
    private String phoneNumber;
    private String profilePhotoUrl;

    public User() {
    }

    public User(int id, String email, String role, String fullName) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
    }

    public User(int id, String email, String role, String fullName, String phoneNumber, String profilePhotoUrl) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.profilePhotoUrl = profilePhotoUrl;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    // Setters (optional)
    public void setRole(String role) {
        this.role = role;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    // New Field for Module 6
    private int trustScore = 100;

    public int getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(int trustScore) {
        this.trustScore = trustScore;
    }

    private boolean emailVerified;

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
