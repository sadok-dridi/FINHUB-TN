package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        // Determine styling based on severity
        String borderColor = "-color-card-border";
        String icon = "ℹ";
        String titleColor = "-color-text-primary";

        if ("WARNING".equals(alert.getSeverity())) {
            borderColor = "-color-warning";
            icon = "⚠";
            titleColor = "-color-warning";
            card.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-padding: 15;");
        } else if ("CRITICAL".equals(alert.getSeverity())) {
            borderColor = "-color-danger";
            icon = "✖";
            titleColor = "-color-danger";
            card.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-padding: 15;");
        } else {
            card.setStyle("-fx-padding: 15;");
        }

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + titleColor + ";");

        Label message = new Label(alert.getMessage());
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-text-primary; -fx-font-weight: bold;");

        header.getChildren().addAll(iconLabel, message);

        String dateStr = alert.getCreatedAt() != null
                ? alert.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "";
        Label meta = new Label(alert.getSource() + " • " + dateStr);
        meta.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 11px; -fx-padding: 0 0 0 25;"); // Indent to
                                                                                                        // align with
                                                                                                        // text

        card.getChildren().addAll(header, meta);
        return card;
    }
}
