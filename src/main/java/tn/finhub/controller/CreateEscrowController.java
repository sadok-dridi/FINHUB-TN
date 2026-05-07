package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.finhub.util.DialogUtil;
import tn.finhub.model.EscrowManager;
import tn.finhub.model.User;
import tn.finhub.model.UserModel;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.util.UserSession;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CreateEscrowController {

    @FXML
    private TextField recipientEmailField;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private CheckBox requireDocusignCheckbox;
    @FXML
    private TextArea conditionField;
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label feeLabel;
    @FXML
    private Label totalLabel;

    private final EscrowManager escrowManager = new EscrowManager();
    private final UserModel userModel = new UserModel();
    private final WalletModel walletModel = new WalletModel();
    private final BigDecimal FEE_PERCENT = new BigDecimal("0.01");

    @FXML
    public void initialize() {
        typeComboBox.getItems().addAll("QR Code Release", "Admin Approval");
        typeComboBox.setValue("QR Code Release");

        // Live Fee Calculation
        amountField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotals());
    }

    private void calculateTotals() {
        try {
            BigDecimal amount = new BigDecimal(amountField.getText());
            BigDecimal fee = amount.multiply(FEE_PERCENT);

            subtotalLabel.setText(amount.setScale(3, RoundingMode.HALF_UP) + " TND");
            feeLabel.setText(fee.setScale(3, RoundingMode.HALF_UP) + " TND (Paid by Seller)");
            totalLabel.setText(amount.setScale(3, RoundingMode.HALF_UP) + " TND");

        } catch (NumberFormatException e) {
            subtotalLabel.setText("0.000 TND");
            feeLabel.setText("0.000 TND");
            totalLabel.setText("0.000 TND");
        }
    }

    @FXML
    private void handleCreate() {
        try {
            // 1. Validation
            String email = recipientEmailField.getText();
            String amountStr = amountField.getText();
            String condition = conditionField.getText();
            String typeSelection = typeComboBox.getValue();
            boolean requireDocusign = requireDocusignCheckbox.isSelected();

            String type = "QR_CODE";
            if ("Admin Approval".equals(typeSelection)) {
                type = "ADMIN_APPROVAL";
            }

            if (email.isEmpty() || amountStr.isEmpty() || condition.isEmpty()) {
                showAlert("Error", "Please fill in all fields.");
                return;
            }

            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Error", "Amount must be positive.");
                return;
            }

            // 2. Resolve User & Wallet
            User sender = UserSession.getInstance().getUser();
            if (email.equalsIgnoreCase(sender.getEmail())) {
                showAlert("Error", "You cannot create an escrow with yourself.");
                return;
            }

            User receiver = userModel.findByEmail(email);
            if (receiver == null) {
                showAlert("Error", "User with email '" + email + "' not found.");
                return;
            }

            Wallet senderWallet = walletModel.findByUserId(sender.getId());
            Wallet receiverWallet = walletModel.findByUserId(receiver.getId());

            if (receiverWallet == null) {
                showAlert("Error", "Receiver does not have an active wallet.");
                return;
            }

            // 3. Create Escrow
            escrowManager.createEscrow(senderWallet.getId(), receiverWallet.getId(), amount, condition, type,
                    requireDocusign);

            // If it is DocuSign, prompt the user that an email has been sent.
            if (requireDocusign) {
                DialogUtil.showInfo("Escrow Created",
                        "Escrow created with DocuSign requirement! Please check your email and have the receiver check theirs to sign the agreement. Funds will not be released until signed.");
            } else {
                DialogUtil.showInfo("Escrow Created", "Escrow successfully created.");
            }

            // 4. Success & Close
            closeDialog();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Escrow Creation Failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) recipientEmailField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        DialogUtil.showError(title, content);
    }
}
