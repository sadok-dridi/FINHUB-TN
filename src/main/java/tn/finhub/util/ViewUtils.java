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
     *
     * @param currentNode A node in the current scene, used to find the content
     *                    area.
     * @param fxmlPath    The path to the FXML file to load.
     */
    public static void setView(Node currentNode, String fxmlPath) {
        try {
            // Find the content area (StackPane)
            Pane contentArea = (Pane) currentNode.getScene().lookup("#contentArea");

            if (contentArea == null) {
                System.err.println("Error: #contentArea not found in scene graph.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(ViewUtils.class.getResource(fxmlPath));
            Parent newView = loader.load();

            if (!contentArea.getChildren().isEmpty()) {
                Node currentView = contentArea.getChildren().get(0);

                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentView);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(newView);
                    fadeIn(newView);
                });
                fadeOut.play();
            } else {
                contentArea.getChildren().setAll(newView);
                fadeIn(newView);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading FXML: " + fxmlPath);
        }
    }

    private static void fadeIn(Node node) {
        node.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
}
