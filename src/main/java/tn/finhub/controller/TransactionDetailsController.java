package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.finhub.model.WalletTransaction;

import java.time.format.DateTimeFormatter;

public class TransactionDetailsController {

    @FXML
    private Label typeLabel;
    @FXML
    private Label amountLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label referenceLabel;
    @FXML
    private Label txIdLabel;
    @FXML
    private Label hashLabel;

    public void setTransaction(WalletTransaction tx) {
        if (tx == null)
            return;

        // Type
        typeLabel.setText(tx.getType());

        // Amount
        boolean isPositive = "CREDIT".equals(tx.getType()) || "RELEASE".equals(tx.getType())
                || "TRANSFER_RECEIVED".equals(tx.getType());
        boolean isNegative = "DEBIT".equals(tx.getType()) || "TRANSFER_SENT".equals(tx.getType());

        String prefix = isPositive ? "+" : (isNegative ? "-" : "");
        amountLabel.setText(prefix + " TND " + tx.getAmount());

        if (isPositive) {
            amountLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 32px; -fx-font-weight: bold;");
        } else if (isNegative) {
            amountLabel.setStyle("-fx-text-fill: #F87171; -fx-font-size: 32px; -fx-font-weight: bold;");
        } else {
            amountLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
        }

        // Date
        dateLabel.setText(tx.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")));

        // Reference
        referenceLabel.setText(tx.getReference());

        // ID
        txIdLabel.setText(String.valueOf(tx.getId()));

        // Hash
        if (tx.getTxHash() != null && !tx.getTxHash().isEmpty()) {
            hashLabel.setText(tx.getTxHash());
        } else {
            hashLabel.setText("Not recorded on ledger");
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) amountLabel.getScene().getWindow();
        stage.close();
    }
}
