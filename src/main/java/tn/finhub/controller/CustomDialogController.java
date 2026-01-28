package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

public class CustomDialogController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Button confirmButton;

    @FXML
    private Button cancelButton;

    @FXML
    private VBox iconContainer;

    @FXML
    private SVGPath iconPath;

    private boolean confirmed = false;
    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setConfirmationData(String title, String message) {
        titleLabel.setText(title);
        messageLabel.setText(message);

        // Confirmation Style (Red/Warning usually for Delete, or Primary for others?
        // let's stick to standard warning for this user case)
        // Check if title mentions 'Delete' or 'Warning'
        if (title.toLowerCase().contains("delete") || title.toLowerCase().contains("warning")) {
            setStyleWarning();
        } else {
            setStyleInfo();
        }

        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
        confirmButton.setText("Yes");
        cancelButton.setText("No");
    }

    public void setInfoData(String title, String message) {
        titleLabel.setText(title);
        messageLabel.setText(message);

        setStyleInfo();

        cancelButton.setVisible(false);
        cancelButton.setManaged(false);

        confirmButton.setText("OK");
    }

    public void setErrorData(String title, String message) {
        titleLabel.setText(title);
        messageLabel.setText(message);

        setStyleError();

        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        confirmButton.setText("OK");
    }

    private void setStyleWarning() {
        // Red Icon
        iconContainer.getStyleClass().setAll("dialog-icon-container", "dialog-icon-warning");
        iconPath.setContent(
                "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"); // Triangle
                                                                                                                                                         // Warning
        iconPath.setStroke(javafx.scene.paint.Color.web("#ef4444"));
    }

    private void setStyleError() {
        // Red Icon X
        iconContainer.getStyleClass().setAll("dialog-icon-container", "dialog-icon-error");
        iconPath.setContent("M18 6L6 18M6 6l12 12"); // X
        iconPath.setStroke(javafx.scene.paint.Color.web("#ef4444"));
    }

    private void setStyleInfo() {
        // Primary Color Icon (Violet)
        iconContainer.getStyleClass().setAll("dialog-icon-container", "dialog-icon-info");
        iconPath.setContent("M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"); // Info Circle
        iconPath.setStroke(javafx.scene.paint.Color.web("#8B5CF6"));
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void handleYes() {
        confirmed = true;
        dialogStage.close();
    }

    @FXML
    private void handleNo() {
        confirmed = false;
        dialogStage.close();
    }
}
