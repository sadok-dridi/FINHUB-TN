package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.finhub.dao.SavedContactDAO;
import tn.finhub.model.SavedContact;
import tn.finhub.model.User;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;

import java.io.IOException;
import java.util.List;

public class ContactsController {

    @FXML
    private VBox contactsContainer;

    private final SavedContactDAO contactDAO = new SavedContactDAO();

    @FXML
    public void initialize() {
        loadContacts();
    }

    private void loadContacts() {
        User user = UserSession.getInstance().getUser();
        if (user == null)
            return;

        List<SavedContact> contacts = contactDAO.getContactsByUserId(user.getId());
        contactsContainer.getChildren().clear();

        if (contacts.isEmpty()) {
            Label placeholder = new Label("No saved contacts yet. Click 'Add Contact' to start.");
            placeholder.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px; -fx-padding: 20;");
            contactsContainer.getChildren().add(placeholder);
        } else {
            for (SavedContact contact : contacts) {
                contactsContainer.getChildren().add(createContactCard(contact));
            }
        }
    }

    private javafx.scene.Node createContactCard(SavedContact contact) {
        HBox card = new HBox(15);
        card.setStyle(
                "-fx-background-color: rgba(31, 41, 55, 0.7); -fx-background-radius: 12; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Avatar / Icon
        javafx.scene.layout.StackPane iconBg = new javafx.scene.layout.StackPane();
        iconBg.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-background-radius: 20;"); // Green tint
        iconBg.setPrefSize(40, 40);
        iconBg.setMaxSize(40, 40);

        Label initial = new Label(contact.getContactName().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 18px;");
        iconBg.getChildren().add(initial);

        // Details
        VBox details = new VBox(2);
        Label nameLabel = new Label(contact.getContactName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label emailLabel = new Label(contact.getContactEmail());
        emailLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");

        details.getChildren().addAll(nameLabel, emailLabel);

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Delete Button
        Button deleteBtn = new Button();
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        SVGPath trashIcon = new SVGPath();
        trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        trashIcon.setStyle("-fx-fill: #EF4444;"); // Red
        deleteBtn.setGraphic(trashIcon);

        deleteBtn.setOnAction(e -> handleDelete(contact));

        card.getChildren().addAll(iconBg, details, spacer, deleteBtn);
        return card;
    }

    private void handleDelete(SavedContact contact) {
        // Confirm delete? For now direct delete.
        contactDAO.deleteContact(contact.getId());
        loadContacts();
    }

    @FXML
    private void handleAddContact() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_contact_dialog.fxml"));
            Parent root = loader.load();

            // We need a controller for the dialog to handle "Save".
            // I'll reuse a simple controller or creating a new one.
            // For now, let's assume AddContactController exists or an inner class/callback.
            tn.finhub.controller.AddContactController controller = loader.getController();
            controller.setOnContactAdded(this::loadContacts);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Could not open add contact dialog.");
        }
    }
}
