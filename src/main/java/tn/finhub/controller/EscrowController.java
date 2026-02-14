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
    @FXML
    private javafx.scene.control.Button createEscrowBtn;
    @FXML
    private javafx.scene.control.Button btnActive;
    @FXML
    private javafx.scene.control.Button btnHistory;
    @FXML
    private javafx.scene.control.Button btnDisputes;
    @FXML
    private javafx.scene.shape.Circle trustScoreCircle;
    @FXML
    private javafx.scene.shape.SVGPath trustScoreIcon;

    private final EscrowManager escrowManager = new EscrowManager();
    private final WalletModel walletModel = new WalletModel();
    private final tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();

    private enum FilterType {
        ACTIVE, HISTORY, DISPUTES
    }

    private FilterType currentFilter = FilterType.ACTIVE;

    @FXML
    public void initialize() {
        loadEscrowData();
        updateFilterUI();
    }

    private void loadEscrowData() {
        // Get Current User
        tn.finhub.model.User sessionUser = UserSession.getInstance().getUser();
        if (sessionUser == null)
            return;

        // FETCH LIVE USER DATA (Fix for stale Trust Score)
        tn.finhub.model.User currentUser = userModel.findById(sessionUser.getId());
        if (currentUser == null)
            currentUser = sessionUser; // Fallback

        Wallet wallet = walletModel.findByUserId(currentUser.getId());

        if (wallet != null) {
            // Load Trust Score
            int score = currentUser.getTrustScore();
            trustScoreLabel.setText(String.valueOf(score));

            // Visual Update (Circle & Color)
            double maxScore = 100.0;
            double radius = 22.0;
            double circumference = 2 * Math.PI * radius;
            double progress = Math.min(Math.max(score, 0), maxScore) / maxScore * circumference;

            if (trustScoreCircle != null) {
                trustScoreCircle.getStrokeDashArray().setAll(progress, circumference);

                // Color Logic
                String color;
                if (score < 30)
                    color = "#ef4444"; // Red
                else if (score < 70)
                    color = "#f59e0b"; // Orange
                else
                    color = "#10b981"; // Green

                trustScoreCircle.setStyle("-fx-fill: transparent; -fx-stroke-width: 4; -fx-stroke: " + color + ";");
                if (trustScoreIcon != null) {
                    trustScoreIcon.setStyle("-fx-fill: " + color + ";");
                }
            }

            // Check Frozen Status
            boolean isFrozen = "FROZEN".equals(wallet.getStatus());
            if (createEscrowBtn != null) {
                createEscrowBtn.setDisable(isFrozen);
            }

            // Load Escrows
            List<Escrow> allEscrows = escrowManager.getEscrowsByWalletId(wallet.getId());
            escrowCardContainer.getChildren().clear();

            // Filter
            List<Escrow> filtered = allEscrows.stream()
                    .filter(e -> {
                        switch (currentFilter) {
                            case ACTIVE:
                                return "LOCKED".equals(e.getStatus());
                            case HISTORY:
                                return "RELEASED".equals(e.getStatus()) || "REFUNDED".equals(e.getStatus());
                            case DISPUTES:
                                return "DISPUTED".equals(e.getStatus());
                            default:
                                return true;
                        }
                    })
                    .toList();

            if (filtered.isEmpty()) {
                Label emptyLabel = new Label("No " + currentFilter.name().toLowerCase() + " escrow transactions.");
                emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
                escrowCardContainer.getChildren().add(emptyLabel);
            } else {
                for (Escrow e : filtered) {
                    escrowCardContainer.getChildren().add(createEscrowCard(e));
                }
            }
        }
    }

    @FXML
    private void handleFilterActive() {
        if (currentFilter != FilterType.ACTIVE) {
            currentFilter = FilterType.ACTIVE;
            updateFilterUI();
            loadEscrowData();
        }
    }

    @FXML
    private void handleFilterHistory() {
        if (currentFilter != FilterType.HISTORY) {
            currentFilter = FilterType.HISTORY;
            updateFilterUI();
            loadEscrowData();
        }
    }

    @FXML
    private void handleFilterDisputes() {
        if (currentFilter != FilterType.DISPUTES) {
            currentFilter = FilterType.DISPUTES;
            updateFilterUI();
            loadEscrowData();
        }
    }

    private void updateFilterUI() {
        String activeStyle = "-fx-background-color: -color-primary; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 5 15;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: -color-text-secondary; -fx-padding: 5 15;";

        btnActive.setStyle(currentFilter == FilterType.ACTIVE ? activeStyle : inactiveStyle);
        btnHistory.setStyle(currentFilter == FilterType.HISTORY ? activeStyle : inactiveStyle);
        btnDisputes.setStyle(currentFilter == FilterType.DISPUTES ? activeStyle : inactiveStyle);
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
            loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
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
            loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
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
