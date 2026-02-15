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
import tn.finhub.model.SupportMessage;
import tn.finhub.model.SupportModel;
import tn.finhub.model.SupportTicket;
import tn.finhub.util.DialogUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;

import java.io.File;
import javafx.stage.FileChooser;

public class UserTicketDetailsController {

    @FXML
    private Label subjectLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label categoryLabel;
    @FXML
    private VBox messagesContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField replyField;
    @FXML
    private Button sendButton;

    private SupportTicket currentTicket;
    private Runnable onBackAction;
    private final SupportModel supportModel = new SupportModel();

    public void setTicket(SupportTicket ticket, Runnable onBack) {
        this.currentTicket = ticket;
        this.onBackAction = onBack;

        if (ticket != null) {
            updateHeader();
            loadMessages();
        }
    }

    private void updateHeader() {
        subjectLabel.setText(currentTicket.getSubject());
        categoryLabel.setText(currentTicket.getCategory());
        statusLabel.setText(currentTicket.getStatus());

        String statusColor = "OPEN".equals(currentTicket.getStatus()) ? "-color-primary" : "-color-text-muted";
        if ("RESOLVED".equals(currentTicket.getStatus()) || "CLOSED".equals(currentTicket.getStatus())) {
            statusColor = "-color-success";
            replyField.setDisable(true);
            sendButton.setDisable(true);
            replyField.setPromptText("This ticket is closed.");
        } else {
            replyField.setDisable(false);
            sendButton.setDisable(false);
            replyField.setPromptText("Type your reply...");
        }

        statusLabel.setStyle("-fx-background-color: " + statusColor
                + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");
    }

    private void loadMessages() {
        messagesContainer.getChildren().clear();
        List<SupportMessage> messages = supportModel.getTicketMessages(currentTicket.getId());

        for (SupportMessage msg : messages) {
            addMessageCard(msg);
        }

        // Scroll to bottom
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void addMessageCard(SupportMessage msg) {
        boolean isUser = "USER".equals(msg.getSenderRole());

        VBox messageBox = new VBox(2);
        messageBox.setMaxWidth(300);

        // Image Attachment
        if (msg.getAttachmentPath() != null && !msg.getAttachmentPath().isEmpty()) {
            try {
                File imgFile = new File(msg.getAttachmentPath());
                if (imgFile.exists()) {
                    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(
                            new javafx.scene.image.Image(imgFile.toURI().toString()));
                    imageView.setFitWidth(200);
                    imageView.setPreserveRatio(true);
                    imageView.setStyle(
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2); -fx-cursor: hand;");

                    // Click to enlarge
                    imageView.setOnMouseClicked(e -> showEnlargedImage(imageView.getImage()));

                    messageBox.getChildren().add(imageView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Label content = new Label(msg.getMessage());
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: -color-text-primary; -fx-font-size: 14px;");

        Label meta = new Label(
                msg.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));

        // Meta style common
        meta.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 10px; -fx-opacity: 0.7;");

        messageBox.getChildren().addAll(content, meta);

        HBox container = new HBox();

        if (isUser) {
            container.setAlignment(Pos.CENTER_RIGHT);
            // Match SupportChatController style for User
            messageBox.setStyle(
                    "-fx-background-color: -color-primary-subtle; -fx-padding: 10; -fx-background-radius: 10;");
            messageBox.setAlignment(Pos.TOP_RIGHT); // Align date to right
        } else {
            container.setAlignment(Pos.CENTER_LEFT);
            // Match SupportChatController style for System
            messageBox.setStyle(
                    "-fx-background-color: -color-bg-subtle; -fx-padding: 10; -fx-background-radius: 10; -fx-border-color: -color-card-border; -fx-border-radius: 10;");
            messageBox.setAlignment(Pos.TOP_LEFT);
        }

        container.getChildren().add(messageBox);
        messagesContainer.getChildren().add(container);
    }

    private File selectedImageFile;

    @FXML
    private void handleAttachImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fileChooser.showOpenDialog(replyField.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            replyField.setPromptText("Image selected: " + file.getName());
        }
    }

    @FXML
    private void handleSend() {
        String content = replyField.getText().trim();
        // Allow sending if there's text OR an image
        if ((content.isEmpty() && selectedImageFile == null) || currentTicket == null)
            return;

        try {
            if (selectedImageFile != null) {
                // Save image to local directory
                String fileName = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                File destDir = new File("uploads/support");
                if (!destDir.exists())
                    destDir.mkdirs();
                File destFile = new File(destDir, fileName);

                // Simple file copy
                java.nio.file.Files.copy(selectedImageFile.toPath(), destFile.toPath());

                supportModel.addUserMessageWithImage(currentTicket.getId(), content, destFile.getPath());
                selectedImageFile = null;
                replyField.setPromptText("Type your reply...");
            } else {
                supportModel.addUserMessage(currentTicket.getId(), content);
            }

            replyField.clear();
            loadMessages(); // Reload to show new message
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Failed to send message: " + e.getMessage());
        }
    }

    private void showEnlargedImage(javafx.scene.image.Image image) {
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
    private void handleBack() {
        if (onBackAction != null) {
            onBackAction.run();
        }
    }
}
