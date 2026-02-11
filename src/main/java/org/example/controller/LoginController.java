package org.example.controller;



import org.example.model.User;
import org.example.utils.SessionManager;
import org.example.utils.MyDB;
import org.example.utils.PasswordUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Hyperlink signupLink;

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        try {
            Connection conn = MyDB.getConnection();
            String query = "SELECT * FROM user WHERE mail = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("mot_de_passe");

                if (PasswordUtils.checkPassword(password, hashedPassword)) {
                    User user = extractUserFromResultSet(rs);

                    if (user.getStatus() == User.Status.ACTIF) {
                        SessionManager.getInstance().login(user);
                        redirectUser(user);
                    } else {
                        errorLabel.setText("Compte désactivé. Contactez l'administrateur.");
                    }
                } else {
                    errorLabel.setText("Mot de passe incorrect");
                }
            } else {
                errorLabel.setText("Email non trouvé");
            }

        } catch (SQLException e) {
            errorLabel.setText("Erreur de connexion: " + e.getMessage());
        }
    }

    @FXML
    private void handleSignupLink() {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/Signup.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Créer un compte");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setDateNaissance(rs.getDate("date_naissance") != null ?
                rs.getDate("date_naissance").toLocalDate() : null);
        user.setMail(rs.getString("mail"));
        user.setNumTel(rs.getString("num_tel"));
        user.setMotDePasse(rs.getString("mot_de_passe"));
        user.setImage(rs.getString("image"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        user.setStatus(User.Status.valueOf(rs.getString("status")));
        return user;
    }

    private void redirectUser(User user) {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Parent root;

            if (user.getRole() == User.Role.ADMIN) {
                root = FXMLLoader.load(getClass().getResource("/UserBack.fxml"));
            } else {
                root = FXMLLoader.load(getClass().getResource("/UserFront.fxml"));
            }

            stage.setScene(new Scene(root));
            stage.setTitle("Tableau de bord - " + user.getNomComplet());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}