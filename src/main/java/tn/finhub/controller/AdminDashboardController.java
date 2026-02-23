package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.util.Duration;
import tn.finhub.util.SessionManager;
import tn.finhub.util.ViewUtils;

public class AdminDashboardController {

    @FXML
    private StackPane adminContentArea;

    @FXML
    private VBox sidebar;
    @FXML
    private HBox sidebarHeader;
    @FXML
    private VBox userInfoBox;
    @FXML
    private Region headerSpacer;
    @FXML
    private Label menuLabel;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnUsers;
    @FXML
    private Button btnTransactions;
    @FXML
    private Button btnEscrows;
    @FXML
    private Button btnSupportHub;
    @FXML
    private Button btnLogout;

    private boolean isSidebarExpanded = false;
    private final double EXPANDED_WIDTH = 200;
    private final double COLLAPSED_WIDTH = 60;

    @FXML
    public void initialize() {
        // Start Collapsed by default for Admin
        sidebar.setPrefWidth(COLLAPSED_WIDTH);
        sidebar.setMinWidth(COLLAPSED_WIDTH);
        sidebar.setMaxWidth(COLLAPSED_WIDTH);
        isSidebarExpanded = false;

        // Ensure text is transparent initially so fade-in works correctly on first
        // expand
        setSidebarTextOpacity(0.0);

        updateSidebarState();

        // Load User Data from Session (Database Local)
        tn.finhub.model.User currentUser = tn.finhub.util.UserSession.getInstance().getUser();
        if (currentUser != null) {
            String displayName = currentUser.getFullName();
            userNameLabel.setText(displayName);
            userRoleLabel.setText(currentUser.getRole());
        }

        // Load Default View (Admin Home Stats)
        ViewUtils.loadContent(adminContentArea, "/view/admin_home.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML

    private void toggleSidebar() {
        isSidebarExpanded = !isSidebarExpanded;

        Timeline timeline = new Timeline();
        double targetWidth = isSidebarExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;

        // Use EASE_OUT for a more natural, "momentum-based" feel
        KeyValue minWidthValue = new KeyValue(sidebar.minWidthProperty(), targetWidth,
                javafx.animation.Interpolator.EASE_OUT);
        KeyValue maxWidthValue = new KeyValue(sidebar.maxWidthProperty(), targetWidth,
                javafx.animation.Interpolator.EASE_OUT);
        KeyValue prefWidthValue = new KeyValue(sidebar.prefWidthProperty(), targetWidth,
                javafx.animation.Interpolator.EASE_OUT);

        // 250ms duration allows for "more frames" to be perceived compared to a
        // too-fast snap
        KeyFrame frame = new KeyFrame(Duration.millis(250), minWidthValue, maxWidthValue, prefWidthValue);
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
        javafx.scene.paint.Color mutedColor = javafx.scene.paint.Color.rgb(156, 163, 175, opacity);

        setTextFill(btnDashboard, opacity);
        setTextFill(btnUsers, opacity);
        setTextFill(btnTransactions, opacity);
        setTextFill(btnEscrows, opacity);
        setTextFill(btnSupportHub, opacity);
        setTextFill(btnLogout, opacity);
        menuLabel.setTextFill(mutedColor);
    }

    private void setTextFill(Button btn, double opacity) {
        btn.setTextFill(javafx.scene.paint.Color.rgb(243, 244, 246, opacity));
    }

    private void animateTextTransparency(double targetOpacity) {
        Timeline fadeTimeline = new Timeline();
        javafx.util.Duration duration = javafx.util.Duration.millis(200);
        javafx.scene.paint.Color targetColorPrim = javafx.scene.paint.Color.rgb(243, 244, 246, targetOpacity);
        javafx.scene.paint.Color targetColorMuted = javafx.scene.paint.Color.rgb(156, 163, 175, targetOpacity);

        fadeTimeline.getKeyFrames().addAll(
                new KeyFrame(duration,
                        new KeyValue(btnDashboard.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new KeyFrame(duration,
                        new KeyValue(btnUsers.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new KeyFrame(duration,
                        new KeyValue(btnTransactions.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new KeyFrame(duration,
                        new KeyValue(btnEscrows.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new KeyFrame(duration,
                        new KeyValue(btnSupportHub.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new KeyFrame(duration,
                        new KeyValue(btnLogout.textFillProperty(), targetColorPrim,
                                javafx.animation.Interpolator.EASE_BOTH)),
                new KeyFrame(duration, new KeyValue(menuLabel.textFillProperty(), targetColorMuted,
                        javafx.animation.Interpolator.EASE_BOTH)));
        fadeTimeline.play();
    }

    @FXML
    private Region menuSpacer;

    private void updateSidebarState() {
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

        userInfoBox.setVisible(isSidebarExpanded);
        userInfoBox.setManaged(isSidebarExpanded);
        headerSpacer.setVisible(isSidebarExpanded);
        headerSpacer.setManaged(isSidebarExpanded);
        menuLabel.setVisible(isSidebarExpanded);
        menuLabel.setManaged(isSidebarExpanded);

        javafx.scene.control.ContentDisplay display = isSidebarExpanded ? javafx.scene.control.ContentDisplay.LEFT
                : javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
        // Alignment managed by CSS

        setButtonStyle(btnDashboard, display);
        setButtonStyle(btnUsers, display);
        setButtonStyle(btnTransactions, display);
        setButtonStyle(btnEscrows, display);
        setButtonStyle(btnSupportHub, display);
        setButtonStyle(btnLogout, display);
    }

    private void setButtonStyle(Button btn, javafx.scene.control.ContentDisplay display) {
        btn.setContentDisplay(display);
        // Alignment handled by CSS
    }

    private void setActiveButton(Button activeButton) {
        // Reset all buttons to default style
        resetButtonStyle(btnDashboard);
        resetButtonStyle(btnUsers);
        resetButtonStyle(btnTransactions);
        resetButtonStyle(btnEscrows);
        resetButtonStyle(btnSupportHub);

        // Apply active style
        activeButton.getStyleClass().add("active");
        activeButton.setStyle("-fx-background-color: -color-primary; -fx-text-fill: white;");
    }

    private void resetButtonStyle(Button btn) {
        btn.getStyleClass().remove("active");
        btn.setStyle(""); // Clear inline styles
    }

    @FXML
    private void handleLogout() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (sidebar.getScene() != null)
            dialog.initOwner(sidebar.getScene().getWindow());

        VBox root = new VBox(20);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new javafx.geometry.Insets(30));
        root.setPrefWidth(340);
        root.setStyle("-fx-background-color: rgba(30, 20, 60, 0.95); -fx-background-radius: 16; "
                + "-fx-border-color: rgba(139, 92, 246, 0.3); -fx-border-radius: 16; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 5);");

        Label icon = new Label("\uD83D\uDEAA");
        icon.setStyle("-fx-font-size: 36px;");

        Label title = new Label("Log Out");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label msg = new Label("Are you sure you want to log out?");
        msg.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 14px;");
        msg.setWrapText(true);
        msg.setAlignment(javafx.geometry.Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: rgba(255,255,255,0.8); "
                + "-fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 28;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button logoutBtn = new Button("Log Out");
        logoutBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 28;");
        logoutBtn.setOnAction(e -> {
            dialog.close();
            SessionManager.logout();
            ViewUtils.setView(sidebar, "/view/login.fxml");
        });

        HBox buttons = new HBox(12, cancelBtn, logoutBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);

        root.getChildren().addAll(icon, title, msg, buttons);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    @FXML
    private void handleGoToDashboard() {
        ViewUtils.loadContent(adminContentArea, "/view/admin_home.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void handleGoToUsers() {
        ViewUtils.loadContent(adminContentArea, "/view/admin_users.fxml");
        setActiveButton(btnUsers);
    }

    @FXML
    private void handleGoToTransactions() {
        ViewUtils.loadContent(adminContentArea, "/view/admin_transactions.fxml");
        setActiveButton(btnTransactions);
    }

    @FXML
    private void handleGoToEscrows() {
        ViewUtils.loadContent(adminContentArea, "/view/admin_escrow.fxml");
        setActiveButton(btnEscrows);
    }

    @FXML
    private void handleGoToSupportHub() {
        ViewUtils.loadContent(adminContentArea, "/view/admin_support_hub.fxml");
        setActiveButton(btnSupportHub);
    }
}
