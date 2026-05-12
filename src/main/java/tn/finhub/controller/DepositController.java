package tn.finhub.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.finhub.model.User;
import tn.finhub.model.UserModel;
import tn.finhub.model.VirtualCard;
import tn.finhub.model.VirtualCardModel;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.util.BrowserUtil;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.MailClient;
import tn.finhub.util.UserSession;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicBoolean;

public class DepositController {

    @FXML
    private TextField amountField;
    @FXML
    private Label subtitleLabel;

    @FXML
    private Button plaidTabButton;
    @FXML
    private Button cardTabButton;

    @FXML
    private VBox plaidSection;
    @FXML
    private VBox cardSection;

    @FXML
    private TextField cardNumberField;
    @FXML
    private TextField expiryField;
    @FXML
    private TextField cvvField;

    @FXML
    private Button payButton;
    @FXML
    private Button cancelButton;

    private final WalletModel walletModel = new WalletModel();
    private final VirtualCardModel virtualCardModel = new VirtualCardModel();
    private final UserModel userModel = new UserModel();
    private Runnable onSuccessCallback;

    private boolean plaidMode = true;

    // Card cash-in state (stored between init and confirm)
    private String cardOtp;
    private String cardNumberInput;
    private BigDecimal cardAmount;
    private int cardSourceWalletId;

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void initialize() {
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(oldVal);
            }
        });

        // Card number: digits max 16
        cardNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;
            String cleaned = newVal.replaceAll("[^\\d]", "");
            if (cleaned.length() > 16)
                cleaned = cleaned.substring(0, 16);
            if (!cleaned.equals(newVal.replaceAll("[^\\d]", ""))) {
                cardNumberField.setText(cleaned);
            }
        });

        // Expiry: digits only, auto-insert '/', max 4 digits
        expiryField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;
            String digits = newVal.replaceAll("[^\\d]", "");
            if (digits.length() > 4)
                digits = digits.substring(0, 4);
            String formatted = digits;
            if (digits.length() >= 3) {
                formatted = digits.substring(0, 2) + "/" + digits.substring(2);
            }
            if (!formatted.equals(newVal)) {
                expiryField.setText(formatted);
            }
        });

        // CVV: digits max 4
        cvvField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;
            String digits = newVal.replaceAll("[^\\d]", "");
            if (digits.length() > 4)
                digits = digits.substring(0, 4);
            if (!digits.equals(newVal)) {
                cvvField.setText(digits);
            }
        });

        showPlaidMode();
    }

    @FXML
    private void switchToPlaid() {
        showPlaidMode();
    }

    @FXML
    private void switchToCard() {
        showCardMode();
    }

    private void showPlaidMode() {
        plaidMode = true;
        plaidTabButton.getStyleClass().remove("deposit-tab-inactive");
        plaidTabButton.getStyleClass().add("deposit-tab-active");
        cardTabButton.getStyleClass().remove("deposit-tab-active");
        cardTabButton.getStyleClass().add("deposit-tab-inactive");
        plaidSection.setVisible(true);
        plaidSection.setManaged(true);
        cardSection.setVisible(false);
        cardSection.setManaged(false);
        subtitleLabel.setText("Secure Plaid Bank Verification");
        payButton.setText("Link Bank Account");
        payButton.setStyle("-fx-background-color: #111827; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
    }

    private void showCardMode() {
        plaidMode = false;
        cardTabButton.getStyleClass().remove("deposit-tab-inactive");
        cardTabButton.getStyleClass().add("deposit-tab-active");
        plaidTabButton.getStyleClass().remove("deposit-tab-active");
        plaidTabButton.getStyleClass().add("deposit-tab-inactive");
        plaidSection.setVisible(false);
        plaidSection.setManaged(false);
        cardSection.setVisible(true);
        cardSection.setManaged(true);
        subtitleLabel.setText("Cash In via Virtual Card");
        payButton.setText("Cash In with Card");
        payButton.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-border-color: #059669; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
    }

    @FXML
    private void handleDeposit() {
        if (plaidMode) {
            handlePlaidDeposit();
        } else {
            handleCardCashInInit();
        }
    }

    // ========================
    // PLAID FLOW
    // ========================

    private void handlePlaidDeposit() {
        String amountText = amountField.getText();
        if (amountText.isEmpty()) {
            DialogUtil.showError("Validation Error", "Please enter an amount to deposit.");
            return;
        }

        BigDecimal amount = new BigDecimal(amountText);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            DialogUtil.showError("Invalid Amount", "Amount must be greater than 0.");
            return;
        }

        payButton.setDisable(true);
        cancelButton.setDisable(true);
        payButton.setText("Browser Opened...");

        int userId = UserSession.getInstance().getUser().getId();
        Wallet currentWallet = walletModel.findByUserId(userId);
        int walletId = currentWallet.getId();
        BigDecimal oldBalance = currentWallet.getBalance();

        String url = String.format("https://escrow.finhub.tn/plaid/link_page?user_id=%d&wallet_id=%d&amount=%s",
                userId, walletId, amountText);

        try {
            BrowserUtil.openUrl(url);

            Stage waitStage = new Stage(StageStyle.UNDECORATED);
            waitStage.initModality(Modality.APPLICATION_MODAL);
            waitStage.initOwner(amountField.getScene().getWindow());

            VBox box = new VBox(20);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color: #1E1B2E; -fx-padding: 30; -fx-border-color: #a855f7; " +
                    "-fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setStyle("-fx-progress-color: #ec4899;");

            Label title = new Label("Secure Bank Gateway");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

            Label subtitle = new Label("A secure portal has opened in your web browser.\n" +
                    "This window will automatically close and credit\nyour account once the transfer succeeds.");
            subtitle.setStyle("-fx-text-fill: #9CA3AF; -fx-text-alignment: center; -fx-font-size: 14px;");

            Button cancelBtn = new Button("Cancel Transaction");
            cancelBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #F87171; -fx-border-color: #F87171; -fx-border-radius: 6; -fx-cursor: hand;");

            box.getChildren().addAll(spinner, title, subtitle, cancelBtn);
            waitStage.setScene(new Scene(box));

            AtomicBoolean polling = new AtomicBoolean(true);

            cancelBtn.setOnAction(e -> {
                polling.set(false);
                waitStage.close();
                resetUI();
            });

            Thread pollThread = new Thread(() -> {
                while (polling.get()) {
                    try {
                        Thread.sleep(2000);
                        Wallet updatedWallet = walletModel.findByUserId(userId);
                        if (updatedWallet.getBalance().compareTo(oldBalance) > 0) {
                            polling.set(false);
                            Platform.runLater(() -> {
                                waitStage.close();
                                DialogUtil.showInfo("Transfer Complete",
                                        "Your FinHub Ledger has been securely credited!");
                                if (onSuccessCallback != null) {
                                    onSuccessCallback.run();
                                }
                                handleCancel();
                            });
                        }
                    } catch (InterruptedException ex) {
                        break;
                    } catch (Exception ignored) {
                    }
                }
            });
            pollThread.setDaemon(true);
            pollThread.start();

            waitStage.show();

        } catch (Exception ex) {
            DialogUtil.showError("Redirect Error", "Failed to open the browser: " + ex.getMessage());
            resetUI();
        }
    }

    // ========================
    // VIRTUAL CARD CASH-IN FLOW
    // ========================

    private void handleCardCashInInit() {
        // Validate amount
        String amountText = amountField.getText();
        if (amountText.isEmpty()) {
            DialogUtil.showError("Validation Error", "Please enter an amount.");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
        } catch (NumberFormatException e) {
            DialogUtil.showError("Invalid Amount", "Please enter a valid amount.");
            return;
        }
        if (amount.compareTo(BigDecimal.ONE) < 0) {
            DialogUtil.showError("Invalid Amount", "Cash in amount must be at least 1 TND.");
            return;
        }

        // Validate card number
        String rawCardNumber = cardNumberField.getText();
        if (rawCardNumber == null || rawCardNumber.isEmpty()) {
            DialogUtil.showError("Validation Error", "Please enter a card number.");
            return;
        }
        String cardNumber = rawCardNumber.replaceAll("\\D+", "");
        if (cardNumber.length() < 12 || cardNumber.length() > 16) {
            DialogUtil.showError("Invalid Card", "Please enter a valid card number.");
            return;
        }

        // Validate expiry
        String expiryText = expiryField.getText();
        if (expiryText == null || !expiryText.matches("^(0[1-9]|1[0-2])/[0-9]{2}$")) {
            DialogUtil.showError("Invalid Expiry", "Please enter a valid expiry date (MM/YY).");
            return;
        }

        // Validate CVV
        String cvv = cvvField.getText();
        if (cvv == null || !cvv.matches("^[0-9]{3,4}$")) {
            DialogUtil.showError("Invalid CVV", "Please enter a valid CVV (3 or 4 digits).");
            return;
        }

        // Look up virtual card
        VirtualCard card = virtualCardModel.findByCardNumber(cardNumber);
        if (card == null) {
            DialogUtil.showError("Card Not Found", "No virtual card found with that number.");
            return;
        }

        if (!"ACTIVE".equals(card.getStatus())) {
            DialogUtil.showError("Card Inactive", "This virtual card is not active.");
            return;
        }

        // Validate CVV
        if (!cvv.equals(card.getCvv())) {
            DialogUtil.showError("Invalid CVV", "The CVV does not match.");
            return;
        }

        // Validate expiry
        try {
            String[] parts = expiryText.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt("20" + parts[1]);
            YearMonth inputExpiry = YearMonth.of(year, month);
            Date cardExpiry = card.getExpiryDate();
            if (cardExpiry == null) {
                DialogUtil.showError("Card Error", "Card expiry date is invalid.");
                return;
            }
            LocalDate cardExpiryLocal = cardExpiry.toLocalDate();
            YearMonth cardExpiryYM = YearMonth.of(cardExpiryLocal.getYear(), cardExpiryLocal.getMonthValue());
            if (!inputExpiry.equals(cardExpiryYM)) {
                DialogUtil.showError("Invalid Expiry", "Expiry date does not match the card.");
                return;
            }
            // Check if expired (last day of the month)
            if (cardExpiryYM.atEndOfMonth().isBefore(LocalDate.now())) {
                DialogUtil.showError("Card Expired", "This virtual card is expired.");
                return;
            }
        } catch (Exception e) {
            DialogUtil.showError("Invalid Expiry", "Could not validate expiry date.");
            return;
        }

        // Get card owner's wallet
        int sourceWalletId = card.getWalletId();
        Wallet sourceWallet = walletModel.findById(sourceWalletId);
        if (sourceWallet == null) {
            DialogUtil.showError("Wallet Error", "Card owner wallet not found.");
            return;
        }

        // Check the card wallet is not frozen
        if ("FROZEN".equals(sourceWallet.getStatus())) {
            DialogUtil.showError("Card Frozen", "The card owner's wallet is frozen.");
            return;
        }

        // Get logged-in user's wallet
        int userId = UserSession.getInstance().getUser().getId();
        Wallet userWallet = walletModel.findByUserId(userId);
        if (userWallet == null || userWallet.getId() == sourceWalletId) {
            DialogUtil.showError("Invalid Operation", "You cannot cash in using your own virtual card.");
            return;
        }

        // Check source wallet has enough balance
        if (sourceWallet.getBalance().compareTo(amount) < 0) {
            DialogUtil.showError("Insufficient Funds", "This card's wallet does not have enough balance.");
            return;
        }

        // Get card holder's email for OTP
        User cardHolder = userModel.findById(sourceWallet.getUserId());
        if (cardHolder == null || cardHolder.getEmail() == null || cardHolder.getEmail().isEmpty()) {
            DialogUtil.showError("Email Error", "Card holder email is unavailable.");
            return;
        }
        String cardHolderEmail = cardHolder.getEmail();

        // Generate OTP
        final String otp = generateSecureOtp();

        // Store state for confirm step
        cardOtp = otp;
        cardNumberInput = cardNumber;
        cardAmount = amount;
        cardSourceWalletId = sourceWalletId;

        // UI feedback
        payButton.setDisable(true);
        cancelButton.setDisable(true);
        payButton.setText("Sending OTP...");

        // Send OTP email to card holder in background
        javafx.concurrent.Task<Void> emailTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                MailClient.sendOtpEmail(cardHolderEmail, otp);
                return null;
            }
        };

        emailTask.setOnSucceeded(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/otp_dialog.fxml"));
                loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
                javafx.scene.Parent root = loader.load();

                OtpController otpController = loader.getController();
                otpController.setExpectedOtp(cardOtp);
                otpController.setOnSuccessCallback(() -> proceedWithCardCashIn());
                otpController.setOnCancelCallback(this::resetUI);

                Stage otpStage = new Stage();
                otpStage.initStyle(StageStyle.TRANSPARENT);
                otpStage.initModality(Modality.APPLICATION_MODAL);

                Scene scene = new Scene(root);
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
            DialogUtil.showError("Email Error", "Could not send verification code: " + error.getMessage());
            resetUI();
        });

        new Thread(emailTask).start();
    }

    private void proceedWithCardCashIn() {
        payButton.setText("Processing...");

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                int userId = UserSession.getInstance().getUser().getId();
                Wallet userWallet = walletModel.findByUserId(userId);

                Wallet sourceWallet = walletModel.findById(cardSourceWalletId);
                User sourceUser = userModel.findById(sourceWallet.getUserId());
                User recipientUser = userModel.findById(userId);

                String senderName = sourceUser != null && sourceUser.getFullName() != null
                        ? sourceUser.getFullName()
                        : "Card Holder";
                String recipientName = recipientUser != null && recipientUser.getFullName() != null
                        ? recipientUser.getFullName()
                        : "User";

                String senderRef = "Card cash-in to " + recipientName + " (Wallet " + userWallet.getId() + ")";
                String receiverRef = "Card cash-in from " + senderName + " via card ending "
                        + cardNumberInput.substring(Math.max(0, cardNumberInput.length() - 4));

                walletModel.transferInternal(cardSourceWalletId, userWallet.getId(),
                        cardAmount, senderRef, receiverRef);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            handleCancel();
            Platform.runLater(() -> {
                DialogUtil.showInfo("Cash In Complete",
                        "Your wallet has been credited with " + cardAmount + " TND via virtual card.");
                if (onSuccessCallback != null) {
                    onSuccessCallback.run();
                }
            });
        });

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            error.printStackTrace();
            DialogUtil.showError("Cash In Failed", error.getMessage());
            resetUI();
        });

        new Thread(task).start();
    }

    // ========================
    // UI HELPERS
    // ========================

    private void resetUI() {
        payButton.setDisable(false);
        cancelButton.setDisable(false);
        if (plaidMode) {
            payButton.setText("Link Bank Account");
        } else {
            payButton.setText("Cash In with Card");
        }
    }

    private String generateSecureOtp() {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(
                            "https://www.random.org/integers/?num=1&min=100000&max=999999&col=1&base=10&format=plain&rnd=new"))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().trim().length() == 6) {
                return response.body().trim();
            }
        } catch (Exception e) {
            // Fall through to secure random fallback
        }
        return String.format("%06d", new SecureRandom().nextInt(999999));
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }
}
