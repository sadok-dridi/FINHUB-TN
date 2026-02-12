package tn.finhub.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.finhub.model.LedgerFlag;
import tn.finhub.model.User;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletTransaction;
import tn.finhub.model.WalletModel;
import tn.finhub.util.DialogUtil;

import java.time.format.DateTimeFormatter;

public class AdminUserTransactionsController {

    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private Label walletStatusLabel;
    @FXML
    private ListView<WalletTransaction> transactionsListView;
    @FXML
    private VBox ledgerAlertBox;
    @FXML
    private Label ledgerAlertMessage;

    private User user;

    private WalletModel walletModel = new WalletModel();

    public void setUser(User user) {
        this.user = user;
        nameLabel.setText(user.getFullName());
        emailLabel.setText(user.getEmail());

        // Set Loading State
        balanceLabel.setText("Loading...");
        walletStatusLabel.setText("...");
        ledgerAlertBox.setVisible(false);
        ledgerAlertBox.setManaged(false);

        Label loadingLabel = new Label("Loading transactions...");
        loadingLabel.setStyle("-fx-text-fill: white;");
        transactionsListView.setPlaceholder(loadingLabel);
        transactionsListView.setItems(FXCollections.observableArrayList()); // Clear old data

        // Background Task
        javafx.concurrent.Task<WalletFetchResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected WalletFetchResult call() throws Exception {
                Wallet w = walletModel.findByUserId(user.getId());
                if (w == null)
                    return null;

                // Re-fetch to ensure fresh data (as per original logic)
                w = walletModel.findById(w.getId());

                // Fetch transactions
                java.util.List<WalletTransaction> txHistory = walletModel.getTransactionHistory(w.getId());

                // Fetch tampered ID
                int tamperedId = walletModel.getTamperedTransactionId(w.getId());

                // Fetch latest flag if frozen
                LedgerFlag flag = null;
                if ("FROZEN".equals(w.getStatus())) {
                    flag = walletModel.getLatestFlag(w.getId());
                }

                return new WalletFetchResult(w, txHistory, tamperedId, flag);
            }
        };

        task.setOnSucceeded(e -> {
            WalletFetchResult result = task.getValue();
            if (result == null) {
                balanceLabel.setText("No Wallet");
                walletStatusLabel.setText("N/A");
                transactionsListView.setPlaceholder(new Label("No wallet found for this user."));
            } else {
                updateUIWithData(result);
            }
        });

