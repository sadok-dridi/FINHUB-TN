package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import tn.finhub.model.Escrow;
import tn.finhub.model.EscrowManager;
import tn.finhub.model.User;
import tn.finhub.model.UserModel;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.util.UserSession;
import java.util.Optional;
import java.util.List;

public class AdminEscrowController {

    @FXML
    private VBox escrowListContainer;

    @FXML
    private VBox listViewContainer;
    @FXML
    private VBox detailsViewContainer;

    // Details View Fields
    @FXML
    private Label detailIdLabel;
    @FXML
    private Label detailStatusLabel;
    @FXML
    private Label detailAmountLabel;
    @FXML
    private Label detailTypeLabel;
    @FXML
    private Label detailConditionLabel;
    @FXML
    private Label detailSenderLabel;
    @FXML
    private Label detailReceiverLabel;
    @FXML
    private Label detailDateLabel;
    @FXML
    private Button btnRelease;
    @FXML
    private Button btnRefund;

    @FXML
    private Label totalEscrowsLabel;
    @FXML
    private Label activeEscrowsLabel;
    @FXML
    private Label disputedEscrowsLabel;
    @FXML
    private Label releasedEscrowsLabel;

    @FXML
    private Button btnToggleView;
    @FXML
    private HBox transactionDetailsBox;
    @FXML
    private HBox profileDetailsBox;

    // Sender Profile
    @FXML
    private Label senderInitials;
    @FXML
    private javafx.scene.image.ImageView senderImage;
    @FXML
    private Label profileSenderName;
    @FXML
    private Label profileSenderEmail;
    @FXML
    private Label senderPhone;
    @FXML
    private Label senderTrustScore;
    @FXML
    private Label senderKycStatus;
    @FXML
    private Label senderWalletStatus;

    // Receiver Profile
    @FXML
    private Label receiverInitials;
    @FXML
    private javafx.scene.image.ImageView receiverImage;
    @FXML
    private Label profileReceiverName;
    @FXML
    private Label profileReceiverEmail;
    @FXML
    private Label receiverPhone;
    @FXML
    private Label receiverTrustScore;
    @FXML
    private Label receiverKycStatus;
    @FXML
    private Label receiverWalletStatus;

    private boolean isProfileView = false;

    private final EscrowManager escrowManager = new EscrowManager();
    private final WalletModel walletModel = new WalletModel();
    private final UserModel userModel = new UserModel();
    private final tn.finhub.model.KYCModel kycModel = new tn.finhub.model.KYCModel();

    private static List<Escrow> cachedEscrows;

    private Escrow currentEscrow;

    public static void setCachedEscrows(List<Escrow> escrows) {
        cachedEscrows = escrows;
    }

    @FXML
    public void initialize() {
        showList();
        loadData();
    }

    private void showList() {
        listViewContainer.setVisible(true);
        detailsViewContainer.setVisible(false);
        currentEscrow = null;
        isProfileView = false;
    }

    private void showDetails(Escrow e) {
        currentEscrow = e;
        isProfileView = false;
        updateViewMode();

        detailIdLabel.setText("#" + e.getId());
        detailStatusLabel.setText(e.getStatus());
        detailAmountLabel.setText(e.getAmount() + " TND");
        detailTypeLabel.setText(e.getEscrowType());
        detailConditionLabel.setText(e.getConditionText());
        detailSenderLabel.setText(getUserNameByWalletId(e.getSenderWalletId()));
        detailReceiverLabel.setText(getUserNameByWalletId(e.getReceiverWalletId()));
        detailDateLabel.setText(e.getCreatedAt().toString());

        // Load Profiles
        loadProfileData(e.getSenderWalletId(), e.getReceiverWalletId());

        // Status Styles
        String statusStyle = "-fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 4; ";
        switch (e.getStatus()) {
            case "RELEASED":
                statusStyle += "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10B981;";
                break;
            case "REFUNDED":
                statusStyle += "-fx-background-color: rgba(107, 114, 128, 0.2); -fx-text-fill: -color-text-muted;";
                break;
            case "DISPUTED":
                statusStyle += "-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #EF4444;";
                break;
            case "LOCKED":
                statusStyle += "-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #3B82F6;";
                break;
            default:
                statusStyle += "-fx-background-color: rgba(107, 114, 128, 0.2); -fx-text-fill: -color-text-muted;";
                break;
        }
        detailStatusLabel.setStyle(statusStyle);

        // Access Control
        boolean isActive = "LOCKED".equals(e.getStatus()) || "DISPUTED".equals(e.getStatus());
        btnRelease.setDisable(!isActive);
        btnRefund.setDisable(!isActive);

        listViewContainer.setVisible(false);
        detailsViewContainer.setVisible(true);
    }

