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
        tn.finhub.model.User currentUser = tn.finhub.util.UserSession.getInstance().getUser();
        if (currentUser != null) {
            String displayName = currentUser.getFullName();
            userNameLabel.setText(displayName);
            userRoleLabel.setText(currentUser.getRole());
        }

        // Load Default View
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/wallet_dashboard.fxml");
    }

    @FXML
    private void handleDashboard() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/wallet_dashboard.fxml");
    }

    @FXML
    private void handleSettings() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/profile.fxml");
    }

    @FXML
    private void toggleSidebar() {
        isSidebarExpanded = !isSidebarExpanded;

        // Animate Width (Min, Max, and Pref together to avoid constraints)
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();

        double targetWidth = isSidebarExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;

        javafx.animation.KeyValue minWidthValue = new javafx.animation.KeyValue(sidebar.minWidthProperty(), targetWidth,
                javafx.animation.Interpolator.EASE_BOTH);
        javafx.animation.KeyValue maxWidthValue = new javafx.animation.KeyValue(sidebar.maxWidthProperty(), targetWidth,
                javafx.animation.Interpolator.EASE_BOTH);
        javafx.animation.KeyValue prefWidthValue = new javafx.animation.KeyValue(sidebar.prefWidthProperty(),
                targetWidth, javafx.animation.Interpolator.EASE_BOTH);

        javafx.animation.KeyFrame frame = new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), minWidthValue,
                maxWidthValue, prefWidthValue);
        timeline.getKeyFrames().add(frame);
        timeline.play();

        updateSidebarState();
    }

    private void updateSidebarState() {
        // Dynamic Style Class Management
        if (isSidebarExpanded) {
            sidebar.getStyleClass().remove("collapsed");
            sidebarHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        } else {
            if (!sidebar.getStyleClass().contains("collapsed")) {
                sidebar.getStyleClass().add("collapsed");
            }
            sidebarHeader.setAlignment(javafx.geometry.Pos.CENTER);
        }

        // Toggle Visibility & Management of Header Elements
        userInfoBox.setVisible(isSidebarExpanded);
        userInfoBox.setManaged(isSidebarExpanded);
        headerSpacer.setVisible(isSidebarExpanded);
        headerSpacer.setManaged(isSidebarExpanded);
        menuLabel.setVisible(isSidebarExpanded);
        menuLabel.setManaged(isSidebarExpanded);

        // Button Layout managed by CSS mostly, but alignment needs helper
        javafx.scene.control.ContentDisplay display = isSidebarExpanded ? javafx.scene.control.ContentDisplay.LEFT
                : javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;

        // When collapsed, we want CENTER alignment. When expanded, CENTER_LEFT.
        javafx.geometry.Pos alignment = isSidebarExpanded ? javafx.geometry.Pos.CENTER_LEFT
                : javafx.geometry.Pos.CENTER;

        setButtonStyle(btnDashboard, display, alignment);
        setButtonStyle(btnTransactions, display, alignment);
        setButtonStyle(btnEscrow, display, alignment);
        setButtonStyle(btnSimulation, display, alignment);
        setButtonStyle(btnSupport, display, alignment);
        setButtonStyle(btnSettings, display, alignment);
        setButtonStyle(btnLogout, display, alignment);
    }

    @FXML
    private void handleTransactions() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/transactions.fxml");
    }

    @FXML
    private void handleContacts() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/contacts.fxml");
    }

    @FXML
    private void handleSupport() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/support_dashboard.fxml");
    }

    @FXML
    private void handleSimulation() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/financial_twin.fxml");
    }

    @FXML
    private void handleEscrow() {
        tn.finhub.util.ViewUtils.loadContent(dashboardContent, "/view/escrow_dashboard.fxml");
    }

    private void setButtonStyle(javafx.scene.control.Button btn, javafx.scene.control.ContentDisplay display,
            javafx.geometry.Pos alignment) {
        btn.setContentDisplay(display);
        btn.setAlignment(alignment);
    }

    @FXML
    private void handleLogout() {
        tn.finhub.util.UserSession.getInstance().cleanUserSession();
        tn.finhub.util.ViewUtils.setView(sidebar, "/view/login.fxml");
    }
}
