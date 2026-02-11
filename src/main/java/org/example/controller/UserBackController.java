package org.example.controller;

import org.example.model.User;
import org.example.utils.SessionManager;
import org.example.utils.MyDB;
import org.example.utils.PasswordUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.*;

public class UserBackController {

    @FXML private Label adminWelcomeLabel;

    // Tableau des utilisateurs
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> nomColumn;
    @FXML private TableColumn<User, String> prenomColumn;
    @FXML private TableColumn<User, String> mailColumn;
    @FXML private TableColumn<User, String> numTelColumn;
    @FXML private TableColumn<User, User.Role> roleColumn;
    @FXML private TableColumn<User, User.Status> statusColumn;

    // Champs du formulaire
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private TextField mailField;
    @FXML private TextField numTelField;
    @FXML private PasswordField motDePasseField;
    @FXML private ComboBox<User.Role> roleCombo;
    @FXML private ComboBox<User.Status> statusCombo;
    @FXML private ImageView profileImageView;
    @FXML private TextField searchField;

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label adminUsersLabel;

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private String selectedImagePath;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        adminWelcomeLabel.setText("Administrateur: " + currentUser.getPrenom() + " " + currentUser.getNom());

        setupTableColumns();
        loadRolesAndStatuses();
        loadUserData();
        loadStatistics();

