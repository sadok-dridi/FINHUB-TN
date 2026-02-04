package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tn.finhub.model.WalletTransaction;
import tn.finhub.service.WalletService;
import tn.finhub.util.DialogUtil;
import java.math.BigDecimal;

public class AdminRepairDialogController {

    @FXML
    private TextField amountField;
    @FXML
    private TextField refField;
    @FXML
    private Label hashLabel;
    @FXML
    private Label correctionHintLabel;

    private Stage dialogStage;
    private WalletTransaction transaction;
    private boolean saved = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setTransaction(WalletTransaction transaction) {
        this.transaction = transaction;
        amountField.setText(transaction.getAmount().toString());
        refField.setText(transaction.getReference());
        hashLabel.setText("Hash: " + transaction.getTxHash());

        // Suggest Correct Value
        WalletService ws = new WalletService();
        BigDecimal suggested = ws.calculateDiscrepancy(transaction.getWalletId(), transaction.getId(),
                transaction.getAmount(), transaction.getType());

        correctionHintLabel.setText("Approximate Value form Ledger: " + suggested.toPlainString());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try {
                BigDecimal newAmount = new BigDecimal(amountField.getText());
                String newRef = refField.getText();

                // Perform Repair
                WalletService walletService = new WalletService();
                walletService.repairTransaction(transaction.getWalletId(), transaction.getId(), newAmount, newRef);

                saved = true;
                DialogUtil.showInfo("Success", "Transaction restored and ledger verified.");
                dialogStage.close();

            } catch (Exception e) {
                DialogUtil.showError("Error", "Repair failed: " + e.getMessage());
            }
        }
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (amountField.getText() == null || amountField.getText().length() == 0) {
            errorMessage += "No valid amount!\n";
        } else {
            try {
                new BigDecimal(amountField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "No valid amount (must be a number)!\n";
            }
        }

        if (refField.getText() == null || refField.getText().length() == 0) {
            errorMessage += "No valid reference!\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            DialogUtil.showError("Invalid Fields", errorMessage);
            return false;
        }
    }
}
