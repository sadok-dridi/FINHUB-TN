package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import tn.finhub.util.ApiClient;
import java.net.http.*;
import java.net.URI;
import tn.finhub.service.MailService;
import org.json.JSONObject;

public class SignupController {

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

    @FXML
    public void handleGoogleSignup() {
        messageLabel.setText("✅ Google account created (simulated)");
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
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
}
