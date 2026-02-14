package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletTransaction;

import tn.finhub.model.WalletModel;
import tn.finhub.model.SavedContactModel;
import tn.finhub.util.UserSession;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionsController {

    @FXML
    private javafx.scene.control.ListView<WalletTransaction> transactionListView;
    @FXML
    private javafx.scene.layout.HBox contactsContainer;

    private final WalletModel walletModel = new WalletModel();
    private final SavedContactModel contactModel = new SavedContactModel();
    private final tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();

    // STALE-WHILE-REVALIDATE CACHE
    private static TransactionData cachedData = null;

    @FXML
    public void initialize() {
        setupTransactionList();
        loadData();
    }

    private void setupTransactionList() {
        transactionListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(WalletTransaction tx, boolean empty) {
                super.updateItem(tx, empty);
                if (empty || tx == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    int badTxId = (cachedData != null) ? cachedData.badTxId : -1;
                    java.util.Map<String, String> photos = (cachedData != null) ? cachedData.profilePhotos : null;
                    setGraphic(createTransactionCard(tx, badTxId, photos));
                    setStyle("-fx-background-color: transparent; -fx-padding: 0 0 10 0;");
                }
            }
        });
    }

    private void loadData() {
        tn.finhub.model.User user = UserSession.getInstance().getUser();
        if (user == null)
            return;

        // 0. Optimistic UI Update
        if (cachedData != null && cachedData.userId == user.getId()) {
            updateUI(cachedData);
        }

        // Background Task
        javafx.concurrent.Task<TransactionData> task = new javafx.concurrent.Task<>() {
            @Override
            protected TransactionData call() throws Exception {
                // 1. Fetch Wallet & Transactions
                Wallet wallet = walletModel.findByUserId(user.getId());
                List<WalletTransaction> transactions = java.util.Collections.emptyList();
                int badTxId = -1;

                if (wallet != null) {
                    transactions = walletModel.getTransactionHistory(wallet.getId());
                    if ("FROZEN".equals(wallet.getStatus())) {
                        badTxId = walletModel.getTamperedTransactionId(wallet.getId());
                    }
                }

                // 2. Fetch Contacts
                List<tn.finhub.model.SavedContact> contacts = contactModel.getContactsByUserId(user.getId());

                // 3. Fetch Profile Photos for Transactions
                java.util.Set<String> namesToFetch = new java.util.HashSet<>();
                for (WalletTransaction tx : transactions) {
                    String ref = tx.getReference();
                    if (ref != null) {
                        String cleanRef = ref.replace("Transfer to ", "")
                                .replace("Transfer from ", "")
                                .trim();
                        // Ignore obvious system messages to reduce DB load
                        if (!cleanRef.toUpperCase().startsWith("MARKET ") &&
                                !cleanRef.toUpperCase().startsWith("DEPOSIT") &&
                                !cleanRef.equals("Initial Bank Balance")) {
                            namesToFetch.add(cleanRef);
                        }
                    }
                }

                // Add Contact Names to Fetch List
                for (tn.finhub.model.SavedContact contact : contacts) {
                    if (contact.getContactName() != null) {
                        namesToFetch.add(contact.getContactName().trim());
                    }
                }

                // Use Case-Insensitive Map for robust lookup (User 'Sadok' vs ref 'sadok')
                java.util.Map<String, String> profilePhotos = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                profilePhotos.putAll(userModel.findProfilePhotosByNames(namesToFetch));

                boolean isFrozen = wallet != null && "FROZEN".equals(wallet.getStatus());

                return new TransactionData(user.getId(), transactions, contacts, badTxId, isFrozen, profilePhotos);
            }
        };

        task.setOnSucceeded(e ->

        {
            TransactionData data = task.getValue();
            cachedData = data;
            updateUI(data);
        });

        task.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void updateUI(TransactionData data) {
        // Update Contacts
        if (contactsContainer != null) {
            contactsContainer.getChildren().clear();
            for (tn.finhub.model.SavedContact contact : data.contacts) {
                contactsContainer.getChildren().add(createContactCard(contact, data.isFrozen, data.profilePhotos));
            }
        }

        // Update Transactions
        if (transactionListView != null) {
            if (data.transactions.isEmpty()) {
                // Ideally show a placeholder on the listview, but for now specific empty data
                transactionListView.setItems(javafx.collections.FXCollections.observableArrayList());
                transactionListView.setPlaceholder(new Label("No transactions found."));
            } else {
                transactionListView.setItems(javafx.collections.FXCollections.observableArrayList(data.transactions));
            }
        }
    }

    private void loadContacts() {
        // Delegated to loadData()
        loadData();
    }

    private void loadTransactions() {
        // Delegated to loadData() but kept for compatibility with callbacks
        loadData();
    }

    private javafx.scene.Node createContactCard(tn.finhub.model.SavedContact contact, boolean isFrozen,
            java.util.Map<String, String> profilePhotos) {
        javafx.scene.layout.StackPane card = new javafx.scene.layout.StackPane();

        // Premium Dark Gradient Background & Border
        // Increased height to accommodate actions under name
        String baseStyle = "-fx-background-color: linear-gradient(to bottom right, #374151, #1F2937); -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 4); -fx-min-width: 110; -fx-pref-width: 110; -fx-min-height: 130; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 15;";
        card.setStyle(baseStyle + (isFrozen ? " -fx-opacity: 0.5;" : " -fx-cursor: hand;"));

        // Avatar with initials
        javafx.scene.layout.StackPane iconBg = new javafx.scene.layout.StackPane();
        iconBg.setStyle(
                "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-background-radius: 30; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 30;");
        iconBg.setPrefSize(48, 48);
        iconBg.setMaxSize(48, 48);

        Label initial = new Label(contact.getContactName().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 20px;");

        boolean photoLoaded = false;
        if (profilePhotos != null) {
            String name = contact.getContactName().trim();
            if (profilePhotos.containsKey(name)) {
                String url = profilePhotos.get(name);
                if (url != null && !url.isEmpty()) {
                    try {
                        javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(
                                tn.finhub.util.ImageCache.get(url));
                        imgView.setFitWidth(48);
                        imgView.setFitHeight(48);
                        imgView.setPreserveRatio(true);
                        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(24, 24, 24);
                        imgView.setClip(clip);
                        iconBg.getChildren().add(imgView);
                        photoLoaded = true;
                    } catch (Exception e) {
                        // Fallback
                    }
                }
            }
        }

        if (!photoLoaded) {
            iconBg.getChildren().add(initial);
        }

        Label nameLabel = new Label(contact.getContactName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        nameLabel.setWrapText(false);

        VBox contentBox = new VBox(8, iconBg, nameLabel);
        contentBox.setAlignment(javafx.geometry.Pos.CENTER);
        contentBox.setPadding(new javafx.geometry.Insets(15)); // Add padding here for the content

        // Action Overlay (Hidden by default, appears under name)
        HBox actions = new HBox(15); // Good spacing between buttons
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        actions.setPadding(new javafx.geometry.Insets(5, 0, 0, 0));

        // Key Fix: Use both Opacity AND Visibility to ensure it doesn't block clicks
        // when hidden but animates well
        actions.setOpacity(0);
        actions.setVisible(false); // Start invisible
        actions.setManaged(true); // Keep layout space reserved if desired, or false if overlay.
        // User wants it "under the name". So it should likely take space.
        // If we want it to "appear" without shifting layout, we keep it managed.
        // If we want it to "slide in", we'd animate height.
        // For now, let's keep it taking space but invisible.

        // Style helper for action buttons
        String actionBtnStyle = "-fx-background-color: rgba(255, 255, 255, 0.1); -fx-cursor: hand; -fx-background-radius: 50; -fx-min-width: 28; -fx-min-height: 28; -fx-max-width: 28; -fx-max-height: 28; -fx-alignment: center;";

        // Edit Button
        Button editBtn = new Button();
        editBtn.setStyle(actionBtnStyle);
        javafx.scene.shape.SVGPath editIcon = new javafx.scene.shape.SVGPath();
        editIcon.setContent(
                "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
        editIcon.setStyle("-fx-fill: white; -fx-scale-x: 0.7; -fx-scale-y: 0.7;");
        editBtn.setGraphic(editIcon);
        editBtn.setOnAction(e -> {
            e.consume();
            handleEditContact(contact);
        });

        editBtn.setOnMouseEntered(
                e -> editBtn.setStyle(actionBtnStyle.replace("rgba(255, 255, 255, 0.1)", "rgba(59, 130, 246, 0.3)")));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(actionBtnStyle));

        // Delete Button
        Button deleteBtn = new Button();
        deleteBtn.setStyle(actionBtnStyle);
        javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
        trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        trashIcon.setStyle("-fx-fill: #EF4444; -fx-scale-x: 0.7; -fx-scale-y: 0.7;");
        deleteBtn.setGraphic(trashIcon);
        deleteBtn.setOnAction(e -> {
            e.consume();
            handleDeleteContact(contact);
        });

        deleteBtn.setOnMouseEntered(
                e -> deleteBtn.setStyle(actionBtnStyle.replace("rgba(255, 255, 255, 0.1)", "rgba(239, 68, 68, 0.3)")));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(actionBtnStyle));

        actions.getChildren().addAll(editBtn, deleteBtn);

        // Add acts to contentBox
        contentBox.getChildren().add(actions);

        card.getChildren().add(contentBox);

        if (!isFrozen) {
            card.setOnMouseClicked(e -> handleQuickSend(contact));

            // Hover logic for Card
            card.setOnMouseEntered(e -> {
                actions.setVisible(true); // Make visible
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(200), actions);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();

                card.setStyle(baseStyle.replace("rgba(0,0,0,0.3)", "rgba(16, 185, 129, 0.4)")
                        + " -fx-cursor: hand; -fx-border-color: rgba(16, 185, 129, 0.5);");
            });
            card.setOnMouseExited(e -> {
                actions.setVisible(false); // Hide immediately or fade out
                actions.setOpacity(0);
                card.setStyle(baseStyle + " -fx-cursor: hand;");
            });
        }

        return card;
    }

    private void handleEditContact(tn.finhub.model.SavedContact contact) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        root.setStyle(
                "-fx-background-color: #1F2937; -fx-border-color: #374151; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);");
        root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        root.setMinWidth(300);

        Label title = new Label("Edit Contact");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label msg = new Label("Enter new name for " + contact.getContactName());
        msg.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px;");

        javafx.scene.control.TextField input = new javafx.scene.control.TextField(contact.getContactName());
        input.setStyle(
                "-fx-background-color: #374151; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 10; -fx-border-color: #4B5563; -fx-border-radius: 5;");

        HBox btns = new HBox(10);
        btns.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button cancel = new Button("Cancel");
        cancel.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-cursor: hand; -fx-font-weight: bold;");
        cancel.setOnAction(e -> dialog.close());

        Button save = new Button("Save");
        save.setStyle(
                "-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-weight: bold;");
        save.setOnAction(e -> {
            String newName = input.getText().trim();
            if (!newName.isEmpty()) {
                contactModel.updateContact(contact.getId(), newName);
                loadContacts();
                dialog.close();
            }
        });

        btns.getChildren().addAll(cancel, save);
        root.getChildren().addAll(title, msg, input, btns);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    private void handleDeleteContact(tn.finhub.model.SavedContact contact) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        root.setStyle(
                "-fx-background-color: #1F2937; -fx-border-color: #EF4444; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);");
        root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        root.setMinWidth(300);

        Label title = new Label("Delete Contact");
        title.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label msg = new Label("Are you sure you want to remove " + contact.getContactName() + "?");
        msg.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        HBox btns = new HBox(10);
        btns.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button cancel = new Button("Cancel");
        cancel.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-cursor: hand; -fx-font-weight: bold;");
        cancel.setOnAction(e -> dialog.close());

        Button confirm = new Button("Delete");
        confirm.setStyle(
                "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-weight: bold;");
        confirm.setOnAction(e -> {
            contactModel.deleteContact(contact.getId());
            loadContacts();
            dialog.close();
        });

        btns.getChildren().addAll(cancel, confirm);
        root.getChildren().addAll(title, msg, btns);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    private void handleQuickSend(tn.finhub.model.SavedContact contact) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/transfer_dialog.fxml"));
            loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
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
            loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
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

    // Data Transfer Object
    public static class TransactionData {
        int userId;
        List<WalletTransaction> transactions;
        List<tn.finhub.model.SavedContact> contacts;
        int badTxId;
        boolean isFrozen;
        java.util.Map<String, String> profilePhotos;

        public TransactionData(int uid, List<WalletTransaction> txs, List<tn.finhub.model.SavedContact> contacts,
                int badTxId, boolean isFrozen, java.util.Map<String, String> profilePhotos) {
            this.userId = uid;
            this.transactions = txs;
            this.contacts = contacts;
            this.badTxId = badTxId;
            this.isFrozen = isFrozen;
            this.profilePhotos = profilePhotos;
        }
    }

    public static void setCachedData(TransactionData data) {
        cachedData = data;
    }

    // --- SHARED LOGIC FROM WalletController (Should be refactored eventually) ---
    private javafx.scene.Node createTransactionCard(WalletTransaction tx, int badTxId,
            java.util.Map<String, String> profilePhotos) {
        HBox card = new HBox(15);
        card.getStyleClass().add("transaction-item");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Ensure the card takes full width of list cell
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

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

        // --- Left Details (Also needed for name lookup) ---
        // Clean text first so we can use it for lookup
        String displayRef = tx.getReference();
        if (displayRef != null) {
            displayRef = displayRef.replace("Transfer to ", "")
                    .replace("Transfer from ", "");
            displayRef = displayRef.replaceAll("(?i)\\s+(from|to)\\s+\\d+$", "");

            if (displayRef.startsWith("DEPOSIT via")) {
                displayRef = "Deposit";
            }
        } else {
            displayRef = "Unknown";
        }

        // --- Profile Photo Override ---
        if (profilePhotos != null) {
            // Use the cleaned displayRef as the lookup key (matches logic in loadData)
            String lookupName = displayRef.trim();

            if (!lookupName.isEmpty() && profilePhotos.containsKey(lookupName)) {
                String url = profilePhotos.get(lookupName);
                if (url != null && !url.isEmpty()) {
                    try {
                        javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(
                                tn.finhub.util.ImageCache.get(url));
                        imgView.setFitWidth(40);
                        imgView.setFitHeight(40);
                        imgView.setPreserveRatio(true);
                        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(20, 20, 20);
                        imgView.setClip(clip);
                        iconBg.getChildren().clear(); // Remove default icon
                        iconBg.getChildren().add(imgView);
                    } catch (Exception e) {
                        // Keep default icon on error
                    }
                }
            }
        }

        // --- Left Details ---
        VBox leftDetails = new VBox(3);
        leftDetails.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // displayRef is already calculated above

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

        if (isPositive) {
            prefix = "+";
        } else if (isNegative) {
            prefix = "-";
        }

        amountLabel.setText(prefix + " TND " + tx.getAmount());
        amountLabel.setStyle(isPositive ? "-fx-text-fill: #10B981; -fx-font-weight: bold;"
                : (isNegative ? "-fx-text-fill: #F59E0B; -fx-font-weight: bold;"
                        : "-fx-text-fill: #F59E0B; -fx-font-weight: bold;"));

        Label dateLabel = new Label(
                tx.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        dateLabel.getStyleClass().add("transaction-date");

        rightDetails.getChildren().addAll(amountLabel, dateLabel);

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
            loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
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
            case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS" -> "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"; // Plus
            case "DEBIT", "TRANSFER_SENT" -> "M19 13H5v-2h14v2z"; // Minus
            case "HOLD" ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.95V5h-2.93v2.63c-1.71.49-3.1 1.4-3.1 3.03 0 1.98 1.85 2.7 3.3 3.02 1.98.51 2.44 1.15 2.44 1.91 0 .88-.74 1.41-1.95 1.41-1.55 0-2.34-.66-2.47-1.8H7.13c.12 1.83 1.32 2.7 2.87 3.12V20h2.93v-2.54c1.81-.39 3.25-1.27 3.25-3.17 0-2.02-1.76-2.81-3.19-3.15z";
            default ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
        };
    }
}
