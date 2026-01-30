package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class OtpController {

    @FXML
    private TextField otpField;

    @FXML
    private Label errorLabel;

    private String expectedOtp;
    private Runnable onSuccessCallback;
    private Runnable onCancelCallback;

    public void setExpectedOtp(String otp) {
        this.expectedOtp = otp;
    }

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    public void setOnCancelCallback(Runnable callback) {
        this.onCancelCallback = callback;
    }

    @FXML
    public void initialize() {
        // Enforce numeric only and max 6 chars
        otpField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;

            if (!newVal.matches("\\d*")) {
                otpField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (otpField.getText().length() > 6) {
                otpField.setText(otpField.getText().substring(0, 6));
            }

            // Auto-hide error on typing
            if (errorLabel.isVisible()) {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            }
        });
    }

    @FXML
    private void handleVerify() {
        String input = otpField.getText();

        if (input == null || input.isEmpty()) {
            showError("Please enter the code.");
            return;
        }

        if (input.equals(expectedOtp)) {
            close();
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
        } else {
            showError("Incorrect code. Please try again.");
        }
    }

    @FXML
    private void handleCancel() {
        close();
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        // Shake animation could go here
    }

    private void close() {
        Stage stage = (Stage) otpField.getScene().getWindow();
        stage.close();
    }
}
