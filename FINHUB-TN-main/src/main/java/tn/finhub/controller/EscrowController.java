package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.control.ScrollPane;
import tn.finhub.model.Escrow;
import tn.finhub.model.EscrowManager;
import tn.finhub.model.WalletModel;
import tn.finhub.model.ContactModel;
import tn.finhub.model.User;
import tn.finhub.util.UserSession;
import tn.finhub.model.Wallet;

import java.util.List;

public class EscrowController {

    @FXML
    private VBox escrowCardContainer;
    @FXML
    private ScrollPane escrowScrollPane;
    @FXML
    private Label trustScoreLabel;
    @FXML
    private javafx.scene.control.Button createEscrowBtn;

    // Filters
    @FXML
    private javafx.scene.control.Button btnContacts;
    @FXML
    private javafx.scene.control.Button btnActive;
    @FXML
    private javafx.scene.control.Button btnHistory;
    @FXML
    private javafx.scene.control.Button btnDisputes;

    // Trust Score UI
    @FXML
    private javafx.scene.shape.Circle trustScoreCircle;
    @FXML
    private javafx.scene.shape.SVGPath trustScoreIcon;

    // Contact UI
    @FXML
    private ScrollPane contactsScrollPane;
    @FXML
    private FlowPane contactsContainer;

    // Main Views
    @FXML
    private VBox mainView;
    @FXML
    private VBox contactDetailsView;

    // Contact Details UI (inside contactDetailsView)
    @FXML
    private Label overlayInitials;
    @FXML
    private ImageView overlayImage;
    @FXML
    private Label overlayName;
    @FXML
    private Label overlayEmail;
    @FXML
    private Label overlayPhone;
    @FXML
    private Label overlayTrustScore;
    @FXML
    private Label overlayWalletStatus;

    @FXML
    private VBox contactActivityContainer;
    @FXML
    private Label noActivityLabel;

    @FXML
    private HBox aliasEditBox;
    @FXML
    private javafx.scene.control.TextField aliasField;

    @FXML
    private javafx.scene.control.Button addContactBtn;

    // State
    private User currentContactUser = null;

    // Models
    private final EscrowManager escrowManager = new EscrowManager();
    private final WalletModel walletModel = new WalletModel();
    private final tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
    private final ContactModel contactModel = new ContactModel();

    private enum FilterType {
        CONTACTS, ACTIVE, HISTORY, DISPUTES
    }

    private FilterType currentFilter = FilterType.CONTACTS;

    @FXML
    public void initialize() {
        contactModel.ensureAliasColumn();
        handleFilterContacts();
        showMainView();
    }