        task.setOnFailed(e -> {
            balanceLabel.setText("Error");
            walletStatusLabel.setText("Error");
            transactionsListView.setPlaceholder(new Label("Failed to load data."));
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void updateUIWithData(WalletFetchResult result) {
        // Update Wallet Info
        balanceLabel.setText(String.format("%.3f TND", result.wallet.getBalance()));
        String status = result.wallet.getStatus();
        walletStatusLabel.setText(status);

        if ("FROZEN".equals(status)) {
            walletStatusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
            ledgerAlertBox.setVisible(true);
            ledgerAlertBox.setManaged(true);

            if (result.flag != null) {
                ledgerAlertMessage.setText(result.flag.getReason() + " (at "
                        + result.flag.getFlaggedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ")");
            } else {
                ledgerAlertMessage.setText("Wallet is frozen.");
            }
        } else {
            walletStatusLabel.setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");
            ledgerAlertBox.setVisible(false);
            ledgerAlertBox.setManaged(false);
        }

        // Update Transactions
        ObservableList<WalletTransaction> items = FXCollections.observableArrayList(result.transactions);
        transactionsListView.setItems(items);
        transactionsListView.setCellFactory(param -> new ListCell<WalletTransaction>() {
            @Override
            protected void updateItem(WalletTransaction tx, boolean empty) {
                super.updateItem(tx, empty);
                if (empty || tx == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox card = createTransactionCard(tx, tx.getId() == result.tamperedId);
                    setGraphic(card);
                    setStyle("-fx-background-color: transparent; -fx-padding: 5;");
                }
            }
        });

        if (items.isEmpty()) {
            transactionsListView.setPlaceholder(new Label("No transactions found."));
        }
    }

    // DTO for result
    private static class WalletFetchResult {
        Wallet wallet;
        java.util.List<WalletTransaction> transactions;
        int tamperedId;
        LedgerFlag flag;

        public WalletFetchResult(Wallet wallet, java.util.List<WalletTransaction> transactions, int tamperedId,
                LedgerFlag flag) {
            this.wallet = wallet;
            this.transactions = transactions;
            this.tamperedId = tamperedId;
            this.flag = flag;
        }
    }

    private HBox createTransactionCard(WalletTransaction tx, boolean isTampered) {
        HBox card = new HBox(15);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: " + (isTampered ? "rgba(239, 68, 68, 0.1)" : "-color-card-bg") + ";" +
                "-fx-border-color: " + (isTampered ? "#EF4444" : "transparent") + ";" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 15;");

        // Icon
        javafx.scene.shape.Circle iconBg = new javafx.scene.shape.Circle(20);
        iconBg.setFill(
                isTampered ? javafx.scene.paint.Color.valueOf("#EF4444") : javafx.scene.paint.Color.valueOf("#2D2A40"));
        Label iconLabel = new Label(isTampered ? "!" : getIconForType(tx.getType()));
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        StackPane icon = new StackPane(iconBg, iconLabel);

        // Details
        VBox details = new VBox(2);
        Label refLabel = new Label(tx.getReference());
        refLabel.setStyle("-fx-text-fill: -color-text-primary; -fx-font-weight: bold;");
        Label dateLabel = new Label(tx.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")));
        dateLabel.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 11px;");

        Label hashLabel = new Label("Hash: " + tx.getTxHash().substring(0, 10) + "...");
        hashLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 10px; -fx-font-family: 'Monospaced';");

        details.getChildren().addAll(refLabel, dateLabel, hashLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Amount
        Label amountLabel = new Label(String.format("%s%.3f", getSign(tx.getType()), tx.getAmount()));
        amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + getColorForType(tx.getType()) + ";");

        // Action Buttons
        card.getChildren().addAll(icon, details, spacer, amountLabel);

        if (isTampered) {
            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().add("button-small-primary");
            editBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;"); // Make it red/urgent
            editBtn.setOnAction(e -> handleEditTransaction(tx));
            card.getChildren().add(editBtn);

            Label tamperedLabel = new Label("VIOLATION");
            tamperedLabel.setStyle(
                    "-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 10px; -fx-border-color: #EF4444; -fx-border-radius: 4px; -fx-padding: 2 4;");
            details.getChildren().add(0, tamperedLabel); // Add to top of details
        }

        return card;
    }

    private String getIconForType(String type) {
        return switch (type) {
            case "CREDIT", "DEPOSIT" -> "↓";
            case "DEBIT", "WITHDRAWAL" -> "↑";
            case "TRANSFER_SENT" -> "→";
            case "TRANSFER_RECEIVED" -> "←";
            default -> "●";
        };
    }

    private String getSign(String type) {
        return switch (type) {
            case "DEBIT", "WITHDRAWAL", "TRANSFER_SENT", "HOLD" -> "-";
            default -> "+";
        };
    }

    private String getColorForType(String type) {
        return switch (type) {
            case "DEBIT", "WITHDRAWAL", "TRANSFER_SENT", "HOLD" -> "#F87171"; // Red
            default -> "#34D399"; // Green
        };
    }

    private void handleEditTransaction(WalletTransaction tx) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_repair_dialog.fxml"));
            javafx.scene.Parent page = loader.load();

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Repair Ledger");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(nameLabel.getScene().getWindow());
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT); // Custom style

            javafx.scene.Scene scene = new javafx.scene.Scene(page);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            // Add CSS if needed, usually global css is enough if added to scene
            if (nameLabel.getScene().getStylesheets() != null) {
                scene.getStylesheets().addAll(nameLabel.getScene().getStylesheets());
            }

            dialogStage.setScene(scene);

            AdminRepairDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setTransaction(tx);

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                setUser(this.user); // Refresh
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Could not open repair dialog.");
        }
    }

    @FXML
    public void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_transactions.fxml"));
            javafx.scene.Parent view = loader.load();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) transactionsListView.getScene()
                    .lookup("#adminContentArea");

            if (contentArea != null) {
                // Fade Out Current
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(200), contentArea);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    // Switch View
                    contentArea.getChildren().clear();
                    contentArea.getChildren().add(view);

                    // Fade In New
                    javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                            javafx.util.Duration.millis(200), contentArea);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            } else {
                System.err.println("Critical Error: #adminContentArea not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGoToUsers() {
        handleBack(); // Since back goes to user list/transactions list
    }
}
