package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.application.Platform;
import tn.finhub.util.*;
import java.net.http.*;
import java.net.URI;
import org.json.JSONObject;
import java.sql.*;
<<<<<<< HEAD
import tn.finhub.service.FinancialProfileService;
=======
import tn.finhub.model.FinancialProfileModel;
>>>>>>> cd680ce (crud+controle de saisie)

import java.net.http.HttpRequest;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

<<<<<<< HEAD
    private void syncUserLocal(int id, String fullName, String email, String role) throws Exception {
        Connection conn = DBConnection.getInstance();

        PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users_local (user_id, full_name, email, role)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  full_name = VALUES(full_name),
                  email = VALUES(email),
                  role = VALUES(role)
                """);

        ps.setInt(1, id);
        ps.setString(2, fullName);
        ps.setString(3, email);
        ps.setString(4, role);

        ps.executeUpdate();
    }

    private void setView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
=======
    private void setView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            // Load with current resource bundle
            loader.setResources(LanguageManager.getInstance().getResourceBundle());
>>>>>>> cd680ce (crud+controle de saisie)
            javafx.scene.Parent newView = loader.load();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) emailField.getScene()
                    .lookup("#contentArea");

            if (!contentArea.getChildren().isEmpty()) {
                javafx.scene.Node currentView = contentArea.getChildren().get(0);
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(300), currentView);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(newView);
                    fadeIn(newView);
                });
                fadeOut.play();
            } else {
                contentArea.getChildren().setAll(newView);
                fadeIn(newView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
<<<<<<< HEAD
                alert.setTitle("Error Loading View");
                alert.setHeaderText("Could not load " + fxmlPath);
=======
                alert.setTitle(LanguageManager.getInstance().getString("login.error.loading.view"));
                alert.setHeaderText(LanguageManager.getInstance().getString("common.error") + " " + fxmlPath);
>>>>>>> cd680ce (crud+controle de saisie)
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private void fadeIn(javafx.scene.Node node) {
        node.setOpacity(0);
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML
    public void goToSignup() {
        setView("/view/signup.fxml");
    }

    @FXML
    public void goToForgotPassword() {
        setView("/view/forgot_password.fxml");
    }

    @FXML
    public void handleLogin() {
        System.out.println("[DEBUG] Login button clicked");
<<<<<<< HEAD
        messageLabel.setText("Logging in...");
=======

        // Input Validation
        String email = emailField.getText();
        if (!ValidationUtils.isValidEmail(email)) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(LanguageManager.getInstance().getString("login.invalid.email"));
            return;
        }

        // Password validation is technically not needed for login (as we just check if
        // it matches DB),
        // but user requested "password should be like the standard" in login/signin.
        // Usually login just checks credentials, but if they want strict enforcement
        // even at login:
        /*
         * String pwdError =
         * ValidationUtils.getPasswordValidationError(passwordField.getText());
         * if (pwdError != null) {
         * messageLabel.setStyle("-fx-text-fill: red;");
         * messageLabel.setText(pwdError);
         * return;
         * }
         */
        // Standard practice: don't validate password complexity on login, just on
        // signup.
        // However, user said "in login and signin page... password should be like the
        // standard".
        // I will interpret this as maybe they want visual consistency, but blocking
        // login due to old weak passwords is bad UX.
        // I will stick to just checking emptiness for login, but apply full rules for
        // Signup.

        if (passwordField.getText().isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(LanguageManager.getInstance().getString("login.empty.password"));
            return;
        }

        messageLabel.setStyle("-fx-text-fill: orange;");
        messageLabel.setText(LanguageManager.getInstance().getString("login.logging.in"));
>>>>>>> cd680ce (crud+controle de saisie)

        new Thread(() -> {
            try {
                System.out.println("[DEBUG] Sending login request...");
                String json = """
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(
<<<<<<< HEAD
                        emailField.getText(),
=======
                        email,
>>>>>>> cd680ce (crud+controle de saisie)
                        passwordField.getText());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ApiClient.BASE_URL + "/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = ApiClient.getClient().send(request,
                        HttpResponse.BodyHandlers.ofString());
                System.out.println("[DEBUG] Login response code: " + response.statusCode());

                if (response.statusCode() != 200) {
<<<<<<< HEAD
                    Platform.runLater(() -> messageLabel.setText(" Login failed"));
=======
                    // Try to parse error details from JSON body
                    String errorMessage = "Login failed"; // Default
                    try {
                        JSONObject errorBody = new JSONObject(response.body());
                        if (errorBody.has("detail")) {
                            errorMessage = errorBody.getString("detail");
                        }
                    } catch (Exception e) {
                        // Fallback if body is not JSON or empty
                        System.out.println("Could not parse error body: " + e.getMessage());
                    }

                    // Override with specific messages based on status code if detail is generic or
                    // missing (optional, but safer)
                    if (response.statusCode() == 404 && "Login failed".equals(errorMessage)) {
                        errorMessage = "User not found";
                    } else if (response.statusCode() == 401 && "Login failed".equals(errorMessage)) {
                        errorMessage = "Invalid credentials";
                    } else if (response.statusCode() == 403 && "Login failed".equals(errorMessage)) {
                        errorMessage = "Email not verified";
                    }

                    String finalMsg = errorMessage;
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText(finalMsg);
                    });
