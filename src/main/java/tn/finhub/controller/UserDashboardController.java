package tn.finhub.controller;

import javafx.fxml.FXML;

public class UserDashboardController {

    @FXML
    private javafx.scene.layout.VBox sidebar;
    @FXML
    private javafx.scene.control.Label menuLabel;
    @FXML
    private javafx.scene.control.Label userNameLabel;
    @FXML
    private javafx.scene.control.Label userRoleLabel;

    // New layout fields
    @FXML
    private javafx.scene.layout.HBox sidebarHeader;
    @FXML
    private javafx.scene.layout.VBox userInfoBox;
    @FXML
    private javafx.scene.layout.Region headerSpacer;
    @FXML
    private javafx.scene.layout.StackPane profileImageContainer;
    @FXML
    private javafx.scene.image.ImageView sidebarProfileImage;

    // Menu Buttons
    @FXML
    private javafx.scene.control.Button btnDashboard;
    @FXML
    private javafx.scene.control.Button btnTransactions;
    @FXML
    private javafx.scene.control.Button btnEscrow;
    @FXML
    private javafx.scene.control.Button btnSimulation;
    @FXML
    private javafx.scene.control.Button btnSupport;
    @FXML
    private javafx.scene.control.Button btnSettings;
    @FXML
    private javafx.scene.control.Button btnLogout;

    private boolean isSidebarExpanded = false;
    private final double EXPANDED_WIDTH = 200; // Tighter width to fit content
    private final double COLLAPSED_WIDTH = 60; // 60px minimum for icons

    @FXML
    private javafx.scene.layout.StackPane dashboardContent;

    private static UserDashboardController instance;

    public static void refreshProfile() {
        if (instance != null) {
            instance.loadProfileImage();
        }
    }

    @FXML
    public void initialize() {
        instance = this; // Capture instance for static access

        sidebar.setPrefWidth(COLLAPSED_WIDTH); // Start Expanded
        sidebar.setMinWidth(COLLAPSED_WIDTH);
        sidebar.setMaxWidth(EXPANDED_WIDTH);

        isSidebarExpanded = false;
        // Initial state logic ensures layout is correct
        updateSidebarState();

        // Load User Data from Session (Database Local)
        tn.finhub.model.User sessionUser = tn.finhub.util.UserSession.getInstance().getUser();
        if (sessionUser != null) {
            // Fetch fresh from DB to get latest
            tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
            tn.finhub.model.User currentUser = userModel.findById(sessionUser.getId());

            if (currentUser == null)
                currentUser = sessionUser;

            String displayName = currentUser.getFullName();
            userNameLabel.setText(displayName);
            userRoleLabel.setText(currentUser.getRole());

            loadProfileImage();
        }

        // Load Default View
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/wallet_dashboard.fxml");
        setActiveButton(btnDashboard);
    }

    private void loadProfileImage() {
        tn.finhub.model.User sessionUser = tn.finhub.util.UserSession.getInstance().getUser();
        if (sessionUser == null)
            return;

        tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
        tn.finhub.model.User currentUser = userModel.findById(sessionUser.getId());
        if (currentUser == null)
            currentUser = sessionUser;

        // Load Profile Image for Sidebar
        if (currentUser.getProfilePhotoUrl() != null && !currentUser.getProfilePhotoUrl().isEmpty()) {
            try {
                // Use centralized UI Helper for Sidebar Image (36px)
                javafx.scene.layout.StackPane customIcon = tn.finhub.util.UIUtils
                        .createCircularImage(currentUser.getProfilePhotoUrl(), 36);

                profileImageContainer.getChildren().clear(); // Remove default placeholders
                profileImageContainer.getChildren().add(customIcon);

            } catch (Exception e) {
                System.err.println("Failed to load sidebar profile image: " + e.getMessage());
                showDefaultProfileIcon();
            }
        } else {
            // Clear any custom image
            profileImageContainer.getChildren().clear();
            // We need to restore the default placeholders if we cleared them
            // Since we cleared them, we should probably just re-create or show default
            showDefaultProfileIcon();
        }
    }

