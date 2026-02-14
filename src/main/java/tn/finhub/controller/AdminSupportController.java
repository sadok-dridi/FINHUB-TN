package tn.finhub.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.finhub.model.SupportModel;
import tn.finhub.model.SupportTicket;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.SessionManager;
import tn.finhub.util.ViewUtils;

import java.util.List;

public class AdminSupportController {

    @FXML
    private TableView<SupportTicket> ticketsTable;
    @FXML
    private TableColumn<SupportTicket, Integer> colId;
    @FXML
    private TableColumn<SupportTicket, String> colSubject;
    @FXML
    private TableColumn<SupportTicket, String> colStatus;
    @FXML
    private TableColumn<SupportTicket, String> colPriority;
    @FXML
    private TableColumn<SupportTicket, String> colDate;

    @FXML
    private VBox chatArea;
    @FXML
    private Label ticketTitleLabel;
    @FXML
    private TextArea replyArea;
    @FXML
    private Button sendButton;
    @FXML
    private Button closeTicketButton;

    private final SupportModel supportModel = new SupportModel();
    private SupportTicket selectedTicket;
    private ObservableList<SupportTicket> ticketsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadTickets();

        // Initial state
        chatArea.setVisible(false);
        replyArea.setDisable(true);
        sendButton.setDisable(true);
        closeTicketButton.setDisable(true);
    }

    private void setupTable() {
        colId.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
        colSubject.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSubject()));
        colStatus.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
        colPriority.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPriority()));
        colDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt().toString()));

        ticketsTable.setItems(ticketsList);

        ticketsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectTicket(newVal);
            }
        });
    }

    private void loadTickets() {
        ticketsList.clear();
        List<SupportTicket> allTickets = supportModel.getAllTickets();
        ticketsList.addAll(allTickets);
    }

    private void selectTicket(SupportTicket ticket) {
        this.selectedTicket = ticket;
        chatArea.setVisible(true);
        ticketTitleLabel.setText("Ticket #" + ticket.getId() + ": " + ticket.getSubject());

        boolean isClosed = "CLOSED".equals(ticket.getStatus()) || "RESOLVED".equals(ticket.getStatus());
        replyArea.setDisable(isClosed);
        sendButton.setDisable(isClosed);
        closeTicketButton.setDisable(isClosed);

        loadMessages(ticket.getId());
    }

    private void loadMessages(int ticketId) {
        // Clear previous chat view logic here if we had a chat container
        // For simplicity in this admin view, we might just show the last messages or a
        // full chat view?
        // Let's assume we just want to reply.
        // Ideally, we'd list messages. For now, let's keep it simple: just list them in
        // console or simple text area?
        // Let's fetch them but maybe we need a ListView in the FXML to show them.
    }

    @FXML
    private void handleSendReply() {
        String content = replyArea.getText().trim();
        if (content.isEmpty() || selectedTicket == null)
            return;

        try {
            supportModel.addSystemMessage(selectedTicket.getId(), content);
            replyArea.clear();
            DialogUtil.showInfo("Success", "Reply sent.");
            // Refresh logic if needed
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Failed to send reply.");
        }
    }

    @FXML
    private void handleCloseTicket() {
        if (selectedTicket == null)
            return;
        try {
            supportModel.resolveTicket(selectedTicket.getId());
            DialogUtil.showInfo("Success", "Ticket resolved.");
            loadTickets(); // Refresh table
            selectTicket(selectedTicket); // Refresh UI state
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Failed to close ticket.");
        }
    }

    @FXML
    private void handleRefresh() {
        loadTickets();
    }

    // Navigation Handlers removed - handled by Dashboard
}