>>>>>>> cd680ce (crud+controle de saisie)
                    return;
                }

                JSONObject body = new JSONObject(response.body());
                JSONObject user = body.getJSONObject("user");

                System.out.println("[DEBUG] Login successful, initializing session...");
<<<<<<< HEAD
=======

                Platform.runLater(() -> {
                    messageLabel.setStyle("-fx-text-fill: green;");
                    messageLabel.setText(LanguageManager.getInstance().getString("login.success"));
                });
>>>>>>> cd680ce (crud+controle de saisie)
                SessionManager.login(
                        user.getInt("id"),
                        user.optString("full_name", ""), // Use optString for safety
                        user.getString("email"),
                        user.getString("role"),
                        body.getString("access_token"));

                System.out.println("[DEBUG] Syncing local DB...");
                syncUserLocal(
                        user.getInt("id"),
                        user.optString("full_name", ""),
                        user.getString("email"),
                        user.getString("role"));

                // Navigate based on role and profile status
<<<<<<< HEAD
                Platform.runLater(() -> {
                    try {
                        if (SessionManager.isAdmin()) {
                            // CHECK MIGRATION REQUIREMENT
                            tn.finhub.service.WalletService ws = new tn.finhub.service.WalletService();
                            if (ws.hasWallet(user.getInt("id"))) {
                                System.out.println("Admin has a personal wallet. Redirecting to Migration Wizard.");
                                setView("/view/migrate_wallet.fxml");
                            } else {
                                System.out.println("User is Admin. Navigating to Admin Dashboard.");
                                setView("/view/admin_users.fxml");
                            }
                        } else {
                            System.out.println("[DEBUG] Checking profile completion...");
                            FinancialProfileService profileService = new FinancialProfileService();
                            int userId = user.getInt("id");

                            // Ensure profile exists (create with default values if not)
                            profileService.ensureProfile(userId);

                            boolean completed = profileService.isProfileCompleted(userId);
                            System.out.println("User ID: " + userId + ", Profile Completed: " + completed);

                            if (!completed) {
                                System.out.println("Redirecting to Complete Profile.");
                                setView("/view/complete_profile.fxml");
                            } else {
                                System.out.println("Redirecting to User Dashboard.");
                                setView("/view/user_dashboard.fxml");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        messageLabel.setText("Error loading view");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> messageLabel.setText(" Server error"));
            }
        }).start();
    }
