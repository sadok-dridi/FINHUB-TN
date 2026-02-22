package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.finhub.model.User;

import tn.finhub.util.CloudinaryService;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;
import tn.finhub.util.ViewUtils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class UserProfileController {

    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label trustScoreLabel;

    @FXML
    private javafx.scene.layout.HBox phoneViewBox;
    @FXML
    private javafx.scene.layout.HBox phoneEditBox;
    @FXML
    private Label phoneDisplayLabel;

    @FXML
    private TextField phoneField;
    @FXML
    private ImageView profileImageView;

    @FXML
    private Label statusLabel;

    private User currentUser;
    private tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
    private tn.finhub.model.FinancialProfileModel financialModel = new tn.finhub.model.FinancialProfileModel();
    @FXML
    private TextField incomeField;
    @FXML
    private TextField expensesField;
    @FXML
    private TextField savingsField;
    @FXML
    private ComboBox<String> riskBox;

    @FXML
    private ComboBox<String> languageBox;

    @FXML
    private TextField apiKeyField;

    @FXML
    private Button kycButton;
    @FXML
    private Label kycStatusLabel;

    @FXML
    public void initialize() {
        setupComboBoxes();
        setupPhoneField();
        loadUserData();
        loadFinancialData();
        loadSettings();
    }

    private void setupPhoneField() {
        // Force "+216 " prefix and limit to 8 numbers
        phoneField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            if (!newText.startsWith("+216 ")) {
                return null; // Reject change if it removes prefix
            }

            // Allow only digits after prefix
            String suffix = newText.substring(5);
            if (!suffix.matches("\\d*")) {
                return null;
            }

            // Limit to 8 digits (Total length 5 + 8 = 13)
            if (newText.length() > 13) {
                return null;
            }

            return change;
        }));

        // Ensure default text if empty
        if (phoneField.getText().isEmpty()) {
            phoneField.setText("+216 ");
        }
    }

    private void setupComboBoxes() {
        riskBox.getItems().addAll("LOW", "MEDIUM", "HIGH");
        // Use LanguageManager for consistency
        languageBox.getItems().addAll(tn.finhub.util.LanguageManager.getAvailableLanguages());

        // Set current language
        String current = tn.finhub.util.LanguageManager.getInstance().getCurrentLanguageName();
        languageBox.setValue(current);
    }

    private void loadUserData() {
        currentUser = UserSession.getInstance().getUser();
        if (currentUser != null) {
            // Refresh from DB to get latest (phone, photo)
            currentUser = userModel.findById(currentUser.getId());
            // Update session with fresh data
            UserSession.getInstance().setUser(currentUser);

            nameLabel.setText(currentUser.getFullName());
            emailLabel.setText(currentUser.getEmail());
            roleLabel.setText(currentUser.getRole());
            if (trustScoreLabel != null) {
                trustScoreLabel.setText(String.valueOf(currentUser.getTrustScore()));
            }

            if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
                phoneField.setText(currentUser.getPhoneNumber());
                phoneDisplayLabel.setText(formatPhoneNumber(currentUser.getPhoneNumber()));
            } else {
                phoneField.setText("+216 ");
                phoneDisplayLabel.setText("No phone number set");
            }
            // Default to View Mode
            if (phoneViewBox != null && phoneEditBox != null) {
                phoneViewBox.setVisible(true);
                phoneEditBox.setVisible(false);
            }

            if (currentUser.getProfilePhotoUrl() != null && !currentUser.getProfilePhotoUrl().isEmpty()) {
                try {
                    profileImageView.setImage(new Image(currentUser.getProfilePhotoUrl()));
                } catch (Exception e) {
                    System.err.println("Failed to load profile image: " + e.getMessage());
                }
            }

            checkKYCStatus();
        }
    }

    private void checkKYCStatus() {
        tn.finhub.model.KYCModel kycModel = new tn.finhub.model.KYCModel();
        java.util.List<tn.finhub.model.KYCRequest> requests = kycModel.findByUserId(currentUser.getId());

        System.out.println("KYC DEBUG: Checking for User ID: " + currentUser.getId());
        System.out.println("KYC DEBUG: Found " + requests.size() + " requests.");
        for (tn.finhub.model.KYCRequest r : requests) {
            System.out.println("KYC DEBUG: Request ID=" + r.getRequestId() + ", Status=" + r.getStatus());
        }

        // Find if there's any PENDING, APPROVED, or REJECTED request
        boolean isPending = requests.stream().anyMatch(r -> "PENDING".equalsIgnoreCase(r.getStatus()));
        boolean isVerified = requests.stream().anyMatch(r -> "APPROVED".equalsIgnoreCase(r.getStatus()));
        boolean isRejected = requests.stream().anyMatch(r -> "REJECTED".equalsIgnoreCase(r.getStatus()));

        System.out.println(
                "KYC DEBUG: isPending=" + isPending + ", isVerified=" + isVerified + ", isRejected=" + isRejected);

        if (kycButton != null) {
            javafx.scene.shape.SVGPath icon = null;
            if (kycButton.getGraphic() instanceof javafx.scene.shape.SVGPath) {
                icon = (javafx.scene.shape.SVGPath) kycButton.getGraphic();
            }

            if (isVerified) {
                kycButton.setDisable(true); // Keep disabled but look "Good"
                kycButton.setText("Identity Verified");
                kycButton.setStyle(
                        "-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #10B981; -fx-border-color: #10B981; -fx-border-radius: 12px; -fx-opacity: 1.0; -fx-font-weight: bold; -fx-border-width: 1.5;");
                if (icon != null)
                    icon.setFill(javafx.scene.paint.Color.valueOf("#10B981")); // Emerald Icon

            } else if (isPending) {
                kycButton.setDisable(true);
                kycButton.setText("Pending Verification");
                kycButton.setStyle(
                        "-fx-background-color: rgba(245, 158, 11, 0.1); -fx-text-fill: #F59E0B; -fx-border-color: #F59E0B; -fx-border-radius: 12px; -fx-opacity: 1.0; -fx-font-weight: bold;");
                if (icon != null)
                    icon.setFill(javafx.scene.paint.Color.valueOf("#F59E0B")); // Orange Icon

            } else if (isRejected) {
                kycButton.setDisable(false); // Enable so they can retry
                kycButton.setText("Verification Rejected");
                kycButton.setStyle(
                        "-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #EF4444; -fx-border-color: #EF4444; -fx-border-radius: 12px; -fx-opacity: 1.0; -fx-font-weight: bold;");
                if (icon != null)
                    icon.setFill(javafx.scene.paint.Color.valueOf("#EF4444")); // Red Icon

            } else {
                kycButton.setDisable(false);
                kycButton.setText("Verify Identity (KYC)");
                kycButton.setStyle(""); // Reset to default CSS class style
                if (icon != null)
                    icon.setFill(javafx.scene.paint.Color.WHITE); // Default White
            }
        }

        if (kycStatusLabel != null) {
            if (isPending) {
                kycStatusLabel.setText("Your verification request is under review.");
                kycStatusLabel.setVisible(true);
                kycStatusLabel.setStyle("-fx-text-fill: #F59E0B;");
            } else if (isVerified) {
                kycStatusLabel.setText("Your identity has been verified.");
                kycStatusLabel.setVisible(true);
                kycStatusLabel.setStyle("-fx-text-fill: #10B981;");
            } else if (isRejected) {
                kycStatusLabel.setText("Your previous request was rejected. Click to try again.");
                kycStatusLabel.setVisible(true);
                kycStatusLabel.setStyle("-fx-text-fill: #EF4444;");
            } else {
                kycStatusLabel.setVisible(false);
            }
        }
    }

    private void loadFinancialData() {
        if (currentUser == null)
            return;

        financialModel.ensureProfile(currentUser.getId());
        tn.finhub.model.FinancialProfile profile = financialModel.findByUserId(currentUser.getId());

        if (profile != null) {
            incomeField.setText(String.valueOf(profile.getMonthlyIncome()));
            expensesField.setText(String.valueOf(profile.getMonthlyExpenses()));
            savingsField.setText(String.valueOf(profile.getSavingsGoal()));
            riskBox.setValue(profile.getRiskTolerance());
        }
    }

    private void loadSettings() {
        // Mock Settings Loading - In a real app, this would come from a Preferences
        // service
        // languageBox.setValue("English"); // REMOVED to avoid overwriting current
        // language
        // apiKeyField.setText("..."); // Keep empty for security or load masked
    }

    @FXML
    private void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File selectedFile = fileChooser.showOpenDialog(nameLabel.getScene().getWindow());

        if (selectedFile != null) {
            try {
                // Open Cropper Dialog
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/image_cropper.fxml"));
                Parent root = loader.load();

                ImageCropperController cropper = loader.getController();

                Stage cropperStage = new Stage();
                cropperStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
                Scene cropperScene = new Scene(root);
                cropperScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                cropperStage.setScene(cropperScene);
                cropperStage.initModality(Modality.WINDOW_MODAL);
                cropperStage.initOwner(nameLabel.getScene().getWindow());

                cropper.setStage(cropperStage);
                cropper.setImage(selectedFile);

                cropperStage.showAndWait();

                // Get Result
                File croppedFile = cropper.getCroppedFile();

                if (croppedFile != null) {
                    // Upload Cropped File to Cloudinary
                    String url = CloudinaryService.upload(croppedFile, "profiles");

                    // Update Model
                    currentUser.setProfilePhotoUrl(url);
                    userModel.updateProfile(currentUser); // Save URL to DB

                    // Update UI
                    profileImageView.setImage(new Image(url));

                    // Show Success Dialog
                    DialogUtil.showInfo("Success", "Profile photo updated successfully!");

                    // Sync Sidebar
                    UserDashboardController.refreshProfile();

                    // Cleanup Temp File
                    croppedFile.delete();
                }

            } catch (Exception e) {
                e.printStackTrace();
                showStatus("Failed to upload photo: " + e.getMessage(), true);
            }
        }
    }

    @FXML
    private void handleDeletePhoto() {
        if (currentUser != null && currentUser.getProfilePhotoUrl() != null) {
            // Confirmation Dialog
            boolean confirmed = DialogUtil.showConfirmation(
                    "Delete Photo",
                    "Are you sure you want to remove your profile photo?");

            if (confirmed) {
                currentUser.setProfilePhotoUrl(null);
                userModel.updateProfile(currentUser);
                UserSession.getInstance().setUser(currentUser);

                // Reset to default view (circle with icon) by clearing image
                profileImageView.setImage(null);

                // Show Success Dialog
                DialogUtil.showInfo("Success", "Profile photo deleted successfully.");

                // Sync Sidebar
                UserDashboardController.refreshProfile();
            }
        }
    }

    @FXML
    private void handleEditPhone() {
        if (phoneViewBox != null && phoneEditBox != null) {
            phoneViewBox.setVisible(false);
            phoneEditBox.setVisible(true);
            phoneField.requestFocus();
        }
    }

    @FXML
    private void handleSavePhone() {
        if (currentUser != null) {
            String newPhone = phoneField.getText();
            if (newPhone != null && !newPhone.trim().isEmpty()) {
                if (newPhone.length() < 13) { // +216 + 8 digits
                    showStatus("Phone number must have 8 digits.", true);
                    return;
                }
                currentUser.setPhoneNumber(newPhone);
                userModel.updateProfile(currentUser);
                UserSession.getInstance().setUser(currentUser);

                // Update Label and Switch to View Mode
                phoneDisplayLabel.setText(formatPhoneNumber(newPhone));
                if (phoneViewBox != null && phoneEditBox != null) {
                    phoneViewBox.setVisible(true);
                    phoneEditBox.setVisible(false);
                }

                showStatus("Phone number saved.", false);
            } else {
                showStatus("Phone number cannot be empty.", true);
            }
        }
    }

    @FXML
    private void handleDeletePhone() {
        if (currentUser != null) {
            currentUser.setPhoneNumber(null);
            phoneField.setText("+216 ");
            userModel.updateProfile(currentUser);
            UserSession.getInstance().setUser(currentUser);

            // Update Label and Switch to View Mode
            phoneDisplayLabel.setText("No phone number set");
            if (phoneViewBox != null && phoneEditBox != null) {
                phoneViewBox.setVisible(true);
                phoneEditBox.setVisible(false);
            }

            showStatus("Phone number deleted.", false);
        }
    }

    @FXML
    private void handleUpdateProfile() {
        try {
            // Update User Personal Info
            // Update User Personal Info
            String newPhone = phoneField.getText();
            if (newPhone != null && newPhone.length() == 13) {
                currentUser.setPhoneNumber(newPhone);
                userModel.updateProfile(currentUser);
                UserSession.getInstance().setUser(currentUser);
            } else if (newPhone != null && !newPhone.equals("+216 ")) {
                showStatus("Invalid phone number format (must be 8 digits)", true);
                return;
            }

            // Update Financial Profile
            tn.finhub.model.FinancialProfile profile = financialModel.findByUserId(currentUser.getId());
            if (profile != null) {
                profile.setMonthlyIncome(Double.parseDouble(incomeField.getText()));
                profile.setMonthlyExpenses(Double.parseDouble(expensesField.getText()));
                profile.setSavingsGoal(Double.parseDouble(savingsField.getText()));
                profile.setRiskTolerance(riskBox.getValue());
                profile.setProfileCompleted(true);

                financialModel.update(profile);
            }

            showStatus("Profile updated successfully!", false);
        } catch (NumberFormatException e) {
            showStatus("Invalid number format in financial fields!", true);
        } catch (Exception e) {
            showStatus("Update failed: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleVerifyIdentity() {
        ViewUtils.loadContent(
                (javafx.scene.layout.StackPane) nameLabel.getScene().getRoot().lookup("#dashboardContent"),
                "/view/kyc_submission.fxml");
    }

    @FXML
    private void handleSaveSettings() {
        String selectedLanguage = languageBox.getValue();
        if (selectedLanguage != null) {
            String currentLang = tn.finhub.util.LanguageManager.getInstance().getCurrentLanguageName();

            if (!selectedLanguage.equals(currentLang)) {
                // Change Language
                if (selectedLanguage.equals("Français")) {
                    tn.finhub.util.LanguageManager.getInstance().setLanguage(tn.finhub.util.LanguageManager.FRENCH);
                } else {
                    tn.finhub.util.LanguageManager.getInstance().setLanguage(tn.finhub.util.LanguageManager.ENGLISH);
                }

                // Reload View to apply changes
                try {
                    // Get the MAIN content area (where UserDashboard lives)
                    javafx.scene.layout.StackPane mainContent = (javafx.scene.layout.StackPane) nameLabel.getScene()
                            .lookup("#contentArea");

                    if (mainContent != null) {
                        // Load the entire UserDashboard (this brings back the Sidebar with new
                        // language)
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/user_dashboard.fxml"));
                        loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
                        Parent dashboardRoot = loader.load();

                        // Navigate to Profile within the new Dashboard
                        tn.finhub.controller.UserDashboardController dashboardCtrl = loader.getController();
                        dashboardCtrl.handleSettings();

                        // Replace content
                        mainContent.getChildren().setAll(dashboardRoot);

                        // Fade In Effect
                        dashboardRoot.setOpacity(0);
                        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                                javafx.util.Duration.millis(300), dashboardRoot);
                        fade.setFromValue(0);
                        fade.setToValue(1);
                        fade.play();
                    }

                    showStatus("Language changed to " + selectedLanguage, false);

                } catch (Exception e) {
                    e.printStackTrace();
                    showStatus("Error reloading view: " + e.getMessage(), true);
                }
            } else {
                showStatus("Settings saved (No changes)", false);
            }
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        statusLabel.setVisible(true);

        // Hide after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            javafx.application.Platform.runLater(() -> statusLabel.setVisible(false));
        }).start();
    }

    private String formatPhoneNumber(String raw) {
        if (raw == null || !raw.startsWith("+216 "))
            return raw;
        String digits = raw.substring(5).trim();
        if (digits.length() == 8) {
            // Format: +216 XX XXX XXX
            return "+216 " + digits.substring(0, 2) + " " + digits.substring(2, 5) + " " + digits.substring(5);
        }
        return raw;
    }
}
