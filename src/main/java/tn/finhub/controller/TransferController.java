package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.finhub.model.WalletModel; // Added import

import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;

import java.math.BigDecimal;

public class TransferController {

    @FXML
    private TextField recipientField;
    @FXML
    private TextField amountField;
    @FXML
    private javafx.scene.control.Button sendButton;
    @FXML
    private javafx.scene.control.Button cancelButton;

    private final WalletModel walletModel = new WalletModel(); // Changed to WalletModel
    private Runnable onSuccessCallback;

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    public void setRecipient(tn.finhub.model.SavedContact contact) {
        // Pre-fill the recipient field
        recipientField.setText(contact.getContactEmail());
        // Focus on amount field
        javafx.application.Platform.runLater(() -> amountField.requestFocus());
    }

    @FXML
    public void initialize() {
        // No contacts to load
    }

    @FXML
    private void handleTransfer() {
        try {
            final String email = recipientField.getText().trim();
            String amountText = amountField.getText();

            // Validation
            if (email.isEmpty() || amountText.isEmpty()) {
                DialogUtil.showError("Validation Error", "Please provide a recipient and amount.");
                return;
            }

            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                DialogUtil.showError("Invalid Amount", "Amount must be greater than 0.");
                return;
            }

            int currentUserId = UserSession.getInstance().getUser().getId();

            // Check Frozen Status
            tn.finhub.model.Wallet wallet = walletModel.findByUserId(currentUserId);
            if (wallet != null && "FROZEN".equals(wallet.getStatus())) {
                DialogUtil.showError("Security Alert", "Wallet is FROZEN. Transfers are disabled.");
                return;
            }

            if (email.equalsIgnoreCase(UserSession.getInstance().getUser().getEmail())) {
                DialogUtil.showError("Error", "You cannot transfer money to yourself.");
                return;
            }

            // UI Feedback
            sendButton.setDisable(true);
            cancelButton.setDisable(true);
            sendButton.setText("Sending Security Code...");

            // 1. Generate OTP using an external API (Random.org)
            String otp;
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(
                                "https://www.random.org/integers/?num=1&min=100000&max=999999&col=1&base=10&format=plain&rnd=new"))
                        .timeout(java.time.Duration.ofSeconds(3)) // 3 second timeout so the UI doesn't hang if the API
                                                                  // is slow
                        .build();
                java.net.http.HttpResponse<String> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 && response.body().trim().length() == 6) {
                    otp = response.body().trim();
                } else {
                    throw new RuntimeException("API Response invalid");
                }
            } catch (Exception e) {
                System.out.println("Failed to fetch OTP from API, using secure fallback: " + e.getMessage());
                // Secure random fallback if the API is down, never use a hardcoded default.
                otp = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
            }

            // Create a final copy so lambda can use it
            final String finalOtp = otp;

            // 2. Send Email (Background)
            javafx.concurrent.Task<Void> emailTask = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    tn.finhub.model.User user = UserSession.getInstance().getUser();
                    tn.finhub.util.MailClient.sendOtpEmail(user.getEmail(), finalOtp);
                    return null;
                }
            };

            emailTask.setOnSucceeded(event -> {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                            getClass().getResource("/view/otp_dialog.fxml"));
                    loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
                    javafx.scene.Parent root = loader.load();

                    tn.finhub.controller.OtpController otpController = loader.getController();
                    otpController.setExpectedOtp(finalOtp);

                    otpController.setOnSuccessCallback(() -> {
                        proceedWithTransfer(currentUserId, email, amount);
                    });

                    otpController.setOnCancelCallback(this::resetUI);

                    Stage otpStage = new Stage();
                    otpStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
                    otpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

                    javafx.scene.Scene scene = new javafx.scene.Scene(root);
                    scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                    otpStage.setScene(scene);
                    otpStage.showAndWait();

                } catch (java.io.IOException ex) {
                    ex.printStackTrace();
                    resetUI();
                }
            });

            emailTask.setOnFailed(event -> {
                Throwable error = emailTask.getException();
                error.printStackTrace();
                DialogUtil.showError("Security Error", "Could not send verification code: " + error.getMessage());
                resetUI();
            });

            new Thread(emailTask).start();

        } catch (NumberFormatException e) {
            DialogUtil.showError("Invalid Input", "Please enter a valid amount.");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Transfer failed: " + e.getMessage());
        }
    }

    private void proceedWithTransfer(int currentUserId, String email, BigDecimal amount) {
        sendButton.setText("Sending...");

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                // Ensure helper method exists in walletModel or use findByUserId
                int senderWalletId = walletModel.findByUserId(currentUserId).getId();
                walletModel.transferByEmail(senderWalletId, email, amount);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            handleCancel();
            javafx.application.Platform.runLater(() -> {
                DialogUtil.showInfo("Success", "Money sent successfully to " + email);

                // Trigger Webhook Notification
                tn.finhub.model.User currentUser = UserSession.getInstance().getUser();
                String senderName = currentUser != null && currentUser.getFullName() != null
                        ? currentUser.getFullName()
                        : currentUser.getEmail();
                String senderEmail = currentUser != null ? currentUser.getEmail() : "Unknown";
                tn.finhub.util.WebhookUtil.sendTransferNotification(senderName, senderEmail, email, amount.toString());

                if (onSuccessCallback != null) {
                    onSuccessCallback.run();
                }
            });
        });

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            error.printStackTrace();
            DialogUtil.showError("Transfer Failed", error.getMessage());
            resetUI();
        });

        new Thread(task).start();
    }

    private void resetUI() {
        sendButton.setDisable(false);
        cancelButton.setDisable(false);
        sendButton.setText("Send Money");
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }
}
