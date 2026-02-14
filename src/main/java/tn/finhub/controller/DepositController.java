package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
<<<<<<< HEAD
import tn.finhub.service.WalletService;
=======
import tn.finhub.model.WalletModel; // Added import

>>>>>>> cd680ce (crud+controle de saisie)
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

<<<<<<< HEAD
    private final WalletService walletService = new WalletService();
=======
    private final WalletModel walletModel = new WalletModel(); // Changed to WalletModel
>>>>>>> cd680ce (crud+controle de saisie)
    private Runnable onSuccessCallback;

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void initialize() {
        // 1. Card Number Formatting (Spaces every 4 digits)
        cardNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;

            // Remove non-digits
            String digits = newVal.replaceAll("[^\\d]", "");
            if (digits.length() > 16)
                digits = digits.substring(0, 16);

            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) {
                    formatted.append(" ");
                }
                formatted.append(digits.charAt(i));
            }

            // Avoid infinite loop by checking if change is needed
            if (!formatted.toString().equals(newVal)) {
                cardNumberField.setText(formatted.toString());
                // Simple Caret positioning (end) - sufficient for sequential typing
                cardNumberField.positionCaret(formatted.length());
            }
        });

        // 2. Expiry Formatting (MM/YY)
        expiryField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;

            // Allow only digits and slash (slash controlled by us, but safeguard)
            String digits = newVal.replaceAll("[^\\d]", "");
            if (digits.length() > 4)
                digits = digits.substring(0, 4); // Max 4 digits (MMYY)

            StringBuilder formatted = new StringBuilder();
            if (digits.length() >= 2) {
                formatted.append(digits.substring(0, 2));
                // Add Slash if we have more than 2 digits or user just typed the 2nd digit
                // (forward typing)
                if (digits.length() > 2
                        || (oldVal != null && newVal.length() > oldVal.length() && digits.length() == 2)) {
                    formatted.append("/");
                }
                if (digits.length() > 2) {
                    formatted.append(digits.substring(2));
                }
            } else {
                formatted.append(digits);
            }

            if (!formatted.toString().equals(newVal)) {
                expiryField.setText(formatted.toString());
                expiryField.positionCaret(formatted.length());
            }
        });

        // 3. CVV Formatting (Max 3 digits)
        cvvField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;
            if (!newVal.matches("\\d*")) {
                cvvField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (cvvField.getText().length() > 3) {
                cvvField.setText(cvvField.getText().substring(0, 3));
            }
        });

        // 4. Amount Formatting (Numeric/Decimal only)
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(oldVal);
            }
        });
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

            // --- STRICT PAYMENT VALIDATION ---
            String cleanCardNumber = cardNumber.replaceAll("\\s+", "");

            // 1. Length & Format
            if (!cleanCardNumber.matches("\\d{16}")) {
                DialogUtil.showError("Invalid Card", "Card number must be 16 digits.");
                return;
            }

            // 2. Luhn Algorithm
<<<<<<< HEAD
            if (!tn.finhub.service.VirtualCardService.checkLuhn(cleanCardNumber)) {
=======
            if (!tn.finhub.model.VirtualCardModel.checkLuhn(cleanCardNumber)) {
>>>>>>> cd680ce (crud+controle de saisie)
                DialogUtil.showError("Invalid Card", "Card number check failed (Luhn). Please check your card.");
                return;
            }

            // 3. CVV Format
            if (!cvv.matches("\\d{3}")) {
                DialogUtil.showError("Invalid CVV", "CVV must be 3 digits.");
                return;
            }

            // 4. Expiry Check (MM/YY)
            if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
                DialogUtil.showError("Invalid Expiry", "Expiry must be in MM/YY format.");
                return;
            }

            // 5. Expiry Date > Current Date
            try {
                String[] parts = expiry.split("/");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt("20" + parts[1]); // Assume 20xx
                java.time.YearMonth expDate = java.time.YearMonth.of(year, month);

                // Allow current month as valid
                if (expDate.isBefore(java.time.YearMonth.now())) {
                    DialogUtil.showError("Card Expired", "This card has expired.");
                    return;
                }
            } catch (Exception e) {
                DialogUtil.showError("Invalid Expiry", "Invalid date.");
                return;
            }

            // ---------------------------------

            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                DialogUtil.showError("Invalid Amount", "Amount must be greater than 0.");
                return;
            }

            // UI Feedback: Sending OTP
            payButton.setDisable(true);
            cancelButton.setDisable(true);
            payButton.setText("Sending Security Code...");

            // 1. Generate OTP
            String otp = String.format("%06d", new java.util.Random().nextInt(999999));

            // 2. Send Email (Background)
            javafx.concurrent.Task<Void> emailTask = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    tn.finhub.model.User currentUser = UserSession.getInstance().getUser();
                    String targetEmail = currentUser.getEmail();

                    // Check if card belongs to another internal user
