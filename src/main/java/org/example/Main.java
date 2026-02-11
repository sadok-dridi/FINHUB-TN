package org.example;

import org.example.utils.MyDB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Test connexion BDD
            MyDB.getConnection();

            // Charger le FXML avec le bon chemin
            URL fxmlUrl = getClass().getResource("/Login.fxml");

            if (fxmlUrl == null) {
                System.err.println("ERREUR CRITIQUE: Fichier Login.fxml introuvable!");
                System.err.println("Chemin recherché: /com/userapp/view/Login.fxml");
                System.err.println("Vérifiez que le dossier resources est bien marqué comme Resources Root");
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);

            Scene scene = new Scene(root);

            // Charger le CSS
            URL cssUrl = getClass().getResource("/com/userapp/view/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setTitle("Gestion des Utilisateurs - Connexion");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(500);
            primaryStage.setMinHeight(600);
            primaryStage.show();

            System.out.println("Application démarrée avec succès!");

        } catch (Exception e) {
            System.err.println("Erreur au démarrage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}