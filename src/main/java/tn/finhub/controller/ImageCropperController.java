package tn.finhub.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class ImageCropperController {

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private ImageView imageView;
    @FXML
    private Slider zoomSlider;
    @FXML
    private StackPane imageContainer;

    private File croppedFile;
    private Stage stage;

    @FXML
    public void initialize() {
        // Zoom Logic
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (imageView.getImage() == null)
                return;

            double zoomFactor = newVal.doubleValue();

            // Calculate scale based on initial fit dimensions
            // But simplify: FitWidth/Height directly

            double initialWidth = imageView.getImage().getWidth();
            double initialHeight = imageView.getImage().getHeight();

            // We need a base scale to fit 300px
            double baseScale = Math.max(300 / initialWidth, 300 / initialHeight);

            imageView.setFitWidth(initialWidth * baseScale * zoomFactor);
            imageView.setFitHeight(initialHeight * baseScale * zoomFactor);
        });

        // Hide scrollbars but allow panning
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setImage(File file) {
        Image image = new Image(file.toURI().toString());
        imageView.setImage(image);

        // Initial Fit: Scale image so at least one dimension fits the 300px viewport
        // Logic: specific side matches viewport size so it covers it fully.
        double viewportSize = 300;

        double width = image.getWidth();
        double height = image.getHeight();

        double scale = Math.max(viewportSize / width, viewportSize / height);

        imageView.setFitWidth(width * scale);
        imageView.setFitHeight(height * scale);
        imageView.setPreserveRatio(true);

        // Reset Zoom Slider
        zoomSlider.setValue(1.0);

        // Center the image in the viewport (UX Request: "Me choose where to crop",
        // starting center is best)
        javafx.application.Platform.runLater(() -> {
            scrollPane.setHvalue(0.5);
            scrollPane.setVvalue(0.5);
        });
    }

    public File getCroppedFile() {
        return croppedFile;
    }

    @FXML
    private void handleSave() {
        try {
            // Snapshot the visible area of the ScrollPane's content (The ImageContainer)
            // The Bounds of the Viewport relative to the Content
            Bounds viewportBounds = scrollPane.getViewportBounds();

            SnapshotParameters snapParams = new SnapshotParameters();
            snapParams.setViewport(new Rectangle2D(
                    Math.abs(viewportBounds.getMinX()),
                    Math.abs(viewportBounds.getMinY()),
                    300, 300));

            // Snapshot the content node (StackPane containing ImageView)
            WritableImage croppedImage = imageContainer.snapshot(snapParams, null);

            // Save to Temp File
            File tempFile = File.createTempFile("crop_" + System.currentTimeMillis(), ".png");
            ImageIO.write(SwingFXUtils.fromFXImage(croppedImage, null), "png", tempFile);

            this.croppedFile = tempFile;
            if (stage != null)
                stage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        this.croppedFile = null;
        if (stage != null)
            stage.close();
    }
}
