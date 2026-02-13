package tn.finhub.model;
<<<<<<< HEAD

import com.fasterxml.jackson.annotation.JsonProperty;

=======
import com.fasterxml.jackson.annotation.JsonProperty;


>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
public class User {

    private int id;
    private String email;
    private String role;
    @JsonProperty("full_name")
    private String fullName;

<<<<<<< HEAD
    public User() {
    }

=======

    public User() {
    }
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
    public User(int id, String email, String role, String fullName) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
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
<<<<<<< HEAD

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
=======
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
}
