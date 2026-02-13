package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class SupportDashboardController {

    @FXML
    private ToggleButton btnAiAssistant;
    @FXML
    private ToggleButton btnMyTickets;
    @FXML
    private ToggleButton btnSystemAlerts;
    @FXML
    private ToggleButton btnKnowledgeBase;

    @FXML
    private Node viewAiAssistant;
    @FXML
    private Node viewMyTickets;
    @FXML
    private Node viewSystemAlerts;
    @FXML
    private Node viewKnowledgeBase;

    @FXML
    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        btnAiAssistant.setToggleGroup(group);
        btnMyTickets.setToggleGroup(group);
        btnSystemAlerts.setToggleGroup(group);
        btnKnowledgeBase.setToggleGroup(group);

        // Handle view switching
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                // Determine which was previously selected to keep it selected (prevent empty
                // selection)
                if (oldVal != null) {
                    group.selectToggle(oldVal);
                }
                return;
            }

            viewAiAssistant.setVisible(newVal == btnAiAssistant);
            viewMyTickets.setVisible(newVal == btnMyTickets);
            viewSystemAlerts.setVisible(newVal == btnSystemAlerts);
            viewKnowledgeBase.setVisible(newVal == btnKnowledgeBase);
        });

        // Default to AI Assistant
        btnAiAssistant.setSelected(true);
        viewAiAssistant.setVisible(true);
        viewMyTickets.setVisible(false);
        viewSystemAlerts.setVisible(false);
        viewKnowledgeBase.setVisible(false);
    }
}