=======
                // Navigate based on role and profile status
                if (SessionManager.isAdmin()) {
                    try {
                        // CHECK MIGRATION REQUIREMENT (Background Thread)
                        tn.finhub.model.WalletModel walletModel = new tn.finhub.model.WalletModel();
                        if (walletModel.hasWallet(user.getInt("id"))) {
                            Platform.runLater(() -> {
                                System.out.println("Admin has a personal wallet. Redirecting to Migration Wizard.");
                                setView("/view/migrate_wallet.fxml");
                            });
                        } else {
                            // PRE-FETCH ADMIN DATA
                            Platform.runLater(() -> Message("Fetching admin data..."));

                            try {
                                tn.finhub.model.UserModel uModel = new tn.finhub.model.UserModel();
                                java.util.List<tn.finhub.model.User> allUsersList = uModel.findAll();
                                java.util.List<tn.finhub.controller.AdminTransactionsController.UserWalletData> cacheList = new java.util.ArrayList<>();

                                for (tn.finhub.model.User u : allUsersList) {
                                    tn.finhub.model.Wallet w = walletModel.findByUserId(u.getId());
                                    String status = (w != null) ? w.getStatus() : "NO_WALLET";
                                    cacheList.add(new tn.finhub.controller.AdminTransactionsController.UserWalletData(
                                            u, status));
                                }
                                tn.finhub.controller.AdminTransactionsController.setCachedData(cacheList);
                            } catch (Exception e) {
                                System.err.println("Admin pre-fetch failed: " + e.getMessage());
                                // Proceed anyway
                            }

                            Platform.runLater(() -> {
                                System.out.println("User is Admin. Navigating to Admin Dashboard.");
                                setView("/view/admin_dashboard.fxml");
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> messageLabel.setText("Error loading view"));
                    }
                } else {
                    System.out.println("[DEBUG] Checking profile completion...");
                    try {
                        tn.finhub.model.FinancialProfileModel profileModel = new tn.finhub.model.FinancialProfileModel();
                        int userId = user.getInt("id");

                        // Ensure profile exists (create with default values if not) - Running on
                        // background thread now
                        profileModel.ensureProfile(userId);

                        boolean completed = profileModel.isProfileCompleted(userId);
                        System.out.println("User ID: " + userId + ", Profile Completed: " + completed);

                        if (!completed) {
                            Platform.runLater(() -> {
                                System.out.println("Redirecting to Complete Profile.");
                                setView("/view/complete_profile.fxml");
                            });
                        } else {
                            Platform.runLater(() -> Message("Fetching your financial data..."));

                            // Pre-fetch Wallet Data for Dashboard - already on background thread, so just
                            // continue
                            try {
                                tn.finhub.model.WalletModel walletModel = new tn.finhub.model.WalletModel();
                                tn.finhub.model.Wallet wallet = walletModel.findByUserId(userId);

                                if (wallet != null) {
                                    // Ensure wallet exists before fetching dependent data
                                    if ("FROZEN".equals(wallet.getStatus())) {
                                        // Handle frozen logic if needed, but mainly just fetch
                                    }

                                    // Fetch dependency models
                                    tn.finhub.model.VirtualCardModel cardModel = new tn.finhub.model.VirtualCardModel();
                                    tn.finhub.model.MarketModel marketModel = new tn.finhub.model.MarketModel();

                                    // 1. Fetch Wallet & Dependencies
                                    java.util.List<tn.finhub.model.VirtualCard> cards = cardModel
                                            .findByWalletId(wallet.getId());
                                    java.util.List<tn.finhub.model.WalletTransaction> transactions = walletModel
                                            .getTransactionHistory(wallet.getId());
                                    int badTxId = "FROZEN".equals(wallet.getStatus())
                                            ? walletModel.getTamperedTransactionId(wallet.getId())
                                            : -1;

                                    // 2. Fetch Portfolio & Market Data
                                    java.util.List<tn.finhub.model.PortfolioItem> items = marketModel
                                            .getPortfolio(userId);
                                    java.util.Map<String, tn.finhub.model.PortfolioItem> portfolioMap = new java.util.HashMap<>();

                                    java.math.BigDecimal portValue = java.math.BigDecimal.ZERO;
                                    java.math.BigDecimal totalInvested = java.math.BigDecimal.ZERO;

                                    for (tn.finhub.model.PortfolioItem item : items) {
                                        totalInvested = totalInvested
                                                .add(item.getAverageCost().multiply(item.getQuantity()));
                                        portfolioMap.put(item.getSymbol(), item);
                                    }

                                    // 3. Fetch Contacts (For Transactions Page)
                                    tn.finhub.model.SavedContactModel contactModel = new tn.finhub.model.SavedContactModel();
                                    java.util.List<tn.finhub.model.SavedContact> contacts = contactModel
                                            .getContactsByUserId(userId);

                                    // 4. Fetch Profile
                                    tn.finhub.model.FinancialProfileModel fpm = new tn.finhub.model.FinancialProfileModel();
                                    tn.finhub.model.FinancialProfile profile = fpm.findByUserId(userId);

                                    // 5. Fetch Support Tickets
                                    tn.finhub.model.SupportModel supportModel = new tn.finhub.model.SupportModel();
                                    java.util.List<tn.finhub.model.SupportTicket> tickets = supportModel
                                            .getTicketsByUserId(userId);

                                    // --- INJECT INTO CACHES ---

                                    // Wallet Controller Cache
                                    String bestAsset = "N/A"; // Simplified for pre-fetch
                                    java.math.BigDecimal maxPnlPercent = java.math.BigDecimal.ZERO;
                                    int assetCount = items.size();

                                    WalletController.WalletDataPacket walletData = new WalletController.WalletDataPacket(
                                            wallet, cards, transactions, badTxId, portValue, totalInvested,
                                            bestAsset,
                                            maxPnlPercent, assetCount, new java.util.HashMap<>());
                                    WalletController.setCachedData(walletData);

                                    // Transactions Controller Cache
                                    boolean isFrozen = "FROZEN".equals(wallet.getStatus());
                                    TransactionsController.TransactionData txData = new TransactionsController.TransactionData(
                                            userId, transactions, contacts, badTxId, isFrozen,
                                            new java.util.HashMap<>());
                                    TransactionsController.setCachedData(txData);

                                    // Financial Twin Cache
                                    FinancialTwinController.setCachedPortfolio(portfolioMap);

                                    // Profile Cache
                                    if (profile != null) {
                                        ProfileController.setCachedProfile(profile);
                                    }

                                    // Support Tickets Cache
                                    SupportTicketsController.setCachedTickets(tickets);
                                }
                            } catch (Exception e) {
                                System.out.println("Pre-fetch failed: " + e.getMessage());
                                // Continue anyway, dashboard will load data itself
                            }

                            // Navigate after fetch (success or fail)
                            Platform.runLater(() -> {
                                System.out.println("Redirecting to User Dashboard.");
                                setView("/view/user_dashboard.fxml");
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> messageLabel.setText("Error loading profile"));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(
                        () -> messageLabel.setText(LanguageManager.getInstance().getString("login.server.error")));
            }
        }).start();
    }

    private void syncUserLocal(int id, String fullName, String email, String role) throws Exception {
        Connection conn = DBConnection.getInstance();

        PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users_local (user_id, full_name, email, role)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  full_name = VALUES(full_name),
                  email = VALUES(email),
                  role = VALUES(role)
                """);

        ps.setInt(1, id);
        ps.setString(2, fullName);
        ps.setString(3, email);
        ps.setString(4, role);

        ps.executeUpdate();
    }

    private void Message(String msg) {
        Platform.runLater(() -> messageLabel.setText(msg));
    }
>>>>>>> cd680ce (crud+controle de saisie)
}
