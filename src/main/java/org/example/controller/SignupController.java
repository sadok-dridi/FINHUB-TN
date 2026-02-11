package org.example.controller;


import org.example.utils.MyDB;
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

public class SignupController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private TextField mailField;
    @FXML private TextField numTelField;
    @FXML private PasswordField motDePasseField;
    @FXML private PasswordField confirmMotDePasseField;
    @FXML private ImageView profileImageView;
    @FXML private Button uploadImageButton;
    @FXML private Button signupButton;
    @FXML private Button cancelButton;
    @FXML private Label errorLabel;
    @FXML private Hyperlink loginLink;

    private String selectedImagePath;

    @FXML
    private void initialize() {
        // Ajouter un écouteur pour validation en temps réel
        mailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Quand le champ perd le focus
                validateEmail();
            }
        });
    }

    @FXML
    private void handleSignup() {
        if (!validateFields()) {
            return;
        }

        try {
            Connection conn = MyDB.getConnection();

            // Vérifier si l'email existe déjà
            String checkQuery = "SELECT COUNT(*) FROM user WHERE mail = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, mailField.getText());
            ResultSet rs = checkStmt.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                errorLabel.setText("Cet email est déjà utilisé. Veuillez en choisir un autre.");
                mailField.setStyle("-fx-border-color: red;");
                return;
            }

            // Insérer le nouvel utilisateur
            String insertQuery = "INSERT INTO user (nom, prenom, date_naissance, mail, num_tel, mot_de_passe, image, role, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, nomField.getText().trim());
            insertStmt.setString(2, prenomField.getText().trim());
            insertStmt.setDate(3, dateNaissancePicker.getValue() != null ?
                    Date.valueOf(dateNaissancePicker.getValue()) : null);
            insertStmt.setString(4, mailField.getText().trim().toLowerCase());
            insertStmt.setString(5, numTelField.getText().trim());
            insertStmt.setString(6, org.example.utils.PasswordUtils.hashPassword(motDePasseField.getText()));
            insertStmt.setString(7, selectedImagePath);
            insertStmt.setString(8, org.example.model.User.Role.USER.name()); // Toujours USER pour l'inscription
            insertStmt.setString(9, org.example.model.User.Status.ACTIF.name()); // Compte actif par défaut

            int result = insertStmt.executeUpdate();

            if (result > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Compte créé avec succès! Vous pouvez maintenant vous connecter.");
                handleLoginLink();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Erreur lors de la création du compte: " + e.getMessage());
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                selectedImagePath = selectedFile.getAbsolutePath();
                Image image = new Image(selectedFile.toURI().toString());
                profileImageView.setImage(image);
                errorLabel.setText(""); // Effacer les messages d'erreur
            } catch (Exception e) {
                errorLabel.setText("Erreur lors du chargement de l'image");
            }
        }
    }

    @FXML
    private void handleCancel() {
        handleLoginLink();
    }

    @FXML
    private void handleLoginLink() {
        try {
            Stage stage = (Stage) signupButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validateFields() {
        StringBuilder errors = new StringBuilder();

        // Validation du nom
        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            errors.append("- Le nom est obligatoire\n");
            nomField.setStyle("-fx-border-color: red;");
        } else {
            nomField.setStyle("");
        }

        // Validation du prénom
        if (prenomField.getText() == null || prenomField.getText().trim().isEmpty()) {
            errors.append("- Le prénom est obligatoire\n");
            prenomField.setStyle("-fx-border-color: red;");
        } else {
            prenomField.setStyle("");
        }

        // Validation de l'email
        if (!validateEmail()) {
            errors.append("- L'email n'est pas valide\n");
        }

        // Validation du téléphone (optionnel mais format valide)
        if (numTelField.getText() != null && !numTelField.getText().trim().isEmpty()) {
            if (!numTelField.getText().matches("^(\\+?[0-9]{8,15})$")) {
                errors.append("- Le numéro de téléphone n'est pas valide\n");
                numTelField.setStyle("-fx-border-color: red;");
            } else {
                numTelField.setStyle("");
            }
        }

        // Validation du mot de passe
        if (motDePasseField.getText() == null || motDePasseField.getText().isEmpty()) {
            errors.append("- Le mot de passe est obligatoire\n");
            motDePasseField.setStyle("-fx-border-color: red;");
        } else if (motDePasseField.getText().length() < 6) {
            errors.append("- Le mot de passe doit contenir au moins 6 caractères\n");
            motDePasseField.setStyle("-fx-border-color: red;");
        } else {
            motDePasseField.setStyle("");
        }

        // Validation de la confirmation du mot de passe
        if (confirmMotDePasseField.getText() == null || confirmMotDePasseField.getText().isEmpty()) {
            errors.append("- Veuillez confirmer le mot de passe\n");
            confirmMotDePasseField.setStyle("-fx-border-color: red;");
        } else if (!motDePasseField.getText().equals(confirmMotDePasseField.getText())) {
            errors.append("- Les mots de passe ne correspondent pas\n");
            confirmMotDePasseField.setStyle("-fx-border-color: red;");
            motDePasseField.setStyle("-fx-border-color: red;");
        } else {
            confirmMotDePasseField.setStyle("");
        }

        // Validation de la date de naissance (optionnelle mais si présente, doit être valide)
        if (dateNaissancePicker.getValue() != null) {
            if (dateNaissancePicker.getValue().isAfter(java.time.LocalDate.now())) {
                errors.append("- La date de naissance ne peut pas être dans le futur\n");
                dateNaissancePicker.setStyle("-fx-border-color: red;");
            } else {
                dateNaissancePicker.setStyle("");
            }
        }

        if (errors.length() > 0) {
            errorLabel.setText("Veuillez corriger les erreurs suivantes :\n" + errors.toString());
            return false;
        }

        return true;
    }

    private boolean validateEmail() {
        String email = mailField.getText();
        if (email == null || email.trim().isEmpty()) {
            mailField.setStyle("-fx-border-color: red;");
            return false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        if (!email.matches(emailRegex)) {
            mailField.setStyle("-fx-border-color: red;");
            return false;
        }

        mailField.setStyle("");
        return true;
    }

    @FXML
    private void clearForm() {
        nomField.clear();
        prenomField.clear();
        dateNaissancePicker.setValue(null);
        mailField.clear();
        numTelField.clear();
        motDePasseField.clear();
        confirmMotDePasseField.clear();
        profileImageView.setImage(null);
        selectedImagePath = null;
        errorLabel.setText("");

        // Réinitialiser les styles
        nomField.setStyle("");
        prenomField.setStyle("");
        mailField.setStyle("");
        numTelField.setStyle("");
        motDePasseField.setStyle("");
        confirmMotDePasseField.setStyle("");
        dateNaissancePicker.setStyle("");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}