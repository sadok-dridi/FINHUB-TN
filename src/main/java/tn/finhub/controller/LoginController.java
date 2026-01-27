package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import tn.finhub.util.*;
import java.net.http.*;
import java.net.URI;
import org.json.JSONObject;
import java.sql.*;

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
    public void handleLogin() {
        try {
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

            HttpResponse<String> response = ApiClient.getClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                messageLabel.setText(" Login failed");
                return;
            }

            JSONObject body = new JSONObject(response.body());

            JSONObject user = body.getJSONObject("user");

            SessionManager.login(
                    user.getInt("id"),
                    user.optString("full_name", ""), // Use optString for safety
                    user.getString("email"),
                    user.getString("role"),
                    body.getString("access_token"));

            syncUserLocal(
                    user.getInt("id"),
                    user.optString("full_name", ""),
                    user.getString("email"),
                    user.getString("role")); // Navigate to Dashboard
            String dashboardView = SessionManager.isAdmin() ? "/view/admin_users.fxml" : "/view/user_dashboard.fxml";
            setView(dashboardView);

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText(" Server error");
        }
    }
}
