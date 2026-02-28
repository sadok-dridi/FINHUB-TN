package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.*;
import java.net.*;
import java.util.*;


public class SignupController {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();
    private volatile boolean keepPolling = true;

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         messageLabel;

    // =========================================================
    // SIGNUP NORMAL (formulaire)
    // =========================================================
    @FXML
    public void handleSignup() {
        try {
            // Validation nom
            if (!ValidationUtils.isValidName(nameField.getText())) {
                showError("Invalid name (min 2 chars, letters only)");
                return;
            }
            // Validation email
            if (!ValidationUtils.isValidEmail(emailField.getText())) {
                showError("Invalid email format");
                return;
            }
            // Validation mot de passe
            String password = passwordField.getText();
            String pwdError = ValidationUtils.getPasswordValidationError(password);
            if (pwdError != null) {
                showError(pwdError);
                return;
            }
            // Confirmation mot de passe
            if (!password.equals(confirmPasswordField.getText())) {
                showError("❌ Passwords do not match");
                return;
            }

            String json = """
                    {
                      "full_name": "%s",
                      "email": "%s",
                      "password": "%s",
                      "role": "CLIENT"
                    }
                    """.formatted(nameField.getText(), emailField.getText(), password);

            showInfo("Creating account...");

            new Thread(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(ApiClient.BASE_URL + "/signup"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

                    HttpResponse<String> response = ApiClient.getClient().send(
                            request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() != 200) {
                        String errorMessage = "Signup failed";
                        try {
                            JSONObject errorBody = new JSONObject(response.body());
                            if (errorBody.has("detail"))
                                errorMessage = errorBody.getString("detail");
                        } catch (Exception ignored) {}

                        final String finalMsg;
                        if (response.statusCode() == 409 || errorMessage.toLowerCase().contains("already")) {
                            finalMsg = "Email already exists";
                        } else {
                            finalMsg = "Signup failed: " + response.statusCode();
                        }
                        Platform.runLater(() -> showError("❌ " + finalMsg));
                        return;
                    }

                    JSONObject body = new JSONObject(response.body());
                    String verificationLink = body.getString("verification_link");
                    boolean emailSent = MailClient.sendVerificationEmail(
                            emailField.getText(), verificationLink);

                    Platform.runLater(() -> {
                        if (emailSent) {
                            showSuccess("✅ Account created. Check your email to verify...");
                        } else {
                            showInfo("⚠️ Account created, but verification email wasn't sent.");
                        }
                    });

                    System.out.println("📧 Verification email sent to: " + emailField.getText());
                    System.out.println("🔗 Link: " + verificationLink);

                    // Polling jusqu'à ce que l'utilisateur clique le lien
                    startPollingForVerification(emailField.getText(), password);

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showError("❌ Error sending request"));
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Error validating input");
        }
    }

    // =========================================================
    // GOOGLE SIGNUP — Bouton FXML
    // =========================================================
    @FXML
    public void handleGoogleSignup() {
        showInfo("🌐 Connecting with Google...");
        handleGoogleLogin();
    }

    // =========================================================
    // GOOGLE OAUTH — Lancement via Task JavaFX
    // =========================================================
    private void handleGoogleLogin() {
        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                return performGoogleOAuth();
            }
        };

        task.setOnSucceeded(e -> {
            JSONObject userInfo = task.getValue();
            if (userInfo != null) {
                String email = userInfo.optString("email", "");
                String name  = userInfo.optString("name", "");
                handleGoogleUserInfo(email, name);
            } else {
                showError("❌ Google sign-in cancelled or failed.");
            }
        });

