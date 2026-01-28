package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.finhub.service.WalletService;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;

import java.math.BigDecimal;

public class DepositController {

    @FXML
    private TextField amountField;
    @FXML
    private TextField cardNumberField;
    @FXML
    private TextField expiryField;
    @FXML
    private TextField cvvField;

    @FXML
    private javafx.scene.control.Button payButton;
    @FXML
    private javafx.scene.control.Button cancelButton;

    private final WalletService walletService = new WalletService();
    private Runnable onSuccessCallback;

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    private void handleDeposit() {
        try {
            String amountText = amountField.getText();
            String cardNumber = cardNumberField.getText();
            String expiry = expiryField.getText();
            String cvv = cvvField.getText();

            // Basic Validation
            if (amountText.isEmpty() || cardNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
                DialogUtil.showError("Validation Error", "Please fill in all fields.");
                return;
            }

            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                DialogUtil.showError("Invalid Amount", "Amount must be greater than 0.");
                return;
            }

            // UI Feedback: Processing State
            payButton.setDisable(true);
            cancelButton.setDisable(true);
            payButton.setText("Processing...");

            // Background Task
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    // Simulate Network Delay
                    Thread.sleep(2000);

                    // Actual Logic (DB Operations)
                    int userId = UserSession.getInstance().getUser().getId();
                    int walletId = walletService.getWallet(userId).getId();

                    walletService.credit(walletId, amount,
                            "DEPOSIT via Card **** " + cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                handleCancel(); // Close current dialog first

                // Run success dialog in next tick to allow stage to close properly
                javafx.application.Platform.runLater(() -> {
                    DialogUtil.showInfo("Success", "Payment Approved! Funds added.");
                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }
                });
            });

            task.setOnFailed(e -> {
                Throwable error = task.getException();
                error.printStackTrace();
                DialogUtil.showError("Transaction Failed", error.getMessage());
                resetUI();
            });

            new Thread(task).start();

        } catch (NumberFormatException e) {
            DialogUtil.showError("Invalid Input", "Please enter a valid amount.");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Deposit failed: " + e.getMessage());
        }
    }

    private void resetUI() {
        payButton.setDisable(false);
        cancelButton.setDisable(false);
        payButton.setText("Confirm Payment");
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }
}
