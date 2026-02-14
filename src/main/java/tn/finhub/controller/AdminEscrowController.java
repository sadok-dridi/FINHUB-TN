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

    private final EscrowManager escrowManager = new EscrowManager();
    private final WalletModel walletModel = new WalletModel();
    private final UserModel userModel = new UserModel();

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
    }

    private void showDetails(Escrow e) {
        currentEscrow = e;

        detailIdLabel.setText("#" + e.getId());
        detailStatusLabel.setText(e.getStatus());
        detailAmountLabel.setText(e.getAmount() + " TND");
        detailTypeLabel.setText(e.getEscrowType());
        detailConditionLabel.setText(e.getConditionText());
        detailSenderLabel.setText(getUserNameByWalletId(e.getSenderWalletId()));
        detailReceiverLabel.setText(getUserNameByWalletId(e.getReceiverWalletId()));
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

        listViewContainer.setVisible(false);
        detailsViewContainer.setVisible(true);
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
        card.setStyle(
                "-fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.CENTER_LEFT);

        // Add Hover Effect & Click Action
        card.setOnMouseEntered(ev -> card.setStyle(
                "-fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 4); -fx-cursor: hand;"));
        card.setOnMouseExited(ev -> card.setStyle(
                "-fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
        card.setOnMouseClicked(ev -> showDetails(e));

        // 1. Icon/ID
        VBox idBox = new VBox(5);
        idBox.setAlignment(Pos.CENTER);
        idBox.setPrefWidth(60);
        Label idLabel = new Label("#" + e.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-default;");
        Label typeLabel = new Label(e.getEscrowType());
        typeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        idBox.getChildren().addAll(idLabel, typeLabel);

        // 2. Participants
        VBox participantsBox = new VBox(5);
        participantsBox.setPrefWidth(200);
        String senderName = getUserNameByWalletId(e.getSenderWalletId());
        String receiverName = getUserNameByWalletId(e.getReceiverWalletId());

        Label fromLabel = new Label("From: " + senderName);
        fromLabel.setStyle("-fx-text-fill: -color-text-default;");
        Label toLabel = new Label("To: " + receiverName);
        toLabel.setStyle("-fx-text-fill: -color-text-default;");
        participantsBox.getChildren().addAll(fromLabel, toLabel);

        // 3. Amount & Condition
        VBox detailsBox = new VBox(5);
        HBox.setHgrow(detailsBox, Priority.ALWAYS);

        Label amountLabel = new Label(e.getAmount() + " TND");
        amountLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-primary;");
        Label conditionLabel = new Label("\"" + e.getConditionText() + "\"");
        conditionLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-style: italic;");
        detailsBox.getChildren().addAll(amountLabel, conditionLabel);

        // 4. Status
        VBox statusBox = new VBox();
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPrefWidth(100);
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
