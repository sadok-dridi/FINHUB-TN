package tn.finhub;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import tn.finhub.controller.MainLayoutController;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainLayout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/style/theme.css").toExternalForm());

        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        stage.setTitle("FINHUB-TN");
        stage.setScene(scene);
        stage.show();

        // maximize
        //MainLayoutController controller = loader.getController();
        //controller.maximizeWindow();
    }

    public static void main(String[] args) {
        launch();
    }
}
