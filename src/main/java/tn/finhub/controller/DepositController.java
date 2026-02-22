package tn.finhub.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
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
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;

import java.math.BigDecimal;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class DepositController {

    @FXML
    private TextField amountField;

    @FXML
    private Button payButton;
    @FXML
    private Button cancelButton;

    private final WalletModel walletModel = new WalletModel();
    private Runnable onSuccessCallback;

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

        // Change button text for new logic
        payButton.setText("Link Bank Account");
    }

    @FXML
    private void handleDeposit() {
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

        // Build the URL for the Python backend page
        String url = String.format("https://escrowfinhub.work.gd/plaid/link_page?user_id=%d&wallet_id=%d&amount=%s",
                userId, walletId, amountText);

        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new URI(url));

                // ---------------------------------------------------------
                // Custom UI Dialog for Polling
                // ---------------------------------------------------------
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

                // Background Polling Thread
                AtomicBoolean polling = new AtomicBoolean(true);

                cancelBtn.setOnAction(e -> {
                    polling.set(false);
                    waitStage.close();
                    resetUI();
                });

                Thread pollThread = new Thread(() -> {
                    while (polling.get()) {
                        try {
                            Thread.sleep(2000); // Check DB every 2 seconds
                            Wallet updatedWallet = walletModel.findByUserId(userId);
                            if (updatedWallet.getBalance().compareTo(oldBalance) > 0) {
                                // Balance went up! Plaid succeeded!
                                polling.set(false);
                                Platform.runLater(() -> {
                                    waitStage.close();
                                    DialogUtil.showInfo("Transfer Complete",
                                            "Your FinHub Ledger has been securely credited!");
                                    if (onSuccessCallback != null) {
                                        onSuccessCallback.run();
                                    }
                                    handleCancel(); // Close the parent deposit dialog too
                                });
                            }
                        } catch (InterruptedException ex) {
                            break;
                        } catch (Exception ignored) {
                            // Ignore occasional DB timeouts during polling
                        }
                    }
                });
                pollThread.setDaemon(true);
                pollThread.start();

                waitStage.show();

            } else {
                DialogUtil.showError("Browser Error",
                        "Your system does not support opening default browsers automatically.");
                resetUI();
            }
        } catch (Exception ex) {
            DialogUtil.showError("Redirect Error", "Failed to open the browser: " + ex.getMessage());
            resetUI();
        }
    }

    private void resetUI() {
        payButton.setDisable(false);
        cancelButton.setDisable(false);
        payButton.setText("Link Bank Account");
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }
}
