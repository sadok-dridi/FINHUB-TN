package tn.finhub.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.finhub.model.User;
import tn.finhub.service.UserService;
import tn.finhub.service.WalletService;
import tn.finhub.util.SessionManager;

public class AdminTransactionsController {

    @FXML
    private javafx.scene.layout.FlowPane usersContainer;

    @FXML
    private TextField searchField;

    private UserService userService = new UserService();
    private WalletService walletService = new WalletService();
    private ObservableList<User> allUsers;

    @FXML
    public void initialize() {
        allUsers = FXCollections.observableArrayList(userService.getAllUsers());
        refreshUserCards(allUsers);

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
            if (user.getFullName().toLowerCase().contains(lowerCaseQuery) ||
                    user.getEmail().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(user);
            }
        }
        refreshUserCards(filteredList);
    }

    @FXML
    public void handleRefresh() {
        allUsers = FXCollections.observableArrayList(userService.getAllUsers());
        refreshUserCards(allUsers);
    }

    private void refreshUserCards(ObservableList<User> users) {
        usersContainer.getChildren().clear();
        for (User user : users) {
            usersContainer.getChildren().add(createUserCard(user));
        }
    }

    private javafx.scene.Node createUserCard(User user) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
        card.getStyleClass().add("user-card");
        card.setPrefWidth(280);
        card.setMinWidth(280);
        card.setMaxWidth(280);

        // Header
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
        javafx.scene.control.Label emailLabel = new javafx.scene.control.Label(user.getEmail());
        emailLabel.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 12px;");

        info.getChildren().addAll(nameLabel, emailLabel);
        header.getChildren().addAll(avatarStack, info);

        // Footer / Status
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox();
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // Check for flags
        boolean hasIssues = false;
        if (walletService.hasWallet(user.getId())) {
            int walletId = walletService.getWallet(user.getId()).getId();
            if (walletService.isFrozen(walletId)) {
                javafx.scene.control.Label alertLabel = new javafx.scene.control.Label("⚠ FROZEN");
                alertLabel.setStyle(
                        "-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: rgba(239, 68, 68, 0.1); -fx-background-radius: 4px;");
                footer.getChildren().add(alertLabel);
                hasIssues = true;
            }
        }

        if (!hasIssues) {
            javafx.scene.control.Label okLabel = new javafx.scene.control.Label("Active");
            okLabel.setStyle("-fx-text-fill: #34D399; -fx-font-size: 10px;");
            footer.getChildren().add(okLabel);
        }

        card.getChildren().addAll(header, new Separator(), footer);

        // Interaction
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> handleShowUserTransactions(user));
        card.setOnMouseEntered(e -> card.getStyleClass().add("user-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("user-card-hover"));

        return card;
    }

    private void handleShowUserTransactions(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_user_transactions.fxml"));
            javafx.scene.Parent view = loader.load();

            AdminUserTransactionsController controller = loader.getController();
            controller.setUser(user);

            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) usersContainer.getScene()
                    .lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            } else {
                usersContainer.getScene().setRoot(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
            tn.finhub.util.DialogUtil.showError("Navigation Error", "Could not load user transactions.");
        }
    }

    @FXML
    public void handleLogout() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/login.fxml"));
            javafx.scene.Parent view = loader.load();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) usersContainer.getScene()
                    .lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Navigation Handlers for Sidebar
    @FXML
    public void handleGoToUsers() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_users.fxml"));
            javafx.scene.Parent view = loader.load();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) usersContainer.getScene()
                    .lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
