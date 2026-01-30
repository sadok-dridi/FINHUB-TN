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
import tn.finhub.service.FinancialProfileService;

import java.net.http.HttpRequest;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

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
                alert.setTitle("Error Loading View");
                alert.setHeaderText("Could not load " + fxmlPath);
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
        messageLabel.setText("Logging in...");

        new Thread(() -> {
            try {
                System.out.println("[DEBUG] Sending login request...");
                String json = """
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(
                        emailField.getText(),
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
                    Platform.runLater(() -> messageLabel.setText(" Login failed"));
                    return;
                }

                JSONObject body = new JSONObject(response.body());
                JSONObject user = body.getJSONObject("user");

                System.out.println("[DEBUG] Login successful, initializing session...");
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
}