    @FXML
    private void handleDashboard() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/wallet_dashboard.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    public void handleSettings() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/profile.fxml");
        setActiveButton(btnSettings);
    }

    @FXML
    private void toggleSidebar() {
        isSidebarExpanded = !isSidebarExpanded;

        // Animate Width (Min, Max, and Pref together to avoid constraints)
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();

        double targetWidth = isSidebarExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;

        // Use EASE_OUT for a more natural, "momentum-based" feel
        javafx.animation.KeyValue minWidthValue = new javafx.animation.KeyValue(sidebar.minWidthProperty(), targetWidth,
                javafx.animation.Interpolator.EASE_OUT);
        javafx.animation.KeyValue maxWidthValue = new javafx.animation.KeyValue(sidebar.maxWidthProperty(), targetWidth,
                javafx.animation.Interpolator.EASE_OUT);
        javafx.animation.KeyValue prefWidthValue = new javafx.animation.KeyValue(sidebar.prefWidthProperty(),
                targetWidth, javafx.animation.Interpolator.EASE_OUT);

        // 250ms duration allows for "more frames" to be perceived compared to a
        // too-fast snap
        javafx.animation.KeyFrame frame = new javafx.animation.KeyFrame(javafx.util.Duration.millis(250), minWidthValue,
                maxWidthValue, prefWidthValue);
        timeline.getKeyFrames().add(frame);

        if (isSidebarExpanded) {
            updateSidebarState(); // Layout changes immediately
            setSidebarTextOpacity(0.0); // But set hidden
            timeline.play(); // Expand width
            animateTextTransparency(1.0); // and fade text in
        } else {
            animateTextTransparency(0.0); // Fade out text
            timeline.setOnFinished(e -> updateSidebarState()); // Then Collapse layout
            timeline.play();
        }
    }

    private void setSidebarTextOpacity(double opacity) {
        javafx.scene.paint.Color color = javafx.scene.paint.Color.rgb(255, 255, 255, opacity);
        javafx.scene.paint.Color mutedColor = javafx.scene.paint.Color.rgb(156, 163, 175, opacity);

        setTextFill(btnDashboard, opacity);
        setTextFill(btnTransactions, opacity);
        setTextFill(btnEscrow, opacity);
        setTextFill(btnSimulation, opacity);
        setTextFill(btnSupport, opacity);
        setTextFill(btnSettings, opacity);
        setTextFill(btnLogout, opacity);
        menuLabel.setTextFill(mutedColor);
    }

    private void setTextFill(javafx.scene.control.Button btn, double opacity) {
        btn.setTextFill(javafx.scene.paint.Color.rgb(243, 244, 246, opacity));
    }

    private void animateTextTransparency(double targetOpacity) {
        javafx.animation.Timeline fadeTimeline = new javafx.animation.Timeline();
        javafx.util.Duration duration = javafx.util.Duration.millis(200);
        javafx.scene.paint.Color targetColorPrim = javafx.scene.paint.Color.rgb(243, 244, 246, targetOpacity);
        javafx.scene.paint.Color targetColorMuted = javafx.scene.paint.Color.rgb(156, 163, 175, targetOpacity);

        fadeTimeline.getKeyFrames().addAll(
                new javafx.animation.KeyFrame(duration,
                        new javafx.animation.KeyValue(btnDashboard.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new javafx.animation.KeyFrame(duration,
                        new javafx.animation.KeyValue(btnTransactions.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new javafx.animation.KeyFrame(duration,
                        new javafx.animation.KeyValue(btnEscrow.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new javafx.animation.KeyFrame(duration,
                        new javafx.animation.KeyValue(btnSimulation.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new javafx.animation.KeyFrame(duration,
                        new javafx.animation.KeyValue(btnSupport.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new javafx.animation.KeyFrame(duration,
                        new javafx.animation.KeyValue(btnSettings.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new javafx.animation.KeyFrame(duration,
                        new javafx.animation.KeyValue(btnLogout.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new javafx.animation.KeyFrame(duration, new javafx.animation.KeyValue(menuLabel.textFillProperty(),
                        targetColorMuted, javafx.animation.Interpolator.EASE_BOTH)));
        fadeTimeline.play();
    }

    @FXML
    private javafx.scene.layout.Region menuSpacer;

    // ... (existing fields)

    private void updateSidebarState() {
        // Dynamic Style Class Management
        if (isSidebarExpanded) {
            sidebar.getStyleClass().remove("collapsed");
            sidebarHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            menuSpacer.setPrefHeight(20);
        } else {
            if (!sidebar.getStyleClass().contains("collapsed")) {
                sidebar.getStyleClass().add("collapsed");
            }
            sidebarHeader.setAlignment(javafx.geometry.Pos.CENTER);
            menuSpacer.setPrefHeight(45); // Compensate for hidden menuLabel
        }

        // Toggle Visibility & Management of Header Elements
        userInfoBox.setVisible(isSidebarExpanded);
        userInfoBox.setManaged(isSidebarExpanded);
        headerSpacer.setVisible(isSidebarExpanded);
        headerSpacer.setManaged(isSidebarExpanded);
        menuLabel.setVisible(isSidebarExpanded);
        menuLabel.setManaged(isSidebarExpanded);

        // Hide profile image in collapsed mode to save space (or keep it if it fits?)
        // User said "dont move any thing". Keeping it aligned with text visibility for
        // now.
        if (profileImageContainer != null) {
            profileImageContainer.setVisible(isSidebarExpanded);
            profileImageContainer.setManaged(isSidebarExpanded);
        }

        // Button Layout managed by CSS entirely now for smoothness
        javafx.scene.control.ContentDisplay display = isSidebarExpanded ? javafx.scene.control.ContentDisplay.LEFT
                : javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;

        setButtonStyle(btnDashboard, display);
        setButtonStyle(btnTransactions, display);
        setButtonStyle(btnEscrow, display);
        setButtonStyle(btnSimulation, display);
        setButtonStyle(btnSupport, display);
        setButtonStyle(btnSettings, display);
        setButtonStyle(btnLogout, display);
    }

    @FXML
    private void handleTransactions() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/transactions.fxml");
        setActiveButton(btnTransactions);
    }

    @FXML
    private void handleContacts() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/contacts.fxml");
        // No button for contacts in sidebar, so we might want to keep the current
        // active button or clear it?
        // Assuming Contacts is accessed via Transactions or isn't a primary sidebar
        // link handled here.
        // If it IS a sidebar link, we need a button for it.
        // Based on fields, there is no btnContacts. So we just leave it or set active
        // button to nothing?
        // Let's leave it for now or maybe it is part of transactions workflow.
    }

    @FXML
    private void handleSupport() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/support_dashboard.fxml");
        setActiveButton(btnSupport);
    }

    @FXML
    private void handleSimulation() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/financial_twin.fxml");
        setActiveButton(btnSimulation);
    }

    @FXML
    private void handleEscrow() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/escrow_dashboard.fxml");
        setActiveButton(btnEscrow);
    }

    private void setButtonStyle(javafx.scene.control.Button btn, javafx.scene.control.ContentDisplay display) {
        btn.setContentDisplay(display);
        // btn.setAlignment(alignment); // Handled by CSS for smooth transition
    }

    private void setActiveButton(javafx.scene.control.Button activeButton) {
        // Reset all buttons to default style
        resetButtonStyle(btnDashboard);
        resetButtonStyle(btnTransactions);
        resetButtonStyle(btnEscrow);
        resetButtonStyle(btnSimulation);
        resetButtonStyle(btnSupport);
        resetButtonStyle(btnSettings);

        // Apply active style
        // We use inline styles here to ensure override, matching Admin controller logic
        activeButton.getStyleClass().add("active");
        activeButton.setStyle("-fx-background-color: -color-primary; -fx-text-fill: white;");
    }

    private void resetButtonStyle(javafx.scene.control.Button btn) {
        btn.getStyleClass().remove("active");
        btn.setStyle(""); // Clear inline styles
    }

    private void showDefaultProfileIcon() {
        if (profileImageContainer.getChildren().size() > 1) {
            profileImageContainer.getChildren().get(0).setVisible(true); // Circle
            profileImageContainer.getChildren().get(1).setVisible(true); // Icon
        }
    }

    @FXML
    private void handleLogout() {
        // Build confirmation dialog
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (sidebar.getScene() != null)
            dialog.initOwner(sidebar.getScene().getWindow());

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(20);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new javafx.geometry.Insets(30));
        root.setPrefWidth(340);
        root.setStyle("-fx-background-color: rgba(30, 20, 60, 0.95); -fx-background-radius: 16; "
                + "-fx-border-color: rgba(139, 92, 246, 0.3); -fx-border-radius: 16; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 5);");

        javafx.scene.control.Label icon = new javafx.scene.control.Label("🚪");
        icon.setStyle("-fx-font-size: 36px;");

        javafx.scene.control.Label title = new javafx.scene.control.Label("Log Out");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        javafx.scene.control.Label msg = new javafx.scene.control.Label("Are you sure you want to log out?");
        msg.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 14px;");
        msg.setWrapText(true);
        msg.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: rgba(255,255,255,0.8); "
                + "-fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 28;");
        cancelBtn.setOnAction(e -> dialog.close());

        javafx.scene.control.Button logoutBtn = new javafx.scene.control.Button("Log Out");
        logoutBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 28;");
        logoutBtn.setOnAction(e -> {
            dialog.close();
            tn.finhub.util.UserSession.getInstance().cleanUserSession();
            tn.finhub.util.ViewUtils.setView(sidebar, "/view/login.fxml");
        });

        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(12, cancelBtn, logoutBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);

        root.getChildren().addAll(icon, title, msg, buttons);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
