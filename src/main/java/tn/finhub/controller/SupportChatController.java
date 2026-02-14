package tn.finhub.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
<<<<<<< HEAD
import tn.finhub.service.ChatBotService;
=======
>>>>>>> cd680ce (crud+controle de saisie)

public class SupportChatController {

    @FXML
    private VBox messageContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField inputField;

<<<<<<< HEAD
    private final ChatBotService chatService = new ChatBotService();
=======
    private final tn.finhub.model.ChatAssistantModel chatModel = new tn.finhub.model.ChatAssistantModel();
>>>>>>> cd680ce (crud+controle de saisie)

    @FXML
    public void initialize() {
        // Add welcome message
        addMessage(
                "Hello! I'm your FinHub Assistant. How can I help you regarding your wallet, transactions, or security?",
                false);
    }

    @FXML
    private void handleSendMessage() {
        String input = inputField.getText().trim();
        if (input.isEmpty())
            return;

        // User Message
        addMessage(input, true);
        inputField.clear();

        // Simulate thinking delay then Bot Response
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
<<<<<<< HEAD
            String response = chatService.getResponse(input);
=======
            String response = chatModel.getResponse(input);
>>>>>>> cd680ce (crud+controle de saisie)
            Platform.runLater(() -> addMessage(response, false));
        }).start();
    }

    private void addMessage(String text, boolean isUser) {
        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        messageLabel.setStyle("-fx-padding: 10; -fx-background-radius: 10; -fx-text-fill: -color-text-primary;");

        HBox container = new HBox();
        if (isUser) {
            container.setAlignment(Pos.CENTER_RIGHT);
            messageLabel.setStyle(messageLabel.getStyle() + "-fx-background-color: -color-primary-subtle;");
        } else {
            container.setAlignment(Pos.CENTER_LEFT);
            messageLabel.setStyle(messageLabel.getStyle()
                    + "-fx-background-color: -color-bg-subtle; -fx-border-color: -color-card-border; -fx-border-radius: 10;");
        }

        container.getChildren().add(messageLabel);
        messageContainer.getChildren().add(container);

        // Auto scroll
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}
