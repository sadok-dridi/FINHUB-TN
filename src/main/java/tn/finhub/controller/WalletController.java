package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import tn.finhub.model.VirtualCard;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletTransaction;
import tn.finhub.service.VirtualCardService;
import tn.finhub.service.WalletService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class WalletController {

    @FXML
    private Label escrowBalanceLabel;

    @FXML
    private Label availableBalanceLabel;

    @FXML
    private VBox transactionContainer;

    @FXML
    private HBox cardsContainer;

    @FXML
    private Button generateCardButton;

    @FXML
    private VBox frozenAlertBox;

    @FXML
    private Button topUpButton;

    private final WalletService walletService = new WalletService();
    private final VirtualCardService virtualCardService = new VirtualCardService();
    private Wallet currentWallet;

    @FXML
    private void handleDeposit() {
        if (currentWallet != null && "FROZEN".equals(currentWallet.getStatus())) {
            return; // Security check
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/deposit_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            DepositController controller = loader.getController();
            controller.setOnSuccessCallback(this::loadWalletData); // Refresh after deposit

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT); // Make scene transparent
            scene.getStylesheets().add(getClass().getResource("/style/theme.css").toExternalForm());

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT); // Remove OS window frame
            stage.setTitle("Top Up Wallet");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(scene);

            // Make window draggable
            final double[] xOffset = { 0 };
            final double[] yOffset = { 0 };

            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });

            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });

            stage.showAndWait();

            // Force refresh after dialog closes to capture any status changes (e.g.,
            // frozen)
            loadWalletData();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGenerateCard() {
        if (currentWallet != null) {
            if ("FROZEN".equals(currentWallet.getStatus()))
                return; // Security check

            virtualCardService.createCardForWallet(currentWallet.getId());
            refreshVirtualCards(currentWallet.getId());
        }
    }

    @FXML
    public void initialize() {
        loadWalletData();
    }

    private void loadWalletData() {
        tn.finhub.model.User user = tn.finhub.util.UserSession.getInstance().getUser();
        if (user == null)
            return;
        int userId = user.getId();

        // Ensure wallet exists
        walletService.createWalletIfNotExists(userId);

        currentWallet = walletService.getWallet(userId);
        if (currentWallet != null) {

            escrowBalanceLabel.setText(currentWallet.getCurrency() + " " + currentWallet.getEscrowBalance());
            availableBalanceLabel.setText(currentWallet.getCurrency() + " " + currentWallet.getBalance());

            // Check Status
            boolean isFrozen = "FROZEN".equals(currentWallet.getStatus());

            if (frozenAlertBox != null) {
                frozenAlertBox.setVisible(isFrozen);
                frozenAlertBox.setManaged(isFrozen);

                if (isFrozen && frozenAlertBox.getChildren().size() > 1) {
                    tn.finhub.model.LedgerFlag flag = walletService.getLatestFlag(currentWallet.getId());
                    if (flag != null) {
                        javafx.scene.Node descNode = frozenAlertBox.getChildren().get(1); // 2nd child is description

                    }
                }
            } else {
                System.err.println("DEBUG: frozenAlertBox is null!");
            }

            if (topUpButton != null)
                topUpButton.setDisable(isFrozen);
            if (generateCardButton != null)
                generateCardButton.setDisable(isFrozen);

            // Check for specific tampered transaction if frozen
            int badTxId = -1;
            if (isFrozen) {
                badTxId = walletService.getTamperedTransactionId(currentWallet.getId());
            }

            // Load Virtual Cards
            refreshVirtualCards(currentWallet.getId());

            // Load Transactions
            transactionContainer.getChildren().clear();
            List<WalletTransaction> transactions = walletService.getTransactionHistory(currentWallet.getId());

            for (WalletTransaction tx : transactions) {
                transactionContainer.getChildren().add(createTransactionCard(tx, badTxId));
            }
        }
    }

    private void refreshVirtualCards(int walletId) {
        cardsContainer.getChildren().clear();
        List<VirtualCard> cards = virtualCardService.getCardsByWallet(walletId);

        for (VirtualCard card : cards) {
            cardsContainer.getChildren().add(createVirtualCardNode(card));
        }

        // Disable/Hide generate button if a card exists (Max 1)
        if (!cards.isEmpty()) {
            generateCardButton.setVisible(false);
            generateCardButton.setManaged(false);
        } else {
            generateCardButton.setVisible(true);
            generateCardButton.setManaged(true);
        }
    }

    private javafx.scene.Node createVirtualCardNode(VirtualCard card) {
        VBox cardNode = new VBox(20);
        cardNode.getStyleClass().add("virtual-card");

        // --- 1. Header Row (Logo Left, Contactless Right) ---
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Logo Image
        javafx.scene.image.ImageView logoView = null;
        try {
            javafx.scene.image.Image logoImg = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/finhub_logo.png"));
            logoView = new javafx.scene.image.ImageView(logoImg);
            logoView.setFitHeight(30); // Adjust size
            logoView.setPreserveRatio(true);
        } catch (Exception e) {
            // Fallback if image fails
            System.err.println("Could not load logo: " + e.getMessage());
        }

        javafx.scene.Node logoNode = (logoView != null) ? logoView : new javafx.scene.text.Text("FINHUB TN");
        if (logoNode instanceof javafx.scene.text.Text) {
            ((javafx.scene.text.Text) logoNode).setFill(javafx.scene.paint.Color.WHITE);
            ((javafx.scene.text.Text) logoNode).setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");
        }

        javafx.scene.layout.Region headerSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(headerSpacer, javafx.scene.layout.Priority.ALWAYS);

        // Contactless Icon (SVG)
        javafx.scene.shape.SVGPath contactlessIcon = new javafx.scene.shape.SVGPath();
        contactlessIcon.setContent(
                "M12 .586l-1.414 1.414C14.053 5.467 14.053 10.533 10.586 14l1.414 1.414c4.326-4.326 4.326-11.674 0-16zM7.586 4.828L6.172 6.242c1.953 1.953 1.953 5.563 0 7.516l1.414 1.414c2.812-2.812 2.812-7.532 0-10.344zM3.586 8.828l1.414 1.414c.39-.39.39-1.024 0-1.414L3.586 7.414c-1.172 1.172-1.172 3.07 0 4.242z");
        // Actually, let's just rotate a wifi icon 90 degrees or use text ")))" style.
        // For simplicity/reliability in this edit, standard text ")))" rotated or a
        // simple arc path.
        // Using Text "))" rotated is easiest.
        javafx.scene.text.Text contactlessText = new javafx.scene.text.Text(")))");
        contactlessText.setFill(javafx.scene.paint.Color.WHITE);
        // contactlessText.setRotate(90);
        contactlessText.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        header.getChildren().addAll(logoNode, headerSpacer, contactlessText);

        // --- 2. Card Number Section (Middle) ---
        VBox numberBox = new VBox(0); // Tighter VBox spacing
        Label numberTitle = new Label("Card number");
        numberTitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px; -fx-font-family: 'Segoe UI', sans-serif;");

        String num = formatCardNumber(card.getCardNumber());
        Label numberLabel = new Label(num);
        numberLabel.setStyle(
                "-fx-text-fill: white; -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 26px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 1, 1);");

        numberBox.getChildren().addAll(numberTitle, numberLabel);
        numberBox.setPadding(new javafx.geometry.Insets(15, 0, 10, 0)); // Reduced Spacing (Top 15, Bottom 10)

        // --- 3. Footer Section (Bottom Left: Details, Bottom Right: VISA) ---
        HBox footer = new HBox();
        footer.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

        // Details Column (Valid Thru, CVV, Name)
        VBox detailsCol = new VBox(10); // Spacing between Dates row and Name

        // Row for Valid Thru / CVV
        HBox dateCvvRow = new HBox(25);

        // Valid Thru Block
        VBox validBox = new VBox(0);
        Label validTitle = new Label("Valid thru");
        validTitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 9px;");
        Label validValue = new Label(card.getExpiryDate().toString().substring(0, 7)); // YYYY-MM
        validValue.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        validBox.getChildren().addAll(validTitle, validValue);

        // CVV Block
        VBox cvvBox = new VBox(0);
        Label cvvTitle = new Label("CVV");
        cvvTitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 9px;");
        Label cvvValue = new Label(card.getCvv());
        cvvValue.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        cvvBox.getChildren().addAll(cvvTitle, cvvValue);

        dateCvvRow.getChildren().addAll(validBox, cvvBox);

        // Name Block
        Label nameLabel = new Label("SADOK DRIDI");
        nameLabel.setStyle(
                "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: normal; -fx-opacity: 0.9; -fx-font-family: 'Segoe UI', sans-serif;");

        detailsCol.getChildren().addAll(dateCvvRow, nameLabel);

        javafx.scene.layout.Region footerSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);

        // VISA Logo (Text)
        javafx.scene.text.Text visaLogo = new javafx.scene.text.Text("VISA");
        visaLogo.setFill(javafx.scene.paint.Color.WHITE);
        visaLogo.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-font-style: italic;");

        footer.getChildren().addAll(detailsCol, footerSpacer, visaLogo);

        // --- Main Layout ---
        VBox contentBox = new VBox(5);
        contentBox.getChildren().addAll(header, numberBox, footer);
        VBox.setVgrow(footer, javafx.scene.layout.Priority.ALWAYS); // Push footer down

        // --- BLUR EFFECT ---
        javafx.scene.effect.GaussianBlur blur = new javafx.scene.effect.GaussianBlur(30);
        contentBox.setEffect(blur); // Blurred by default

        cardNode.getChildren().add(contentBox);

        // --- CLICK TO REVEAL (Smooth Animation) ---
        cardNode.setOnMouseClicked(e -> {
            double currentRadius = blur.getRadius();
            double targetRadius = (currentRadius > 0) ? 0 : 30; // Toggle target

            // Create Timeline for smooth animation
            javafx.animation.Timeline timeline = new javafx.animation.Timeline();
            javafx.animation.KeyValue kv = new javafx.animation.KeyValue(
                    blur.radiusProperty(),
                    targetRadius,
                    javafx.animation.Interpolator.EASE_BOTH);
            javafx.animation.KeyFrame kf = new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(500),
                    kv);

            timeline.getKeyFrames().add(kf);
            timeline.play();
        });

        // --- STYLE: Gradient Background (Purple -> Pink -> Cyan) ---
        cardNode.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #22D3EE, #C026D3, #4C1D95); " +
                        "-fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.4), 20, 0, 0, 10); " + // Soft shadow
                        "-fx-padding: 25; " +
                        "-fx-min-width: 340; " +
                        "-fx-min-height: 220; " +
                        "-fx-cursor: hand;");

        return cardNode;
    }

    private String formatCardNumber(String number) {
        // ... (unchanged) ...
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            if (i > 0 && i % 4 == 0)
                sb.append(" ");
            sb.append(number.charAt(i));
        }
        return sb.toString();
    }

    private javafx.scene.Node createTransactionCard(WalletTransaction tx, int badTxId) {
        HBox card = new HBox(15);
        card.getStyleClass().add("transaction-item");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Highlight if tampering detected
        if (tx.getId() == badTxId) {
            card.setStyle(
                    "-fx-border-color: #EF4444; -fx-border-width: 2; -fx-background-color: rgba(239, 68, 68, 0.1);");
        }

        // Icon
        javafx.scene.layout.StackPane iconBg = new javafx.scene.layout.StackPane();
        iconBg.getStyleClass().add("transaction-icon-bg");
        iconBg.setPrefSize(40, 40);
        iconBg.setMinSize(40, 40); // Force circle shape
        iconBg.setMaxSize(40, 40); // Prevent stretching

        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        icon.setContent(getIconPath(tx.getType()));
        icon.getStyleClass().add("transaction-icon");
        iconBg.getChildren().add(icon);

        // Details
        VBox details = new VBox(3);
        Label refLabel = new Label(tx.getReference());
        refLabel.getStyleClass().add("transaction-ref");

        Label dateLabel = new Label(
                tx.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        dateLabel.getStyleClass().add("transaction-date");

        details.getChildren().addAll(refLabel, dateLabel);

        // Add "TAMPERED" Badge if applicable
        if (tx.getId() == badTxId) {
            Label tamperLabel = new Label("TAMPER DETECTED");
            tamperLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 10px;");
            details.getChildren().add(tamperLabel);
        }

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Amount
        Label amountLabel = new Label();
        String prefix = "";
        String styleClass = "";

        if ("CREDIT".equals(tx.getType()) || "RELEASE".equals(tx.getType())) {
            prefix = "+";
            styleClass = "amount-positive";
        } else if ("DEBIT".equals(tx.getType())) {
            prefix = "-";
            styleClass = "amount-negative";
        } else {
            styleClass = "amount-hold"; // Hold
        }

        amountLabel.setText(prefix + " TND " + tx.getAmount());
        amountLabel.getStyleClass().add(styleClass);

        card.getChildren().addAll(iconBg, details, spacer, amountLabel);
        return card;
    }

    private String getIconPath(String type) {
        return switch (type) {
            case "CREDIT", "RELEASE" -> "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"; // Plus
            case "DEBIT" -> "M19 13H5v-2h14v2z"; // Minus
            case "HOLD" ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.95V5h-2.93v2.63c-1.71.49-3.1 1.4-3.1 3.03 0 1.98 1.85 2.7 3.3 3.02 1.98.51 2.44 1.15 2.44 1.91 0 .88-.74 1.41-1.95 1.41-1.55 0-2.34-.66-2.47-1.8H7.13c.12 1.83 1.32 2.7 2.87 3.12V20h2.93v-2.54c1.81-.39 3.25-1.27 3.25-3.17 0-2.02-1.76-2.81-3.19-3.15z"; // Dollar/Lock
            default ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
        };
    }
}
