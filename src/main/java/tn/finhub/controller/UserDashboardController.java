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

    @FXML
    public void initialize() {
        sidebar.setPrefWidth(COLLAPSED_WIDTH); // Start Expanded
        sidebar.setMinWidth(COLLAPSED_WIDTH);
        sidebar.setMaxWidth(EXPANDED_WIDTH);

        isSidebarExpanded = false;
        // Initial state logic ensures layout is correct
        updateSidebarState();

        // Load User Data from Session (Database Local)
        // Load User Data from Session (Database Local)
        tn.finhub.model.User sessionUser = tn.finhub.util.UserSession.getInstance().getUser();
        if (sessionUser != null) {
            // Fetch fresh from DB to get latest profile photo
            tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
            tn.finhub.model.User currentUser = userModel.findById(sessionUser.getId());

            // Fallback to session if DB fails (shouldn't happen)
            if (currentUser == null)
                currentUser = sessionUser;

            String displayName = currentUser.getFullName();
            userNameLabel.setText(displayName);
            userRoleLabel.setText(currentUser.getRole());

            // Load Profile Image for Sidebar
            if (currentUser.getProfilePhotoUrl() != null && !currentUser.getProfilePhotoUrl().isEmpty()) {
                try {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(currentUser.getProfilePhotoUrl(),
                            true);
                    sidebarProfileImage.setImage(image);

                    // Update size to be larger for clarity (36px)
                    sidebarProfileImage.setFitWidth(36);
                    sidebarProfileImage.setFitHeight(36);

                    // Circular Clip for new size
                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                    sidebarProfileImage.setClip(clip);

                    sidebarProfileImage.setVisible(true);

                    // Hide default placeholder
                    if (profileImageContainer.getChildren().size() > 0) {
                        profileImageContainer.getChildren().get(0).setVisible(false); // Circle
                        profileImageContainer.getChildren().get(1).setVisible(false); // Icon
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load sidebar profile image: " + e.getMessage());
                    sidebarProfileImage.setVisible(false);
                    showDefaultProfileIcon();
                }
            } else {
                sidebarProfileImage.setVisible(false);
                showDefaultProfileIcon();
            }
        }

        // Load Default View
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/wallet_dashboard.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void handleDashboard() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/wallet_dashboard.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void handleSettings() {
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
        tn.finhub.util.UserSession.getInstance().cleanUserSession();
        tn.finhub.util.ViewUtils.setView(sidebar, "/view/login.fxml");
    }
}
