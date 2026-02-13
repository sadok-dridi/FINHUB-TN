package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import tn.finhub.util.ApiClient;
import tn.finhub.util.MailClient;
import java.io.IOException;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;
    @FXML
    private Button sendBtn;
    @FXML
    private Label messageLabel;

    @FXML
    public void handleSendResetLink() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            messageLabel.setText("Please enter your email address.");
            return;
        }

        sendBtn.setDisable(true);
        sendBtn.setText("Sending...");
        messageLabel.setText("");

        new Thread(() -> {
            try {
                // 1. Request Reset Link from API
                String resetLink = ApiClient.sendForgotPasswordRequest(email);

                // 2. Send Email
                MailClient.sendResetPasswordEmail(email, resetLink);

                Platform.runLater(() -> {
                    messageLabel.setStyle("-fx-text-fill: -color-success;");
                    messageLabel.setText("Reset link sent! Check your email.");
                    sendBtn.setDisable(false);
                    sendBtn.setText("Send Reset Link");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    messageLabel.setStyle("-fx-text-fill: -color-danger;");
                    messageLabel.setText("Error: " + e.getMessage());
                    sendBtn.setDisable(false);
                    sendBtn.setText("Send Reset Link");
                });
            }
        }).start();
    }

    @FXML
    public void handleBackToLogin() {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/view/login.fxml"));

            // Find the content area to preserve the main layout/background
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) emailField.getScene()
                    .lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(loginView);
            } else {
                // Fallback only if contentArea is missing
                emailField.getScene().setRoot(loginView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
