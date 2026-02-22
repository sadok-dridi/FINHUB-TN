package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.finhub.model.SupportModel;
import tn.finhub.model.UserModel;
import tn.finhub.model.WalletModel;
import tn.finhub.util.DialogUtil;

public class AdminHomeController {

    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label pendingKycLabel;
    @FXML
    private Label openTicketsLabel;
    @FXML
    private Label activeEscrowLabel;

    private final UserModel userModel = new UserModel();
    // private final WalletModel walletModel = new WalletModel(); // Unused after
    // removing volume card
    private final SupportModel supportModel = new SupportModel();
    private final tn.finhub.model.KYCModel kycModel = new tn.finhub.model.KYCModel();
    private final tn.finhub.model.EscrowManager escrowManager = new tn.finhub.model.EscrowManager();

    @FXML
    public void initialize() {
        loadStats();
    }

    private void loadStats() {
        try {
            // Users
            int userCount = userModel.countUsers();
            totalUsersLabel.setText(String.valueOf(userCount));

            // Pending KYC
            int pendingKyc = kycModel.findPendingRequests().size();
            pendingKycLabel.setText(String.valueOf(pendingKyc));

            // Tickets
            int openTickets = supportModel.getOpenTicketCount();
            openTicketsLabel.setText(String.valueOf(openTickets));

            // Active Escrow Volume
            java.math.BigDecimal escrowVolume = escrowManager.getTotalActiveEscrowAmount();
            activeEscrowLabel.setText(escrowVolume.setScale(2, java.math.RoundingMode.HALF_UP).toString() + " TND");

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Failed to load dashboard statistics.");
        }
    }
}