    private void loadEscrowData() {
        // Only load if not in Contact mode (optimization)
        if (currentFilter == FilterType.CONTACTS)
            return;

        User sessionUser = UserSession.getInstance().getUser();
        if (sessionUser == null)
            return;

        // Visual updates for Trust Score (Same as before)
        updateTrustScoreUI(sessionUser);

        Wallet wallet = walletModel.findByUserId(sessionUser.getId());
        if (wallet != null) {
            boolean isFrozen = "FROZEN".equals(wallet.getStatus());
            if (createEscrowBtn != null)
                createEscrowBtn.setDisable(isFrozen);

            List<Escrow> allEscrows = escrowManager.getEscrowsByWalletId(wallet.getId());
            escrowCardContainer.getChildren().clear();

            List<Escrow> filtered = allEscrows.stream().filter(e -> {
                switch (currentFilter) {
                    case ACTIVE:
                        return "LOCKED".equals(e.getStatus());
                    case HISTORY:
                        return "RELEASED".equals(e.getStatus()) || "REFUNDED".equals(e.getStatus());
                    case DISPUTES:
                        return "DISPUTED".equals(e.getStatus());
                    default:
                        return false;
                }
            }).toList();

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

    private void loadContacts() {
        User sessionUser = UserSession.getInstance().getUser();
        if (sessionUser == null)
            return;

        // Also update trust score UI here since it's shared
        updateTrustScoreUI(sessionUser);

        List<User> contacts = contactModel.getContacts(sessionUser.getId());
        contactsContainer.getChildren().clear();

        if (contacts.isEmpty()) {
            Label emptyLabel = new Label("No contacts found. Add trusted users to see them here.");
            emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
            contactsContainer.getChildren().add(emptyLabel);
        } else {
            for (User contact : contacts) {
                contactsContainer.getChildren().add(createContactCard(contact));
            }
        }
    }

    private void updateTrustScoreUI(User sessionUser) {
        User currentUser = userModel.findById(sessionUser.getId());
        if (currentUser == null)
            currentUser = sessionUser;

        int score = currentUser.getTrustScore();
        if (trustScoreLabel != null)
            trustScoreLabel.setText(String.valueOf(score));

        if (trustScoreCircle != null) {
            double maxScore = 100.0;
            double radius = 22.0;
            double circumference = 2 * Math.PI * radius;
            double progress = Math.min(Math.max(score, 0), maxScore) / maxScore * circumference;
            trustScoreCircle.getStrokeDashArray().setAll(progress, circumference);

            String color;
            if (score < 30)
                color = "#ef4444";
            else if (score < 70)
                color = "#f59e0b";
            else
                color = "#10b981";

            trustScoreCircle.setStyle("-fx-fill: transparent; -fx-stroke-width: 4; -fx-stroke: " + color + ";");
            if (trustScoreIcon != null)
                trustScoreIcon.setStyle("-fx-fill: " + color + ";");
        }
    }

    @FXML
    private void handleFilterContacts() {
        currentFilter = FilterType.CONTACTS;
        updateFilterUI();
        escrowScrollPane.setVisible(false);
        contactsScrollPane.setVisible(true);
        loadContacts();
    }

    @FXML
    private void handleFilterActive() {
        currentFilter = FilterType.ACTIVE;
        updateFilterUI();
        escrowScrollPane.setVisible(true);
        contactsScrollPane.setVisible(false);
        loadEscrowData();
    }

    @FXML
    private void handleFilterHistory() {
        currentFilter = FilterType.HISTORY;
        updateFilterUI();
        escrowScrollPane.setVisible(true);
        contactsScrollPane.setVisible(false);
        loadEscrowData();
    }

    @FXML
    private void handleFilterDisputes() {
        currentFilter = FilterType.DISPUTES;
        updateFilterUI();
        escrowScrollPane.setVisible(true);
        contactsScrollPane.setVisible(false);
        loadEscrowData();
    }

    private void updateFilterUI() {
        resetFilterStyles();

        if (addContactBtn != null) {
            boolean isContacts = currentFilter == FilterType.CONTACTS;
            addContactBtn.setVisible(isContacts);
            addContactBtn.setManaged(isContacts);
        }

        if (currentFilter == FilterType.CONTACTS) {
            addActiveClass(btnContacts);
        } else if (currentFilter == FilterType.ACTIVE) {
            addActiveClass(btnActive);
        } else if (currentFilter == FilterType.HISTORY) {
            addActiveClass(btnHistory);
        } else if (currentFilter == FilterType.DISPUTES) {
            addActiveClass(btnDisputes);
        }
    }

    private void resetFilterStyles() {
        removeActiveClass(btnContacts);
        removeActiveClass(btnActive);
        removeActiveClass(btnHistory);
        removeActiveClass(btnDisputes);
    }

    private void addActiveClass(javafx.scene.control.Button btn) {
        if (!btn.getStyleClass().contains("active")) {
            btn.getStyleClass().add("active");
        }
    }

    private void removeActiveClass(javafx.scene.control.Button btn) {
        btn.getStyleClass().remove("active");
    }

    private javafx.scene.Node createContactCard(User user) {
        // Main card — horizontal layout, FIXED width for uniformity
        HBox card = new HBox(0);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setPrefWidth(320);
        card.setMinWidth(300);
        card.setMaxWidth(340);
        card.setPadding(new javafx.geometry.Insets(16, 18, 16, 18));
        card.setStyle(
                "-fx-background-color: rgba(30, 27, 46, 0.75); -fx-background-radius: 14; "
                        + "-fx-border-color: rgba(139, 92, 246, 0.15); -fx-border-radius: 14; -fx-border-width: 1; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 3); -fx-cursor: hand;");

        // Hover effect
        card.setOnMouseEntered(ev -> card.setStyle(
                "-fx-background-color: rgba(30, 27, 46, 0.75); -fx-background-radius: 14; "
                        + "-fx-border-color: rgba(139, 92, 246, 0.45); -fx-border-radius: 14; -fx-border-width: 1; "
                        + "-fx-effect: dropshadow(gaussian, rgba(139, 92, 246, 0.3), 14, 0, 0, 3); -fx-cursor: hand;"));
        card.setOnMouseExited(ev -> card.setStyle(
                "-fx-background-color: rgba(30, 27, 46, 0.75); -fx-background-radius: 14; "
                        + "-fx-border-color: rgba(139, 92, 246, 0.15); -fx-border-radius: 14; -fx-border-width: 1; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 3); -fx-cursor: hand;"));

        // === LEFT SIDE: Image + Name (fixed width, centered) ===
        VBox leftSide = new VBox(6);
        leftSide.setAlignment(javafx.geometry.Pos.CENTER);
        leftSide.setPrefWidth(90);
        leftSide.setMinWidth(90);
        leftSide.setMaxWidth(90);

        StackPane imgContainer = new StackPane();
        imgContainer.setPrefSize(56, 56);
        imgContainer.setMinSize(56, 56);
        imgContainer.setMaxSize(56, 56);

        Circle bg = new Circle(28);
        bg.setStyle("-fx-fill: -color-bg-subtle; -fx-stroke: transparent;");

        Label initials = new Label(getInitials(user.getFullName()));
        initials.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-primary;");

        ImageView img = new ImageView();
        img.setFitWidth(56);
        img.setFitHeight(56);
        img.setPreserveRatio(true);
        Circle clip = new Circle(28, 28, 28);
        img.setClip(clip);

        if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
            try {
                img.setImage(new javafx.scene.image.Image(user.getProfilePhotoUrl()));
                initials.setVisible(false);
            } catch (Exception e) {
                img.setVisible(false);
            }
        } else {
            img.setVisible(false);
        }

        Circle border = new Circle(28);
        border.setStyle("-fx-fill: transparent; -fx-stroke: -color-primary; -fx-stroke-width: 2;");
        border.setMouseTransparent(true);

        imgContainer.getChildren().addAll(bg, initials, img, border);

        Label name = new Label(user.getFullName());
        name.setWrapText(true);
        name.setMaxWidth(88);
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        name.setAlignment(javafx.geometry.Pos.CENTER);
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-primary; -fx-font-size: 13px;");

        leftSide.getChildren().addAll(imgContainer, name);

        // === SEPARATOR ===
        javafx.scene.layout.Region separator = new javafx.scene.layout.Region();
        separator.setPrefWidth(1);
        separator.setMinWidth(1);
        separator.setMaxWidth(1);
        separator.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2);");
        VBox.setVgrow(separator, javafx.scene.layout.Priority.ALWAYS);
        HBox.setMargin(separator, new javafx.geometry.Insets(4, 14, 4, 14));

        // === RIGHT SIDE: Email, Phone, Score (vertically centered) ===
        VBox rightSide = new VBox(10);
        rightSide.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(rightSide, javafx.scene.layout.Priority.ALWAYS);

        // Email row
        HBox emailRow = createInfoRow(
                "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z",
                user.getEmail() != null ? user.getEmail() : "N/A",
                "#8b5cf6");

        // Phone row
        String phoneText = user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()
                ? user.getPhoneNumber()
                : "N/A";
        HBox phoneRow = createInfoRow(
                "M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z",
                phoneText,
                "#8b5cf6");

        // Score row
        String scoreColor = user.getTrustScore() >= 70 ? "#10b981"
                : (user.getTrustScore() >= 30 ? "#f59e0b" : "#ef4444");
        HBox scoreRow = createInfoRow(
                "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z",
                "Trust Score: " + user.getTrustScore(),
                scoreColor);
        // Override text color for score
        if (scoreRow.getChildren().size() > 1) {
            ((Label) scoreRow.getChildren().get(1)).setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");
        }

        rightSide.getChildren().addAll(emailRow, phoneRow, scoreRow);

        // === Assemble Card ===
        card.getChildren().addAll(leftSide, separator, rightSide);
        card.setOnMouseClicked(e -> showContactDetails(user));

        return card;
    }

