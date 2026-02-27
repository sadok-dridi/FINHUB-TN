package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import tn.finhub.model.KYCModel;
import tn.finhub.model.KYCRequest;
import tn.finhub.model.User;
import tn.finhub.util.CloudinaryService;
import tn.finhub.util.UserSession;
import tn.finhub.util.ViewUtils;

import java.io.File;

public class KYCSubmissionController {

    @FXML
    private ComboBox<String> documentTypeBox;
    @FXML
    private Label fileLabel;
    @FXML
    private Button submitButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Button backButton;

    private File selectedFile;
    private KYCModel kycModel = new KYCModel();

    @FXML
    public void initialize() {
        documentTypeBox.getItems().addAll("ID_CARD", "VIDEO");
        documentTypeBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Document");

        String type = documentTypeBox.getValue();
        if ("VIDEO".equals(type)) {
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mov", "*.avi"));
        } else {
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        }

        selectedFile = fileChooser.showOpenDialog(fileLabel.getScene().getWindow());

        if (selectedFile != null) {
            fileLabel.setText(selectedFile.getName());
            submitButton.setDisable(false);
        }
    }

    @FXML
    private void handleSubmit() {
        if (selectedFile == null)
            return;

        submitButton.setDisable(true);
        statusLabel.setText("Uploading...");
        statusLabel.setVisible(true);
        statusLabel.setStyle("-fx-text-fill: -color-text-primary;");

        new Thread(() -> {
            try {
                // Upload
                String folder = "kyc/" + documentTypeBox.getValue().toLowerCase();
                String url = CloudinaryService.upload(selectedFile, folder);

                // Create Request
                User user = UserSession.getInstance().getUser();
                KYCRequest request = new KYCRequest();
                request.setUserId(user.getId());
                request.setDocumentType(documentTypeBox.getValue());
                request.setDocumentUrl(url);
                request.setStatus("PENDING");

                kycModel.createRequest(request);

                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Submission Successful! Pending Admin Approval.");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    fileLabel.setText("No file selected");
                    selectedFile = null;
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    submitButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        ViewUtils.loadContent(
                (javafx.scene.layout.StackPane) backButton.getScene().getRoot().lookup("#dashboardContent"),
                "/view/profile.fxml");
    }
}
