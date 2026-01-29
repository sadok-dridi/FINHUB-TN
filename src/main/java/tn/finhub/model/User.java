package tn.finhub.model;
import com.fasterxml.jackson.annotation.JsonProperty;


public class User {

    private int id;
    private String email;
    private String role;
    @JsonProperty("full_name")
    private String fullName;


    public User() {
    }
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
}
