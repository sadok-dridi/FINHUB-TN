package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import tn.finhub.model.SupportModel;
import tn.finhub.model.SupportTicket;
import tn.finhub.util.DialogUtil;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminSupportController {

    @FXML
    private StackPane rootStackPane;
    @FXML
    private VBox ticketsListContainer;
    @FXML
    private VBox ticketsContainer;

    private final SupportModel supportModel = new SupportModel();

    private static List<SupportTicket> cachedTickets;

    public static void setCachedTickets(List<SupportTicket> tickets) {
        cachedTickets = tickets;
    }

    @FXML
    public void initialize() {
        loadTickets();
    }

    private void loadTickets() {
        ticketsContainer.getChildren().clear();
        List<SupportTicket> allTickets;

        if (cachedTickets != null) {
            allTickets = cachedTickets;
            cachedTickets = null; // Consume cache so refresh works normally next time
        } else {
            allTickets = supportModel.getAllTickets();
        }

        if (allTickets.isEmpty()) {
            Label emptyLabel = new Label("No active tickets.");
            emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 20;");
            ticketsContainer.getChildren().add(emptyLabel);
        } else {
            for (SupportTicket ticket : allTickets) {
                ticketsContainer.getChildren().add(createTicketCard(ticket));
            }
        }
    }

    private HBox createTicketCard(SupportTicket ticket) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> showTicketDetails(ticket));

        // Icon based on status
        SVGPath statusIcon = new SVGPath();
        String statusColor;

        switch (ticket.getStatus()) {
            case "OPEN":
                statusIcon.setContent(
                        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"); // Info
                statusColor = "-color-primary";
                break;
            case "RESOLVED":
            case "CLOSED":
                statusIcon.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"); // Check
                statusColor = "-color-success";
                break;
            default:
                statusIcon.setContent(
                        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z");
                statusColor = "-color-text-muted";
                break;
        }

        if (ticket.getStatus().equals("RESOLVED")) {
            statusIcon.setStyle("-fx-fill: #10B981;");
        } else if (ticket.getStatus().equals("CLOSED")) {
            statusIcon.setStyle("-fx-fill: -color-text-muted;");
        } else {
            statusIcon.setStyle("-fx-fill: " + statusColor + ";");
        }

        VBox details = new VBox(5);
        Label subject = new Label(ticket.getSubject());
        subject.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-text-primary;");

        String metaText = String.format("#%d • %s • %s",
                ticket.getId(),
                ticket.getPriority(),
                ticket.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd")));
        Label meta = new Label(metaText);
        meta.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 12px;");

        details.getChildren().addAll(subject, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Badge
        Label statusBadge = new Label(ticket.getStatus());
        String badgeStyle = "-fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;";
        if ("OPEN".equals(ticket.getStatus())) {
            badgeStyle += "-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: -color-primary;";
        } else if ("RESOLVED".equals(ticket.getStatus())) {
            badgeStyle += "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10B981;";
        } else {
            badgeStyle += "-fx-background-color: rgba(107, 114, 128, 0.2); -fx-text-fill: -color-text-muted;";
        }
        statusBadge.setStyle(badgeStyle);

        card.getChildren().addAll(statusIcon, details, spacer, statusBadge);
        return card;
    }

    private void showTicketDetails(SupportTicket ticket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_ticket_details.fxml"));
            Region detailsView = loader.load();
            AdminTicketDetailsController controller = loader.getController();

            controller.setTicket(ticket, this::closeTicketDetails);

            rootStackPane.getChildren().add(detailsView);
            ticketsListContainer.setVisible(false);

        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Could not load ticket details.");
        }
    }

    private void closeTicketDetails() {
        if (rootStackPane.getChildren().size() > 1) {
            rootStackPane.getChildren().remove(rootStackPane.getChildren().size() - 1);
        }
        ticketsListContainer.setVisible(true);
        loadTickets(); // Refresh list to reflect interaction
    }

    @FXML
    private void handleRefresh() {
        loadTickets();
    }
}
