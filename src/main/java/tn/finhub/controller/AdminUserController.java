package tn.finhub.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
// import javafx.scene.Node; // Removed unused import
import tn.finhub.model.User;
import tn.finhub.util.MailClient;

import tn.finhub.util.ApiClient;
import tn.finhub.util.SessionManager;
import tn.finhub.util.ViewUtils;

public class AdminUserController {

    @FXML
    private javafx.scene.layout.FlowPane usersContainer;

    @FXML
    private TextField searchField;

    private tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
    private ObservableList<User> allUsers;

    @FXML
    public void initialize() {
        // Load all users initially
        allUsers = FXCollections.observableArrayList(userModel.findAll());
        refreshUserCards(allUsers);

        // Add search listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers(newValue);
        });
    }

    private void filterUsers(String query) {
        if (query == null || query.isEmpty()) {
            refreshUserCards(allUsers);
            return;
        }

        String lowerCaseQuery = query.toLowerCase();
        ObservableList<User> filteredList = FXCollections.observableArrayList();

        for (User user : allUsers) {
            String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
            String name = user.getFullName() != null ? user.getFullName().toLowerCase() : "";
            String role = user.getRole() != null ? user.getRole().toLowerCase() : "";

            if (email.contains(lowerCaseQuery) || name.contains(lowerCaseQuery) || role.contains(lowerCaseQuery)) {
                filteredList.add(user);
            }
        }

        refreshUserCards(filteredList);
    }

    private void refreshUserCards(ObservableList<User> users) {
        usersContainer.getChildren().clear();
        for (User user : users) {
            usersContainer.getChildren().add(createUserCard(user));
        }
    }

    private javafx.scene.Node createUserCard(User user) {
        // Card Container
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
        card.getStyleClass().add("user-card");
        card.setPrefWidth(240);
        card.setMinWidth(240);
        card.setMaxWidth(240);

        // Header: Initial + Name + Role
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.shape.Circle avatar = new javafx.scene.shape.Circle(20);
        avatar.setFill(javafx.scene.paint.Color.valueOf("#2D2A40"));
        javafx.scene.control.Label initialLabel = new javafx.scene.control.Label(
                user.getFullName() != null && !user.getFullName().isEmpty()
                        ? user.getFullName().substring(0, 1).toUpperCase()
                        : "U");
        initialLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        javafx.scene.layout.StackPane avatarStack = new javafx.scene.layout.StackPane(avatar, initialLabel);

        javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(user.getFullName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        javafx.scene.control.Label roleLabel = new javafx.scene.control.Label(user.getRole());
        roleLabel.getStyleClass().add("role-badge");
        if ("ADMIN".equals(user.getRole())) {
            roleLabel.setStyle(
                    roleLabel.getStyle() + "; -fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #A78BFA;");
        } else {
            roleLabel.setStyle(
                    roleLabel.getStyle() + "; -fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34D399;");
        }

        info.getChildren().addAll(nameLabel, roleLabel);
        header.getChildren().addAll(avatarStack, info);

        // Details: Email, ID
        javafx.scene.layout.VBox details = new javafx.scene.layout.VBox(5);
        javafx.scene.control.Label emailLabel = new javafx.scene.control.Label("Email: " + user.getEmail());
        emailLabel.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 12px;");

        javafx.scene.control.Label idLabel = new javafx.scene.control.Label("ID: " + user.getId());
        idLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 10px;");

        details.getChildren().addAll(emailLabel, idLabel);

        // Only show Trust Score for non-admins
        if (!"ADMIN".equals(user.getRole())) {
            javafx.scene.control.Label trustLabel = new javafx.scene.control.Label(
                    "Trust Score: " + user.getTrustScore());
            trustLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 11px;");
            details.getChildren().add(trustLabel);
        }

        // Actions
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        boolean isSelf = user.getId() == SessionManager.getUserId();
        boolean isAdmin = "ADMIN".equals(user.getRole());

        if (!isSelf && !isAdmin) {
            Button makeAdminBtn = new Button("Promote");
            makeAdminBtn.getStyleClass().add("button-small-primary");
            makeAdminBtn.setOnAction(e -> handlePromoteUser(user));
            actions.getChildren().add(makeAdminBtn);
        }

        // Delete button removed as per requirements

        // Add Hover Effect & Click Action ONLY for non-admins
        if (!isAdmin) {
            card.setCursor(javafx.scene.Cursor.HAND);
            card.setOnMouseClicked(e -> {
                // Avoid triggering when clicking buttons inside the card
                if (e.getTarget() instanceof javafx.scene.Node) {
                    javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
                    // Traverse up to check if we clicked a button
                    while (target != null && target != card) {
                        if (target instanceof Button)
                            return;
                        target = target.getParent();
                    }
                }
                handleShowDetails(user, card);
            });

            card.setOnMouseEntered(e -> card.getStyleClass().add("user-card-hover"));
            card.setOnMouseExited(e -> card.getStyleClass().remove("user-card-hover"));
        }

        card.getChildren().addAll(header, new Separator(), details, new javafx.scene.layout.Region(), actions);
        javafx.scene.layout.VBox.setVgrow(details, javafx.scene.layout.Priority.ALWAYS); // Push actions to bottom if
                                                                                         // fixed height

        return card;
    }

    private void handlePromoteUser(User user) {
        boolean confirmed = tn.finhub.util.DialogUtil.showConfirmation(
                "Promote User to Admin",
                "Are you sure you want to promote this user?\n\nEmail: " + user.getEmail());

        if (!confirmed)
            return;

        try {
            String jsonResponse = ApiClient.inviteAdmin(user.getEmail());
            org.json.JSONObject json = new org.json.JSONObject(jsonResponse);
            String inviteLink = json.getString("invite_link");
            MailClient.sendAdminInviteEmail(user.getEmail(), inviteLink);

            tn.finhub.util.DialogUtil.showInfo(
                    "Admin Invitation Sent",
                    "An admin invitation email was sent successfully to:\n\n" + user.getEmail());

            // Refresh list to update UI if role changed (though usually invite implies
            // pending, assume role might update later or refresh required)
            // For now just refresh view
            loadUsers();

        } catch (Exception e) {
            e.printStackTrace();
            tn.finhub.util.DialogUtil.showError("Operation Failed",
                    "Could not promote this user.\n\n" + e.getMessage());
        }
    }

    @FXML
    public void handleRefresh() {
        try {
            userModel.syncUsersFromServer();
            loadUsers();

            tn.finhub.util.DialogUtil.showInfo(
                    "Sync Completed",
                    "Users successfully refreshed from server.");
        } catch (Exception e) {
            e.printStackTrace();
            tn.finhub.util.DialogUtil.showError(
                    "Sync Failed",
                    "Could not refresh users from server.");
        }
    }

    // ...

    private void handleDelete(User user) {
        boolean confirmed = tn.finhub.util.DialogUtil.showConfirmation("Delete User",
                "Are you sure you want to delete " + user.getFullName() + "?");

        if (!confirmed)
            return;

        try {
            userModel.deleteUser(user.getId());
            loadUsers();
        } catch (Exception e) {
            e.printStackTrace();
            tn.finhub.util.DialogUtil.showError("Deletion Failed", "Could not delete user. Please try again.");
        }
    }

    // Helper to reload data
    public void loadUsers() {
        allUsers = FXCollections.observableArrayList(userModel.findAll());
        filterUsers(searchField.getText());
    }

    private void handleShowDetails(User user, javafx.scene.Node sourceNode) {
        try {
            System.out.println("DEBUG: handleShowDetails called for " + user.getEmail());

            // CRITICAL: Get scene from the clicked card (which is definitely in the scene)
            javafx.scene.Scene scene = sourceNode.getScene();
            if (scene == null) {
                System.err.println("ERROR: sourceNode has no scene!");
                return;
            }

            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) scene
                    .lookup("#adminContentArea");
            if (contentArea == null) {
                System.err.println("CRITICAL ERROR: #adminContentArea not found in scene. Navigation aborted.");
                tn.finhub.util.DialogUtil.showError("System Error",
                        "Navigation component missing. Please restart the application.");
                return;
            }

            System.out.println("DEBUG: adminContentArea found. Loading view...");
            System.out.println("DEBUG: Current adminContentArea children: " + contentArea.getChildren().size());
            if (!contentArea.getChildren().isEmpty()) {
                System.out
                        .println("DEBUG: Current view type: " + contentArea.getChildren().get(0).getClass().getName());
            }

            // Now load the new view
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_user_details.fxml"));
            javafx.scene.Parent view = loader.load();

            AdminUserDetailsController controller = loader.getController();
            controller.setUser(user);

            // Debug: Check sidebar state BEFORE replacing content
            javafx.scene.Parent root = scene.getRoot();
            javafx.scene.Node sidebar = root.lookup("#mainSidebar");

            if (sidebar != null) {
                System.out.println("DEBUG: Sidebar found before content swap.");
                System.out.println("DEBUG: Sidebar visible=" + sidebar.isVisible());
                System.out.println("DEBUG: Sidebar managed=" + sidebar.isManaged());

                // Force it to be visible
                sidebar.setVisible(true);
                sidebar.setManaged(true);
            } else {
                System.err.println("CRITICAL DEBUG: Sidebar #mainSidebar NOT found!");
                System.err.println("DEBUG: Scene root type: " + root.getClass().getName());

                // Debug: Check if root is BorderPane and what's in its left
                if (root instanceof javafx.scene.layout.BorderPane) {
                    javafx.scene.layout.BorderPane bp = (javafx.scene.layout.BorderPane) root;
                    javafx.scene.Node left = bp.getLeft();
                    System.err.println(
                            "DEBUG: BorderPane left node: " + (left != null ? left.getClass().getName() : "NULL"));
                    if (left != null) {
                        System.err.println("DEBUG: Left node ID: " + left.getId());
                    }
                }
            }

            // Replace content (this should NOT affect the sidebar since it's in the
            // BorderPane's <left>)
            contentArea.getChildren().setAll(view);

            // Fade in animation
            view.setOpacity(0);
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), view);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

        } catch (Exception e) {
            e.printStackTrace();
            tn.finhub.util.DialogUtil.showError("Navigation Error", "Could not load user details.");
        }
    }

    // Navigation Handlers removed - these belong in AdminDashboardController only
}
