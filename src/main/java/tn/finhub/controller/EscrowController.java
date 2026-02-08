package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import tn.finhub.model.Escrow;
import tn.finhub.model.EscrowManager;
import tn.finhub.model.WalletModel;
import tn.finhub.util.UserSession;
import tn.finhub.model.Wallet;

import java.util.List;

public class EscrowController {

    @FXML
    private VBox escrowCardContainer;
    @FXML
    private Label trustScoreLabel;

    private final EscrowManager escrowManager = new EscrowManager();
    private final WalletModel walletModel = new WalletModel();

    @FXML
    public void initialize() {
        loadEscrowData();
    }

    private void loadEscrowData() {
        // Get Current User Wallet
        tn.finhub.model.User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null)
            return;

        Wallet wallet = walletModel.findByUserId(currentUser.getId());

        if (wallet != null) {
            // Load Trust Score
            trustScoreLabel.setText(String.valueOf(currentUser.getTrustScore()));

            // Load Escrows
            List<Escrow> escrows = escrowManager.getEscrowsByWalletId(wallet.getId());
            escrowCardContainer.getChildren().clear();

            if (escrows.isEmpty()) {
                Label emptyLabel = new Label("No active escrow transactions.");
                emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
                escrowCardContainer.getChildren().add(emptyLabel);
            } else {
                for (Escrow e : escrows) {
                    escrowCardContainer.getChildren().add(createEscrowCard(e));
                }
            }
        }
    }

    private javafx.scene.Node createEscrowCard(Escrow e) {
        HBox card = new HBox();
        card.setSpacing(20);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: -color-card-bg; -fx-background-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");

        // Icon / Type Indicator
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane();
        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle(25);
        bg.setFill(javafx.scene.paint.Color.web("#eef2ff")); // Light blue

        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        if ("QR_CODE".equals(e.getEscrowType())) {
            icon.setContent(
                    "M3 5v4h2V5h4V3H5c-1.1 0-2 .9-2 2zm2 10H3v4c0 1.1.9 2 2 2h4v-2H5v-4zm14 4h-4v2h4c1.1 0 2-.9 2-2v-4h-2v4zm0-16h-4v2h4v4h2V5c0-1.1-.9-2-2-2z");
        } else {
            icon.setContent(
                    "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z");
        }
        icon.setStyle("-fx-fill: -color-primary;");
        iconPane.getChildren().addAll(bg, icon);

        // Details
        VBox details = new VBox(5);
        Label title = new Label("Escrow #" + e.getId());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-primary; -fx-font-size: 16px;");
        Label condition = new Label(e.getConditionText());
        condition.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 13px;");
        details.getChildren().addAll(title, condition);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Amount
        Label amountLabel = new Label(e.getAmount() + " TND");
        amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-primary; -fx-font-size: 18px;");

        // Status Badge
        Label statusBadge = new Label(e.getStatus());
        String statusStyle = "-fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold; text-transform: uppercase;";

        switch (e.getStatus()) {
            case "LOCKED" -> statusStyle += "-fx-background-color: #fff7ed; -fx-text-fill: #c2410c;"; // Orange
            case "RELEASED" -> statusStyle += "-fx-background-color: #ecfdf5; -fx-text-fill: #047857;"; // Green
            case "DISPUTED" -> statusStyle += "-fx-background-color: #fef2f2; -fx-text-fill: #b91c1c;"; // Red
            default -> statusStyle += "-fx-background-color: #f3f4f6; -fx-text-fill: #4b5563;"; // Gray
        }
        statusBadge.setStyle(statusStyle);

        // Action Button (Interactive)
        javafx.scene.control.Button actionBtn = new javafx.scene.control.Button("View");
        actionBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-text-fill: -color-text-secondary; -fx-cursor: hand;");
        actionBtn.setOnAction(event -> handleViewDetails(e));

        card.getChildren().addAll(iconPane, details, spacer, amountLabel, statusBadge, actionBtn);
        return card;
    }

    private void handleViewDetails(Escrow e) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/escrow_details_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            EscrowDetailsController controller = loader.getController();
            controller.setEscrow(e);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style/theme.css").toExternalForm());

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Escrow Details #" + e.getId());
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            if (escrowCardContainer.getScene() != null) {
                stage.initOwner(escrowCardContainer.getScene().getWindow());
            }

            stage.showAndWait();

            // Refresh list in case status changed
            loadEscrowData();

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleCreateEscrow() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/create_escrow_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style/theme.css").toExternalForm());

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Create Secure Escrow");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            // Set owner if possible (not strictly necessary but good practice)
            if (escrowCardContainer.getScene() != null) {
                stage.initOwner(escrowCardContainer.getScene().getWindow());
            }

            stage.showAndWait();

            // Refresh list after dialog closes
            loadEscrowData();

        } catch (java.io.IOException e) {
            e.printStackTrace();
            // Optional: Show error alert
        }
    }
}