<<<<<<< HEAD
                    tn.finhub.service.VirtualCardService cardService = new tn.finhub.service.VirtualCardService();
                    tn.finhub.model.Wallet cardOwnerWallet = cardService.findCardOwner(cleanCardNumber);

                    if (cardOwnerWallet != null) {
                        tn.finhub.service.UserService userService = new tn.finhub.service.UserService();
                        tn.finhub.model.User owner = userService.getUserById(cardOwnerWallet.getUserId());
=======
                    tn.finhub.model.VirtualCardModel cardModel = new tn.finhub.model.VirtualCardModel();
                    tn.finhub.model.Wallet cardOwnerWallet = cardModel.findCardOwner(cleanCardNumber);

                    if (cardOwnerWallet != null) {
                        tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
                        tn.finhub.model.User owner = userModel.findById(cardOwnerWallet.getUserId());
>>>>>>> cd680ce (crud+controle de saisie)
                        if (owner != null) {
                            targetEmail = owner.getEmail();
                        }
                    }

<<<<<<< HEAD
                    tn.finhub.service.MailService.sendOtpEmail(targetEmail, otp);
=======
                    tn.finhub.util.MailClient.sendOtpEmail(targetEmail, otp);
>>>>>>> cd680ce (crud+controle de saisie)
                    return null;
                }
            };

            emailTask.setOnSucceeded(event -> {
                // 3. Open OTP Dialog
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                            getClass().getResource("/view/otp_dialog.fxml"));
<<<<<<< HEAD
=======
                    loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
>>>>>>> cd680ce (crud+controle de saisie)
                    javafx.scene.Parent root = loader.load();

                    OtpController otpController = loader.getController();
                    otpController.setExpectedOtp(otp);

                    // Verify Callback
                    otpController.setOnSuccessCallback(() -> {
                        // 4. Proceed with Transaction
                        proceedWithTransaction(amountText, cleanCardNumber, cvv);
                    });

                    // Cancel Callback
                    otpController.setOnCancelCallback(() -> {
                        resetUI();
                    });

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
            DialogUtil.showError("Error", "Request failed: " + e.getMessage());
        }
    }

    private void proceedWithTransaction(String amountText, String cleanCardNumber, String cvv) {
        payButton.setText("Processing Payment...");
        BigDecimal amount = new BigDecimal(amountText);

        // Background Task
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                // Simulate Gateway Delay
                Thread.sleep(1500);

                // Actual Logic (DB Operations)
                int userId = UserSession.getInstance().getUser().getId();
<<<<<<< HEAD
                int userWalletId = walletService.getWallet(userId).getId();

                tn.finhub.service.VirtualCardService cardService = new tn.finhub.service.VirtualCardService();
                tn.finhub.model.Wallet cardOwnerWallet = cardService.findCardOwner(cleanCardNumber);
=======
                int userWalletId = walletModel.findByUserId(userId).getId();

                tn.finhub.model.VirtualCardModel cardModel = new tn.finhub.model.VirtualCardModel();
                tn.finhub.model.Wallet cardOwnerWallet = cardModel.findCardOwner(cleanCardNumber);
>>>>>>> cd680ce (crud+controle de saisie)

                if (cardOwnerWallet != null) {
                    // 1. Internal Transfer (P2P via Card)
                    if (cardOwnerWallet.getId() == userWalletId) {
                        throw new RuntimeException("Cannot use your own card to top up the same wallet.");
                    }

                    // Verify CVV for Internal Cards matches DB
<<<<<<< HEAD
                    tn.finhub.model.VirtualCard card = cardService.findCard(cleanCardNumber);
=======
                    tn.finhub.model.VirtualCard card = cardModel.findCard(cleanCardNumber);
>>>>>>> cd680ce (crud+controle de saisie)
                    if (!card.getCvv().equals(cvv)) {
                        throw new RuntimeException("Incorrect CVV.");
                    }
                    if (!"ACTIVE".equals(card.getStatus())) {
                        throw new RuntimeException("Card is inactive or blocked.");
                    }

                    // Execute Transfer: Card Owner -> Current User
<<<<<<< HEAD
                    walletService.transfer(cardOwnerWallet.getId(), userWalletId, amount,
=======
                    walletModel.transfer(cardOwnerWallet.getId(), userWalletId, amount,
>>>>>>> cd680ce (crud+controle de saisie)
                            "P2P Transfer via Card **** " + cleanCardNumber.substring(12));

                } else {
                    // 2. External Transfer (Bank -> User) simulated
                    // Card is valid (Luhn checked), so we approve "External Payment"
                    try {
<<<<<<< HEAD
                        int bankWalletId = walletService.getBankWalletId();
                        walletService.transfer(bankWalletId, userWalletId, amount,
=======
                        int bankWalletId = walletModel.getBankWalletId();
                        walletModel.transfer(bankWalletId, userWalletId, amount,
>>>>>>> cd680ce (crud+controle de saisie)
                                "DEPOSIT via Card **** " + cleanCardNumber.substring(12));
                    } catch (RuntimeException re) {
                        throw new RuntimeException("Payment Gateway Error: " + re.getMessage());
                    }
                }

                return null;
            }
        };

        task.setOnSucceeded(e -> {
            handleCancel(); // Close deposit dialog

            // Run success dialog in next tick
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
