package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.application.Platform;
import tn.finhub.util.ApiClient;
import java.net.http.*;
import java.net.URI;
import tn.finhub.util.MailClient;
import tn.finhub.util.ValidationUtils;
import tn.finhub.util.LanguageManager;
import org.json.JSONObject;

import tn.finhub.util.SessionManager;
import tn.finhub.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.awt.Desktop;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import io.github.cdimascio.dotenv.Dotenv;

public class SignupController {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private volatile boolean keepPolling = true;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField nameField;

    @FXML
    private Label messageLabel;

    @FXML
    public void handleSignup() {
        try {
            // Input Validation
            if (!ValidationUtils.isValidName(nameField.getText())) {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("Invalid name (min 2 chars, letters only)");
                return;
            }

            if (!ValidationUtils.isValidEmail(emailField.getText())) {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("Invalid email format");
                return;
            }

            String password = passwordField.getText();
            String pwdError = ValidationUtils.getPasswordValidationError(password);
            if (pwdError != null) {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText(pwdError);
                return;
            }

            String role = "CLIENT";

            String json = """
                    {
                      "full_name": "%s",
                      "email": "%s",
                      "password": "%s",
                      "role": "%s"
                    }
                    """.formatted(
                    nameField.getText(),
                    emailField.getText(),
                    password,
                    role);

            messageLabel.setStyle("-fx-text-fill: orange;");
            messageLabel.setText("Creating account...");

            // Run network operations in background thread
            new Thread(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(ApiClient.BASE_URL + "/signup"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

                    HttpResponse<String> response = ApiClient.getClient().send(request,
                            HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() != 200) {
                        // Try to parse error details from JSON body
                        String errorMessage = "Signup failed"; // Default
                        try {
                            JSONObject errorBody = new JSONObject(response.body());
                            if (errorBody.has("detail")) {
                                errorMessage = errorBody.getString("detail");
                            }
                        } catch (Exception e) {
                            // Fallback if body is not JSON or empty
                        }

                        // Handle specific status codes
                        final String finalErrorMessage;
                        if (response.statusCode() == 409) {
                            finalErrorMessage = "Email already exists";
                        } else if (response.statusCode() == 400) {
                            // 400 Bad Request could be duplicate email based on user's API
                            if (errorMessage.toLowerCase().contains("already")) {
                                finalErrorMessage = "Email already exists";
                            } else {
                                finalErrorMessage = errorMessage;
                            }
                        } else {
                            finalErrorMessage = "Signup failed: " + response.statusCode();
                        }

                        Platform.runLater(() -> {
                            messageLabel.setStyle("-fx-text-fill: red;");
                            messageLabel.setText("❌ " + finalErrorMessage);
                        });
                        return;
                    }

                    JSONObject body = new JSONObject(response.body());
                    String verificationLink = body.getString("verification_link");

                    boolean emailSent = MailClient.sendVerificationEmail(
                            emailField.getText(),
                            verificationLink);

                    Platform.runLater(() -> {
                        if (emailSent) {
                            messageLabel.setStyle("-fx-text-fill: green;");
                            messageLabel.setText("✅ Account created. Verify email to login...");
                        } else {
                            messageLabel.setStyle("-fx-text-fill: orange;");
                            messageLabel.setText("⚠️ Account created, but verification email wasn't sent. Use the link in console.");
                        }
                    });

                    System.out.println("📧 Sending verification email to: " + emailField.getText());
                    System.out.println("🔗 Link: " + verificationLink);

                    // Start polling for auto-login
                    startPollingForVerification(emailField.getText(), password);

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> messageLabel.setText("❌ Error sending request"));
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("❌ Error validating input");
        }
    }

    private void startPollingForVerification(String email, String password) {
        keepPolling = true;
        Thread pollingThread = new Thread(() -> {
            int attempts = 0;
            // Poll for 5 minutes (300 seconds / 3 seconds = 100 attempts)
            while (keepPolling && attempts < 100) {
                try {
                    Thread.sleep(3000); // Wait 3 seconds
                    attempts++;

                    if (!keepPolling)
                        break; // Check again after sleep

                    System.out.println("[DEBUG] Polling for verification... Attempt " + attempts);

                    String json = """
                            {
                              "email": "%s",
                              "password": "%s"
                            }
                            """.formatted(email, password);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(ApiClient.BASE_URL + "/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

                    HttpResponse<String> response = ApiClient.getClient().send(request,
                            HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        System.out.println("[DEBUG] Verification confirmed! Logging in.");
                        keepPolling = false;
                        JSONObject body = new JSONObject(response.body());
                        JSONObject user = body.getJSONObject("user");
                        String token = body.getString("access_token");
                        handleLoginSuccess(user, token);
                        break;
                    } else if (response.statusCode() == 403) {
                        // Still not verified, continue polling
                        continue;
                    } else {
                        // Other error (e.g. 401 if password somehow changed, or 500), stop polling
                        System.out.println("[DEBUG] Polling stopped due to error: " + response.statusCode());
                        keepPolling = false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    keepPolling = false;
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    @FXML
    public void handleGoogleSignup() {
        messageLabel.setStyle("-fx-text-fill: orange;");
        messageLabel.setText("Connecting with Google...");

        new Thread(() -> {
            try {
                JSONObject googleUser = performGoogleDeviceFlow();
                if (googleUser == null) {
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText("Google sign-in cancelled or failed.");
                    });
                    return;
                }

                String email = googleUser.optString("email", "");
                String fullName = googleUser.optString("name", "");

                if (email.isBlank()) {
                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText("Unable to retrieve Google email.");
                    });
                    return;
                }

                if (fullName.isBlank()) {
                    fullName = email;
                }

                // Must satisfy backend password rules (upper/lower/digit/special, min length)
                String generatedPassword = "Gg1!" + UUID.randomUUID();

                String json = """
                        {
                          "full_name": "%s",
                          "email": "%s",
                          "password": "%s",
                          "role": "CLIENT"
                        }
                        """.formatted(
                        fullName,
                        email,
                        generatedPassword);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ApiClient.BASE_URL + "/signup"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = ApiClient.getClient().send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    String errorMessage = "Signup failed";
                    try {
                        JSONObject errorBody = new JSONObject(response.body());
                        if (errorBody.has("detail")) {
                            errorMessage = errorBody.getString("detail");
                        }
                    } catch (Exception ignored) {
                    }

                    final String finalErrorMessage;
                    if (response.statusCode() == 409 || errorMessage.toLowerCase().contains("already")) {
                        finalErrorMessage = "An account with this Google email already exists. Please log in instead.";
                    } else {
                        finalErrorMessage = "Google signup failed: " + errorMessage;
                    }

                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText("❌ " + finalErrorMessage);
                    });
                    return;
                }

                JSONObject body = new JSONObject(response.body());
                String verificationLink = body.getString("verification_link");

                boolean emailSent = MailClient.sendVerificationEmail(
                        email,
                        verificationLink);

                Platform.runLater(() -> {
                    if (emailSent) {
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("✅ Google account created. Verify email to login...");
                    } else {
                        messageLabel.setStyle("-fx-text-fill: orange;");
                        messageLabel.setText("⚠️ Google account created, but verification email wasn't sent. Use the link in console.");
                    }
                });

                System.out.println("📧 Sending verification email to: " + email);
                System.out.println("🔗 Link: " + verificationLink);

                startPollingForVerification(email, generatedPassword);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    messageLabel.setStyle("-fx-text-fill: red;");
                    messageLabel.setText("❌ Error during Google signup");
                });
            }
        }).start();
    }

    @FXML
    public void goToLogin() {
        keepPolling = false; // Stop auto-login polling
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            loader.setResources(LanguageManager.getInstance().getResourceBundle());
            javafx.scene.Parent newView = loader.load();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameField.getScene()
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

    private void handleLoginSuccess(JSONObject user, String token) {
        try {
            System.out.println("[DEBUG] Auto-login successful, initializing session...");

            SessionManager.login(
                    user.getInt("id"),
                    user.optString("full_name", ""),
                    user.getString("email"),
                    user.getString("role"),
                    token);

            System.out.println("[DEBUG] Syncing local DB...");
            syncUserLocal(
                    user.getInt("id"),
                    user.optString("full_name", ""),
                    user.getString("email"),
                    user.getString("role"));

            Platform.runLater(() -> {
                messageLabel.setStyle("-fx-text-fill: green;");
                messageLabel.setText("✅ Verified! Logging in...");
            });

            // Pre-fetch logic (simplified version of LoginController to avoid code
            // duplication hell)
            // We'll rely on Dashboard's own init if data is missing, or minimal pre-fetch
            // here.
            // Actually, for best UX, we should fetch core data.
            int userId = user.getInt("id");
            tn.finhub.model.FinancialProfileModel profileModel = new tn.finhub.model.FinancialProfileModel();

            // Background thread is already running this, so we can do blocking calls here
            profileModel.ensureProfile(userId);
            boolean completed = profileModel.isProfileCompleted(userId);

            if (!completed) {
                Platform.runLater(() -> {
                    try {
                        System.out.println("Redirecting to Complete Profile.");
                        // Use the existing goToLogin's logic but for complete profile
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/complete_profile.fxml"));
                        loader.setResources(LanguageManager.getInstance().getResourceBundle());
                        javafx.scene.Parent newView = loader.load();
                        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameField.getScene()
                                .lookup("#contentArea");
                        contentArea.getChildren().setAll(newView);
                        fadeIn(newView);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                // Fetch Wallet and other data similar to LoginController
                try {
                    tn.finhub.model.WalletModel walletModel = new tn.finhub.model.WalletModel();
                    tn.finhub.model.Wallet wallet = walletModel.findByUserId(userId);
                    if (wallet != null) {
                        tn.finhub.model.VirtualCardModel cardModel = new tn.finhub.model.VirtualCardModel();
                        tn.finhub.model.MarketModel marketModel = new tn.finhub.model.MarketModel();

                        var cards = cardModel.findByWalletId(wallet.getId());
                        var transactions = walletModel.getTransactionHistory(wallet.getId());
                        var items = marketModel.getPortfolio(userId);

                        // Populate Wallet Cache
                        java.math.BigDecimal totalInvested = java.math.BigDecimal.ZERO;
                        for (var item : items) {
                            totalInvested = totalInvested.add(item.getAverageCost().multiply(item.getQuantity()));
                        }

                        tn.finhub.controller.WalletController.WalletDataPacket packet = new tn.finhub.controller.WalletController.WalletDataPacket(
                                wallet, cards, transactions, -1, java.math.BigDecimal.ZERO, totalInvested, "N/A",
                                java.math.BigDecimal.ZERO, items.size(), new java.util.HashMap<>(),
                                new java.util.HashMap<>());
                        WalletController.setCachedData(packet);
                    }
                } catch (Exception e) {
                    System.out.println("Pre-fetch failed: " + e.getMessage());
                }

                Platform.runLater(() -> {
                    try {
                        System.out.println("Redirecting to User Dashboard.");
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/user_dashboard.fxml"));
                        loader.setResources(LanguageManager.getInstance().getResourceBundle());
                        javafx.scene.Parent newView = loader.load();
                        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameField.getScene()
                                .lookup("#contentArea");
                        contentArea.getChildren().setAll(newView);
                        fadeIn(newView);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> messageLabel.setText("❌ Error during auto-login"));
        }
    }

    /**
     * Performs Google OAuth 2.0 Device Authorization Grant to obtain the user's
     * basic profile (email, name).
     * <p>
     * Requires valid Google OAuth credentials in the .env file:
     * GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.
     */
    private JSONObject performGoogleDeviceFlow() throws Exception {
        String clientId = DOTENV.get("GOOGLE_CLIENT_ID");
        String clientSecret = DOTENV.get("GOOGLE_CLIENT_SECRET");
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID not configured in .env");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_SECRET not configured in .env");
        }

        HttpClient client = ApiClient.getClient();

        String deviceRequestBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8);

        HttpRequest deviceRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/device/code"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(deviceRequestBody))
                .build();

        HttpResponse<String> deviceResponse = client.send(deviceRequest, HttpResponse.BodyHandlers.ofString());

        if (deviceResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to start Google device flow: " + deviceResponse.body());
        }

        JSONObject deviceJson = new JSONObject(deviceResponse.body());
        String deviceCode = deviceJson.getString("device_code");
        String userCode = deviceJson.getString("user_code");
        String verificationUrl = deviceJson.optString("verification_url",
                deviceJson.optString("verification_uri", "https://www.google.com/device"));
        int interval = deviceJson.optInt("interval", 5);

        Platform.runLater(() -> {
            messageLabel.setStyle("-fx-text-fill: orange;");
            messageLabel.setText("Enter code " + userCode + " at " + verificationUrl);
        });

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(verificationUrl));
            } catch (Exception ignored) {
            }
        }

        long startTime = System.currentTimeMillis();
        long expiresInSeconds = deviceJson.optLong("expires_in", 900);

        while ((System.currentTimeMillis() - startTime) / 1000 < expiresInSeconds) {
            Thread.sleep(interval * 1000L);

            String tokenRequestBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&device_code=" + URLEncoder.encode(deviceCode, StandardCharsets.UTF_8)
                    + "&grant_type="
                    + URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", StandardCharsets.UTF_8);

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody))
                    .build();

            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            JSONObject tokenJson = new JSONObject(tokenResponse.body());

            if (tokenResponse.statusCode() == 200 && tokenJson.has("access_token")) {
                String accessToken = tokenJson.getString("access_token");

                HttpRequest userInfoRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://openidconnect.googleapis.com/v1/userinfo"))
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .build();

                HttpResponse<String> userInfoResponse = client.send(userInfoRequest,
                        HttpResponse.BodyHandlers.ofString());

                if (userInfoResponse.statusCode() != 200) {
                    throw new RuntimeException("Failed to fetch Google user info: " + userInfoResponse.body());
                }

                return new JSONObject(userInfoResponse.body());
            }

            String error = tokenJson.optString("error", "");
            if ("authorization_pending".equals(error)) {
                continue;
            }
            if ("slow_down".equals(error)) {
                interval += 5;
                continue;
            }
            if ("access_denied".equals(error) || "expired_token".equals(error)) {
                return null;
            }

            throw new RuntimeException("Google token error: " + tokenResponse.body());
        }

        return null;
    }
}