    /** Helper: creates a compact info row with an SVG icon + label */
    private HBox createInfoRow(String svgPath, String text, String iconColor) {
        HBox row = new HBox(8);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        icon.setContent(svgPath);
        icon.setStyle("-fx-fill: " + iconColor + ";");
        icon.setScaleX(0.65);
        icon.setScaleY(0.65);

        // Wrap in fixed-size pane for alignment
        StackPane iconPane = new StackPane(icon);
        iconPane.setPrefSize(18, 18);
        iconPane.setMinSize(18, 18);
        iconPane.setMaxSize(18, 18);

        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-secondary;");
        label.setWrapText(true);

        row.getChildren().addAll(iconPane, label);
        return row;
    }

    private void showContactDetails(User user) {
        currentContactUser = user;

        // Use alias if available, otherwise full name
        String displayName = user.getContactAlias() != null && !user.getContactAlias().isEmpty()
                ? user.getContactAlias()
                : user.getFullName();

        overlayName.setText(displayName);
        overlayEmail.setText(user.getEmail());
        overlayPhone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
        overlayInitials.setText(getInitials(user.getFullName()));

        // Reset Alias Edit Mode
        if (aliasEditBox != null) {
            aliasEditBox.setVisible(false);
            aliasEditBox.setManaged(false);
        }

        // Image
        if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
            try {
                overlayImage.setImage(new javafx.scene.image.Image(user.getProfilePhotoUrl()));
                overlayImage.setVisible(true);
                overlayInitials.setVisible(false);
            } catch (Exception e) {
                overlayImage.setVisible(false);
                overlayInitials.setVisible(true);
            }
        } else {
            overlayImage.setVisible(false);
            overlayInitials.setVisible(true);
        }

