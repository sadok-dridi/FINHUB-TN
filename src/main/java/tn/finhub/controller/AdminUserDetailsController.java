package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.finhub.model.LedgerAuditLog;
import tn.finhub.model.LedgerFlag;
import tn.finhub.model.User;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.model.UserModel;

import java.util.List;

public class AdminUserDetailsController {

    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label idLabel;

    @FXML
    private Label balanceLabel;
    @FXML
    private Label currencyLabel;
    @FXML
    private Label walletStatusLabel;

    @FXML
    private VBox ledgerAlertBox;
    @FXML
    private Label ledgerAlertMessage;
    @FXML
    private VBox auditLogsContainer;

    private User user;
    private WalletModel walletModel = new WalletModel();
    private UserModel userModel = new UserModel();

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
            Wallet wallet = walletModel.findByUserId(user.getId());
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
        LedgerFlag flag = walletModel.getLatestFlag(walletId);
        if (flag != null) {
            ledgerAlertBox.setVisible(true);
            ledgerAlertBox.setManaged(true);
            ledgerAlertMessage.setText("SECURITY ALERT: " + flag.getReason() + "\nFlagged at: " + flag.getFlaggedAt());
        } else {
            ledgerAlertBox.setVisible(false);
            ledgerAlertBox.setManaged(false);
        }

        // Audit Logs (Show last 5 for example)
        List<LedgerAuditLog> logs = walletModel.getAuditLogs(walletId);
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
        }
    }

    @FXML
    private javafx.scene.control.Button freezeBtn;

    // ... existing loadLedgerInfo ...

    @FXML
    public void handleFreezeWallet() {
        if (user == null)
            return;
        try {
            Wallet wallet = walletModel.findByUserId(user.getId());
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
                    walletModel.updateStatus(wallet.getId(), "ACTIVE");
                    updateUI();
                    tn.finhub.util.DialogUtil.showInfo("Success", "Wallet has been unfrozen.");
                } else {
                    tn.finhub.util.DialogUtil.showError("Access Denied", "Incorrect Password.");
                }
            } else {
                // Freeze
                if (tn.finhub.util.DialogUtil.showConfirmation("Freeze Wallet",
                        "Are you sure you want to freeze this wallet?")) {
                    walletModel.updateStatus(wallet.getId(), "FROZEN");
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
                userModel.deleteUser(user.getId());

                tn.finhub.util.DialogUtil.showInfo("User Deleted",
                        "The user and their data have been permanently removed.");
                handleBack();

            } catch (Exception e) {
                e.printStackTrace();
                tn.finhub.util.DialogUtil.showError("Deletion Failed", "An error occurred: " + e.getMessage());
            }
        } else {
            tn.finhub.util.DialogUtil.showError("Access Denied", "Incorrect Password. Deletion cancelled.");
        }
    }

    private boolean verifyAdminPassword(String password) {
        String email = tn.finhub.util.SessionManager.getEmail();
        try {
            // Validate against API Login
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

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

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
}
