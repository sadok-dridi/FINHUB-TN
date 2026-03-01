package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class AdminSupportHubController {

    @FXML
    private ToggleButton btnSupport;
    @FXML
    private ToggleButton btnAlerts;
    @FXML
    private ToggleButton btnKYC;
    @FXML
    private ToggleButton btnKnowledgeBase;

    @FXML
    private Node viewSupport;
    @FXML
    private Node viewAlerts;
    @FXML
    private Node viewKYC;
    @FXML
    private Node viewKnowledgeBase;

    @FXML
    public void initialize() {
        ToggleGroup group = new ToggleGroup();

        btnSupport.setToggleGroup(group);
        btnAlerts.setToggleGroup(group);
        btnKYC.setToggleGroup(group);
        btnKnowledgeBase.setToggleGroup(group);

        // Handle view switching
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                if (oldVal != null) {
                    group.selectToggle(oldVal);
                }
                return;
            }

            viewSupport.setVisible(newVal == btnSupport);
            viewAlerts.setVisible(newVal == btnAlerts);
            viewKYC.setVisible(newVal == btnKYC);
            viewKnowledgeBase.setVisible(newVal == btnKnowledgeBase);
        });

        // Default valid selection
        btnSupport.setSelected(true);
        viewSupport.setVisible(true);
        viewAlerts.setVisible(false);
        viewKYC.setVisible(false);
        viewKnowledgeBase.setVisible(false);
    }
}
