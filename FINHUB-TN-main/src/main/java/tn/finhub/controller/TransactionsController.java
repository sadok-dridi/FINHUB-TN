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
                    java.util.Map<Integer, String> names = (cachedData != null) ? cachedData.walletOwnerNames : null;
                    setGraphic(createTransactionCard(tx, badTxId, photos, names));
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
                java.util.Set<String> transactionRefs = new java.util.HashSet<>();
                java.util.Set<Integer> walletIdsToFetch = new java.util.HashSet<>();
                java.util.Map<String, Integer> nameToWalletIdMap = new java.util.HashMap<>();

                // Regex to find Wallet IDs hidden in ref
                // Pattern 1: Legacy "Transfer to 123"
                java.util.regex.Pattern legacyIdPattern = java.util.regex.Pattern
                        .compile("\\s+(?:to|from)\\s+(\\d+)(?:$|\\s)");
                // Pattern 2: New "Name (Wallet 123)"
                java.util.regex.Pattern walletIdPattern = java.util.regex.Pattern.compile("\\(Wallet\\s+(\\d+)\\)");
                // Pattern 3: Escrow "Released to Wallet 123"
                java.util.regex.Pattern escrowIdPattern = java.util.regex.Pattern
                        .compile("Released to Wallet\\s+(\\d+)");
                // Pattern 4: Escrow "Received from Escrow Wallet 123"
                java.util.regex.Pattern escrowRcvdPattern = java.util.regex.Pattern
                        .compile("Received from Escrow Wallet\\s+(\\d+)");

                for (WalletTransaction tx : transactions) {
                    String ref = tx.getReference();
                    if (ref != null) {
                        String cleanRef = ref.replace("Transfer to ", "")
                                .replace("Transfer from ", "")
                                .trim();

                        int wId = -1;
                        // Try New Pattern First
                        java.util.regex.Matcher mNew = walletIdPattern.matcher(ref);
                        if (mNew.find()) {
                            try {
                                wId = Integer.parseInt(mNew.group(1));
                            } catch (Exception ignore) {
                            }
                        }

                        // Try Legacy Pattern if no ID found
                        if (wId == -1) {
                            java.util.regex.Matcher mOld = legacyIdPattern.matcher(ref);
                            if (mOld.find()) {
                                try {
                                    wId = Integer.parseInt(mOld.group(1));
                                } catch (Exception ignore) {
                                }
                            }
                        }

                        // Try Escrow Pattern if no ID found
                        if (wId == -1) {
                            java.util.regex.Matcher mEscrow = escrowIdPattern.matcher(ref);
                            if (mEscrow.find()) {
                                try {
                                    wId = Integer.parseInt(mEscrow.group(1));
                                } catch (Exception ignore) {
                                }
                            }
                        }

                        // Try Escrow Received Pattern if no ID found
                        if (wId == -1) {
                            java.util.regex.Matcher mEscrowRcvd = escrowRcvdPattern.matcher(ref);
                            if (mEscrowRcvd.find()) {
                                try {
                                    wId = Integer.parseInt(mEscrowRcvd.group(1));
                                } catch (Exception ignore) {
                                }
                            }
                        }

                        // Identifier Cleaning (Strip ID for display/name-lookup)
                        String displayRef = cleanRef.replaceAll("(?i)\\s+(from|to)\\s+\\d+$", "")
                                .replaceAll("\\(Wallet\\s+\\d+\\)", "")
                                .trim();

                        if (wId != -1) {
                            walletIdsToFetch.add(wId);
                            if (!displayRef.isEmpty()) {
                                nameToWalletIdMap.put(displayRef, wId);
                                if (displayRef.isEmpty() || displayRef.matches("\\d+")) {
                                    nameToWalletIdMap.put(String.valueOf(wId), wId);
                                }
                            }
                        }

                        // Identifier Collection (Legacy Name or Email)
                        // Ignore obvious system messages to reduce DB load
                        if (!displayRef.toUpperCase().startsWith("MARKET ") &&
                                !displayRef.toUpperCase().startsWith("DEPOSIT") &&
                                !displayRef.equals("Initial Bank Balance") &&
                                !displayRef.isEmpty()) {
                            transactionRefs.add(displayRef);
                        }
                    }
                }

                // Add Contact Names to Fetch List (Legacy/Fallback)
                for (tn.finhub.model.SavedContact contact : contacts) {
                    if (contact.getContactName() != null) {
                        transactionRefs.add(contact.getContactName().trim());
                    }
                }

                // CRITICAL FIX: Exclude current user's name to prevent showing own photo for
                // same-name recipients
                if (user.getFullName() != null) {
                    transactionRefs.remove(user.getFullName().trim());
                }

                // Use Case-Insensitive Map for robust lookup (User 'Sadok' vs ref 'sadok')
                java.util.Map<String, String> profilePhotos = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                if (!transactionRefs.isEmpty()) {
                    profilePhotos.putAll(userModel.findProfilePhotosByNames(transactionRefs));
                    profilePhotos.putAll(userModel.findProfilePhotosByEmails(transactionRefs));
                }

                // Overlay Wallet ID results (Highest Priority)
                java.util.Map<Integer, String> walletOwnerNames = new java.util.HashMap<>();
                if (!walletIdsToFetch.isEmpty()) {
                    java.util.Map<Integer, String> idPhotos = walletModel
                            .findProfilePhotosByWalletIds(walletIdsToFetch);
                    walletOwnerNames = walletModel.findOwnerNamesByWalletIds(walletIdsToFetch);

                    for (java.util.Map.Entry<String, Integer> entry : nameToWalletIdMap.entrySet()) {
                        String url = idPhotos.get(entry.getValue());
                        if (url != null) {
                            profilePhotos.put(entry.getKey(), url); // Overwrite any name-based match
                        }
                    }

                    // Also map resolved owner names to their photos (for escrow name resolution)
                    for (java.util.Map.Entry<Integer, String> ownerEntry : walletOwnerNames.entrySet()) {
                        String photoUrl = idPhotos.get(ownerEntry.getKey());
                        if (photoUrl != null && ownerEntry.getValue() != null) {
                            profilePhotos.put(ownerEntry.getValue(), photoUrl);
                        }
                    }
                }

                // 4. Fetch Profile Photos by Email (For Saved Contacts - Better Accuracy)
                java.util.Set<String> emailsToFetch = new java.util.HashSet<>();
                for (tn.finhub.model.SavedContact contact : contacts) {
                    if (contact.getContactEmail() != null && !contact.getContactEmail().isEmpty()) {
                        emailsToFetch.add(contact.getContactEmail().trim());
                    }
                }
                if (!emailsToFetch.isEmpty()) {
                    profilePhotos.putAll(userModel.findProfilePhotosByEmails(emailsToFetch));
                }

                boolean isFrozen = wallet != null && "FROZEN".equals(wallet.getStatus());

                return new TransactionData(user.getId(), transactions, contacts, badTxId, isFrozen, profilePhotos,
                        walletOwnerNames);
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
                "-fx-background-color: rgba(16, 185, 129, 0.1); -fx-background-radius: 30; -fx-border-color: #10B981; -fx-border-width: 2; -fx-border-radius: 30; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.4), 8, 0, 0, 0);");
        iconBg.setPrefSize(48, 48);
        iconBg.setMaxSize(48, 48);

        Label initial = new Label(contact.getContactName().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 20px;");

        boolean photoLoaded = false;
        if (profilePhotos != null) {
            // Try lookup by Email first (Primary match)
            String lookupKey = contact.getContactEmail();
            if (lookupKey == null || lookupKey.isEmpty()) {
                lookupKey = contact.getContactName().trim(); // Fallback to name
            }

            if (profilePhotos.containsKey(lookupKey)) {
                String url = profilePhotos.get(lookupKey);
                if (url != null && !url.isEmpty()) {
                    try {
                        // Use centralized UI Helper
                        iconBg = tn.finhub.util.UIUtils.createCircularImage(url, 48);
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
        java.util.Map<Integer, String> walletOwnerNames;

        public TransactionData(int uid, List<WalletTransaction> txs, List<tn.finhub.model.SavedContact> contacts,
                int badTxId, boolean isFrozen, java.util.Map<String, String> profilePhotos,
                java.util.Map<Integer, String> walletOwnerNames) {
            this.userId = uid;
            this.transactions = txs;
            this.contacts = contacts;
            this.badTxId = badTxId;
            this.isFrozen = isFrozen;
            this.profilePhotos = profilePhotos;
            this.walletOwnerNames = walletOwnerNames;
        }
    }

    public static void setCachedData(TransactionData data) {
        cachedData = data;
    }

    // --- SHARED LOGIC FROM WalletController (Should be refactored eventually) ---
    private javafx.scene.Node createTransactionCard(WalletTransaction tx, int badTxId,
            java.util.Map<String, String> profilePhotos, java.util.Map<Integer, String> walletOwnerNames) {
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
        String type = tx.getType();
        String ref = tx.getReference();

        // Broader check for Escrow (Type OR Reference text)
        boolean isEscrow = (type != null && type.startsWith("ESCROW_")) ||
                (ref != null && ref.startsWith("Escrow")); // Removed colon to catch "Escrow Creation:"

        boolean isPositive = "CREDIT".equals(type) || "DEPOSIT".equals(type) || "RELEASE".equals(type)
                || "TRANSFER_RECEIVED".equals(type) || "GENESIS".equals(type);
        boolean isNegative = "DEBIT".equals(type) || "TRANSFER_SENT".equals(type);
        boolean isTransferSent = "TRANSFER_SENT".equals(type);

        // Overwrite for Escrow (Type Logic)
        if (isEscrow) {
            if ("ESCROW_RCVD".equals(type) || "ESCROW_REFUND".equals(type)) {
                // Incoming money (Release to winner or Refund)
                isPositive = true;
                isNegative = false;
            } else {
                // Outgoing money (Creation/Hold or Release from sender side)
                // HOLD, ESCROW_SENT, ESCROW_CREATE, or generic DEBIT with Escrow ref
                isPositive = false;
                isNegative = true;
            }
        }

        String iconClass = isPositive ? "transaction-icon-positive"
                : (isNegative ? "transaction-icon-negative" : "transaction-icon");

        // --- Icon ---
        javafx.scene.layout.StackPane iconBg = new javafx.scene.layout.StackPane();
        iconBg.getStyleClass().add("transaction-icon-bg");
        iconBg.setPrefSize(40, 40);
        iconBg.setMinSize(40, 40);
        iconBg.setMaxSize(40, 40);

        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        if (isEscrow) {
            // Shield/Lock Icon
            icon.setContent(
                    "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z");
            if (isPositive)
                icon.setStyle("-fx-fill: #10B981;");
            else
                icon.setStyle("-fx-fill: #F59E0B;");
        } else {
            icon.setContent(getIconPath(tx.getType()));
            icon.getStyleClass().addAll("transaction-icon", iconClass);
        }
        iconBg.getChildren().add(icon);

        // --- Left Details (Also needed for name lookup) ---
        // Clean text first so we can use it for lookup
        String displayRef = tx.getReference();
        if (displayRef != null) {
            displayRef = displayRef.replace("Transfer to ", "")
                    .replace("Transfer from ", "");
            displayRef = displayRef.replaceAll("(?i)\\s+(from|to)\\s+\\d+$", "");
            displayRef = displayRef.replaceAll("\\(Wallet\\s+\\d+\\)", "").trim();

            if (displayRef.startsWith("DEPOSIT via")) {
                displayRef = "Deposit";
            }
        } else {
            displayRef = "Unknown";
        }

        // Escrow Override
        // Escrow Override (Text & Display Logic)
        if (isEscrow) {
            if ("HOLD".equals(type) || "ESCROW_CREATE".equals(type)) {
                displayRef = "Escrow On Hold";
            } else if ("ESCROW_SENT".equals(type)) {
                // Check if we can extract name from ref "Released to ..."
                if (ref != null && ref.startsWith("Released to ")) {
                    String extractedName = ref.substring("Released to ".length()).trim();
                    if (!extractedName.isEmpty()) {
                        // Check if extractedName is "Wallet X" and if we have a resolved name
                        if (extractedName.startsWith("Wallet ") && walletOwnerNames != null) {
                            try {
                                int wId = Integer.parseInt(extractedName.substring("Wallet ".length()));
                                if (walletOwnerNames.containsKey(wId)) {
                                    extractedName = walletOwnerNames.get(wId);
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        displayRef = "Escrow Released to " + extractedName;
                    } else {
                        displayRef = "Escrow Released";
                    }
                } else {
                    displayRef = "Escrow Released";
                }
            } else if ("ESCROW_RCVD".equals(type)) {
                // Check if we can extract name from ref "Received from Escrow Wallet X"
                if (ref != null && ref.startsWith("Received from Escrow Wallet ")) {
                    String extractedName = ref.substring("Received from Escrow ".length()).trim(); // "Wallet X"
                    if (!extractedName.isEmpty()) {
                        // Resolve "Wallet X" to Name if possible
                        if (extractedName.startsWith("Wallet ") && walletOwnerNames != null) {
                            try {
                                int wId = Integer.parseInt(extractedName.substring("Wallet ".length()));
                                if (walletOwnerNames.containsKey(wId)) {
                                    extractedName = walletOwnerNames.get(wId);
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        displayRef = "Escrow Received from " + extractedName;
                    } else {
                        displayRef = "Escrow Payment Received";
                    }
                } else {
                    displayRef = "Escrow Payment Received";
                }
            } else if ("ESCROW_REFUND".equals(type)) {
                displayRef = "Escrow Refunded";
            } else {
                // Fallback for generic types (DEBIT/CREDIT) with "Escrow:" ref
                // "Escrow: condition" usually means Creation/Hold
                if (isNegative)
                    displayRef = "Escrow On Hold";
                else if (isPositive)
                    displayRef = "Escrow Refunded";
                else
                    displayRef = "Escrow Transaction";
            }
        }

        // Logic Mapping for Escrow Sub-cases (Refined)
        if (isEscrow && displayRef.startsWith("Escrow On Hold")) {
            // Force Blue
        } else if (isEscrow && displayRef.startsWith("Escrow Released")) {
            // Force Orange/Amber (Refined text)
        }

        // --- Profile Photo Override ---
        if (profilePhotos != null) {
            String lookupName = displayRef.trim();
            // If it's an "Escrow Released to ..." string, extract the name for lookup
            if (isEscrow && displayRef.startsWith("Escrow Released to ")) {
                lookupName = displayRef.substring("Escrow Released to ".length()).trim();
            } else if (isEscrow && displayRef.startsWith("Escrow Received from ")) {
                lookupName = displayRef.substring("Escrow Received from ".length()).trim();
            }
            if (!lookupName.isEmpty() && profilePhotos.containsKey(lookupName)) {
                String url = profilePhotos.get(lookupName);
                if (url != null && !url.isEmpty()) {
                    try {
                        // Centralized helper for circular image with border
                        javafx.scene.layout.StackPane customIcon = tn.finhub.util.UIUtils.createCircularImage(url, 40);
                        iconBg.getChildren().clear(); // Remove default icon
                        iconBg.getChildren().add(customIcon);
                        // Hide original background if needed, or overlay it.
                        // The customIcon has its own background/border.
                        iconBg.setStyle("-fx-background-color: transparent;"); // Clear parent bg
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

        if (isEscrow && displayRef.equals("Escrow On Hold")) {
            amountLabel.setStyle("-fx-text-fill: #3B82F6; -fx-font-weight: bold;"); // Blue
        } else if (isEscrow && displayRef.equals("Escrow Released")) {
            amountLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;"); // Orange
        } else {
            amountLabel.setStyle(isPositive ? "-fx-text-fill: #10B981; -fx-font-weight: bold;"
                    : (isNegative ? "-fx-text-fill: #F59E0B; -fx-font-weight: bold;"
                            : "-fx-text-fill: #F59E0B; -fx-font-weight: bold;"));
        }

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
            case "DEPOSIT" ->
                "M4 10v7h3v-7H4zm6 0v7h3v-7h-3zM2 22h19v-3H2v3zm14-12v7h3v-7h-3zm-4.5-9L2 6v2h19V6l-9.5-5z"; // Bank
            case "CREDIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS" -> "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"; // Plus
            case "DEBIT", "TRANSFER_SENT" -> "M19 13H5v-2h14v2z"; // Minus
            case "HOLD" ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.95V5h-2.93v2.63c-1.71.49-3.1 1.4-3.1 3.03 0 1.98 1.85 2.7 3.3 3.02 1.98.51 2.44 1.15 2.44 1.91 0 .88-.74 1.41-1.95 1.41-1.55 0-2.34-.66-2.47-1.8H7.13c.12 1.83 1.32 2.7 2.87 3.12V20h2.93v-2.54c1.81-.39 3.25-1.27 3.25-3.17 0-2.02-1.76-2.81-3.19-3.15z";
            default ->
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
        };
    }
}
