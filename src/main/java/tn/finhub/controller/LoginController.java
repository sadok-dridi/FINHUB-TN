package tn.finhub.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import netscape.javascript.JSObject;
import org.json.JSONObject;
import tn.finhub.model.FinancialProfileModel;
import tn.finhub.util.ApiClient;
import tn.finhub.util.DBConnection;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.LanguageManager;
import tn.finhub.util.DesktopNotificationUtil;
import tn.finhub.util.RecaptchaLocalServer;
import tn.finhub.util.RecaptchaService;
import tn.finhub.util.SessionManager;
import tn.finhub.util.ValidationUtils;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    // Receives the reCAPTCHA token from the WebView (see login.fxml)
    @FXML
    private TextField recaptchaTokenField;

    // WebView that hosts the Google reCAPTCHA widget
    @FXML
    private WebView recaptchaWebView;

    @FXML
    private void initialize() {
        setupRecaptchaWidget();
    }

    // Overlay shown when the image challenge is open
    private javafx.scene.layout.StackPane recaptchaOverlay;

    private void setupRecaptchaWidget() {
        String siteKey = RecaptchaService.getSiteKey();
        if (recaptchaWebView == null || siteKey == null || siteKey.isBlank()) {
            System.err.println("[reCAPTCHA] Setup skipped — siteKey is null or WebView missing");
            return;
        }

        WebEngine engine = recaptchaWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        recaptchaWebView.setContextMenuEnabled(false);
        recaptchaWebView.setStyle("-fx-background-color: transparent;");

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            System.out.println("[reCAPTCHA] WebView state: " + newState);
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaBridge", new RecaptchaBridge());
                    System.out.println("[reCAPTCHA] Java bridge injected successfully");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (newState == Worker.State.FAILED) {
                System.err.println("[reCAPTCHA] WebView FAILED: " + engine.getLoadWorker().getException());
            }
        });

        String url = RecaptchaLocalServer.getInstance().start(siteKey, token -> Platform.runLater(() -> {
            if (recaptchaTokenField != null) {
                recaptchaTokenField.setText(token != null ? token : "");
                System.out.println("[reCAPTCHA] Token received: " + (token != null && !token.isBlank() ? "✓" : "cleared/expired"));
            }
            // Always close popup when token arrives (success or expired)
            hideRecaptchaPopup();
        }));

        System.out.println("[reCAPTCHA] Loading URL: " + url);
        if (url != null) {
            engine.load(url);
        }
    }

    /**
     * Called by JS (via RecaptchaBridge) when the image challenge opens or closes.
     * Shows/hides a floating popup WebView over the whole scene.
     */
    private void showRecaptchaPopup() {
        Platform.runLater(() -> {
            javafx.scene.Scene scene = emailField.getScene();
            if (scene == null) return;

            if (recaptchaOverlay != null) return; // already showing

            // Semi-transparent dark backdrop
            javafx.scene.layout.StackPane backdrop = new javafx.scene.layout.StackPane();
            backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
            backdrop.setOnMouseClicked(e -> hideRecaptchaPopup()); // click outside = close

            // White card containing a second WebView loaded to the same local URL
            WebView popupWebView = new WebView();
            popupWebView.setContextMenuEnabled(false);
            popupWebView.setPrefWidth(310);
            popupWebView.setPrefHeight(500);
            popupWebView.setMaxWidth(310);
            popupWebView.setMaxHeight(500);

            // Share the same engine so state is preserved — load same URL in popup
            String url = RecaptchaLocalServer.getInstance().getRecaptchaUrl();
            if (url != null) {
                popupWebView.getEngine().setJavaScriptEnabled(true);
                popupWebView.getEngine().load(url);
                // Re-inject bridge into popup engine
                popupWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, o, n) -> {
                    if (n == Worker.State.SUCCEEDED) {
                        try {
                            JSObject w = (JSObject) popupWebView.getEngine().executeScript("window");
                            w.setMember("javaBridge", new RecaptchaBridge());
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                });
            }

            javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(popupWebView);
            card.setStyle("-fx-background-color: #1E1B2E; -fx-border-color: #2E2A45; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
            card.setAlignment(javafx.geometry.Pos.CENTER);
            card.setOnMouseClicked(javafx.event.Event::consume); // don't close when clicking card

            backdrop.getChildren().add(card);

            // Add overlay on top of the root scene node
            javafx.scene.layout.StackPane root = (javafx.scene.layout.StackPane) scene.getRoot();
            root.getChildren().add(backdrop);
            recaptchaOverlay = backdrop;
        });
    }

    private void hideRecaptchaPopup() {
        Platform.runLater(() -> {
            if (recaptchaOverlay == null) return;
            javafx.scene.Scene scene = emailField.getScene();
            if (scene != null) {
                javafx.scene.layout.StackPane root = (javafx.scene.layout.StackPane) scene.getRoot();
                root.getChildren().remove(recaptchaOverlay);
            }
            recaptchaOverlay = null;
        });
    }

    /**
     * Resets the reCAPTCHA widget and clears the stored token.
     * Call this after a failed login attempt so the user must re-verify.
     */
    private void resetRecaptcha() {
        if (recaptchaTokenField != null) {
            recaptchaTokenField.clear();
        }
        if (recaptchaWebView != null) {
            try {
                recaptchaWebView.getEngine().executeScript("grecaptcha.reset()");
            } catch (Exception e) {
                // Widget might not be ready yet — safe to ignore
                System.err.println("[reCAPTCHA] Reset failed: " + e.getMessage());
            }
        }
    }

    /**
     * Bridge object exposed to JavaScript as "javaBridge" inside the WebView.
     * JS calls javaBridge.onTokenReceived(token), which updates the hidden field.
     */
    public class RecaptchaBridge {
        public void onTokenReceived(String token) {
            Platform.runLater(() -> {
                if (recaptchaTokenField != null) {
                    recaptchaTokenField.setText(token != null ? token : "");
                }
            });
        }

        /** Called by MutationObserver in the WebView when the challenge iframe appears/disappears */
        public void onChallengeVisibilityChanged(boolean visible) {
            // We now keep the WebView tall at all times; no size change needed.
            // This hook is kept for potential future UX tweaks.
        }
    }

    private void setView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            // Load with current resource bundle
            loader.setResources(LanguageManager.getInstance().getResourceBundle());
            javafx.scene.Parent newView = loader.load();
            if (emailField.getScene() == null) {
                System.out.println("[DEBUG] View is no longer active, aborting navigation to " + fxmlPath);
                return;
            }
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
                DialogUtil.showError(
                        LanguageManager.getInstance().getString("login.error.loading.view"),
                        LanguageManager.getInstance().getString("common.error") + " " + fxmlPath + "\n"
                                + e.getMessage());
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

        // Input Validation
        String email = emailField.getText();
        if (!ValidationUtils.isValidEmail(email)) {
            messageLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 13px; -fx-font-weight: bold;");
            messageLabel.setText(LanguageManager.getInstance().getString("login.invalid.email"));
            return;
        }

        if (passwordField.getText().isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 13px; -fx-font-weight: bold;");
            messageLabel.setText(LanguageManager.getInstance().getString("login.empty.password"));
            return;
        }

        // ✅ reCAPTCHA v2: token must be present BEFORE login is attempted
        // (user must have checked the box already)
        final String recaptchaToken = (recaptchaTokenField != null) ? recaptchaTokenField.getText() : null;

        if (RecaptchaService.isConfigured() && (recaptchaToken == null || recaptchaToken.isBlank())) {
            messageLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 13px; -fx-font-weight: bold;");
            messageLabel.setText("Please complete the security verification (reCAPTCHA).");
            return;
        }

        messageLabel.setStyle("-fx-text-fill: orange;");
        messageLabel.setText(LanguageManager.getInstance().getString("login.logging.in"));

        new Thread(() -> {
            try {
                // Verify reCAPTCHA server-side if configured
                if (RecaptchaService.isConfigured()) {
                    boolean captchaOk = RecaptchaService.verify(recaptchaToken);
                    if (!captchaOk) {
                        Platform.runLater(() -> {
                            messageLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 13px; -fx-font-weight: bold;");
                            messageLabel.setText("Security verification failed. Please try again.");
                            // ✅ Reset widget so user can re-check the box
                            resetRecaptcha();
                        });
                        return;
                    }
                }

                System.out.println("[DEBUG] Sending login request...");
                String json = """
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(
                        email,
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
                    String errorMessage = "Login failed";
                    try {
                        JSONObject errorBody = new JSONObject(response.body());
                        if (errorBody.has("detail")) {
                            errorMessage = errorBody.getString("detail");
                        }
                    } catch (Exception e) {
                        System.out.println("Could not parse error body: " + e.getMessage());
                    }

                    if (response.statusCode() == 404 && "Login failed".equals(errorMessage)) {
                        errorMessage = "User not found";
                    } else if (response.statusCode() == 401 && "Login failed".equals(errorMessage)) {
                        errorMessage = "Invalid credentials";
                    } else if (response.statusCode() == 403 && "Login failed".equals(errorMessage)) {
                        errorMessage = "Email not verified";
                    }

                    String finalMsg = errorMessage;
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 13px; -fx-font-weight: bold;");
                        messageLabel.setText(finalMsg);
                        // ✅ Reset reCAPTCHA on any login failure so user must re-verify
                        resetRecaptcha();
                    });
                    return;
                }

                JSONObject body = new JSONObject(response.body());
                JSONObject user = body.getJSONObject("user");

                System.out.println("[DEBUG] Login successful, initializing session...");

                Platform.runLater(() -> {
                    messageLabel.setStyle("-fx-text-fill: green;");
                    messageLabel.setText(LanguageManager.getInstance().getString("login.success"));
                });
                SessionManager.login(
                        user.getInt("id"),
                        user.optString("full_name", ""),
                        user.getString("email"),
                        user.getString("role"),
                        body.getString("access_token"));

                // After session is initialized, show latest system alert (if any) as
                // desktop notification.
                showLatestSystemAlertNotification(user.getInt("id"));

                System.out.println("[DEBUG] Syncing local DB...");
                syncUserLocal(
                        user.getInt("id"),
                        user.optString("full_name", ""),
                        user.getString("email"),
                        user.getString("role"));

                if (SessionManager.isAdmin()) {
                    try {
                        tn.finhub.model.WalletModel walletModel = new tn.finhub.model.WalletModel();
                        if (walletModel.hasWallet(user.getInt("id"))) {
                            Platform.runLater(() -> {
                                System.out.println("Admin has a personal wallet. Redirecting to Migration Wizard.");
                                setView("/view/migrate_wallet.fxml");
                            });
                        } else {
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

                                Platform.runLater(() -> Message("Fetching support data..."));

                                tn.finhub.model.SupportModel supportModel = new tn.finhub.model.SupportModel();
                                java.util.List<tn.finhub.model.SupportTicket> tickets = supportModel.getAllTickets();
                                tn.finhub.controller.AdminSupportController.setCachedTickets(tickets);

                                tn.finhub.model.KYCModel kycModel = new tn.finhub.model.KYCModel();
                                java.util.List<tn.finhub.model.KYCRequest> kycRequests = kycModel.findAllRequests();
                                tn.finhub.controller.AdminKYCController.setCachedRequests(kycRequests);

                                tn.finhub.model.KnowledgeBaseModel kbModel = new tn.finhub.model.KnowledgeBaseModel();
                                java.util.List<tn.finhub.model.KnowledgeBase> kbArticles = kbModel.getAllArticles();

                                tn.finhub.controller.AdminEscrowController.prefetchData();
                                tn.finhub.controller.AdminKnowledgeBaseController.setCachedArticles(kbArticles);

                                tn.finhub.model.SystemAlertModel alertModel = new tn.finhub.model.SystemAlertModel();
                                java.util.List<tn.finhub.model.SystemAlert> alerts = alertModel.getAllBroadcasts();
                                tn.finhub.controller.AdminAlertsController.setCachedAlerts(alerts);

                                tn.finhub.model.EscrowManager escrowManager = new tn.finhub.model.EscrowManager();
                                java.util.List<tn.finhub.model.Escrow> escrows = escrowManager.getEscrowsForAdmin();
                                tn.finhub.controller.AdminEscrowController.setCachedEscrows(escrows);

                            } catch (Exception e) {
                                System.err.println("Admin pre-fetch failed: " + e.getMessage());
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

                            try {
                                tn.finhub.model.WalletModel walletModel = new tn.finhub.model.WalletModel();
                                tn.finhub.model.Wallet wallet = walletModel.findByUserId(userId);

                                if (wallet != null) {
                                    tn.finhub.model.VirtualCardModel cardModel = new tn.finhub.model.VirtualCardModel();
                                    tn.finhub.model.MarketModel marketModel = new tn.finhub.model.MarketModel();

                                    java.util.List<tn.finhub.model.VirtualCard> cards = cardModel
                                            .findByWalletId(wallet.getId());
                                    java.util.List<tn.finhub.model.WalletTransaction> transactions = walletModel
                                            .getTransactionHistory(wallet.getId());
                                    int badTxId = "FROZEN".equals(wallet.getStatus())
                                            ? walletModel.getTamperedTransactionId(wallet.getId())
                                            : -1;

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

                                    tn.finhub.model.SavedContactModel contactModel = new tn.finhub.model.SavedContactModel();
                                    java.util.List<tn.finhub.model.SavedContact> contacts = contactModel
                                            .getContactsByUserId(userId);

                                    tn.finhub.model.FinancialProfileModel fpm = new tn.finhub.model.FinancialProfileModel();
                                    tn.finhub.model.FinancialProfile profile = fpm.findByUserId(userId);

                                    tn.finhub.model.SupportModel supportModel = new tn.finhub.model.SupportModel();
                                    java.util.List<tn.finhub.model.SupportTicket> tickets = supportModel
                                            .getTicketsByUserId(userId);

                                    String bestAsset = "N/A";
                                    java.math.BigDecimal maxPnlPercent = java.math.BigDecimal.ZERO;
                                    int assetCount = items.size();

                                    WalletController.WalletDataPacket walletData = new WalletController.WalletDataPacket(
                                            wallet, cards, transactions, badTxId, portValue, totalInvested,
                                            bestAsset, maxPnlPercent, assetCount, new java.util.HashMap<>(),
                                            new java.util.HashMap<>());
                                    WalletController.setCachedData(walletData);

                                    boolean isFrozen = "FROZEN".equals(wallet.getStatus());
                                    TransactionsController.TransactionData txData = new TransactionsController.TransactionData(
                                            userId, transactions, contacts, badTxId, isFrozen,
                                            new java.util.HashMap<>(), new java.util.HashMap<>());
                                    TransactionsController.setCachedData(txData);

                                    FinancialTwinController.setCachedPortfolio(portfolioMap);

                                    if (profile != null) {
                                        ProfileController.setCachedProfile(profile);
                                    }

                                    SupportTicketsController.setCachedTickets(tickets);
                                }
                            } catch (Exception e) {
                                System.out.println("Pre-fetch failed: " + e.getMessage());
                            }

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

    /**
     * Fetches the latest system alert for the given user and shows it as a native
     * desktop notification.
     */
    private void showLatestSystemAlertNotification(int userId) {
        new Thread(() -> {
            try {
                tn.finhub.model.SystemAlertModel alertModel = new tn.finhub.model.SystemAlertModel();
                java.util.List<tn.finhub.model.SystemAlert> alerts = alertModel.getAlertsByUserId(userId);
                if (alerts == null || alerts.isEmpty()) {
                    return;
                }

                tn.finhub.model.SystemAlert latest = alerts.get(0); // list is ordered DESC by created_at
                String title = "FinHub Alert";
                String message = latest.getMessage() != null ? latest.getMessage() : "You have a new system alert.";

                DesktopNotificationUtil.showInfo(title, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}