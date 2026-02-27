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
import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;
import java.util.List;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

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

    // Auto-refresh
    private Timeline autoRefreshTimeline;

    public static void setCachedEscrows(List<Escrow> escrows) {
        cachedEscrows = escrows;
    }

    // Public method to pre-fetch data
    public static void prefetchData() {
        new Thread(() -> {
            try {
                System.out.println("Prefetching Escrow Data...");
                EscrowManager mgr = new EscrowManager();
                List<Escrow> data = mgr.getEscrowsForAdmin();
                setCachedEscrows(data);
                System.out.println("[DEBUG] Escrow Data pre-fetched: " + data.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void initialize() {
        showList();
        loadData();
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
        // Refresh every 30 seconds
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), ev -> {
            // Only refresh if we are viewing the list to avoid disturbing details view
            // interaction
            if (listViewContainer.isVisible()) {
                loadData();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    @FXML
    private void handleRefresh() {
        if (listViewContainer.isVisible()) {
            loadData();
        } else {
            // If in details view, maybe refresh details?
            // For now, let's refresh the underlying list but keep user on details if they
            // want.
            // Or simpler: just reload data which updates the cached list.
            loadData();
        }
    }

    // Ensure we stop timeline if the controller is disposed (though JavaFX
    // controllers don't have a standard destroy)
    // We can rely on view visibility check to simply skip heavy work.

    private void showList() {
        listViewContainer.setVisible(true);
        detailsViewContainer.setVisible(false);
        currentEscrow = null;
        isProfileView = false;
    }

    // --- Optimized Details Loading ---

    private static class ProfileUiData {
        String name;
        String email;
        String trustScore;
        String initials;
        String phone;
        String photoUrl;
        String walletStatus;
        String kycStatus;

        // Helper to create empty/default data
        static ProfileUiData empty() {
            ProfileUiData d = new ProfileUiData();
            d.name = "Unknown";
            d.email = "";
            d.trustScore = "0";
            d.initials = "?";
            d.phone = "N/A";
            d.walletStatus = "N/A";
            d.kycStatus = "UNVERIFIED";
            return d;
        }
    }

    private static class DetailsDataPacket {
        ProfileUiData sender;
        ProfileUiData receiver;
    }

    private void showDetails(Escrow e) {
        currentEscrow = e;
        isProfileView = false;
        updateViewMode();

        // 1. Immediate UI Updates (Static Data)
        detailIdLabel.setText("#" + e.getId());
        detailStatusLabel.setText(e.getStatus());
        detailAmountLabel.setText(e.getAmount() + " TND");
        detailTypeLabel.setText(e.getEscrowType());
        detailConditionLabel.setText(e.getConditionText());
        detailDateLabel.setText(e.getCreatedAt().toString());

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

        // 2. Clear / Show Loading for Dynamic Data
        detailSenderLabel.setText("Loading...");
        detailReceiverLabel.setText("Loading...");

        profileSenderName.setText("Loading...");
        profileReceiverName.setText("Loading...");
        // Reset images
        senderImage.setVisible(false);
        senderInitials.setVisible(true);
        senderInitials.setText("...");
        receiverImage.setVisible(false);
        receiverInitials.setVisible(true);
        receiverInitials.setText("...");

        // 3. Background Task for Profiles
        javafx.concurrent.Task<DetailsDataPacket> task = new javafx.concurrent.Task<>() {
            @Override
            protected DetailsDataPacket call() throws Exception {
                DetailsDataPacket packet = new DetailsDataPacket();
                packet.sender = fetchProfileData(e.getSenderWalletId());
                packet.receiver = fetchProfileData(e.getReceiverWalletId());
                return packet;
            }
        };

        task.setOnSucceeded(ev -> {
            DetailsDataPacket data = task.getValue();
            updateProfileUI(data.sender, true);
            updateProfileUI(data.receiver, false);

            // Update mini-summary labels
            detailSenderLabel.setText(data.sender.name);
            detailReceiverLabel.setText(data.receiver.name);
        });

        task.setOnFailed(ev -> {
            // Handle error silently or show unknown
            detailSenderLabel.setText("Unknown");
            detailReceiverLabel.setText("Unknown");
            task.getException().printStackTrace();
        });

        new Thread(task).start();

        listViewContainer.setVisible(false);
        detailsViewContainer.setVisible(true);
    }

    private ProfileUiData fetchProfileData(int walletId) {
        ProfileUiData data = new ProfileUiData();
        Wallet wallet = walletModel.findById(walletId);

        if (wallet != null) {
            data.walletStatus = wallet.getStatus();
            User user = userModel.findById(wallet.getUserId());
            if (user != null) {
                data.name = user.getFullName();
                data.email = user.getEmail();
                data.trustScore = String.valueOf(user.getTrustScore());
                data.phone = user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A";
                data.initials = getInitials(user.getFullName());
                data.photoUrl = user.getProfilePhotoUrl();

                // KYC Check
                boolean isVerified = checkKycstatus(user.getId());
                data.kycStatus = isVerified ? "VERIFIED" : "UNVERIFIED";
            } else {
                return ProfileUiData.empty();
            }
        } else {
            return ProfileUiData.empty();
        }
        return data;
    }

    private void updateProfileUI(ProfileUiData data, boolean isSender) {
        // Elements selection
        Label nameLbl = isSender ? profileSenderName : profileReceiverName;
        Label emailLbl = isSender ? profileSenderEmail : profileReceiverEmail;
        Label phoneLbl = isSender ? senderPhone : receiverPhone;
        Label trustLbl = isSender ? senderTrustScore : receiverTrustScore;
        Label initialsLbl = isSender ? senderInitials : receiverInitials;
        javafx.scene.image.ImageView imgView = isSender ? senderImage : receiverImage;
        Label walletStatusLbl = isSender ? senderWalletStatus : receiverWalletStatus;
        Label kycStatusLbl = isSender ? senderKycStatus : receiverKycStatus;

        nameLbl.setText(data.name);
        emailLbl.setText(data.email);
        phoneLbl.setText(data.phone);
        trustLbl.setText(data.trustScore);
        initialsLbl.setText(data.initials);

        // Image
        if (data.photoUrl != null && !data.photoUrl.isEmpty()) {
            try {
                // Load in background? We are on UI thread now.
                // Image constructor with backgroundLoading = true
                imgView.setImage(new javafx.scene.image.Image(data.photoUrl, true));
                imgView.setVisible(true);
                initialsLbl.setVisible(false);
            } catch (Exception e) {
                imgView.setVisible(false);
                initialsLbl.setVisible(true);
            }
        } else {
            imgView.setVisible(false);
            initialsLbl.setVisible(true);
        }

        // Wallet Status
        walletStatusLbl.setText(data.walletStatus);
        if ("ACTIVE".equals(data.walletStatus)) {
            walletStatusLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-success;");
        } else {
            walletStatusLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
        }

        // KYC Status
        kycStatusLbl.setText(data.kycStatus);
        if ("VERIFIED".equals(data.kycStatus)) {
            kycStatusLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-info;");
        } else {
            kycStatusLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
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

    // --- Optimized Data Loading ---

    // Value Object to hold pre-fetched data
    private static class EscrowUiData {
        Escrow escrow;
        String senderName;
        String receiverName;

        public EscrowUiData(Escrow escrow, String senderName, String receiverName) {
            this.escrow = escrow;
            this.senderName = senderName;
            this.receiverName = receiverName;
        }
    }

    private void loadData() {
        // OPTIMIZATION: Check cache first
        if (cachedEscrows != null && !cachedEscrows.isEmpty()) {
            // Render immediately with cached data
            renderList(cachedEscrows);
            // Don't return, continue to fetch fresh data in background
        } else {
            // Show loading state only if we have no data
            escrowListContainer.getChildren().clear();
            Label loadingLabel = new Label("Loading escrows...");
            loadingLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
            escrowListContainer.getChildren().add(loadingLabel);
        }

        // Background Task to fetch fresh data
        javafx.concurrent.Task<List<EscrowUiData>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<EscrowUiData> call() throws Exception {
                List<Escrow> escrows = escrowManager.getEscrowsForAdmin();
                // Update cache with fresh data
                cachedEscrows = escrows;

                List<EscrowUiData> uiDataList = new java.util.ArrayList<>();
                for (Escrow e : escrows) {
                    // Pre-fetch names in background thread
                    String sender = getUserNameByWalletId(e.getSenderWalletId());
                    String receiver = getUserNameByWalletId(e.getReceiverWalletId());
                    uiDataList.add(new EscrowUiData(e, sender, receiver));
                }
                return uiDataList;
            }
        };

        task.setOnSucceeded(e -> {
            List<EscrowUiData> dataList = task.getValue();
            escrowListContainer.getChildren().clear();

            if (dataList.isEmpty()) {
                Label empty = new Label("No active escrow transactions found.");
                empty.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
                escrowListContainer.getChildren().add(empty);
            } else {
                for (EscrowUiData data : dataList) {
                    escrowListContainer.getChildren().add(createEscrowCard(data));
                }
            }

            // Extract original escrows for stats calculation
            List<Escrow> originalEscrows = new java.util.ArrayList<>();
            for (EscrowUiData data : dataList) {
                originalEscrows.add(data.escrow);
            }
            calculateSummaryStats(originalEscrows);
        });

        task.setOnFailed(e -> {
            escrowListContainer.getChildren().clear();
            Label error = new Label("Error loading data.");
            error.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-padding: 20;");
            escrowListContainer.getChildren().add(error);
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private HBox createEscrowCard(EscrowUiData data) {
        Escrow e = data.escrow;
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
        // Use pre-fetched names
        Label fromLabel = new Label("From: " + data.senderName);
        Label toLabel = new Label("To: " + data.receiverName);
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

    // Helper to render from raw Escrow list (for cache)
    private void renderList(List<Escrow> escrows) {
        escrowListContainer.getChildren().clear();
        for (Escrow e : escrows) {
            // For cache rendering, we use placeholders for names
            EscrowUiData uiData = new EscrowUiData(e, "...", "...");
            HBox card = createEscrowCard(uiData);
            escrowListContainer.getChildren().add(card);
        }
        calculateSummaryStats(escrows);
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
        boolean confirmed = DialogUtil.showConfirmation(
                "Confirm Release",
                "This will transfer " + e.getAmount() + " TND to the Receiver. Are you sure?");

        if (confirmed) {
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
        boolean confirmed = DialogUtil.showConfirmation(
                "Confirm Refund",
                "This will return " + e.getAmount() + " TND to the Sender. Are you sure?");

        if (confirmed) {
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

        DialogUtil.showInfo("Blockchain Record",
                "Transaction Hash:\n" + txHash + "\n\n" +
                        "Block Height: " + blockHeight + "\n" +
                        "Timestamp: " + timestamp + "\n" +
                        "Status: CONFIRMED");
    }

    @FXML
    private void handleBack() {
        showList();
    }

    private void showInfo(String title, String content) {
        DialogUtil.showInfo(title, content);
    }

    private void showError(String content) {
        DialogUtil.showError("Error", content);
    }
}
