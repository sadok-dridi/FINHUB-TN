package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletTransaction;
import tn.finhub.service.WalletService;
import tn.finhub.util.UserSession;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionsController {

    @FXML
    private VBox transactionContainer;
    @FXML
    private javafx.scene.layout.HBox contactsContainer;

    private final WalletService walletService = new WalletService();
    private final tn.finhub.dao.SavedContactDAO contactDAO = new tn.finhub.dao.SavedContactDAO();

    @FXML
    public void initialize() {
        loadTransactions();
        loadContacts();
    }

    private void loadContacts() {
        tn.finhub.model.User user = UserSession.getInstance().getUser();
        if (user == null)
            return;

        List<tn.finhub.model.SavedContact> contacts = contactDAO.getContactsByUserId(user.getId());
        contactsContainer.getChildren().clear();

        for (tn.finhub.model.SavedContact contact : contacts) {
            contactsContainer.getChildren().add(createContactCard(contact));
        }
    }

    private javafx.scene.Node createContactCard(tn.finhub.model.SavedContact contact) {
        VBox card = new VBox(5);
        card.setStyle(
                "-fx-background-color: rgba(31, 41, 55, 0.7); -fx-background-radius: 12; -fx-padding: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1); -fx-cursor: hand; -fx-min-width: 100; -fx-alignment: center;");

        // Avatar
        javafx.scene.layout.StackPane iconBg = new javafx.scene.layout.StackPane();
        iconBg.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-background-radius: 20;");
        iconBg.setPrefSize(40, 40);

        Label initial = new Label(contact.getContactName().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 18px;");
        iconBg.getChildren().add(initial);

        Label nameLabel = new Label(contact.getContactName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        nameLabel.setWrapText(false);

        card.getChildren().addAll(iconBg, nameLabel);

        card.setOnMouseClicked(e -> handleQuickSend(contact));
        return card;
    }

    private void handleQuickSend(tn.finhub.model.SavedContact contact) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/transfer_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            TransferController controller = loader.getController();
            controller.setRecipient(contact);
            controller.setOnSuccessCallback(this::loadTransactions); // Refresh history after send

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddContact() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/add_contact_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            AddContactController controller = loader.getController();
            controller.setOnContactAdded(this::loadContacts);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTransactions() {
        tn.finhub.model.User user = UserSession.getInstance().getUser();
        if (user == null)
            return;

        Wallet wallet = walletService.getWallet(user.getId());
        if (wallet == null)
            return;

        List<WalletTransaction> transactions = walletService.getTransactionHistory(wallet.getId());

        // Check for tampering
        int badTxId = -1;
        if ("FROZEN".equals(wallet.getStatus())) {
            badTxId = walletService.getTamperedTransactionId(wallet.getId());
        }

        transactionContainer.getChildren().clear();

        if (transactions.isEmpty()) {
            Label placeholder = new Label("No transactions found.");
            placeholder.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px; -fx-padding: 20;");
            transactionContainer.getChildren().add(placeholder);
        } else {
            for (WalletTransaction tx : transactions) {
                transactionContainer.getChildren().add(createTransactionCard(tx, badTxId));
            }
        }
    }

    // --- SHARED LOGIC FROM WalletController (Should be refactored eventually) ---
    private javafx.scene.Node createTransactionCard(WalletTransaction tx, int badTxId) {
        HBox card = new HBox(15);
        card.getStyleClass().add("transaction-item");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Highlight if tampering detected
        if (tx.getId() == badTxId) {
            card.setStyle(
                    "-fx-border-color: #EF4444; -fx-border-width: 2; -fx-background-color: rgba(239, 68, 68, 0.1);");
        }

        // --- Determine Type & Styles ---
        boolean isPositive = "CREDIT".equals(tx.getType()) || "RELEASE".equals(tx.getType())
                || "TRANSFER_RECEIVED".equals(tx.getType()) || "GENESIS".equals(tx.getType());
        boolean isNegative = "DEBIT".equals(tx.getType()) || "TRANSFER_SENT".equals(tx.getType());
        boolean isTransferSent = "TRANSFER_SENT".equals(tx.getType());

        String iconClass = isPositive ? "transaction-icon-positive"
                : (isNegative ? "transaction-icon-negative" : "transaction-icon");

        // --- Icon ---
        javafx.scene.layout.StackPane iconBg = new javafx.scene.layout.StackPane();
        iconBg.getStyleClass().add("transaction-icon-bg");
        iconBg.setPrefSize(40, 40);
        iconBg.setMinSize(40, 40);
        iconBg.setMaxSize(40, 40);

        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        icon.setContent(getIconPath(tx.getType()));
        icon.getStyleClass().addAll("transaction-icon", iconClass);
        iconBg.getChildren().add(icon);

        // --- Left Details (Reference + "Money Send") ---
        VBox leftDetails = new VBox(3);
        leftDetails.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Text Cleaning
        String displayRef = tx.getReference();
        if (displayRef != null) {
            displayRef = displayRef.replace("Transfer to ", "")
                    .replace("Transfer from ", "");
            displayRef = displayRef.replaceAll("(?i)\\s+(from|to)\\s+\\d+$", "");

            if (displayRef.startsWith("DEPOSIT via")) {
                displayRef = "Deposit";
            }
        }

        Label refLabel = new Label(displayRef);
        refLabel.getStyleClass().add("transaction-ref");

        leftDetails.getChildren().add(refLabel);

        if (isTransferSent) {
            Label moneySendLabel = new Label("Money Send");
            moneySendLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            leftDetails.getChildren().add(moneySendLabel);
        }

        if (tx.getId() == badTxId) {
            Label tamperLabel = new Label("TAMPER DETECTED");
            tamperLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 10px;");
            leftDetails.getChildren().add(tamperLabel);
        }

        // --- Spacer ---
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // --- Right Details (Amount + Date) ---
        VBox rightDetails = new VBox(3);
        rightDetails.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label amountLabel = new Label();
        String prefix = "";
        String styleClass = "";

        if (isPositive) {
            prefix = "+";
            styleClass = "amount-positive";
        } else if (isNegative) {
            prefix = "-";
            styleClass = "amount-negative";
        } else {
            styleClass = "amount-hold";
        }

        amountLabel.setText(prefix + " TND " + tx.getAmount());
        amountLabel.getStyleClass().add(styleClass);

        Label dateLabel = new Label(
                tx.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        dateLabel.getStyleClass().add("transaction-date");

        rightDetails.getChildren().addAll(amountLabel, dateLabel);

        card.getChildren().addAll(iconBg, leftDetails, spacer, rightDetails);
        return card;
    }

    private String getIconPath(String type) {
        return switch (type) {
            case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS" -> "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"; // Plus
            case "DEBIT", "TRANSFER_SENT" -> "M19 13H5v-2h14v2z"; // Minus
            case "HOLD" ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.95V5h-2.93v2.63c-1.71.49-3.1 1.4-3.1 3.03 0 1.98 1.85 2.7 3.3 3.02 1.98.51 2.44 1.15 2.44 1.91 0 .88-.74 1.41-1.95 1.41-1.55 0-2.34-.66-2.47-1.8H7.13c.12 1.83 1.32 2.7 2.87 3.12V20h2.93v-2.54c1.81-.39 3.25-1.27 3.25-3.17 0-2.02-1.76-2.81-3.19-3.15z";
            default ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
        };
    }
}
