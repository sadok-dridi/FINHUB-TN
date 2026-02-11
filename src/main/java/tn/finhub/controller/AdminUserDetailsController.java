package tn.finhub.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.finhub.model.User;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.ViewUtils;

public class AdminUserDetailsController {

    // Personal Information fields
    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label idLabel;

    // Wallet Status fields
    @FXML
    private Label balanceLabel;
    @FXML
    private Label currencyLabel;
    @FXML
    private Label walletStatusLabel;

    // Ledger Alert fields
    @FXML
    private VBox ledgerAlertBox;
    @FXML
    private Label ledgerAlertMessage;

    // Audit Logs
    @FXML
    private VBox auditLogsContainer;

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

        // Hide ledger alert by default (placeholder for now)
        if (ledgerAlertBox != null) {
            ledgerAlertBox.setVisible(false);
            ledgerAlertBox.setManaged(false);
        }
    }

    private void refreshWalletInfo() {
        if (currentUser == null)
            return;

        Wallet wallet = walletModel.findByUserId(currentUser.getId());
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
    }

    @FXML
    private void handleBack() {
        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameLabel.getScene()
                .lookup("#adminContentArea");
        if (contentArea != null) {
            ViewUtils.loadContent(contentArea, "/view/admin_users.fxml");
        }
    }

    @FXML
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

        boolean confirmed = DialogUtil.showConfirmation(
                "Delete User",
                "Are you sure you want to delete " + currentUser.getFullName() + "?\n\nThis action cannot be undone.");

        if (confirmed) {
            try {
                // TODO: Implement user deletion logic
                DialogUtil.showInfo("Success", "User deleted successfully.");
                handleBack(); // Navigate back to users list
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Deletion Failed", "Could not delete user. Please try again.");
            }
        }
    }
}
