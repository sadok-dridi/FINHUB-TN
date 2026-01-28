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

    private boolean isSidebarExpanded = true;
    private final double EXPANDED_WIDTH = 220; // set to 220 as requested
    private final double COLLAPSED_WIDTH = 60; // 60px minimum for icons

    @FXML
    private javafx.scene.layout.StackPane dashboardContent;

    @FXML
    public void initialize() {
        sidebar.setPrefWidth(EXPANDED_WIDTH);
        sidebar.setMinWidth(EXPANDED_WIDTH);
        sidebar.setMaxWidth(EXPANDED_WIDTH);

        isSidebarExpanded = true;
        // Load User Data from Session (Database Local)
        tn.finhub.model.User currentUser = tn.finhub.util.UserSession.getInstance().getUser();
        if (currentUser != null) {
            String displayName = currentUser.getEmail().split("@")[0];
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

        // Animate Width
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();

        javafx.animation.KeyValue widthValue = new javafx.animation.KeyValue(sidebar.prefWidthProperty(),
                isSidebarExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH,
                javafx.animation.Interpolator.EASE_BOTH);

        javafx.animation.KeyFrame frame = new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), widthValue);
        timeline.getKeyFrames().add(frame);
        timeline.play();

        // Toggle Labels Visibility
        menuLabel.setVisible(isSidebarExpanded);
        userNameLabel.setVisible(isSidebarExpanded);
        userRoleLabel.setVisible(isSidebarExpanded);

        // Handle Labels taking up space when invisible (managed property)
        menuLabel.setManaged(isSidebarExpanded);
        userNameLabel.setManaged(isSidebarExpanded);
        userRoleLabel.setManaged(isSidebarExpanded);

        // Toggle Button Text (Content Display)
        javafx.scene.control.ContentDisplay display = isSidebarExpanded ? javafx.scene.control.ContentDisplay.LEFT
                : javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;

        btnDashboard.setContentDisplay(display);
        btnTransactions.setContentDisplay(display);
        btnEscrow.setContentDisplay(display);
        btnSimulation.setContentDisplay(display);
        btnSupport.setContentDisplay(display);
        btnSettings.setContentDisplay(display);
        btnLogout.setContentDisplay(display);
    }

    @FXML
    private void handleLogout() {
        tn.finhub.util.UserSession.getInstance().cleanUserSession();
        tn.finhub.util.ViewUtils.setView(sidebar, "/view/login.fxml");
    }
}
