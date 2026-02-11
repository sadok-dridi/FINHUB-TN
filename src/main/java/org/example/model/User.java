package org.example.model;


import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String mail;
    private String numTel;
    private String motDePasse;
    private String image;
    private Role role;
    private Status status;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    public enum Role {
        ADMIN, USER
    }

    public enum Status {
        ACTIF, INACTIF, SUSPENDU
    }

    // Constructeurs
    public User() {}

    public User(String nom, String prenom, String mail, String motDePasse) {
        this.nom = nom;
        this.prenom = prenom;
        this.mail = mail;
        this.motDePasse = motDePasse;
        this.role = Role.USER;
        this.status = Status.ACTIF;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getNumTel() { return numTel; }
    public void setNumTel(String numTel) { this.numTel = numTel; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public String getNomComplet() {
        return nom + " " + prenom;
    }
}