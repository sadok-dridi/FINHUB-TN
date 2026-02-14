package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import tn.finhub.util.ApiClient;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.SessionManager;
<<<<<<< HEAD
import tn.finhub.service.WalletService;
import tn.finhub.service.MailService;
import tn.finhub.service.UserService;
import tn.finhub.dao.FinancialProfileDAO;
import tn.finhub.dao.impl.FinancialProfileDAOImpl;
import tn.finhub.model.User;
import javafx.fxml.FXMLLoader;
=======
import tn.finhub.util.MailClient;
import tn.finhub.model.User;
import javafx.fxml.FXMLLoader;
import tn.finhub.util.LanguageManager;
>>>>>>> cd680ce (crud+controle de saisie)

public class MigrateWalletController {

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button migrateBtn;
    @FXML
    private Label errorLabel;

    // State management
    private boolean waitingForVerification = false;
<<<<<<< HEAD
    private WalletService walletService = new WalletService();
    private UserService userService = new UserService();
    private FinancialProfileDAO profileDAO = new FinancialProfileDAOImpl();
=======
    private tn.finhub.model.WalletModel walletModel = new tn.finhub.model.WalletModel();
    private tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
    private tn.finhub.model.FinancialProfileModel profileModel = new tn.finhub.model.FinancialProfileModel();
>>>>>>> cd680ce (crud+controle de saisie)

    @FXML
    public void handleMigration() {
        String name = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String pwd = passwordField.getText();

        if (name.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
            errorLabel.setText("All fields are required.");
            return;
        }

        if (waitingForVerification) {
            handleFinalizeMigration(email, pwd);
            return;
        }

        migrateBtn.setDisable(true);
        migrateBtn.setText("Processing...");
        errorLabel.setText("");

        new Thread(() -> {
            try {
                // 1. Signup only (capturing verification link)
                String verificationLink = ApiClient.signup(name, email, pwd);

                // 2. Send Verification Email
                System.out.println("Sending verification email to: " + email);
<<<<<<< HEAD
                MailService.sendVerificationEmail(email, verificationLink);
=======
                MailClient.sendVerificationEmail(email, verificationLink);
>>>>>>> cd680ce (crud+controle de saisie)
                System.out.println("Verification Link: " + verificationLink); // Log for debugging

                // 3. Switch UI to verification mode
                Platform.runLater(() -> {
                    waitingForVerification = true;
                    migrateBtn.setDisable(false);
                    migrateBtn.setText("I Have Verified My Email");
                    // Disable inputs
                    fullNameField.setDisable(true);
                    emailField.setDisable(true);
                    passwordField.setDisable(true);

                    DialogUtil.showInfo("Verification Email Sent",
                            "We have sent a verification link to " + email
                                    + ".\n\nPlease check your inbox and verify your account, then click the button below.");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    errorLabel.setText("Signup Error: " + e.getMessage());
                    migrateBtn.setDisable(false);
                    migrateBtn.setText("Create Account & Migrate Wallet");
                });
            }
        }).start();
    }

    private void handleFinalizeMigration(String email, String password) {
        migrateBtn.setDisable(true);
        migrateBtn.setText("Migrating...");
        errorLabel.setText("");

        new Thread(() -> {
            try {
                // 4. Login to get ID (now that email is verified)
                User newUser = ApiClient.login(email, password);
                if (newUser == null || newUser.getId() == 0) {
                    throw new RuntimeException("Failed to retrieve new user ID.");
                }

                // IMPORTANT: Save user to local DB *before* transferring wallet
                // to satisfy Foreign Key constraints.
<<<<<<< HEAD
                userService.saveUser(newUser);

                // 5. Transfer Wallet
                int currentAdminId = SessionManager.getUserId();
                walletService.transferWalletToUser(currentAdminId, newUser.getId());

                // 5b. Transfer Financial Profile
                profileDAO.updateUserId(currentAdminId, newUser.getId());
=======
                userModel.insert(newUser);

                // 5. Transfer Wallet
                int currentAdminId = SessionManager.getUserId();
                // Find admin wallet logic customized here:
                // We assume current session user is the one migrating FROM.
                tn.finhub.model.Wallet adminWallet = walletModel.findByUserId(currentAdminId);
                if (adminWallet != null) {
                    walletModel.updateUserId(adminWallet.getId(), newUser.getId());
                }

                // 5b. Transfer Financial Profile
                profileModel.updateUserId(currentAdminId, newUser.getId());
>>>>>>> cd680ce (crud+controle de saisie)

                // 6. Success
                Platform.runLater(() -> {
                    DialogUtil.showInfo("Migration Successful",
                            "Your wallet and profile have been transferred to " + newUser.getEmail()
                                    + ".\n\nYou may now access the Admin Dashboard.");
                    navigateToAdminDashboard();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    errorLabel
                            .setText("Migration Failed: " + e.getMessage() + "\nIf you verified, try clicking again.");
                    migrateBtn.setDisable(false);
                    migrateBtn.setText("I Have Verified My Email");
                });
            }
        }).start();
    }

    private void navigateToAdminDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_users.fxml"));
<<<<<<< HEAD
=======
            loader.setResources(LanguageManager.getInstance().getResourceBundle());
>>>>>>> cd680ce (crud+controle de saisie)
            javafx.scene.Parent view = loader.load();

            javafx.scene.Scene scene = migrateBtn.getScene();
            // Try to find content area, else replace root
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) scene.lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            } else {
                scene.setRoot(view);
            }

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Navigation Error", "Could not load Admin Dashboard.");
        }
    }
}
