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
    private final tn.finhub.dao.MarketDAO marketDAO = new tn.finhub.dao.MarketDAO();
    private final tn.finhub.service.MarketDataService marketDataService = new tn.finhub.service.MarketDataService();
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
    private void handleTransfer() {
        if (currentWallet != null && "FROZEN".equals(currentWallet.getStatus())) {
            return; // Security check
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/transfer_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            TransferController controller = loader.getController();
            controller.setOnSuccessCallback(this::loadWalletData); // Refresh after transfer

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            // Apply theme if available, or rely on inline styles which we used in FXML
            // scene.getStylesheets().add(getClass().getResource("/style/theme.css").toExternalForm());

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setTitle("Send Money");
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
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(31), e -> loadWalletData()));
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void loadWalletData() {
        tn.finhub.model.User user = tn.finhub.util.UserSession.getInstance().getUser();
        if (user == null)
            return;
        int userId = user.getId();

        // Ensure wallet exists ONLY for regular users
        if ("USER".equalsIgnoreCase(user.getRole())) {
            walletService.createWalletIfNotExists(userId);
        }

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
                        // Logic for setting flag description if needed
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

            // Load Transactions (Limit to 5 for Dashboard)
            transactionContainer.getChildren().clear();
            List<WalletTransaction> transactions = walletService.getTransactionHistory(currentWallet.getId());

            int limit = Math.min(transactions.size(), 4);
            for (int i = 0; i < limit; i++) {
                transactionContainer.getChildren().add(createTransactionCard(transactions.get(i), badTxId));
            }
        }
    }

    private void refreshVirtualCards(int walletId) {
        cardsContainer.getChildren().clear();
        List<VirtualCard> cards = virtualCardService.getCardsByWallet(walletId);

        // Left Wrapper (Aligns with Available Balance Card)
        javafx.scene.layout.HBox leftWrapper = new javafx.scene.layout.HBox(15);
        leftWrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.HBox.setHgrow(leftWrapper, javafx.scene.layout.Priority.ALWAYS);
        leftWrapper.setMinWidth(0);
        leftWrapper.setPrefWidth(0); // Force equal distribution start point

        for (VirtualCard card : cards) {
            leftWrapper.getChildren().add(createVirtualCardNode(card));
        }

        // Right Wrapper (Aligns with Escrow Card - Centers Portfolio Card)
        javafx.scene.layout.VBox rightWrapper = new javafx.scene.layout.VBox();
        javafx.scene.layout.HBox.setHgrow(rightWrapper, javafx.scene.layout.Priority.ALWAYS);
        rightWrapper.setMinWidth(0);
        rightWrapper.setPrefWidth(0); // Force equal distribution start point
        rightWrapper.setAlignment(javafx.geometry.Pos.TOP_LEFT); // Default VBox fill behavior works best without
                                                                 // centering alignment on the box itself if we want
                                                                 // fill. But child is max_value.

        if (walletId > 0) {
            rightWrapper.getChildren().add(createPortfolioPerformanceNode(walletId));
        }

        cardsContainer.getChildren().addAll(leftWrapper, rightWrapper);

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
        tn.finhub.model.User user = tn.finhub.util.UserSession.getInstance().getUser();
        String holderName = (user != null && user.getFullName() != null) ? user.getFullName().toUpperCase()
                : "VALUED USER";
        Label nameLabel = new Label(holderName);
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

        // --- Determine Type & Styles ---
        boolean isPositive = "CREDIT".equals(tx.getType()) || "RELEASE".equals(tx.getType())
                || "TRANSFER_RECEIVED".equals(tx.getType());
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
        icon.getStyleClass().addAll("transaction-icon", iconClass); // Base class + color modifier
        iconBg.getChildren().add(icon);

        // --- Left Details (Reference + "Money Send") ---
        VBox leftDetails = new VBox(3);
        leftDetails.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Text Cleaning
        String displayRef = tx.getReference();
        if (displayRef != null) {
            // Remove "Transfer to/from " prefixes
            displayRef = displayRef.replace("Transfer to ", "")
                    .replace("Transfer from ", "");

            // Remove " to <id>" or " from <id>" at end of string
            // Regex: match space + (from|to) + space + digits + end of line
            displayRef = displayRef.replaceAll("(?i)\\s+(from|to)\\s+\\d+$", "");

            if (displayRef.startsWith("DEPOSIT via")) {
                displayRef = "Deposit";
            }
            // Clean up P2P artifacts if any ("P2P Transfer via..." -> "P2P Transfer")
            // User asked to remove "from .."
            if (displayRef.startsWith("P2P Transfer")) {
                // The regex above handles the trailing ID.
                // If it's "P2P Transfer via Card **** 1234", maybe simplify?
                // User request: "for p2p changee to to another text remove from .." -> implied
                // cleaning.
            }
        }

        Label refLabel = new Label(displayRef);
        refLabel.getStyleClass().add("transaction-ref");

        leftDetails.getChildren().add(refLabel);

        // Add "Money Send" subtext if it's a sent transfer
        if (isTransferSent) {
            Label moneySendLabel = new Label("Money Send");
            moneySendLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            leftDetails.getChildren().add(moneySendLabel);
        }

        // Add "TAMPERED" Badge if applicable
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

        // Amount Logic
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

        // Date Logic
        Label dateLabel = new Label(
                tx.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        dateLabel.getStyleClass().add("transaction-date");
        // Align date right explicitly if needed by CSS but VBox handles it
        // dateLabel.setStyle("-fx-alignment: CENTER-RIGHT;");

        rightDetails.getChildren().addAll(amountLabel, dateLabel);

        // Assemble
        card.getChildren().addAll(iconBg, leftDetails, spacer, rightDetails);

        // --- Click Action ---
        card.setOnMouseClicked(e -> showTransactionDetails(tx));
        card.setCursor(javafx.scene.Cursor.HAND);

        return card;
    }

    private void showTransactionDetails(WalletTransaction tx) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/transaction_details.fxml"));
            javafx.scene.Parent root = loader.load();

            TransactionDetailsController controller = loader.getController();
            controller.setTransaction(tx);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setTitle("Transaction Details");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(scene);

            // Close on lost focus (optional, but nice for details popups)
            stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    // stage.close(); // Uncomment if auto-close is desired
                }
            });

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

            stage.show();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private String getIconPath(String type) {
        return switch (type) {
            // Plus Icon for incoming
            case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS" -> "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
            // Minus Icon for outgoing
            case "DEBIT", "TRANSFER_SENT" -> "M19 13H5v-2h14v2z";
            // Lock/Hold Icon
            case "HOLD" ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.95V5h-2.93v2.63c-1.71.49-3.1 1.4-3.1 3.03 0 1.98 1.85 2.7 3.3 3.02 1.98.51 2.44 1.15 2.44 1.91 0 .88-.74 1.41-1.95 1.41-1.55 0-2.34-.66-2.47-1.8H7.13c.12 1.83 1.32 2.7 2.87 3.12V20h2.93v-2.54c1.81-.39 3.25-1.27 3.25-3.17 0-2.02-1.76-2.81-3.19-3.15z";
            default ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
        };
    }

    private javafx.scene.Node createPortfolioPerformanceNode(int walletId) {
        VBox node = new VBox(15);
        node.setPrefHeight(220);
        node.setMinHeight(220);
        node.setMaxWidth(Double.MAX_VALUE); // Allow growing to fill width
        node.getStyleClass().add("card-total"); // Use consistent theme style

        // Header
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label("Market Portfolio");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Live Indicator
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(3, javafx.scene.paint.Color.valueOf("#10B981"));
        Label live = new Label(" LIVE");
        live.setStyle("-fx-text-fill: #10B981; -fx-font-size: 10px; -fx-font-weight: bold;");
        HBox badge = new HBox(dot, live);
        badge.setAlignment(javafx.geometry.Pos.CENTER);
        badge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.1); -fx-padding: 4 8; -fx-background-radius: 10;");

        header.getChildren().addAll(title, spacer, badge);

        // Calculate PnL
        tn.finhub.model.User user = tn.finhub.util.UserSession.getInstance().getUser();
        int userId = user != null ? user.getId() : 0;

        java.math.BigDecimal totalInvested = java.math.BigDecimal.ZERO;
        java.math.BigDecimal currentValue = java.math.BigDecimal.ZERO;

        java.math.BigDecimal maxPnlPercent = java.math.BigDecimal.valueOf(-9999);
        String bestAsset = "None";
        int assetCount = 0;

        java.util.List<tn.finhub.model.PortfolioItem> items = marketDAO.getPortfolio(userId);
        java.math.BigDecimal exchangeRate = marketDataService.getUsdToTndRate(); // Ensure TND conversion

        for (tn.finhub.model.PortfolioItem item : items) {
            if (item.getQuantity().compareTo(java.math.BigDecimal.ZERO) > 0) {
                assetCount++;
                // Invested (Cost Basis) - Stored in USD usually, convert to TND
                // But wait, averageCost in SimulationService was stored in USD.
                // Let's assume averageCost is USD.
                java.math.BigDecimal investedUSD = item.getAverageCost().multiply(item.getQuantity());
                totalInvested = totalInvested.add(investedUSD.multiply(exchangeRate));

                // Current Value
                tn.finhub.model.MarketPrice price = marketDataService.getPrice(item.getSymbol());
                if (price != null) {
                    java.math.BigDecimal valUSD = price.getPrice().multiply(item.getQuantity());
                    currentValue = currentValue.add(valUSD.multiply(exchangeRate));

                    // Check Logic for Best Performer (based on individual item PnL %)
                    if (item.getAverageCost().doubleValue() > 0) {
                        java.math.BigDecimal itemPnlPct = (price.getPrice().subtract(item.getAverageCost()))
                                .divide(item.getAverageCost(), 4, java.math.RoundingMode.HALF_UP)
                                .multiply(java.math.BigDecimal.valueOf(100));

                        if (itemPnlPct.compareTo(maxPnlPercent) > 0) {
                            maxPnlPercent = itemPnlPct;
                            bestAsset = item.getSymbol();
                        }
                    }
                }
            }
        }

        java.math.BigDecimal pnl = currentValue.subtract(totalInvested);
        double pnlValue = pnl.doubleValue();
        double percent = (totalInvested.doubleValue() > 0) ? (pnlValue / totalInvested.doubleValue()) * 100 : 0;

        // Limit PnL decimal points
        String pnlStr = String.format("%.2f", pnlValue);
        String pctStr = String.format("%.2f%%", percent);
        boolean isProfit = pnlValue >= 0;
        String color = isProfit ? "#10B981" : "#EF4444";
        String sign = isProfit ? "+" : "";

        // --- Main Content Row (Horizontal Split) ---
        HBox contentRow = new HBox(20);
        contentRow.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT); // Align to bottom baseline
        VBox.setVgrow(contentRow, javafx.scene.layout.Priority.ALWAYS); // Fill height

        // 1. Left Side: Total Value
        VBox leftSide = new VBox(5);
        leftSide.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT); // Bottom aligned

        String valText = "TND " + String.format("%.2f", currentValue.doubleValue());
        Label valLabel = new Label(valText);

        // Auto-Resize Logic (Simple Length Heuristic)
        int len = valText.length();
        int fontSize = 32;
        if (len > 18)
            fontSize = 18;
        else if (len > 14)
            fontSize = 22;
        else if (len > 10)
            fontSize = 26;

        valLabel.setStyle("-fx-text-fill: white; -fx-font-size: " + fontSize + "px; -fx-font-weight: bold;");

        Label subLabel = new Label("Total Asset Value");
        subLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");

        leftSide.getChildren().addAll(valLabel, subLabel);

        // Spacer between left and right
        javafx.scene.layout.Region midSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(midSpacer, javafx.scene.layout.Priority.ALWAYS);

        // 2. Right Side: PnL & Invested
        VBox rightSide = new VBox(8);
        rightSide.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT); // Bottom aligned (Baseline match)

        // PnL Badge (Row)
        HBox pnlBadge = new HBox(10);
        pnlBadge.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        String pnlText = sign + "TND " + pnlStr;
        Label pnlAmtLabel = new Label(pnlText);

        // PnL Resize Logic
        int pnlSize = 14;
        if (pnlText.length() > 12)
            pnlSize = 11;

        pnlAmtLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + pnlSize + "px; -fx-font-weight: bold;");

        Label pnlPctLabel = new Label(sign + pctStr);
        pnlPctLabel.setStyle("-fx-text-fill: " + color + "; -fx-background-color: "
                + (isProfit ? "rgba(16, 185, 129, 0.1)" : "rgba(239, 68, 68, 0.1)")
                + "; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: bold;");

        pnlBadge.getChildren().addAll(pnlAmtLabel, pnlPctLabel);

        // Invested Label
        Label investedLabel = new Label("Est. Cost: TND " + String.format("%.2f", totalInvested.doubleValue()));
        investedLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");

        rightSide.getChildren().addAll(pnlBadge, investedLabel);

        contentRow.getChildren().addAll(leftSide, midSpacer, rightSide);

        // --- Footer Row (New Details) ---
        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-opacity: 0.1; -fx-background-color: white;");

        HBox footerRow = new HBox(20);
        footerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Detail 1: Asset Count
        HBox assetDetail = new HBox(5);
        assetDetail.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.shape.SVGPath assetIcon = new javafx.scene.shape.SVGPath();
        assetIcon.setContent("M4 6h16v2H4zm2 4h12v2H6zm2 4h8v2H8z"); // Simple stack icon
        assetIcon.setStyle("-fx-fill: #9CA3AF;");
        Label assetLbl = new Label(assetCount + " Assets");
        assetLbl.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 11px; -fx-font-weight: bold;");
        assetDetail.getChildren().addAll(assetIcon, assetLbl);

        // Detail 2: Best Performer
        HBox perfDetail = new HBox(5);
        perfDetail.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.shape.SVGPath starIcon = new javafx.scene.shape.SVGPath();
        starIcon.setContent(
                "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z");
        starIcon.setStyle("-fx-fill: #F59E0B;"); // Gold/Orange star

        String perfText = "Top: " + (bestAsset.equals("None") ? bestAsset : bestAsset.toUpperCase());
        if (!"None".equals(bestAsset)) {
            perfText += String.format(" (%+.2f%%)", maxPnlPercent.doubleValue());
        }
        Label perfLbl = new Label(perfText);
        perfLbl.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 11px; -fx-font-weight: bold;");
        perfDetail.getChildren().addAll(starIcon, perfLbl);

        footerRow.getChildren().addAll(assetDetail, perfDetail);

        node.getChildren().addAll(header, contentRow, sep, footerRow);

        // Make Clickable
        node.setCursor(javafx.scene.Cursor.HAND);
        node.setOnMouseClicked(e -> navigateToMarket());

        return node;
    }

    private void navigateToMarket() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/financial_twin.fxml"));
            javafx.scene.Parent view = loader.load();

            javafx.scene.Scene scene = cardsContainer.getScene();
            if (scene != null) {
                // Target the inner dashboard content container (inside UserDashboard)
                // This preserves the sidebar!
                javafx.scene.layout.StackPane dashboardContent = (javafx.scene.layout.StackPane) scene
                        .lookup("#dashboardContent");

                if (dashboardContent != null) {
                    dashboardContent.getChildren().setAll(view);
                } else {
                    // Fallback: If we assume we are not in dashboard (shouldn't happen), try main
                    // content
                    javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) scene
                            .lookup("#contentArea");
                    if (contentArea != null) {
                        contentArea.getChildren().setAll(view);
                    }
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