        task.setOnFailed(e ->
                showError("❌ Google error: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    // =========================================================
    // GOOGLE USER INFO — Cœur du flow Google corrigé
    //
    // Flow :
    //   1. Tenter inscription via /signup
    //   2a. Nouveau compte (200) → vérifier auto via /verify-google
    //                            → login direct → Dashboard
    //   2b. Compte existant (409) → login direct via /google-login
    //                             → Dashboard
    //   ✅ Jamais de "vérifie ton email" pour Google
    // =========================================================
    private void handleGoogleUserInfo(String email, String fullName) {
        if (email.isBlank()) {
            Platform.runLater(() -> showError("Unable to retrieve Google email."));
            return;
        }

        final String name     = fullName.isBlank() ? email : fullName;
        final String password = "Gg1!" + UUID.randomUUID(); // password sécurisé généré

        new Thread(() -> {
            try {
                // ── ÉTAPE 1 : Tentative d'inscription ──────────────────────
                String signupJson = """
                        {
                          "full_name": "%s",
                          "email": "%s",
                          "password": "%s",
                          "role": "CLIENT"
                        }
                        """.formatted(name, email, password);

                HttpRequest signupRequest = HttpRequest.newBuilder()
                        .uri(URI.create(ApiClient.BASE_URL + "/signup"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(signupJson))
                        .build();

                HttpResponse<String> signupResponse = ApiClient.getClient().send(
                        signupRequest, HttpResponse.BodyHandlers.ofString());

                // ── CAS A : Nouveau compte créé ────────────────────────────
                if (signupResponse.statusCode() == 200) {
                    Platform.runLater(() -> showInfo("⏳ Finalizing Google login..."));

                    // Vérification automatique (pas besoin d'email pour Google)
                    String verifyJson = """
                            { "email": "%s" }
                            """.formatted(email);

                    HttpRequest verifyRequest = HttpRequest.newBuilder()
                            .uri(URI.create(ApiClient.BASE_URL + "/verify-google"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(verifyJson))
                            .build();

                    HttpResponse<String> verifyResponse = ApiClient.getClient().send(
                            verifyRequest, HttpResponse.BodyHandlers.ofString());

                    if (verifyResponse.statusCode() != 200) {
                        Platform.runLater(() -> showError(
                                "❌ Auto-verification failed: " + verifyResponse.body()));
                        return;
                    }

                    // Login direct avec les credentials générés
                    loginWithCredentials(email, password);
                    return;
                }

                // ── CAS B : Compte Google déjà existant ───────────────────
                if (signupResponse.statusCode() == 409) {
                    Platform.runLater(() -> showInfo("⏳ Signing in to existing account..."));

                    // Login via endpoint dédié Google (pas besoin du password)
                    loginWithGoogleEndpoint(email);
                    return;
                }

                // ── CAS C : Autre erreur ───────────────────────────────────
                String errorMsg = "Google signup failed";
                try {
                    JSONObject errBody = new JSONObject(signupResponse.body());
                    if (errBody.has("detail")) errorMsg = errBody.getString("detail");
                } catch (Exception ignored) {}

                final String finalMsg = errorMsg;
                Platform.runLater(() -> showError("❌ " + finalMsg));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("❌ Error during Google signup"));
            }
        }).start();
    }

    // =========================================================
    // LOGIN avec email + password (nouveau compte Google)
    // =========================================================
    private void loginWithCredentials(String email, String password) throws Exception {
        String json = """
                { "email": "%s", "password": "%s" }
                """.formatted(email, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiClient.BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = ApiClient.getClient().send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject body = new JSONObject(response.body());
            JSONObject user = body.getJSONObject("user");
            String token    = body.getString("access_token");
            handleLoginSuccess(user, token);
        } else {
            Platform.runLater(() -> showError("❌ Auto-login failed. Please try logging in manually."));
        }
    }

    // =========================================================
    // LOGIN via endpoint Google dédié (compte existant)
    // Endpoint backend : POST /google-login { "email": "..." }
    // =========================================================
    private void loginWithGoogleEndpoint(String email) throws Exception {
        String json = """
                { "email": "%s" }
                """.formatted(email);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiClient.BASE_URL + "/google-login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = ApiClient.getClient().send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject body = new JSONObject(response.body());
            JSONObject user = body.getJSONObject("user");
            String token    = body.getString("access_token");
            handleLoginSuccess(user, token);
        } else {
            // Fallback : rediriger vers login manuel
            Platform.runLater(() -> {
                showError("❌ Please log in manually with this email.");
                // Optionnel : naviguer vers login après 2 secondes
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> navigateTo("/view/login.fxml"));
                }).start();
            });
        }
    }

    // =========================================================
    // GOOGLE OAUTH — Authorization Code + localhost callback
    // =========================================================
    private JSONObject performGoogleOAuth() throws Exception {
        String clientId     = DOTENV.get("GOOGLE_CLIENT_ID");
        String clientSecret = DOTENV.get("GOOGLE_CLIENT_SECRET");

        if (clientId == null || clientId.isBlank())
            throw new IllegalStateException("GOOGLE_CLIENT_ID missing in .env");
        if (clientSecret == null || clientSecret.isBlank())
            throw new IllegalStateException("GOOGLE_CLIENT_SECRET missing in .env");

        int    port        = 8085;
        String redirectUri = "http://localhost:" + port + "/callback";
        String state       = UUID.randomUUID().toString();

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id="    + URLEncoder.encode(clientId,               StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri,            StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope="        + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8)
                + "&state="        + URLEncoder.encode(state,                  StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=select_account";

        Platform.runLater(() -> showInfo("🌐 Opening Google browser..."));

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(authUrl));
        } else {
            throw new RuntimeException("Cannot open browser on this device.");
        }

        String code = waitForAuthCode(port, state);
        if (code == null) return null;

        // Échanger le code contre un access_token
        HttpClient http = ApiClient.getClient();
        String tokenBody = "code="          + URLEncoder.encode(code,         StandardCharsets.UTF_8)
                + "&client_id="     + URLEncoder.encode(clientId,     StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&redirect_uri="  + URLEncoder.encode(redirectUri,  StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                .build();

        HttpResponse<String> tokenResponse = http.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        JSONObject tokenJson = new JSONObject(tokenResponse.body());

        if (!tokenJson.has("access_token")) {
            throw new RuntimeException("Token refused: " + tokenResponse.body());
        }

        // Récupérer le profil Google
        String accessToken = tokenJson.getString("access_token");

        HttpRequest userInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://openidconnect.googleapis.com/v1/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> userInfoResponse = http.send(userInfoRequest, HttpResponse.BodyHandlers.ofString());

        if (userInfoResponse.statusCode() != 200) {
            throw new RuntimeException("Profile fetch failed: " + userInfoResponse.body());
        }

        return new JSONObject(userInfoResponse.body());
    }

    // =========================================================
    // SERVEUR LOCALHOST — Capture du code OAuth
    // =========================================================
    private String waitForAuthCode(int port, String expectedState) throws Exception {
        Platform.runLater(() -> showInfo("⏳ Waiting for Google authentication..."));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(120_000);

            try (Socket socket = serverSocket.accept()) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                String requestLine = reader.readLine();

                String htmlResponse =
                        "<html><body style='font-family:sans-serif;text-align:center;margin-top:80px'>"
                                + "<h2 style='color:#7c3aed'>✅ Authentication successful!</h2>"
                                + "<p style='color:#555'>You can close this tab and return to FinHub.</p>"
                                + "</body></html>";

                String httpResponse = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/html; charset=UTF-8\r\n"
                        + "Connection: close\r\n\r\n"
                        + htmlResponse;

                socket.getOutputStream().write(httpResponse.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();

                if (requestLine == null || !requestLine.contains("?")) return null;

                String queryString = requestLine.split(" ")[1];
                if (queryString.contains("error=")) return null;

                String paramsPart = queryString.split("\\?")[1];
                Map<String, String> params = new HashMap<>();
                for (String param : paramsPart.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                    }
                }

                if (!expectedState.equals(params.get("state"))) {
                    throw new SecurityException("Invalid OAuth state — possible CSRF attack");
                }

                return params.get("code");
            }

        } catch (java.net.SocketTimeoutException e) {
            Platform.runLater(() -> showError("❌ Timeout: Google authentication expired."));
            return null;
        }
    }

    // =========================================================
    // POLLING — Pour signup normal (email/password) uniquement
    // =========================================================
    private void startPollingForVerification(String email, String password) {
        keepPolling = true;
        Thread pollingThread = new Thread(() -> {
            int attempts = 0;
            while (keepPolling && attempts < 100) {
                try {
                    Thread.sleep(3000);
                    attempts++;
                    if (!keepPolling) break;

                    System.out.println("[DEBUG] Polling attempt " + attempts);

                    String json = """
                            { "email": "%s", "password": "%s" }
                            """.formatted(email, password);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(ApiClient.BASE_URL + "/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

                    HttpResponse<String> response = ApiClient.getClient().send(
                            request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        System.out.println("[DEBUG] Email verified! Logging in.");
                        keepPolling = false;
                        JSONObject body = new JSONObject(response.body());
                        JSONObject user = body.getJSONObject("user");
                        String token    = body.getString("access_token");
                        handleLoginSuccess(user, token);
                        break;
                    } else if (response.statusCode() == 403) {
                        continue; // pas encore vérifié, on réessaie
                    } else {
                        System.out.println("[DEBUG] Polling stopped: " + response.statusCode());
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

    // =========================================================
    // LOGIN SUCCESS — Session + Redirection dashboard
    // =========================================================
    private void handleLoginSuccess(JSONObject user, String token) {
        try {
            System.out.println("[DEBUG] Login successful, initializing session...");

            SessionManager.login(
                    user.getInt("id"),
                    user.optString("full_name", ""),
                    user.getString("email"),
                    user.getString("role"),
                    token);

            syncUserLocal(
                    user.getInt("id"),
                    user.optString("full_name", ""),
                    user.getString("email"),
                    user.getString("role"));

            Platform.runLater(() -> showSuccess("✅ Logged in! Redirecting..."));

            int userId = user.getInt("id");
            tn.finhub.model.FinancialProfileModel profileModel = new tn.finhub.model.FinancialProfileModel();
            profileModel.ensureProfile(userId);
            boolean completed = profileModel.isProfileCompleted(userId);

            if (!completed) {
                // Profil financier incomplet → compléter d'abord
                Platform.runLater(() -> navigateTo("/view/complete_profile.fxml"));
            } else {
                // Pré-chargement du wallet en arrière-plan
                try {
                    tn.finhub.model.WalletModel walletModel = new tn.finhub.model.WalletModel();
                    tn.finhub.model.Wallet wallet = walletModel.findByUserId(userId);
                    if (wallet != null) {
                        tn.finhub.model.VirtualCardModel cardModel = new tn.finhub.model.VirtualCardModel();
                        tn.finhub.model.MarketModel marketModel    = new tn.finhub.model.MarketModel();
                        var cards        = cardModel.findByWalletId(wallet.getId());
                        var transactions = walletModel.getTransactionHistory(wallet.getId());
                        var items        = marketModel.getPortfolio(userId);

                        java.math.BigDecimal totalInvested = java.math.BigDecimal.ZERO;
                        for (var item : items) {
                            totalInvested = totalInvested.add(
                                    item.getAverageCost().multiply(item.getQuantity()));
                        }

                        WalletController.WalletDataPacket packet = new WalletController.WalletDataPacket(
                                wallet, cards, transactions, -1,
                                java.math.BigDecimal.ZERO, totalInvested, "N/A",
                                java.math.BigDecimal.ZERO, items.size(),
                                new java.util.HashMap<>(), new java.util.HashMap<>());
                        WalletController.setCachedData(packet);
                    }
                } catch (Exception e) {
                    System.out.println("Pre-fetch failed: " + e.getMessage());
                }
                Platform.runLater(() -> navigateTo("/view/user_dashboard.fxml"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showError("❌ Error during login"));
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================
    @FXML
    public void goToLogin() {
        keepPolling = false;
        navigateTo("/view/login.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setResources(LanguageManager.getInstance().getResourceBundle());
            javafx.scene.Parent newView = loader.load();

            javafx.scene.layout.StackPane contentArea =
                    (javafx.scene.layout.StackPane) nameField.getScene().lookup("#contentArea");

            if (!contentArea.getChildren().isEmpty()) {
                javafx.scene.Node current = contentArea.getChildren().get(0);
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(300), current);
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
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(300), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    // =========================================================
    // HELPERS UI
    // =========================================================
    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: green;");
        messageLabel.setText(msg);
    }

    private void showInfo(String msg) {
        messageLabel.setStyle("-fx-text-fill: orange;");
        messageLabel.setText(msg);
    }

    // =========================================================
    // DB — Sync local
    // =========================================================
    private void syncUserLocal(int id, String fullName, String email, String role) throws Exception {
        Connection conn = DBConnection.getInstance();
        PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users_local (user_id, full_name, email, role)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  full_name = VALUES(full_name),
                  email     = VALUES(email),
                  role      = VALUES(role)
                """);
        ps.setInt(1, id);
        ps.setString(2, fullName);
        ps.setString(3, email);
        ps.setString(4, role);
        ps.executeUpdate();
    }
}