package tn.finhub.controller;

import javafx.fxml.FXML;
<<<<<<< HEAD
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
=======
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
>>>>>>> cd680ce (crud+controle de saisie)

public class OtpController {

    @FXML
    private TextField otpField;

    @FXML
    private Label errorLabel;

<<<<<<< HEAD
=======
    @FXML
    private Button verifyButton;

    @FXML
    private Label timerLabel;

    private Timeline timeline;
    private int timeSeconds = 60;
    private int attempts = 0;
    private static final int MAX_ATTEMPTS = 3;

>>>>>>> cd680ce (crud+controle de saisie)
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
<<<<<<< HEAD
=======
        startTimer();
    }

    private void startTimer() {
        if (timeline != null) {
            timeline.stop();
        }
        timeSeconds = 60;
        timerLabel.setText("01:00");
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1), event -> {
                    timeSeconds--;
                    // Update label
                    int minutes = timeSeconds / 60;
                    int seconds = timeSeconds % 60;
                    timerLabel.setText(String.format("%02d:%02d", minutes, seconds));

                    if (timeSeconds <= 0) {
                        timeline.stop();
                        timerLabel.setText("00:00");
                        otpField.setDisable(true);
                        verifyButton.setDisable(true);
                        showError("Time Expired. Transaction Cancelled.");
                        if (onCancelCallback != null) {
                            onCancelCallback.run();
                        }
                    }
                }));
        timeline.playFromStart();
>>>>>>> cd680ce (crud+controle de saisie)
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
<<<<<<< HEAD
            showError("Incorrect code. Please try again.");
=======
            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                if (timeline != null)
                    timeline.stop();
                otpField.setDisable(true);
                verifyButton.setDisable(true);
                showError("Max attempts reached. Transaction Cancelled.");
                if (onCancelCallback != null) {
                    onCancelCallback.run();
                }
            } else {
                showError("Incorrect code. " + (MAX_ATTEMPTS - attempts) + " attempts remaining.");
            }
>>>>>>> cd680ce (crud+controle de saisie)
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
<<<<<<< HEAD
        Stage stage = (Stage) otpField.getScene().getWindow();
        stage.close();
=======
        Stage stage = (otpField.getScene() != null) ? (Stage) otpField.getScene().getWindow() : null;
        if (stage != null)
            stage.close();
>>>>>>> cd680ce (crud+controle de saisie)
    }
}
