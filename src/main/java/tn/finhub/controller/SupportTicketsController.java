package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.finhub.model.SupportTicket;

import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class SupportTicketsController {

    @FXML
    private StackPane rootStackPane;
    @FXML
    private VBox ticketsListParams;
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

        // Header: Subject + Status + Spacer + Delete Button
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

        // Delete Button (Trash Icon)
        javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
        trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        trashIcon.setStyle("-fx-fill: -color-text-muted;");
        trashIcon.setScaleX(0.8);
        trashIcon.setScaleY(0.8);

        javafx.scene.control.Button deleteBtn = new javafx.scene.control.Button();
        deleteBtn.setGraphic(trashIcon);
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        deleteBtn.getStyleClass().add("button-icon"); // Optional if you have hover effects

        // Prevent card click when clicking delete
        deleteBtn.setOnMouseClicked(e -> {
            e.consume();
            handleDeleteTicket(ticket);
        });

        header.getChildren().addAll(subject, status, spacer, deleteBtn);

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

        // Add click handler to view details
        card.setOnMouseClicked(e -> showTicketDetails(ticket));

        return card;
    }

    private void handleDeleteTicket(SupportTicket ticket) {
        boolean confirm = DialogUtil.showConfirmation("Delete Ticket",
                "Are you sure you want to delete this ticket? All messages will be lost.");
        if (confirm) {
            try {
                supportModel.deleteTicket(ticket.getId());
                refreshTickets();
                // If successful, maybe show a small toast/notification
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Error", "Failed to delete ticket.");
            }
        }
    }

    private void showTicketDetails(SupportTicket ticket) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/user_ticket_details.fxml"));
            Region detailsView = loader.load();
            UserTicketDetailsController controller = loader.getController();

            controller.setTicket(ticket, this::closeTicketDetails);

            rootStackPane.getChildren().add(detailsView);
            ticketsListParams.setVisible(false);

        } catch (java.io.IOException e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Could not load ticket details.");
        }
    }

    private void closeTicketDetails() {
        if (rootStackPane.getChildren().size() > 1) {
            rootStackPane.getChildren().remove(rootStackPane.getChildren().size() - 1);
        }
        ticketsListParams.setVisible(true);
        refreshTickets(); // Refresh list to update status if changed
    }

    @FXML
    private void handleCreateTicket() {
        boolean success = DialogUtil.showCustomDialog("/view/support_create_ticket.fxml", "Create Ticket");
        if (success) {
            refreshTickets();
        }
    }
}