    private void loadProfileData(int senderWalletId, int receiverWalletId) {
        Wallet senderWallet = walletModel.findById(senderWalletId);
        Wallet receiverWallet = walletModel.findById(receiverWalletId);

        if (senderWallet != null) {
            User sender = userModel.findById(senderWallet.getUserId());
            if (sender != null) {
                profileSenderName.setText(sender.getFullName());
                profileSenderEmail.setText(sender.getEmail());
                senderTrustScore.setText(String.valueOf(sender.getTrustScore()));
                senderInitials.setText(getInitials(sender.getFullName()));
                senderPhone.setText(sender.getPhoneNumber() != null ? sender.getPhoneNumber() : "N/A");

                // Image Logic
                if (sender.getProfilePhotoUrl() != null && !sender.getProfilePhotoUrl().isEmpty()) {
                    try {
                        senderImage.setImage(new javafx.scene.image.Image(sender.getProfilePhotoUrl()));
                        senderImage.setVisible(true);
                        senderInitials.setVisible(false);
                    } catch (Exception e) {
                        senderImage.setVisible(false);
                        senderInitials.setVisible(true);
                    }
                } else {
                    senderImage.setVisible(false);
                    senderInitials.setVisible(true);
                }

                // Wallet Status
                senderWalletStatus.setText(senderWallet.getStatus());
                if ("ACTIVE".equals(senderWallet.getStatus())) {
                    senderWalletStatus
                            .setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-success;");
                } else {
                    senderWalletStatus
                            .setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
                }

                // KYC Status Logic
                boolean isVerified = checkKycstatus(sender.getId());
                if (isVerified) {
                    senderKycStatus.setText("VERIFIED");
                    senderKycStatus.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-info;");
                } else {
                    senderKycStatus.setText("UNVERIFIED");
                    senderKycStatus
                            .setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
                }
            }
        }

        if (receiverWallet != null) {
            User receiver = userModel.findById(receiverWallet.getUserId());
            if (receiver != null) {
                profileReceiverName.setText(receiver.getFullName());
                profileReceiverEmail.setText(receiver.getEmail());
                receiverTrustScore.setText(String.valueOf(receiver.getTrustScore()));
                receiverInitials.setText(getInitials(receiver.getFullName()));
                receiverPhone.setText(receiver.getPhoneNumber() != null ? receiver.getPhoneNumber() : "N/A");

                // Image Logic
                if (receiver.getProfilePhotoUrl() != null && !receiver.getProfilePhotoUrl().isEmpty()) {
                    try {
                        receiverImage.setImage(new javafx.scene.image.Image(receiver.getProfilePhotoUrl()));
                        receiverImage.setVisible(true);
                        receiverInitials.setVisible(false);
                    } catch (Exception e) {
                        receiverImage.setVisible(false);
                        receiverInitials.setVisible(true);
                    }
                } else {
                    receiverImage.setVisible(false);
                    receiverInitials.setVisible(true);
                }

                // Wallet Status
                receiverWalletStatus.setText(receiverWallet.getStatus());
                if ("ACTIVE".equals(receiverWallet.getStatus())) {
                    receiverWalletStatus
                            .setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-success;");
                } else {
                    receiverWalletStatus
                            .setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
                }

                // KYC Status Logic
                boolean isVerified = checkKycstatus(receiver.getId());
                if (isVerified) {
                    receiverKycStatus.setText("VERIFIED");
                    receiverKycStatus
                            .setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-info;");
                } else {
                    receiverKycStatus.setText("UNVERIFIED");
                    receiverKycStatus
                            .setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
                }
            }
        }
    }

