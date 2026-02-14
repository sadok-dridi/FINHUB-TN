package tn.finhub.util;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class UIUtils {

    public static StackPane createCircularImage(String url, double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);

        // Base Style (Background + Glow)
        // We remove the border from here and draw it as an overlay
        container.setStyle(
                "-fx-background-color: rgba(16, 185, 129, 0.1); " +
                        "-fx-background-radius: " + size + "; " +
                        "-fx-effect: dropshadow(gaussian, rgba(16, 189, 129, 0.4), 8, 0, 0, 0);");

        try {
            Image img = ImageCache.get(url);
            ImageView imgView;

            if (img != null && !img.isError()) {
                imgView = new ImageView(img);
                // 1. Center Crop Logic (Fill the FULL size)
                if (img.getWidth() > img.getHeight()) {
                    imgView.setFitHeight(size);
                } else {
                    imgView.setFitWidth(size);
                }
                imgView.setPreserveRatio(true);
            } else {
                throw new Exception("Image fetch failed");
            }

            // 2. Image Container (Clipped)
            StackPane imgContainer = new StackPane(imgView);
            imgContainer.setPrefSize(size, size);
            imgContainer.setMaxSize(size, size);

            double radius = size / 2.0;
            imgContainer.setClip(new Circle(radius, radius, radius));

            container.getChildren().add(imgContainer);

        } catch (Exception e) {
            // Fallback: Empty container or default icon usually handled by caller or
            // background
        }

        // 3. Border Overlay (Circle)
        // This sits ON TOP of the image, covering the edges perfectly.
        double radius = size / 2.0;
        Circle borderRing = new Circle(radius, radius, radius);
        borderRing.setFill(javafx.scene.paint.Color.TRANSPARENT);
        borderRing.setStroke(javafx.scene.paint.Color.valueOf("#10B981"));
        borderRing.setStrokeWidth(2);
        // Important: Disable mouse events on the ring so clicks pass through to the
        // container/image
        borderRing.setMouseTransparent(true);

        container.getChildren().add(borderRing);

        return container;
    }
}
