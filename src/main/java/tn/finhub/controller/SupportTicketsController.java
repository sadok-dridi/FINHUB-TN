package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.finhub.model.SupportTicket;

import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class SupportTicketsController {

    @FXML
    private VBox ticketsContainer;

    private final tn.finhub.model.SupportModel supportModel = new tn.finhub.model.SupportModel();

    // STALE-WHILE-REVALIDATE CACHE
    private static List<SupportTicket> cachedTickets = null;

    public static void setCachedTickets(List<SupportTicket> tickets) {
        cachedTickets = tickets;
    }

    @FXML
    public void initialize() {
        refreshTickets();
    }

    @FXML
    private void refreshTickets() {
        int userId = UserSession.getInstance().getUser() != null ? UserSession.getInstance().getUser().getId() : 0;

        // 0. Optimistic UI Update
        if (cachedTickets != null) {
            updateUI(cachedTickets);
        }

        // Background Task
        javafx.concurrent.Task<List<SupportTicket>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<SupportTicket> call() throws Exception {
                return supportModel.getTicketsByUserId(userId);
            }
        };

        task.setOnSucceeded(e -> {
            List<SupportTicket> tickets = task.getValue();
            cachedTickets = tickets;
            updateUI(tickets);
        });

        task.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void updateUI(List<SupportTicket> tickets) {
        ticketsContainer.getChildren().clear();

        if (tickets.isEmpty()) {
            Label emptyLabel = new Label("No tickets found. Need help? Create a new ticket.");
            emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
            ticketsContainer.getChildren().add(emptyLabel);
        } else {
            for (SupportTicket ticket : tickets) {
                ticketsContainer.getChildren().add(createTicketCard(ticket));
            }
        }
    }

    private VBox createTicketCard(SupportTicket ticket) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20; -fx-cursor: hand;");

        // Header: Subject + Status
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label subject = new Label(ticket.getSubject());
        subject.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -color-text-primary;");

        Label status = new Label(ticket.getStatus());
        String statusColor = "OPEN".equals(ticket.getStatus()) ? "-color-primary" : "-color-text-muted";
        status.setStyle("-fx-background-color: " + statusColor
                + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(subject, status, spacer);

        // Meta: Category • Date
        String dateStr = ticket.getCreatedAt() != null
                ? ticket.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
                : "Unknown";
        Label meta = new Label(ticket.getCategory() + " • " + dateStr);
        meta.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");

        // Priority Badge (if High/Urgent)
        HBox footer = new HBox(10);
        if ("HIGH".equals(ticket.getPriority()) || "URGENT".equals(ticket.getPriority())) {
            Label priority = new Label(ticket.getPriority());
            String pColor = "URGENT".equals(ticket.getPriority()) ? "-color-error" : "-color-warning";
            priority.setStyle("-fx-text-fill: " + pColor + "; -fx-border-color: " + pColor
                    + "; -fx-border-radius: 4; -fx-padding: 2 6; -fx-font-size: 10px;");
            footer.getChildren().add(priority);
        }

        card.getChildren().addAll(header, meta, footer);

        // Add click handler to view details (feature for later or simple alert for now)
        card.setOnMouseClicked(e -> {
            // For now just show details in a simple confirmation-like dialog or toast
            // In full implementation, this would open a ticket details view
            DialogUtil.showInfo("Ticket #" + ticket.getId(),
                    "Subject: " + ticket.getSubject() + "\n" +
                            "Status: " + ticket.getStatus() + "\n" +
                            "Category: " + ticket.getCategory() + "\n\n" +
                            "(Full conversation view to be implemented)");
        });

        return card;
    }

    @FXML
    private void handleCreateTicket() {
        boolean success = DialogUtil.showCustomDialog("/view/support_create_ticket.fxml", "Create Ticket");
        if (success) {
            refreshTickets();
        }
    }
}