    private boolean checkKycstatus(int userId) {
        List<tn.finhub.model.KYCRequest> requests = kycModel.findByUserId(userId);
        for (tn.finhub.model.KYCRequest req : requests) {
            if ("APPROVED".equals(req.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty())
            return "?";
        String[] parts = name.split(" ");
        String initials = "";
        if (parts.length > 0)
            initials += parts[0].charAt(0);
        if (parts.length > 1)
            initials += parts[1].charAt(0);
        return initials.toUpperCase();
    }

    @FXML
    private void handleToggleProfileView() {
        isProfileView = !isProfileView;
        updateViewMode();
    }

    private void updateViewMode() {
        if (isProfileView) {
            transactionDetailsBox.setVisible(false);
            transactionDetailsBox.setManaged(false);
            profileDetailsBox.setVisible(true);
            profileDetailsBox.setManaged(true);
            btnToggleView.setText("View Transaction");
        } else {
            transactionDetailsBox.setVisible(true);
            transactionDetailsBox.setManaged(true);
            profileDetailsBox.setVisible(false);
            profileDetailsBox.setManaged(false);
            btnToggleView.setText("View Profiles");
        }
    }

    private void loadData() {
        List<Escrow> escrows;
        if (cachedEscrows != null) {
            escrows = cachedEscrows;
            cachedEscrows = null;
        } else {
            escrows = escrowManager.getEscrowsForAdmin();
        }

        escrowListContainer.getChildren().clear();

        if (escrows.isEmpty()) {
            Label empty = new Label("No active escrow transactions found.");
            empty.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
            escrowListContainer.getChildren().add(empty);
        } else {
            for (Escrow e : escrows) {
                escrowListContainer.getChildren().add(createEscrowCard(e));
            }
        }

        calculateSummaryStats(escrows);
    }

    private HBox createEscrowCard(Escrow e) {
        HBox card = new HBox(15);
        card.getStyleClass().add("escrow-card");
        card.setAlignment(Pos.CENTER_LEFT);

        card.setOnMouseClicked(ev -> showDetails(e));

        // 1. Icon/ID
        VBox idBox = new VBox(5);
        idBox.setAlignment(Pos.CENTER);
        idBox.setPrefWidth(110);
        idBox.setMinWidth(110);
        idBox.setMaxWidth(110);
        Label idLabel = new Label("#" + e.getId());
        idLabel.getStyleClass().add("escrow-id");
        Label typeLabel = new Label(e.getEscrowType());
        typeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        idBox.getChildren().addAll(idLabel, typeLabel);

        // 2. Participants
        VBox participantsBox = new VBox(5);
        participantsBox.setPrefWidth(200);
        participantsBox.setMinWidth(200);
        participantsBox.setMaxWidth(200);
        String senderName = getUserNameByWalletId(e.getSenderWalletId());
        String receiverName = getUserNameByWalletId(e.getReceiverWalletId());

        Label fromLabel = new Label("From: " + senderName);
        Label toLabel = new Label("To: " + receiverName);
        participantsBox.getChildren().addAll(fromLabel, toLabel);

        // 3. Amount & Condition
        VBox detailsBox = new VBox(5);
        HBox.setHgrow(detailsBox, Priority.ALWAYS);

        Label amountLabel = new Label(e.getAmount() + " TND");
        amountLabel.getStyleClass().add("escrow-amount");

        String conditionText = e.getConditionText();
        if (conditionText != null && conditionText.length() > 60) {
            conditionText = conditionText.substring(0, 60) + "...";
        }
        Label conditionLabel = new Label("\"" + conditionText + "\"");
        conditionLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-style: italic;");
        detailsBox.getChildren().addAll(amountLabel, conditionLabel);

        // 4. Status
        VBox statusBox = new VBox();
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPrefWidth(120);
        statusBox.setMinWidth(120);
        statusBox.setMaxWidth(120);
        Label statusLabel = new Label(e.getStatus());
        String statusStyle = "-fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px; ";

        switch (e.getStatus()) {
            case "RELEASED":
                statusStyle += "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10B981;";
                break;
            case "REFUNDED":
                statusStyle += "-fx-background-color: rgba(107, 114, 128, 0.2); -fx-text-fill: -color-text-muted;";
                break;
            case "DISPUTED":
                statusStyle += "-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #EF4444;";
                break;
            case "LOCKED":
                statusStyle += "-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #3B82F6;";
                break;
            default:
                statusStyle += "-fx-background-color: rgba(107, 114, 128, 0.2); -fx-text-fill: -color-text-muted;";
                break;
        }
        statusLabel.setStyle(statusStyle);
        statusBox.getChildren().add(statusLabel);

        // 5. Actions
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        actionsBox.setMinWidth(110);
        actionsBox.setPrefWidth(110);
        actionsBox.setMaxWidth(110);

        Label viewLabel = new Label("View Details >");
        viewLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 11px; -fx-cursor: hand;");
        actionsBox.getChildren().add(viewLabel);

        card.getChildren().addAll(idBox, participantsBox, detailsBox, statusBox, actionsBox);
        return card;
    }

    private String getUserNameByWalletId(int walletId) {
        Wallet w = walletModel.findById(walletId);
        if (w != null) {
            User u = userModel.findById(w.getUserId());
            if (u != null)
                return u.getFullName();
        }
        return "Unknown (" + walletId + ")";
    }

    private void calculateSummaryStats(List<Escrow> list) {
        if (list == null)
            return;

        int total = list.size();
        long active = list.stream().filter(e -> "LOCKED".equals(e.getStatus())).count();
        long disputed = list.stream().filter(e -> "DISPUTED".equals(e.getStatus())).count();
        long released = list.stream().filter(e -> "RELEASED".equals(e.getStatus())).count();

        totalEscrowsLabel.setText(String.valueOf(total));
        activeEscrowsLabel.setText(String.valueOf(active));
        disputedEscrowsLabel.setText(String.valueOf(disputed));
        releasedEscrowsLabel.setText(String.valueOf(released));
    }

    private void handleRelease(Escrow e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Release");
        alert.setHeaderText("Release Funds for Escrow #" + e.getId());
        alert.setContentText("This will transfer " + e.getAmount() + " TND to the Receiver. Are you sure?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int adminId = UserSession.getInstance().getUser().getId(); // Current Admin
                escrowManager.releaseEscrowByAdmin(e.getId(), adminId);
                showInfo("Success", "Funds released successfully.");
                loadData(); // Will refresh and recalc stats
            } catch (Exception ex) {
                showError("Error releasing funds: " + ex.getMessage());
            }
        }
    }

    private void handleRefund(Escrow e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Refund");
        alert.setHeaderText("Refund Escrow #" + e.getId());
        alert.setContentText("This will return " + e.getAmount() + " TND to the Sender. Are you sure?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                escrowManager.refundEscrow(e.getId());
                showInfo("Success", "Funds refunded successfully.");
                loadData();
            } catch (Exception ex) {
                showError("Error refunding funds: " + ex.getMessage());
            }
        }
    }

    // Detail View Actions
    @FXML
    private void handleDetailRelease() {
        if (currentEscrow == null)
            return;
        handleRelease(currentEscrow);
    }

    @FXML
    private void handleDetailRefund() {
        if (currentEscrow == null)
            return;
        handleRefund(currentEscrow);
    }

    @FXML
    private void handleViewBlockchain() {
        if (currentEscrow == null)
            return;

        // Mock Blockchain Record since we don't have a real heavy blockchain
        // integration yet
        String txHash = "0x" + Integer.toHexString(currentEscrow.getId() * 123456789).toUpperCase()
                + "ABCDEF1234567890";
        String blockHeight = String.valueOf(100000 + currentEscrow.getId());
        String timestamp = currentEscrow.getCreatedAt().toString();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Blockchain Record");
        alert.setHeaderText("Immutable Ledger Entry");
        alert.setContentText("Transaction Hash:\n" + txHash + "\n\n" +
                "Block Height: " + blockHeight + "\n" +
                "Timestamp: " + timestamp + "\n" +
                "Status: CONFIRMED");
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        showList();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
