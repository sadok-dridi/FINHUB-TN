package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
<<<<<<< HEAD
import tn.finhub.service.SupportService;
=======

>>>>>>> cd680ce (crud+controle de saisie)
import tn.finhub.util.UserSession;
import tn.finhub.util.DialogUtil;

public class CreateTicketController {

    @FXML
    private TextField subjectField;
    @FXML
    private ComboBox<String> categoryCombo;
    @FXML
    private TextArea messageArea;

<<<<<<< HEAD
    private final SupportService supportService = new SupportService();
=======
    private final tn.finhub.model.SupportModel supportModel = new tn.finhub.model.SupportModel();
>>>>>>> cd680ce (crud+controle de saisie)

    @FXML
    public void initialize() {
        categoryCombo.getItems().addAll("WALLET", "TRANSACTION", "AUTH", "SYSTEM", "OTHER");
        categoryCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSubmit() {
        String subject = subjectField.getText().trim();
        String category = categoryCombo.getValue();
        String message = messageArea.getText().trim();

        if (subject.isEmpty() || message.isEmpty()) {
            DialogUtil.showError("Missing Information", "Please fill in all fields.");
            return;
        }

        int userId = UserSession.getInstance().getUser() != null ? UserSession.getInstance().getUser().getId() : 0;

        try {
<<<<<<< HEAD
            supportService.createTicket(userId, subject, category, message);
=======
            supportModel.createTicketWithInitialMessage(userId, subject, category, message);
>>>>>>> cd680ce (crud+controle de saisie)
            DialogUtil.setLastDialogResult(true); // Signal success
            closeDialog();
        } catch (Exception e) {
            DialogUtil.showError("Error", "Failed to create ticket: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        DialogUtil.setLastDialogResult(false);
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) subjectField.getScene().getWindow();
        stage.close();
    }
}
