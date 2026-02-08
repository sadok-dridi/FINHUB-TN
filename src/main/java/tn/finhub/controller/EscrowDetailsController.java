package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.finhub.model.*;
import tn.finhub.util.UserSession;

public class EscrowDetailsController {

    @FXML
    private Label escrowIdLabel;
    @FXML
    private Label amountLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextArea conditionArea;

    // Sender View (QR Display)
    @FXML
    private VBox senderSection;
    @FXML
    private ImageView qrCodeImageView;
    @FXML
    private Label secretCodeLabel;

    // Receiver View (Code Input)
    @FXML
    private VBox receiverSection;
    @FXML
    private TextField codeInputField;
    @FXML
    private Button claimButton;

    // Common
    @FXML
    private Button disputeButton;
    @FXML
    private Label errorLabel;

    private Escrow currentEscrow;
    private final EscrowManager escrowManager = new EscrowManager();
    private final WalletModel walletModel = new WalletModel();

    public void setEscrow(Escrow escrow) {
        this.currentEscrow = escrow;
        populateData();
        setupViewBasedOnRole();
    }

    private void populateData() {
        escrowIdLabel.setText("Escrow #" + currentEscrow.getId());
        amountLabel.setText(currentEscrow.getAmount() + " TND");
        statusLabel.setText(currentEscrow.getStatus());
        conditionArea.setText(currentEscrow.getConditionText());

        // CSS for Status
        String statusStyle = "-fx-padding: 5 12; -fx-background-radius: 15; -fx-font-weight: bold; text-transform: uppercase;";
        switch (currentEscrow.getStatus()) {
            case "LOCKED" -> statusStyle += "-fx-background-color: #fff7ed; -fx-text-fill: #c2410c;";
            case "RELEASED" -> statusStyle += "-fx-background-color: #ecfdf5; -fx-text-fill: #047857;";
            case "DISPUTED" -> statusStyle += "-fx-background-color: #fef2f2; -fx-text-fill: #b91c1c;";
            default -> statusStyle += "-fx-background-color: #f3f4f6; -fx-text-fill: #4b5563;";
        }
        statusLabel.setStyle(statusStyle);
    }

    private void setupViewBasedOnRole() {
        User currentUser = UserSession.getInstance().getUser();
        Wallet currentWallet = walletModel.findByUserId(currentUser.getId());

        boolean isSender = currentWallet.getId() == currentEscrow.getSenderWalletId();
        boolean isReceiver = currentWallet.getId() == currentEscrow.getReceiverWalletId();
        boolean isLocked = "LOCKED".equals(currentEscrow.getStatus());

        senderSection.setVisible(false);
        senderSection.setManaged(false);
        receiverSection.setVisible(false);
        receiverSection.setManaged(false);
        disputeButton.setVisible(isLocked); // Only dispute if locked

        if (isLocked) {
            if (isSender) {
                // Sender sees QR and Secret Code
                senderSection.setVisible(true);
                senderSection.setManaged(true);
                secretCodeLabel.setText("Secret Code: " + currentEscrow.getSecretCode());

                if ("QR_CODE".equals(currentEscrow.getEscrowType())) {
                    loadQrCode(currentEscrow.getSecretCode());
                } else {
                    qrCodeImageView.setVisible(false);
                    secretCodeLabel.setText("Waiting for Admin Approval");
                }
            } else if (isReceiver) {
                // Receiver sees Input Field
                receiverSection.setVisible(true);
                receiverSection.setManaged(true);
            }
        }
    }

    private void loadQrCode(String data) {
        // Use QR Server API
        String url = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + data;
        Image image = new Image(url, true); // background loading
        qrCodeImageView.setImage(image);
    }

    @FXML
    private void handleClaim() {
        try {
            String code = codeInputField.getText().trim();
            if (code.isEmpty()) {
                showError("Please enter the secret code.");
                return;
            }

            escrowManager.releaseEscrow(currentEscrow.getId(), code);

            showInfo("Funds Released!", "The escrow funds have been successfully released to your wallet.");
            closeDialog();

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleDispute() {
        try {
            escrowManager.raiseDispute(currentEscrow.getId());
            showInfo("Dispute Raised", "The transaction has been disputed. Support will contact you shortly.");
            closeDialog();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) escrowIdLabel.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
