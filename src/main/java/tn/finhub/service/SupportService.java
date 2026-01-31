package tn.finhub.service;

import tn.finhub.dao.KnowledgeBaseDAO;
import tn.finhub.dao.SupportMessageDAO;
import tn.finhub.dao.SupportTicketDAO;
import tn.finhub.dao.SystemAlertDAO;
import tn.finhub.model.KnowledgeBase;
import tn.finhub.model.SupportMessage;
import tn.finhub.model.SupportTicket;
import tn.finhub.model.SystemAlert;

import java.util.List;

public class SupportService {

    private final SupportTicketDAO ticketDAO = new SupportTicketDAO();
    private final SupportMessageDAO messageDAO = new SupportMessageDAO();
    private final KnowledgeBaseDAO kbDAO = new KnowledgeBaseDAO();
    private final SystemAlertDAO alertDAO = new SystemAlertDAO();

    // Ticket Management
    public void createTicket(int userId, String subject, String category, String initialMessage) {
        SupportTicket ticket = new SupportTicket(userId, subject, category, "NORMAL");
        ticketDAO.createTicket(ticket); // This sets the ID in the ticket object

        // Add initial message
        SupportMessage message = new SupportMessage(ticket.getId(), "USER", initialMessage);
        messageDAO.createMessage(message);
    }

    public List<SupportTicket> getUserTickets(int userId) {
        return ticketDAO.getTicketsByUserId(userId);
    }

    public SupportTicket getTicketDetails(int ticketId) {
        return ticketDAO.getTicketById(ticketId);
    }

    public void resolveTicket(int ticketId) {
        ticketDAO.updateTicketStatus(ticketId, "RESOLVED");
        // Add system message
        addSystemMessage(ticketId, "Ticket marked as resolved by user.");
    }

    // Message Management
    public List<SupportMessage> getTicketMessages(int ticketId) {
        return messageDAO.getMessagesByTicketId(ticketId);
    }

    public void addUserMessage(int ticketId, String content) {
        SupportMessage msg = new SupportMessage(ticketId, "USER", content);
        messageDAO.createMessage(msg);

        // Update ticket status to OPEN if it was resolved? Optional logic.
    }

    public void addSystemMessage(int ticketId, String content) {
        SupportMessage msg = new SupportMessage(ticketId, "SYSTEM", content);
        messageDAO.createMessage(msg);
    }

    // Knowledge Base
    public List<KnowledgeBase> getAllHelpArticles() {
        return kbDAO.getAllArticles();
    }

    public List<KnowledgeBase> searchHelp(String query) {
        return kbDAO.searchArticles(query);
    }

    // System Alerts
    public List<SystemAlert> getUserAlerts(int userId) {
        return alertDAO.getAlertsByUserId(userId);
    }

    // Call this from other services when an event happens
    public void createSystemAlert(int userId, String severity, String message, String source) {
        SystemAlert alert = new SystemAlert(userId, severity, message, source);
        alertDAO.createAlert(alert);
    }
}