        // Écouter la sélection dans le tableau
        userTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        showUserDetails(newSelection);
                    }
                });
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        mailColumn.setCellValueFactory(new PropertyValueFactory<>("mail"));
        numTelColumn.setCellValueFactory(new PropertyValueFactory<>("numTel"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadRolesAndStatuses() {
        roleCombo.setItems(FXCollections.observableArrayList(User.Role.values()));
        statusCombo.setItems(FXCollections.observableArrayList(User.Status.values()));
    }

    private void loadUserData() {
        userList.clear();

        try {
            Connection conn = MyDB.getConnection();
            String query = "SELECT * FROM user ORDER BY id DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                userList.add(extractUserFromResultSet(rs));
            }

            userTable.setItems(userList);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les utilisateurs: " + e.getMessage());
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

    private void showUserDetails(User user) {
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        dateNaissancePicker.setValue(user.getDateNaissance());
        mailField.setText(user.getMail());
        numTelField.setText(user.getNumTel());
        roleCombo.setValue(user.getRole());
        statusCombo.setValue(user.getStatus());
        motDePasseField.clear();

        if (user.getImage() != null && !user.getImage().isEmpty()) {
            try {
                profileImageView.setImage(new Image(new File(user.getImage()).toURI().toString()));
                selectedImagePath = user.getImage();
            } catch (Exception e) {
                profileImageView.setImage(null);
            }
        } else {
            profileImageView.setImage(null);
        }
    }

    @FXML
    private void handleAddUser() {
        if (!validateUserFields()) {
            return;
        }

        try {
            Connection conn = MyDB.getConnection();
            String query = "INSERT INTO user (nom, prenom, date_naissance, mail, num_tel, mot_de_passe, image, role, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, nomField.getText());
            stmt.setString(2, prenomField.getText());
            stmt.setDate(3, dateNaissancePicker.getValue() != null ?
                    Date.valueOf(dateNaissancePicker.getValue()) : null);
            stmt.setString(4, mailField.getText());
            stmt.setString(5, numTelField.getText());
            stmt.setString(6, PasswordUtils.hashPassword(motDePasseField.getText()));
            stmt.setString(7, selectedImagePath);
            stmt.setString(8, roleCombo.getValue() != null ? roleCombo.getValue().name() : User.Role.USER.name());
            stmt.setString(9, statusCombo.getValue() != null ? statusCombo.getValue().name() : User.Status.ACTIF.name());

            int result = stmt.executeUpdate();

            if (result > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur ajouté avec succès!");
                clearForm();
                loadUserData();
                loadStatistics();
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Cet email est déjà utilisé!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'ajout: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdateUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un utilisateur");
            return;
        }

        if (!validateUserFields()) {
            return;
        }

        try {
            Connection conn = MyDB.getConnection();
            String query = "UPDATE user SET nom = ?, prenom = ?, date_naissance = ?, mail = ?, num_tel = ?, " +
                    "image = ?, role = ?, status = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, nomField.getText());
            stmt.setString(2, prenomField.getText());
            stmt.setDate(3, dateNaissancePicker.getValue() != null ?
                    Date.valueOf(dateNaissancePicker.getValue()) : null);
            stmt.setString(4, mailField.getText());
            stmt.setString(5, numTelField.getText());
            stmt.setString(6, selectedImagePath);
            stmt.setString(7, roleCombo.getValue().name());
            stmt.setString(8, statusCombo.getValue().name());
            stmt.setInt(9, selectedUser.getId());

            int result = stmt.executeUpdate();

            if (result > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur mis à jour avec succès!");
                loadUserData();
                loadStatistics();
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la mise à jour: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un utilisateur");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'utilisateur");
        confirm.setContentText("Voulez-vous vraiment supprimer " + selectedUser.getNomComplet() + " ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                Connection conn = MyDB.getConnection();
                String query = "DELETE FROM user WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, selectedUser.getId());

                int result = stmt.executeUpdate();

                if (result > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur supprimé!");
                    clearForm();
                    loadUserData();
                    loadStatistics();
                }

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase();

        if (searchTerm.isEmpty()) {
            userTable.setItems(userList);
            return;
        }

        ObservableList<User> filteredList = FXCollections.observableArrayList();

        for (User user : userList) {
            if (user.getNom().toLowerCase().contains(searchTerm) ||
                    user.getPrenom().toLowerCase().contains(searchTerm) ||
                    user.getMail().toLowerCase().contains(searchTerm) ||
                    user.getNumTel().contains(searchTerm)) {
                filteredList.add(user);
            }
        }

        userTable.setItems(filteredList);
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            profileImageView.setImage(new Image(selectedFile.toURI().toString()));
        }
    }

    @FXML
    private void handleClearForm() {
        clearForm();
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();

        try {
            Stage stage = (Stage) adminWelcomeLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearForm() {
        nomField.clear();
        prenomField.clear();
        dateNaissancePicker.setValue(null);
        mailField.clear();
        numTelField.clear();
        motDePasseField.clear();
        roleCombo.setValue(null);
        statusCombo.setValue(null);
        profileImageView.setImage(null);
        selectedImagePath = null;
        userTable.getSelectionModel().clearSelection();
    }

    private boolean validateUserFields() {
        if (nomField.getText().isEmpty() || prenomField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le nom et le prénom sont obligatoires");
            return false;
        }

        if (mailField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "L'email est obligatoire");
            return false;
        }

        if (!mailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Format d'email invalide");
            return false;
        }

        if (userTable.getSelectionModel().getSelectedItem() == null &&
                motDePasseField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le mot de passe est obligatoire pour un nouvel utilisateur");
            return false;
        }

        return true;
    }

    private void loadStatistics() {
        try {
            Connection conn = MyDB.getConnection();

            // Total utilisateurs
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM user");
            if (rs.next()) {
                totalUsersLabel.setText("Total: " + rs.getInt("total"));
            }

            // Utilisateurs actifs
            rs = stmt.executeQuery("SELECT COUNT(*) as actif FROM user WHERE status = 'ACTIF'");
            if (rs.next()) {
                activeUsersLabel.setText("Actifs: " + rs.getInt("actif"));
            }

            // Administrateurs
            rs = stmt.executeQuery("SELECT COUNT(*) as admin FROM user WHERE role = 'ADMIN'");
            if (rs.next()) {
                adminUsersLabel.setText("Admins: " + rs.getInt("admin"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
