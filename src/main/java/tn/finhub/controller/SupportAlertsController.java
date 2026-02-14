package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import tn.finhub.model.SystemAlert;

import tn.finhub.util.UserSession;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class SupportAlertsController {

    @FXML
    private VBox alertsContainer;

    private final tn.finhub.model.SystemAlertModel alertModel = new tn.finhub.model.SystemAlertModel();

    @FXML
    public void initialize() {
        refreshAlerts();
    }

    @FXML
    private void refreshAlerts() {
        alertsContainer.getChildren().clear();
        int userId = UserSession.getInstance().getUser() != null ? UserSession.getInstance().getUser().getId() : 0;
        List<SystemAlert> alerts = alertModel.getAlertsByUserId(userId);

        if (alerts.isEmpty()) {
            Label emptyLabel = new Label("No system alerts.");
            emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
            alertsContainer.getChildren().add(emptyLabel);
        } else {
            for (SystemAlert alert : alerts) {
                alertsContainer.getChildren().add(createAlertCard(alert));
            }
        }
    }

    private VBox createAlertCard(SystemAlert alert) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");

        // Determine styling based on severity
        String borderColor;
        String iconContent;
        String titleColor;

        String severity = alert.getSeverity() != null ? alert.getSeverity().toUpperCase() : "INFO";

        switch (severity) {
            case "WARNING":
                borderColor = "-color-warning";
                // Warning Triangle
                iconContent = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
                titleColor = "-color-warning";
                break;
            case "CRITICAL":
            case "ERROR":
                borderColor = "-color-error";
                // X Circle
                iconContent = "M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z";
                titleColor = "-color-error";
                break;
            default: // INFO
                borderColor = "-color-primary";
                // Info Circle
                iconContent = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
                titleColor = "-color-primary";
                break;
        }

        // Apply Left Border to ALL cards for consistent "Warning Card" look
        card.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-padding: 15;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = new SVGPath();
        icon.setContent(iconContent);
        icon.setStyle("-fx-fill: " + titleColor + ";");
        icon.setScaleX(1.1);
        icon.setScaleY(1.1);

        Label message = new Label(alert.getMessage());
        message.setWrapText(true);
        // Improved text style: slightly larger, clean
        message.setStyle("-fx-font-size: 15px; -fx-text-fill: -color-text-primary; -fx-font-weight: bold;");

        header.getChildren().addAll(icon, message);

        String dateStr = alert.getCreatedAt() != null
                ? alert.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
                : "";

        String source = alert.getSource() != null ? alert.getSource() : "System";
        Label meta = new Label(source + " • " + dateStr);
        // Align meta with text (approximate icon width + gap padding)
        meta.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 12px; -fx-padding: 0 0 0 30;");

        card.getChildren().addAll(header, meta);
        return card;
    }
}
