package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.finhub.model.SupportModel;
import tn.finhub.model.UserModel;
import tn.finhub.model.WalletModel;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.SessionManager;
import tn.finhub.util.ViewUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AdminDashboardController {

    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label totalVolumeLabel;
    @FXML
    private Label openTicketsLabel;
    @FXML
    private Label systemHealthLabel;

    private final UserModel userModel = new UserModel();
    private final WalletModel walletModel = new WalletModel();
    private final SupportModel supportModel = new SupportModel();

    @FXML
    public void initialize() {
        loadStats();
    }

    private void loadStats() {
        try {
            // Users
            int userCount = userModel.countUsers();
            totalUsersLabel.setText(String.valueOf(userCount));

            // Volume
            BigDecimal volume = walletModel.getTotalVolume();
            totalVolumeLabel.setText(volume.setScale(2, RoundingMode.HALF_UP).toString() + " TND");

            // Tickets
            int openTickets = supportModel.getOpenTicketCount();
            openTicketsLabel.setText(String.valueOf(openTickets));

            // System Health (Mock for now, or check DB)
            systemHealthLabel.setText("Healthy");
            systemHealthLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Failed to load dashboard statistics.");
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        ViewUtils.setView(totalUsersLabel, "/view/login.fxml");
    }

    @FXML
    private void handleGoToUsers() {
        ViewUtils.setView(totalUsersLabel, "/view/admin_users.fxml");
    }

    @FXML
    private void handleGoToTransactions() {
        ViewUtils.setView(totalUsersLabel, "/view/admin_transactions.fxml");
    }

    @FXML
    private void handleGoToEscrows() {
        ViewUtils.setView(totalUsersLabel, "/view/admin_escrow.fxml");
    }

    @FXML
    private void handleGoToSupport() {
        ViewUtils.setView(totalUsersLabel, "/view/admin_support.fxml");
    }

    @FXML
    private void handleGoToAlerts() {
        ViewUtils.setView(totalUsersLabel, "/view/admin_alerts.fxml");
    }
}
