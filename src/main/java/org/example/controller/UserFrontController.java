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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserFrontController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;

    // Profile fields
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private TextField mailField;
    @FXML private TextField numTelField;
    @FXML private PasswordField motDePasseField;
    @FXML private PasswordField confirmMotDePasseField;
    @FXML private ImageView profileImageView;

    // Account info labels
    @FXML private Label roleLabel;
    @FXML private Label accountStatusLabel;
    @FXML private Label dateCreationLabel;

    private User currentUser;
    private String selectedImagePath;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            handleLogout();
            return;
        }

        welcomeLabel.setText("Bienvenue, " + currentUser.getPrenom() + " " + currentUser.getNom() + "!");
        loadUserData();
    }

    private void loadUserData() {
        try (Connection conn = MyDB.getConnection()) {
            String query = "SELECT * FROM user WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, currentUser.getId());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Informations personnelles
                nomField.setText(rs.getString("nom"));
                prenomField.setText(rs.getString("prenom"));

                if (rs.getDate("date_naissance") != null) {
                    dateNaissancePicker.setValue(rs.getDate("date_naissance").toLocalDate());
                }

                mailField.setText(rs.getString("mail"));
                numTelField.setText(rs.getString("num_tel"));

                // Status avec style
                String status = rs.getString("status");
                statusLabel.setText("Status: " + status);
                accountStatusLabel.setText(status);

                // Rôle
                String role = rs.getString("role");
                roleLabel.setText(role);

                // Date de création
                if (rs.getTimestamp("date_creation") != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    dateCreationLabel.setText(rs.getTimestamp("date_creation").toLocalDateTime().format(formatter));
                }

                // Image de profil
                String imagePath = rs.getString("image");
                if (imagePath != null && !imagePath.isEmpty()) {
                    try {
                        File imageFile = new File(imagePath);
                        if (imageFile.exists()) {
                            profileImageView.setImage(new Image(imageFile.toURI().toString()));
                            selectedImagePath = imagePath;
                        }
                    } catch (Exception e) {
                        // Image par défaut si erreur
                        setDefaultProfileImage();
                    }
                } else {
                    setDefaultProfileImage();
                }
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les données: " + e.getMessage());
        }
    }

    private void setDefaultProfileImage() {
        try {
            // Image par défaut (vous pouvez mettre le chemin vers une image par défaut)
            profileImageView.setImage(null);
        } catch (Exception e) {
            // Ignorer
        }
    }

    @FXML
    private void handleUpdateProfile() {
        if (!validateProfileFields()) {
            return;
        }

        try (Connection conn = MyDB.getConnection()) {
            String query = "UPDATE user SET nom = ?, prenom = ?, date_naissance = ?, num_tel = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, nomField.getText().trim());
            stmt.setString(2, prenomField.getText().trim());
            stmt.setDate(3, dateNaissancePicker.getValue() != null ?
                    Date.valueOf(dateNaissancePicker.getValue()) : null);
            stmt.setString(4, numTelField.getText().trim());
            stmt.setInt(5, currentUser.getId());

            int result = stmt.executeUpdate();

            if (result > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Profil mis à jour avec succès!");

                // Mettre à jour la session
                currentUser.setNom(nomField.getText().trim());
                currentUser.setPrenom(prenomField.getText().trim());
                currentUser.setDateNaissance(dateNaissancePicker.getValue());
                currentUser.setNumTel(numTelField.getText().trim());

                // Mettre à jour le message de bienvenue
                welcomeLabel.setText("Bienvenue, " + currentUser.getPrenom() + " " + currentUser.getNom() + "!");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la mise à jour: " + e.getMessage());
        }
    }

    @FXML
    private void handleChangePassword() {
        String newPassword = motDePasseField.getText();
        String confirmPassword = confirmMotDePasseField.getText();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir tous les champs");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Les mots de passe ne correspondent pas");
            return;
        }

        if (newPassword.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        try {
            String hashedPassword = PasswordUtils.hashPassword(newPassword);

            try (Connection conn = MyDB.getConnection()) {
                String query = "UPDATE user SET mot_de_passe = ? WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, hashedPassword);
                stmt.setInt(2, currentUser.getId());

                int result = stmt.executeUpdate();

                if (result > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Mot de passe changé avec succès!");
                    motDePasseField.clear();
                    confirmMotDePasseField.clear();
                }
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec du changement: " + e.getMessage());
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                selectedImagePath = selectedFile.getAbsolutePath();

                try (Connection conn = MyDB.getConnection()) {
                    String query = "UPDATE user SET image = ? WHERE id = ?";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setString(1, selectedImagePath);
                    stmt.setInt(2, currentUser.getId());

                    int result = stmt.executeUpdate();

                    if (result > 0) {
                        Image image = new Image(selectedFile.toURI().toString());
                        profileImageView.setImage(image);
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Photo de profil mise à jour!");
                    }
                }

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec du téléchargement: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleLogout() {
        org.example.utils.SessionManager.getInstance().logout();

        try {
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validateProfileFields() {
        StringBuilder errors = new StringBuilder();

        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            errors.append("- Le nom est obligatoire\n");
            nomField.setStyle("-fx-border-color: red;");
        } else {
            nomField.setStyle("");
        }

        if (prenomField.getText() == null || prenomField.getText().trim().isEmpty()) {
            errors.append("- Le prénom est obligatoire\n");
            prenomField.setStyle("-fx-border-color: red;");
        } else {
            prenomField.setStyle("");
        }

        if (numTelField.getText() != null && !numTelField.getText().trim().isEmpty()) {
            if (!numTelField.getText().matches("^(\\+?[0-9]{8,15})$")) {
                errors.append("- Le numéro de téléphone n'est pas valide\n");
                numTelField.setStyle("-fx-border-color: red;");
            } else {
                numTelField.setStyle("");
            }
        }

        if (dateNaissancePicker.getValue() != null) {
            if (dateNaissancePicker.getValue().isAfter(LocalDate.now())) {
                errors.append("- La date de naissance ne peut pas être dans le futur\n");
                dateNaissancePicker.setStyle("-fx-border-color: red;");
            } else {
                dateNaissancePicker.setStyle("");
            }
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Erreur de validation",
                    "Veuillez corriger les erreurs suivantes :\n" + errors.toString());
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/com/userapp/view/styles.css").toExternalForm()
        );
        dialogPane.getStyleClass().add("custom-alert");

        alert.showAndWait();
    }

    @FXML
    private void handleRefreshData() {
        loadUserData();
        showAlert(Alert.AlertType.INFORMATION, "Actualisation", "Données actualisées avec succès!");
    }

    @FXML
    private void handleDeleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer votre compte");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer votre compte ? Cette action est irréversible.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = MyDB.getConnection()) {
                String query = "DELETE FROM user WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, currentUser.getId());

                int result = stmt.executeUpdate();

                if (result > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre compte a été supprimé.");
                    handleLogout();
                }

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression: " + e.getMessage());
            }
        }
    }
}