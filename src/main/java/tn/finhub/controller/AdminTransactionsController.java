package tn.finhub.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.finhub.model.User;
import tn.finhub.model.UserModel;
import tn.finhub.model.WalletModel;

public class AdminTransactionsController {

    @FXML
    private javafx.scene.layout.FlowPane usersContainer;

    @FXML
    private TextField searchField;

    private UserModel userModel = new UserModel();
    private WalletModel walletModel = new WalletModel();

    // Master list of all loaded data
    private ObservableList<UserWalletData> allUsersData = FXCollections.observableArrayList();

    // Cache for pre-fetching
    private static ObservableList<UserWalletData> cachedData = null;

    public static void setCachedData(java.util.List<UserWalletData> data) {
        cachedData = FXCollections.observableArrayList(data);
    }

    @FXML
    public void initialize() {
        if (cachedData != null && !cachedData.isEmpty()) {
            allUsersData.setAll(cachedData);
            refreshUserCards(allUsersData);
        } else {
            loadData();
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers(newValue);
        });

        // Add listener for responsive layout
        usersContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                adjustCardLayout();
            }
        });
    }

    private void loadData() {
        // Show Loading State
        usersContainer.getChildren().clear();
        javafx.scene.control.Label loadingLabel = new javafx.scene.control.Label("Loading users...");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        usersContainer.getChildren().add(loadingLabel);

        // Background Task
        javafx.concurrent.Task<java.util.List<UserWalletData>> task = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<UserWalletData> call() throws Exception {
                java.util.List<User> users = userModel.findAll();
                java.util.List<UserWalletData> dataList = new java.util.ArrayList<>();

                for (User user : users) {
                    // Fetch wallet info for each user (THIS IS THE SLOW PART - N+1)
                    // But now it happens in background
                    tn.finhub.model.Wallet wallet = walletModel.findByUserId(user.getId());
                    String status = (wallet != null) ? wallet.getStatus() : "NO_WALLET";
                    dataList.add(new UserWalletData(user, status));
                }
                return dataList;
            }
        };

        task.setOnSucceeded(e -> {
            java.util.List<UserWalletData> result = task.getValue();
            allUsersData.setAll(result);
            cachedData = FXCollections.observableArrayList(result); // Update cache
            refreshUserCards(allUsersData);
        });

        task.setOnFailed(e -> {
            usersContainer.getChildren().clear();
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label("Failed to load users.");
            errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px;");
            usersContainer.getChildren().add(errorLabel);
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void filterUsers(String query) {
        if (query == null || query.isEmpty()) {
            refreshUserCards(allUsersData);
            return;
        }

        String lowerCaseQuery = query.toLowerCase();
        ObservableList<UserWalletData> filteredList = FXCollections.observableArrayList();

        for (UserWalletData data : allUsersData) {
            User user = data.user;
            if (user.getFullName().toLowerCase().contains(lowerCaseQuery) ||
                    user.getEmail().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(data);
            }
        }
        refreshUserCards(filteredList);
    }

    @FXML
    public void handleRefresh() {
        loadData();
    }

    private void refreshUserCards(ObservableList<UserWalletData> dataList) {
        usersContainer.getChildren().clear();
        if (dataList.isEmpty()) {
            javafx.scene.control.Label emptyLabel = new javafx.scene.control.Label("No users found.");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
            usersContainer.getChildren().add(emptyLabel);
            return;
        }

        for (UserWalletData data : dataList) {
            usersContainer.getChildren().add(createUserCard(data));
        }

        // Adjust layout initially
        javafx.application.Platform.runLater(this::adjustCardLayout);
    }

    private void adjustCardLayout() {
        double containerWidth = usersContainer.getWidth();
        if (containerWidth <= 0)
            return;

        double hGap = usersContainer.getHgap();
        double hPadding = usersContainer.getPadding().getLeft() + usersContainer.getPadding().getRight();

        // Calculate available width for content
        double availableWidth = containerWidth - hPadding;

        // Determine columns: if sidebar is expanded (width < threshold), use 2, else 3
        int columns = (availableWidth < 1100) ? 2 : 3;

        // Calculate card width: (availableWidth - (columns - 1) * gap) / columns
        // Subtract a tiny bit (2px) to prevent wrapping due to floating point precision
        double cardWidth = (availableWidth - (columns - 1) * hGap) / columns - 2;

        for (javafx.scene.Node node : usersContainer.getChildren()) {
            if (node instanceof javafx.scene.layout.VBox) {
                javafx.scene.layout.VBox card = (javafx.scene.layout.VBox) node;
                card.setPrefWidth(cardWidth);
                card.setMinWidth(cardWidth);
                card.setMaxWidth(cardWidth);
            }
        }
    }

    private javafx.scene.Node createUserCard(UserWalletData data) {
        User user = data.user;
        String walletStatus = data.walletStatus;

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

        // Check for flags based on pre-fetched status
        if ("FROZEN".equals(walletStatus)) {
            javafx.scene.control.Label alertLabel = new javafx.scene.control.Label("⚠ FROZEN");
            alertLabel.setStyle(
                    "-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: rgba(239, 68, 68, 0.1); -fx-background-radius: 4px;");
            footer.getChildren().add(alertLabel);
        } else {
            javafx.scene.control.Label okLabel = new javafx.scene.control.Label("Active");
            okLabel.setStyle("-fx-text-fill: #34D399; -fx-font-size: 10px;");
            footer.getChildren().add(okLabel);
        }

        card.getChildren().addAll(header, new Separator(), footer);

        // Interaction
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            card.setCursor(javafx.scene.Cursor.DEFAULT);
            // Optional: Visual cue for non-clickable
            card.setStyle(card.getStyle() + "-fx-opacity: 0.7;");
        } else {
            card.setCursor(javafx.scene.Cursor.HAND);
            card.setOnMouseClicked(e -> handleShowUserTransactions(user));
            card.setOnMouseEntered(e -> card.getStyleClass().add("user-card-hover"));
            card.setOnMouseExited(e -> card.getStyleClass().remove("user-card-hover"));
        }

        return card;
    }

    private void handleShowUserTransactions(User user) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_user_transactions.fxml"));
            javafx.scene.Parent view = loader.load();

            AdminUserTransactionsController controller = loader.getController();

            // Get content area
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) usersContainer.getScene()
                    .lookup("#adminContentArea");

            if (contentArea != null) {
                // Fade Out Current
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(200), contentArea);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    // Switch View
                    contentArea.getChildren().setAll(view);
                    // Initialize controller AFTER view is attached so it can set up UI state
                    controller.setUser(user);

                    // Fade In New
                    javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                            javafx.util.Duration.millis(200), contentArea);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            } else {
                System.err.println("Critical Error: #adminContentArea not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            tn.finhub.util.DialogUtil.showError("Navigation Error", "Could not load user transactions.");
        }
    }

    // DTO for Background Loading
    public static class UserWalletData {
        public User user;
        public String walletStatus;

        public UserWalletData(User user, String walletStatus) {
            this.user = user;
            this.walletStatus = walletStatus;
        }
    }

    // Navigation Handlers removed - handled by Dashboard
}