        // Overlay Score and Status logic...
        overlayTrustScore.setText(String.valueOf(user.getTrustScore()));

        Wallet w = walletModel.findByUserId(user.getId());
        if (w != null) {
            overlayWalletStatus.setText(w.getStatus());
            if ("ACTIVE".equals(w.getStatus())) {
                overlayWalletStatus
                        .setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-success;");
            } else {
                overlayWalletStatus.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
            }
        } else {
            overlayWalletStatus.setText("UNKNOWN");
            overlayWalletStatus
                    .setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        }

        loadContactActivity(user);
        showContactDetailsView();
    }

    private void loadContactActivity(User contact) {
        User sessionUser = UserSession.getInstance().getUser();
        if (sessionUser == null || contact == null)
            return;

        Wallet myWallet = walletModel.findByUserId(sessionUser.getId());
        Wallet contactWallet = walletModel.findByUserId(contact.getId());

        if (myWallet == null || contactWallet == null) {
            contactActivityContainer.getChildren().clear();
            contactActivityContainer.getChildren().add(noActivityLabel);
            noActivityLabel.setVisible(true);
            return;
        }

        List<Escrow> allEscrows = escrowManager.getEscrowsByWalletId(myWallet.getId());
        List<Escrow> contactEscrows = allEscrows.stream()
                .filter(e -> e.getSenderWalletId() == contactWallet.getId()
                        || e.getReceiverWalletId() == contactWallet.getId())
                .limit(5) // Limit to 5 recent
                .toList();

        contactActivityContainer.getChildren().clear();

        if (contactEscrows.isEmpty()) {
            contactActivityContainer.getChildren().add(noActivityLabel);
            noActivityLabel.setVisible(true);
        } else {
            noActivityLabel.setVisible(false);
            for (Escrow e : contactEscrows) {
                // Use compact card for this view
                javafx.scene.Node card = createCompactEscrowCard(e);
                contactActivityContainer.getChildren().add(card);
            }
        }
    }

    // --- View Switching Logic ---

    private void showMainView() {
        mainView.setVisible(true);
        contactDetailsView.setVisible(false);
    }

    private void showContactDetailsView() {
        mainView.setVisible(false);
        contactDetailsView.setVisible(true);
    }

    @FXML
    private void handleShowAddContactView() {
        openAddContactDialog();
    }

    @FXML
    private void handleBackToMain() {
        showMainView();
        loadContacts(); // Refresh list in case of changes
    }

    @FXML
    private void closeProfileOverlay() {
        handleBackToMain();
    }

    @FXML
    private void handleAddContact() {
        openAddContactDialog();
    }

    private void openAddContactDialog() {
        User sessionUser = UserSession.getInstance().getUser();
        if (sessionUser == null)
            return;

        // Build dialog stage
        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (mainView.getScene() != null) {
            dialogStage.initOwner(mainView.getScene().getWindow());
        }

        // Root container
        VBox root = new VBox(20);
        root.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        root.setPadding(new javafx.geometry.Insets(30));
        root.setMaxWidth(420);
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: rgba(30, 20, 60, 0.95); -fx-background-radius: 16; "
                + "-fx-border-color: rgba(139, 92, 246, 0.3); -fx-border-radius: 16; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 5);");

        // Title
        Label title = new Label("Add New Contact");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Email field
        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 12px; -fx-font-weight: bold;");

        javafx.scene.control.TextField emailField = new javafx.scene.control.TextField();
        emailField.setPromptText("Enter user email...");
        emailField.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; "
                + "-fx-prompt-text-fill: rgba(255,255,255,0.3); -fx-background-radius: 8; -fx-padding: 12; "
                + "-fx-border-color: rgba(139, 92, 246, 0.2); -fx-border-radius: 8;");

        VBox fieldBox = new VBox(6, emailLabel, emailField);

        // Error label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);

        // Buttons
        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.7); "
                + "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 10 20;");
        cancelBtn.setOnAction(e -> dialogStage.close());

        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("Save Contact");
        saveBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 24;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox buttonRow = new HBox(10, spacer, cancelBtn, saveBtn);
        buttonRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // Save action
        saveBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            if (email.isEmpty()) {
                errorLabel.setText("Please enter an email address.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            if (email.equalsIgnoreCase(sessionUser.getEmail())) {
                errorLabel.setText("You cannot add yourself as a contact.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            // Find user by email
            User found = userModel.findByEmail(email);
            if (found == null) {
                try {
                    userModel.syncUsersFromServer();
                    found = userModel.findByEmail(email);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (found == null) {
                errorLabel.setText("No user found with this email.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            // Add as escrow contact
            boolean success = contactModel.addContact(sessionUser.getId(), found.getId());
            if (success) {
                dialogStage.close();
                loadContacts();
            } else {
                errorLabel.setText("Contact already exists or could not be added.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        root.getChildren().addAll(title, fieldBox, errorLabel, buttonRow);

        // Make draggable
        final double[] dragDelta = new double[2];
        root.setOnMousePressed(e -> {
            dragDelta[0] = dialogStage.getX() - e.getScreenX();
            dragDelta[1] = dialogStage.getY() - e.getScreenY();
        });
        root.setOnMouseDragged(e -> {
            dialogStage.setX(e.getScreenX() + dragDelta[0]);
            dialogStage.setY(e.getScreenY() + dragDelta[1]);
        });

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/style/theme.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    // --- Alias Editing ---

    @FXML
    private void handleEditAlias() {
        if (aliasEditBox != null) {
            aliasEditBox.setVisible(true);
            aliasEditBox.setManaged(true);
            aliasField.setText(currentContactUser.getContactAlias() != null ? currentContactUser.getContactAlias()
                    : currentContactUser.getFullName());
            aliasField.requestFocus();
        }
    }

    @FXML
    private void handleCancelEditAlias() {
        if (aliasEditBox != null) {
            aliasEditBox.setVisible(false);
            aliasEditBox.setManaged(false);
        }
    }

    @FXML
    private void handleSaveAlias() {
        String newAlias = aliasField.getText().trim();
        if (newAlias.isEmpty())
            return;

        int currentUserId = UserSession.getInstance().getUser().getId();
        boolean success = contactModel.updateAlias(currentUserId, currentContactUser.getId(), newAlias);

        if (success) {
            currentContactUser.setContactAlias(newAlias);
            overlayName.setText(newAlias);
            handleCancelEditAlias();
        }
    }

    // --- Contact Actions ---

    @FXML
    private void handleDeleteContact() {
        if (currentContactUser != null) {
            // In a real app, show confirmation dialog here
            int currentUserId = UserSession.getInstance().getUser().getId();
            boolean success = contactModel.removeContact(currentUserId, currentContactUser.getId());
            if (success) {
                handleBackToMain();
            }
        }
    }

    @FXML
    private void handleCreateEscrowFromContact() {
        // Open create escrow dialog
        // Ideally pass the contact to pre-fill, but for now just open dialog
        handleCreateEscrow();
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty())
            return "?";
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    // ... Existing display logic for Escrow Cards ...
    private javafx.scene.Node createEscrowCard(Escrow e) {
        HBox card = new HBox();
        card.setSpacing(20);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(30, 27, 46, 0.75); -fx-background-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");

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

        String conditionText = e.getConditionText();
        if (conditionText != null && conditionText.length() > 60) {
            conditionText = conditionText.substring(0, 60) + "...";
        }
        Label condition = new Label(conditionText);
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

    private javafx.scene.Node createCompactEscrowCard(Escrow e) {
        HBox card = new HBox();
        card.setSpacing(10);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(30, 27, 46, 0.75); -fx-background-radius: 8; -fx-padding: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 3, 0, 0, 0); -fx-cursor: hand;");

        // Smaller Icon
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane();
        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle(18); // Smaller
        bg.setFill(javafx.scene.paint.Color.web("#eef2ff"));

        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        if ("QR_CODE".equals(e.getEscrowType())) {
            icon.setContent(
                    "M3 5v4h2V5h4V3H5c-1.1 0-2 .9-2 2zm2 10H3v4c0 1.1.9 2 2 2h4v-2H5v-4zm14 4h-4v2h4c1.1 0 2-.9 2-2v-4h-2v4zm0-16h-4v2h4v4h2V5c0-1.1-.9-2-2-2z");
        } else {
            icon.setContent("M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z");
        }
        icon.setScaleX(0.7); // Scale down icon content
        icon.setScaleY(0.7);
        icon.setStyle("-fx-fill: -color-primary;");
        iconPane.getChildren().addAll(bg, icon);

        // Details (Compact)
        VBox details = new VBox(2);
        Label title = new Label("Escrow #" + e.getId());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-primary; -fx-font-size: 13px;");

        String conditionText = e.getConditionText();
        if (conditionText != null && conditionText.length() > 25) { // Shorter truncation
            conditionText = conditionText.substring(0, 25) + "...";
        }
        Label condition = new Label(conditionText);
        condition.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 11px;");
        details.getChildren().addAll(title, condition);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Status Only (No Amount to save space, or Amount + Small Status Dot)
        VBox rightSide = new VBox(2);
        rightSide.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label amountLabel = new Label(e.getAmount() + " TND");
        amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-primary; -fx-font-size: 13px;");

        Label statusLabel = new Label(e.getStatus());
        String statusColor = switch (e.getStatus()) {
            case "LOCKED" -> "-color-warning";
            case "RELEASED" -> "-color-success";
            case "DISPUTED" -> "-color-danger";
            default -> "-color-text-muted";
        };
        statusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");

        rightSide.getChildren().addAll(amountLabel, statusLabel);

        card.getChildren().addAll(iconPane, details, spacer, rightSide);

        // Whole card action
        card.setOnMouseClicked(event -> handleViewDetails(e));

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
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            if (escrowCardContainer.getScene() != null) {
                stage.initOwner(escrowCardContainer.getScene().getWindow());
            }

            stage.showAndWait();
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
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            if (escrowCardContainer != null && escrowCardContainer.getScene() != null) {
                stage.initOwner(escrowCardContainer.getScene().getWindow());
            }

            stage.showAndWait();
            loadEscrowData();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
