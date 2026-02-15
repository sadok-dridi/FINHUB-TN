package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import tn.finhub.model.SystemAlert;
import tn.finhub.model.SystemAlertModel;
import tn.finhub.util.DialogUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminAlertsController {

    @FXML
    private ComboBox<String> severityBox;
    @FXML
    private TextField sourceField;
    @FXML
    private TextArea messageArea;
    @FXML
    private VBox broadcastsList;

    private final SystemAlertModel alertModel = new SystemAlertModel();

    private static List<SystemAlert> cachedAlerts;

    public static void setCachedAlerts(List<SystemAlert> alerts) {
        cachedAlerts = alerts;
    }

    @FXML
    public void initialize() {
        severityBox.getItems().addAll("INFO", "WARNING", "CRITICAL");
        severityBox.setValue("INFO");
        sourceField.setText("System Admin");
        refreshBroadcasts();
    }

    @FXML
    private void handleRefresh() {
        refreshBroadcasts();
    }

    private void refreshBroadcasts() {
        if (broadcastsList == null)
            return;
        broadcastsList.getChildren().clear();

        List<SystemAlert> alerts;
        if (cachedAlerts != null) {
            alerts = cachedAlerts;
            cachedAlerts = null;
        } else {
            alerts = alertModel.getAllBroadcasts();
        }

        if (alerts.isEmpty()) {
            Label emptyLabel = new Label("No active broadcasts.");
            emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 10;");
            broadcastsList.getChildren().add(emptyLabel);
        } else {
            for (SystemAlert alert : alerts) {
                broadcastsList.getChildren().add(createAlertCard(alert));
            }
        }
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
                refreshBroadcasts();
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Error", "Failed to broadcast alert.");
            }
        }
    }

    private VBox createAlertCard(SystemAlert alert) {
        VBox card = new VBox(5);
        card.setStyle(
                "-fx-background-color: -color-card-bg; -fx-background-radius: 12; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");

        // Styling based on severity (copied from SupportAlertsController)
        String borderColor;
        String iconContent;
        String titleColor;
        String severity = alert.getSeverity() != null ? alert.getSeverity().toUpperCase() : "INFO";

        switch (severity) {
            case "WARNING":
                borderColor = "-color-warning";
                iconContent = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
                titleColor = "-color-warning";
                break;
            case "CRITICAL":
            case "ERROR":
                borderColor = "-color-error";
                iconContent = "M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z";
                titleColor = "-color-error";
                break;
            default: // INFO
                borderColor = "-color-primary";
                iconContent = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
                titleColor = "-color-primary";
                break;
        }

        // Apply Left Border
        card.setStyle(card.getStyle() + "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = new SVGPath();
        icon.setContent(iconContent);
        icon.setStyle("-fx-fill: " + titleColor + ";");
        icon.setScaleX(1.1);
        icon.setScaleY(1.1);

        Label message = new Label(alert.getMessage());
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 15px; -fx-text-fill: -color-text-primary; -fx-font-weight: bold;");
        message.setMaxWidth(400); // Prevent label from growing too wide

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Delete Button
        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().add("button-icon");
        deleteBtn.setStyle("-fx-text-fill: -color-error;");
        SVGPath trashIcon = new SVGPath();
        trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        trashIcon.setStyle("-fx-fill: -color-error;");
        deleteBtn.setGraphic(trashIcon);
        deleteBtn.setTooltip(new Tooltip("Delete Broadcast"));
        deleteBtn.setOnAction(e -> {
            if (DialogUtil.showConfirmation("Delete Broadcast",
                    "This will remove the alert for ALL users. Continue?")) {
                alertModel.deleteBroadcast(alert.getMessage(), alert.getCreatedAt());
                refreshBroadcasts();
            }
        });

        header.getChildren().addAll(icon, message, spacer, deleteBtn);

        String dateStr = alert.getCreatedAt() != null
                ? alert.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
                : "";
        String source = alert.getSource() != null ? alert.getSource() : "System";
        Label meta = new Label(source + " • " + dateStr);
        meta.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 12px; -fx-padding: 0 0 0 30;");

        card.getChildren().addAll(header, meta);
        return card;
    }
}
