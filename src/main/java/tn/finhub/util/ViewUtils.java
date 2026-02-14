package tn.finhub.util;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.io.IOException;

public class ViewUtils {

    /**
     * Navigates to a new view with a fade-out/fade-in transition.
     * Searches for #contentArea in the scene.
     */
    public static void setView(Node currentNode, String fxmlPath) {
        Pane contentArea = (Pane) currentNode.getScene().lookup("#contentArea");
        if (contentArea != null) {
            loadContent(contentArea, fxmlPath);
        } else {
            System.err.println("Error: #contentArea not found in scene graph.");
        }
    }

    /**
     * Navigates to a new view with a fade-out/fade-in transition.
     * Uses the provided container.
     */
<<<<<<< HEAD
    public static void loadContent(Pane targetContainer, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewUtils.class.getResource(fxmlPath));
=======
    public static FXMLLoader loadContent(Pane targetContainer, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewUtils.class.getResource(fxmlPath));
            // Load with resource bundle for internationalization
            loader.setResources(LanguageManager.getInstance().getResourceBundle());
>>>>>>> cd680ce (crud+controle de saisie)
            Parent newView = loader.load();

            if (!targetContainer.getChildren().isEmpty()) {
                Node currentView = targetContainer.getChildren().get(0);

<<<<<<< HEAD
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentView);
=======
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentView);
>>>>>>> cd680ce (crud+controle de saisie)
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    targetContainer.getChildren().setAll(newView);
                    fadeIn(newView);
                });
                fadeOut.play();
            } else {
                targetContainer.getChildren().setAll(newView);
                fadeIn(newView);
            }

<<<<<<< HEAD
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading FXML: " + fxmlPath);
=======
            return loader;

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading FXML: " + fxmlPath);
            return null;
>>>>>>> cd680ce (crud+controle de saisie)
        }
    }

    private static void fadeIn(Node node) {
        node.setOpacity(0);
<<<<<<< HEAD
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
=======
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), node);
>>>>>>> cd680ce (crud+controle de saisie)
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
}
