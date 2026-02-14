package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.finhub.model.SystemAlertModel;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.SessionManager;
import tn.finhub.util.ViewUtils;

public class AdminAlertsController {

    @FXML
    private ComboBox<String> severityBox;
    @FXML
    private TextField sourceField;
    @FXML
    private TextArea messageArea;

    private final SystemAlertModel alertModel = new SystemAlertModel();

    @FXML
    public void initialize() {
        severityBox.getItems().addAll("INFO", "WARNING", "CRITICAL");
        severityBox.setValue("INFO");
        sourceField.setText("System Admin");
    }

    @FXML
    private void handleBroadcast() {
        String severity = severityBox.getValue();
        String source = sourceField.getText().trim();
        String message = messageArea.getText().trim();

        if (message.isEmpty() || source.isEmpty()) {
            DialogUtil.showError("Validation Error", "Please fill in all fields.");
            return;
        }

        if (DialogUtil.showConfirmation("Confirm Broadcast",
                "Are you sure you want to send this alert to ALL users?")) {
            try {
                alertModel.broadcastAlert(severity, message, source);
                DialogUtil.showInfo("Success", "Alert broadcasted successfully.");
                messageArea.clear();
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Error", "Failed to broadcast alert.");
            }
        }
    }

    // Navigation Handlers removed - handled by Dashboard
}
