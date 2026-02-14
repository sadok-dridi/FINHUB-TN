package tn.finhub;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import tn.finhub.controller.MainLayoutController;
<<<<<<< HEAD
=======
import tn.finhub.util.LanguageManager;
>>>>>>> cd680ce (crud+controle de saisie)

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainLayout.fxml"));
<<<<<<< HEAD
=======
        // Load with resource bundle for internationalization
        loader.setResources(LanguageManager.getInstance().getResourceBundle());
>>>>>>> cd680ce (crud+controle de saisie)
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/style/theme.css").toExternalForm());

        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        stage.setTitle("FINHUB-TN");
        stage.setScene(scene);
        stage.show();

        // maximize
<<<<<<< HEAD
        //MainLayoutController controller = loader.getController();
        //controller.maximizeWindow();
=======
        // MainLayoutController controller = loader.getController();
        // controller.maximizeWindow();
>>>>>>> cd680ce (crud+controle de saisie)
    }

    public static void main(String[] args) {
        launch();
    }
}
