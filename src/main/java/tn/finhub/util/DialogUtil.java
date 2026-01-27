package tn.finhub.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.finhub.controller.CustomDialogController;

import java.io.IOException;

public class DialogUtil {

    public static boolean showConfirmation(String title, String message) {
        return showDialog(title, message, "CONFIRMATION");
    }
    public static void showInfo(String title, String message) {
        showDialog(title, message, "INFO");
    }

    public static void showError(String title, String message) {
        showDialog(title, message, "ERROR");
    }

    private static boolean showDialog(String title, String message, String type) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogUtil.class.getResource("/view/CustomDialog.fxml"));
            Parent root = loader.load();
            CustomDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);

            // Set data
            controller.setDialogStage(dialogStage);

            switch (type) {
                case "ERROR" -> controller.setErrorData(title, message);
                case "INFO" -> controller.setInfoData(title, message);
                default -> controller.setConfirmationData(title, message);
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);

            dialogStage.centerOnScreen();
            dialogStage.showAndWait();

            return controller.isConfirmed();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
