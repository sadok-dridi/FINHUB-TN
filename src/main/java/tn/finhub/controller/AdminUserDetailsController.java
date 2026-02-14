package tn.finhub.controller;

<<<<<<< HEAD
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.finhub.model.LedgerAuditLog;
import tn.finhub.model.LedgerFlag;
import tn.finhub.model.User;
import tn.finhub.model.Wallet;
import tn.finhub.service.WalletService;

import java.util.List;

public class AdminUserDetailsController {

=======
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import tn.finhub.model.User;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.ViewUtils;

public class AdminUserDetailsController {

    // Personal Information fields
>>>>>>> cd680ce (crud+controle de saisie)
    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label idLabel;

<<<<<<< HEAD
=======
    // Wallet Status fields
>>>>>>> cd680ce (crud+controle de saisie)
    @FXML
    private Label balanceLabel;
    @FXML
    private Label currencyLabel;
    @FXML
    private Label walletStatusLabel;

<<<<<<< HEAD
    @FXML
    private VBox ledgerAlertBox;
    @FXML
    private Label ledgerAlertMessage;
    @FXML
    private VBox auditLogsContainer;

    private User user;
    private WalletService walletService = new WalletService();

    public void setUser(User user) {
        this.user = user;
        updateUI();
    }

    private void updateUI() {
        if (user == null)
            return;

        // User Info
        nameLabel.setText(user.getFullName());
        emailLabel.setText(user.getEmail());
        roleLabel.setText(user.getRole());
        idLabel.setText("User ID: " + user.getId());

        // Wallet Info
        try {
            Wallet wallet = walletService.getWallet(user.getId());
            if (wallet != null) {
                balanceLabel.setText(String.format("%.2f", wallet.getBalance()));
                currencyLabel.setText(wallet.getCurrency());
                walletStatusLabel.setText(wallet.getStatus());

                if ("FROZEN".equalsIgnoreCase(wallet.getStatus())) {
                    walletStatusLabel.setStyle("-fx-text-fill: -color-error; -fx-font-weight: bold;");
                } else {
                    walletStatusLabel.setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold;");
                }

                // Update Freeze Button Text
                if (freezeBtn != null) {
                    if ("FROZEN".equalsIgnoreCase(wallet.getStatus())) {
                        freezeBtn.setText("Unfreeze Wallet");
                        freezeBtn.getStyleClass().removeAll("button-warning");
                        freezeBtn.getStyleClass().add("button-success");
                    } else {
                        freezeBtn.setText("Freeze Wallet");
                        freezeBtn.getStyleClass().removeAll("button-success");
                        freezeBtn.getStyleClass().add("button-warning");
                    }
                }

                // Ledger Info
                loadLedgerInfo(wallet.getId());

            } else {
                balanceLabel.setText("N/A");
                currencyLabel.setText("");
                walletStatusLabel.setText("No Wallet");
                ledgerAlertBox.setVisible(false);
                ledgerAlertBox.setManaged(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            balanceLabel.setText("Error");
        }
    }

    private void loadLedgerInfo(int walletId) {
        // Check for Flags
        LedgerFlag flag = walletService.getLatestFlag(walletId);
        if (flag != null) {
            ledgerAlertBox.setVisible(true);
            ledgerAlertBox.setManaged(true);
            ledgerAlertMessage.setText("SECURITY ALERT: " + flag.getReason() + "\nFlagged at: " + flag.getFlaggedAt());
        } else {
            ledgerAlertBox.setVisible(false);
            ledgerAlertBox.setManaged(false);
        }

        // Audit Logs (Show last 5 for example)
        List<LedgerAuditLog> logs = walletService.getAuditLogs(walletId);
        auditLogsContainer.getChildren().clear();

        int count = 0;
        for (LedgerAuditLog log : logs) {
            if (count >= 5)
                break;
            Label logLabel = new Label(
                    (log.isVerified() ? "✔" : "❌") + " " + log.getMessage() + " (" + log.getCheckedAt() + ")");
            logLabel.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 11px;");
            auditLogsContainer.getChildren().add(logLabel);
            count++;
=======
    // Buttons
    @FXML
    private Button freezeBtn;

    private User currentUser;
    private final WalletModel walletModel = new WalletModel();

    @FXML
    public void initialize() {
        // Initialization logic if needed
    }

    public void setUser(User user) {
        if (user == null)
            return;

        this.currentUser = user;

        // Set personal information
        nameLabel.setText(user.getFullName() != null ? user.getFullName() : "N/A");
        emailLabel.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        roleLabel.setText(user.getRole() != null ? user.getRole() : "USER");
        idLabel.setText("User ID: " + user.getId());

        // Refresh wallet info
        refreshWalletInfo();
    }

    @FXML
    private void handleViewPortfolio() {
        if (currentUser == null)
            return;

        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameLabel.getScene()
                .lookup("#adminContentArea");
        if (contentArea != null) {
            javafx.fxml.FXMLLoader loader = ViewUtils.loadContent(contentArea, "/view/admin_user_portfolio.fxml");
            if (loader != null) {
                Object controller = loader.getController();
                if (controller instanceof AdminUserPortfolioController portfolioController) {
                    portfolioController.setUser(currentUser);
                }
            }
        }
    }

    private void refreshWalletInfo() {
        if (currentUser == null)
            return;

        // Show loading state
        balanceLabel.setText("Loading...");
        walletStatusLabel.setText("...");
        freezeBtn.setDisable(true);

        // Run DB fetch in background
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            return walletModel.findByUserId(currentUser.getId());
        }).thenAcceptAsync(wallet -> {
            // Update UI on JavaFX thread
            if (wallet != null) {
                balanceLabel.setText(wallet.getBalance().toString());
                currencyLabel.setText(wallet.getCurrency());

                String status = wallet.getStatus();
                walletStatusLabel.setText(status);

                if ("FROZEN".equals(status)) {
                    walletStatusLabel.setStyle("-fx-text-fill: -color-error; -fx-font-weight: bold;");
                    freezeBtn.setText("Unfreeze Wallet");
                    freezeBtn.getStyleClass().removeAll("button-warning");
                    freezeBtn.getStyleClass().add("button-success");
                } else {
                    walletStatusLabel.setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold;");
                    freezeBtn.setText("Freeze Wallet");
                    freezeBtn.getStyleClass().removeAll("button-success");
                    freezeBtn.getStyleClass().add("button-warning");
                }
                freezeBtn.setDisable(false);
            } else {
                balanceLabel.setText("N/A");
                currencyLabel.setText("TND");
                walletStatusLabel.setText("No Wallet");
                walletStatusLabel.setStyle("-fx-text-fill: -color-text-muted;");
                freezeBtn.setDisable(true);
            }
        }, javafx.application.Platform::runLater).exceptionally(ex -> {
            javafx.application.Platform.runLater(() -> {
                tn.finhub.util.DialogUtil.showError("Error", "Failed to load wallet info.");
                ex.printStackTrace();
            });
            return null;
        });
    }

    @FXML
    private void handleBack() {
        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameLabel.getScene()
                .lookup("#adminContentArea");
        if (contentArea != null) {
            ViewUtils.loadContent(contentArea, "/view/admin_users.fxml");
>>>>>>> cd680ce (crud+controle de saisie)
        }
    }

    @FXML
<<<<<<< HEAD
    private javafx.scene.control.Button freezeBtn;

    // ... existing loadLedgerInfo ...

    @FXML
    public void handleFreezeWallet() {
        if (user == null)
            return;
        try {
            Wallet wallet = walletService.getWallet(user.getId());
            if (wallet == null)
                return;

            boolean isFrozen = "FROZEN".equalsIgnoreCase(wallet.getStatus());

            // If currently frozen, we might want to Unfreeze?
            // Current toggle logic:
            if (isFrozen) {
                // Unfreeze Logic - might need Admin Password here too for security
                String password = tn.finhub.util.DialogUtil.showPasswordInput("Security Check",
                        "Enter Admin Password to unfreeze wallet:");
                if (password == null)
                    return;

                if (verifyAdminPassword(password)) {
                    walletService.unfreezeWallet(wallet.getId());
                    updateUI();
                    tn.finhub.util.DialogUtil.showInfo("Success", "Wallet has been unfrozen.");
                } else {
                    tn.finhub.util.DialogUtil.showError("Access Denied", "Incorrect Password.");
                }
            } else {
                // Freeze
                if (tn.finhub.util.DialogUtil.showConfirmation("Freeze Wallet",
                        "Are you sure you want to freeze this wallet?")) {
                    walletService.freezeWallet(wallet.getId());
                    updateUI();
                    tn.finhub.util.DialogUtil.showInfo("Success", "Wallet has been frozen.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            tn.finhub.util.DialogUtil.showError("Error", "Could not update wallet status.");
        }
    }

    @FXML
    public void handleDeleteUser() {
        if (user == null)
            return;

        // 1. Initial Confirmation
        if (!tn.finhub.util.DialogUtil.showConfirmation("Delete User",
                "Are you sure you want to delete user " + user.getFullName() + "?\nThis action cannot be undone.")) {
            return;
        }

        // 2. Password Verification
        String password = tn.finhub.util.DialogUtil.showPasswordInput("Security Check",
                "Enter Admin Password to confirm deletion:");
        if (password == null)
            return; // Cancelled

        // 3. Verify Password
        if (verifyAdminPassword(password)) {
            try {
                // DELETE
                tn.finhub.service.UserService userService = new tn.finhub.service.UserService();
                userService.deleteUser(user.getId());

                tn.finhub.util.DialogUtil.showInfo("User Deleted",
                        "The user and their data have been permanently removed.");
                handleBack();

            } catch (Exception e) {
                e.printStackTrace();
                tn.finhub.util.DialogUtil.showError("Deletion Failed", "An error occurred: " + e.getMessage());
            }
        } else {
            tn.finhub.util.DialogUtil.showError("Access Denied", "Incorrect Password. Deletion cancelled.");
=======
    private void handleFreezeWallet() {
        if (currentUser == null) {
            DialogUtil.showError("Error", "No user selected.");
            return;
        }

        Wallet wallet = walletModel.findByUserId(currentUser.getId());
        if (wallet == null) {
            DialogUtil.showError("Error", "User has no wallet.");
            return;
        }

        boolean isFrozen = "FROZEN".equals(wallet.getStatus());
        String actionRequest = isFrozen ? "Unfreeze" : "Freeze";

        boolean confirmed = DialogUtil.showConfirmation(
                actionRequest + " Wallet",
                "Are you sure you want to " + actionRequest.toLowerCase() + " the wallet for "
                        + currentUser.getFullName() + "?");

        if (confirmed) {
            try {
                if (isFrozen) {
                    walletModel.unfreezeWallet(wallet.getId());
                    DialogUtil.showInfo("Success", "Wallet unfrozen successfully.");
                } else {
                    walletModel.freezeWallet(wallet.getId());
                    DialogUtil.showInfo("Success", "Wallet frozen successfully.");
                }
                refreshWalletInfo();
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Operation Failed", "Could not update wallet status: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteUser() {
        if (currentUser == null) {
            DialogUtil.showError("Error", "No user selected.");
            return;
        }

        // 1. Password Confirmation
        javafx.scene.control.TextInputDialog passwordDialog = new javafx.scene.control.TextInputDialog();
        passwordDialog.setTitle("Admin Authentication");
        passwordDialog.setHeaderText("Security Verification Required");
        passwordDialog.setContentText("Please enter your Admin Password to confirm deletion:");

        java.util.Optional<String> result = passwordDialog.showAndWait();
        if (result.isEmpty())
            return; // Cancelled

        String inputPassword = result.get();
        if (inputPassword.isEmpty()) {
            DialogUtil.showError("Error", "Password cannot be empty.");
            return;
        }

        // 2. Verify Password (Re-Auth)
        if (!verifyAdminPassword(inputPassword)) {
            DialogUtil.showError("Authentication Failed", "Incorrect Admin Password.");
            return;
        }

        // 3. Final Warning
        boolean confirmed = DialogUtil.showConfirmation(
                "Delete User",
                "Are you sure you want to delete " + currentUser.getFullName() + "?\n\n" +
                        "This will:\n" +
                        "- LIQUIDATE all portfolio assets.\n" +
                        "- TRANSFER total balance to Central Bank.\n" +
                        "- PERMANENTLY DELETE all user data.\n" +
                        "- THIS CANNOT BE UNDONE.");

        if (confirmed) {
            try {
                tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
                userModel.deleteUser(currentUser.getId());

                DialogUtil.showInfo("Success", "User deleted successfully. Assets liquidated and transferred.");
                handleBack(); // Navigate back to users list
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Deletion Failed", e.getMessage());
            }
>>>>>>> cd680ce (crud+controle de saisie)
        }
    }

    private boolean verifyAdminPassword(String password) {
        String email = tn.finhub.util.SessionManager.getEmail();
<<<<<<< HEAD
        try {
            // Validate against API Login
=======
        if (email == null)
            return false;

        try {
>>>>>>> cd680ce (crud+controle de saisie)
            String json = """
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(tn.finhub.util.ApiClient.BASE_URL + "/login"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();

            java.net.http.HttpResponse<String> response = tn.finhub.util.ApiClient.getClient().send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
<<<<<<< HEAD

=======
>>>>>>> cd680ce (crud+controle de saisie)
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
<<<<<<< HEAD

    @FXML
    public void handleBack() {
        try {
            // Navigate back to Admin Users List
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_users.fxml"));
            javafx.scene.Parent view = loader.load();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameLabel.getScene()
                    .lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            } else {
                // Fallback if not in main layout/contentArea structure (though unlikely given
                // context)
                nameLabel.getScene().setRoot(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGoToTransactions() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_transactions.fxml"));
            javafx.scene.Parent view = loader.load();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameLabel.getScene()
                    .lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
=======
>>>>>>> cd680ce (crud+controle de saisie)
}
