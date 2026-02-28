package tn.finhub.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import tn.finhub.model.SupportMessage;
import tn.finhub.model.SupportModel;
import tn.finhub.model.SupportTicket;
import tn.finhub.util.DialogUtil;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminTicketDetailsController {

    @FXML
    private Label subjectLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label userLabel;
    @FXML
    private VBox messagesContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField replyField;
    @FXML
    private Button sendButton;
    @FXML
    private Button resolveButton;

    private SupportTicket currentTicket;
    private Runnable onBackAction;
    private final SupportModel supportModel = new SupportModel();

    public void setTicket(SupportTicket ticket, Runnable onBack) {
        this.currentTicket = ticket;
        this.onBackAction = onBack;

        if (ticket != null) {
            subjectLabel.setText(ticket.getSubject());
            statusLabel.setText(ticket.getStatus());
            userLabel.setText("User ID: " + ticket.getUserId());

            String style = "-fx-text-fill: white; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;";
            if ("OPEN".equals(ticket.getStatus())) {
                style += "-fx-background-color: -color-primary;";
            } else if ("RESOLVED".equals(ticket.getStatus())) {
                style += "-fx-background-color: -color-success;";
            } else {
                style += "-fx-background-color: -color-text-muted;";
            }
            statusLabel.setStyle(style);

            boolean isClosed = "CLOSED".equals(ticket.getStatus()) || "RESOLVED".equals(ticket.getStatus());
            replyField.setDisable(isClosed);
            sendButton.setDisable(isClosed);
            resolveButton.setDisable(isClosed);

            loadMessages();
        }
    }

    private void loadMessages() {
        messagesContainer.getChildren().clear();
        List<SupportMessage> messages = supportModel.getTicketMessages(currentTicket.getId());
        for (SupportMessage msg : messages) {
            addMessageCard(msg);
        }
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void addMessageCard(SupportMessage msg) {
        boolean isAdmin = !"USER".equals(msg.getSenderRole());

        HBox row = new HBox();
        row.setAlignment(isAdmin ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(5);
        bubble.setMaxWidth(400);

        String bgStyle = isAdmin
                ? "-fx-background-color: -color-primary; -fx-background-radius: 15 15 0 15;"
                : "-fx-background-color: -color-bg-subtle; -fx-background-radius: 15 15 15 0;";

        bubble.setStyle(bgStyle + "-fx-padding: 12;");

        Label text = new Label(msg.getMessage());
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: " + (isAdmin ? "white" : "-color-text-primary") + "; -fx-font-size: 13px;");

        bubble.getChildren().add(text);

        // Attachment
        if (msg.getAttachmentPath() != null && !msg.getAttachmentPath().isEmpty()) {
            try {
                File imgFile = new File(msg.getAttachmentPath());
                if (imgFile.exists()) {
                    Image image = new Image(imgFile.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(200);
                    imageView.setPreserveRatio(true);
                    imageView.setCursor(javafx.scene.Cursor.HAND);
                    // Click to enlarge
                    imageView.setOnMouseClicked(e -> showImageModal(image));

                    bubble.getChildren().add(imageView);
                }
            } catch (Exception e) {
                // Ignore invalid images
            }
        }

        String dateStr = msg.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        dateLabel.setAlignment(isAdmin ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        dateLabel.setMaxWidth(Double.MAX_VALUE);

        VBox col = new VBox(2);
        col.setAlignment(isAdmin ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        col.getChildren().addAll(bubble, dateLabel);

        row.getChildren().add(col);
        messagesContainer.getChildren().add(row);
    }

    private void showImageModal(Image image) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("Image Viewer");

        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(800);
        imageView.setFitHeight(600);

        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(imageView);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.9); -fx-padding: 20;");
        // Close on click
        root.setOnMouseClicked(e -> stage.close());

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleSend() {
        String text = replyField.getText().trim();
        if (text.isEmpty() || currentTicket == null)
            return;

        try {
            supportModel.addSystemMessage(currentTicket.getId(), text);
            replyField.clear();
            loadMessages();
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Failed to send message.");
        }
    }

    @FXML
    private void handleResolve() {
        if (currentTicket == null)
            return;
        boolean confirm = DialogUtil.showConfirmation("Resolve Ticket", "Mark this ticket as resolved?");
        if (confirm) {
            supportModel.resolveTicket(currentTicket.getId());
            setTicket(supportModel.getTicketById(currentTicket.getId()), onBackAction); // Reload
            DialogUtil.showInfo("Success", "Ticket resolved.");
        }
    }

    @FXML
    private void handleBack() {
        if (onBackAction != null) {
            onBackAction.run();
        }
    }
}
