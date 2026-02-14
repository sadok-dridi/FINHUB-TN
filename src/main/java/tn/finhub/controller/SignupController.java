package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
<<<<<<< HEAD
import tn.finhub.util.ApiClient;
import java.net.http.*;
import java.net.URI;
import tn.finhub.service.MailService;
import org.json.JSONObject;

public class SignupController {

=======
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

public class SignupController {

    private volatile boolean keepPolling = true;

>>>>>>> cd680ce (crud+controle de saisie)
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
<<<<<<< HEAD
=======
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

>>>>>>> cd680ce (crud+controle de saisie)
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
<<<<<<< HEAD
                    passwordField.getText(),
                    role);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ApiClient.BASE_URL + "/signup"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = ApiClient.getClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                if (response.statusCode() == 409) {
                    messageLabel.setText("❌ Email already exists");
                } else {
                    messageLabel.setText("❌ Signup failed: " + response.statusCode());
                }
                return;
            }

            JSONObject body = new JSONObject(response.body());

            String verificationLink = body.getString("verification_link");

            // 🚨 EMAIL SENT FROM JAVA (YES, WE KNOW)
            MailService.sendVerificationEmail(
                    emailField.getText(),
                    verificationLink);

            messageLabel.setText(
                    "✅ Account created. Verification email sent.");
            System.out.println("📧 Sending verification email to: " + emailField.getText());
            System.out.println("🔗 Link: " + verificationLink);

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("❌ Error sending email");
        }
    }

=======
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

                    // 🚨 EMAIL SENT FROM JAVA (YES, WE KNOW)
                    MailClient.sendVerificationEmail(
                            emailField.getText(),
                            verificationLink);

                    Platform.runLater(() -> {
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("✅ Account created. Verify email to login...");
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

>>>>>>> cd680ce (crud+controle de saisie)
    @FXML
    public void handleGoogleSignup() {
        messageLabel.setText("✅ Google account created (simulated)");
    }

    @FXML
    public void goToLogin() {
<<<<<<< HEAD
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
=======
        keepPolling = false; // Stop auto-login polling
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            loader.setResources(LanguageManager.getInstance().getResourceBundle());
>>>>>>> cd680ce (crud+controle de saisie)
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
<<<<<<< HEAD
=======

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
                                java.math.BigDecimal.ZERO, items.size(), new java.util.HashMap<>());
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
>>>>>>> cd680ce (crud+controle de saisie)
}
