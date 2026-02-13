package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

public class MainLayoutController {

    @FXML
    private javafx.scene.layout.BorderPane mainContainer;
    @FXML
    private javafx.scene.layout.BorderPane innerContainer;
    @FXML
    private javafx.scene.layout.StackPane contentArea;
    @FXML
    private javafx.scene.layout.HBox titleBar;
    @FXML
    private javafx.scene.layout.Pane backgroundAnimationPane;

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private javafx.geometry.Rectangle2D savedBounds;

    @FXML
    public void initialize() {
        // Load initial view (Login)
        setView("/view/splash.fxml");
        startBackgroundAnimation();
<<<<<<< HEAD

        // Clip content to background pane
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(backgroundAnimationPane.widthProperty());
        clip.heightProperty().bind(backgroundAnimationPane.heightProperty());
        backgroundAnimationPane.setClip(clip);
=======
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
    }

    private void startBackgroundAnimation() {
        // Static Twinkling Stars
        int starCount = 140;
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < starCount; i++) {
            javafx.scene.shape.Circle star = new javafx.scene.shape.Circle();
            star.setRadius(random.nextDouble() * 2.0 + 0.01); // 1.0 to 3.0
            star.setFill(javafx.scene.paint.Color.WHITE);
            // Higher base opacity
            star.setOpacity(random.nextDouble() * 0.6 + 0.2);

            // Bind position to pane size so it scales with window
            double xPos = random.nextDouble();
            double yPos = random.nextDouble();
            star.centerXProperty().bind(backgroundAnimationPane.widthProperty().multiply(xPos));
            star.centerYProperty().bind(backgroundAnimationPane.heightProperty().multiply(yPos));

            backgroundAnimationPane.getChildren().add(star);

            // Twinkle effect
            // Faster duration: 1s to 3s for more activity
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                    javafx.util.Duration.seconds(random.nextDouble() * 2 + 1), star);

            fade.setFromValue(0.0); // Dim
            fade.setToValue(1.0); // Bright shine
            fade.setAutoReverse(true);
            fade.setCycleCount(javafx.animation.Animation.INDEFINITE);
            fade.setDelay(javafx.util.Duration.seconds(random.nextDouble() * 2)); // Random start times
            fade.play();
        }

        // Shooting Stars
        javafx.animation.AnimationTimer shootingStarTimer = new javafx.animation.AnimationTimer() {
            private long lastSpawn = 0;
            private java.util.Random rand = new java.util.Random();

            @Override
            public void handle(long now) {
                if (now - lastSpawn > (3 + rand.nextDouble() * 5) * 1_000_000_000L) { // Every 3-8s
                    spawnShootingStar();
                    lastSpawn = now;
                }
            }
        };
        shootingStarTimer.start();
    }

    private void spawnShootingStar() {
        java.util.Random rand = new java.util.Random();
        double paneWidth = backgroundAnimationPane.getWidth();
<<<<<<< HEAD
        double paneHeight = backgroundAnimationPane.getHeight();

        // Randomize direction: true = Left-to-Right, false = Right-to-Left
        boolean leftToRight = rand.nextBoolean();

        double startX, startY;
        if (leftToRight) {
            // Start on left side (0% to 40% of width)
            startX = paneWidth * (rand.nextDouble() * 0.4) - 100; // Offset by line length
            startY = rand.nextDouble() * (paneHeight * 0.4); // Top 40%
        } else {
            // Start on right side (60% to 100% of width)
            startX = paneWidth * (0.6 + rand.nextDouble() * 0.4);
            startY = rand.nextDouble() * (paneHeight * 0.4); // Top 40%
        }

        // fast moving "streak"
        javafx.scene.shape.Line streak = new javafx.scene.shape.Line(0, 0, 100, 0); // 100px long streak

        // Gradient matches direction: Head (Stop 1) is white
        javafx.scene.paint.LinearGradient gradient;
        if (leftToRight) {
            gradient = new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                    new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.WHITE));
            streak.setRotate(45); // Pointing Down-Right
        } else {
            gradient = new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                    new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.WHITE));
            streak.setRotate(135); // Pointing Down-Left
        }

        streak.setStroke(gradient);
        streak.setStrokeWidth(2);
        streak.setOpacity(0.0); // Start invisible
=======
        // Start on right side (60% to 100% of width)
        double startX = paneWidth * (0.6 + rand.nextDouble() * 0.4);
        double startY = rand.nextDouble() * 300; // Start in top area (0-300)

        // fast moving "streak"
        javafx.scene.shape.Line streak = new javafx.scene.shape.Line(0, 0, 100, 0); // 100px long streak
        streak.setStroke(
                new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.WHITE))); // Head (Stop 1) is white
        streak.setStrokeWidth(2);

        // Rotate to point Top-Right to Bottom-Left (approx 135 degrees)
        // Line default is 0 deg (Right). 135 deg points Down-Left.
        streak.setRotate(135);

        streak.setOpacity(0.8);
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
        streak.setEffect(new javafx.scene.effect.Glow(0.8));

        // Position
        streak.setLayoutX(startX);
        streak.setLayoutY(startY);

        backgroundAnimationPane.getChildren().add(streak);

        // Animate
<<<<<<< HEAD
        double travelDist = 400 + rand.nextDouble() * 600;
        double duration = 1.5 + rand.nextDouble() * 1.0; // Slower: 1.5s to 2.5s

        javafx.animation.TranslateTransition move = new javafx.animation.TranslateTransition(
                javafx.util.Duration.seconds(duration), streak);

        if (leftToRight) {
            move.setByX(travelDist); // Move RIGHT
        } else {
            move.setByX(-travelDist); // Move LEFT
        }
        move.setByY(travelDist * 0.8); // Move DOWN (slightly less steep)

        // Fade In (Appear slowly)
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                javafx.util.Duration.seconds(duration * 0.2), streak);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(0.8);

        // Fade Out (Disappear)
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                javafx.util.Duration.seconds(duration * 0.8), streak);
        fadeOut.setFromValue(0.8);
        fadeOut.setToValue(0.0);

        javafx.animation.SequentialTransition fadeSeq = new javafx.animation.SequentialTransition(fadeIn, fadeOut);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(move, fadeSeq);
=======
        double travelDist = 500 + rand.nextDouble() * 300;
        double duration = 0.8 + rand.nextDouble() * 0.5;

        javafx.animation.TranslateTransition move = new javafx.animation.TranslateTransition(
                javafx.util.Duration.seconds(duration), streak);
        move.setByX(-travelDist); // Move LEFT (Negative X)
        move.setByY(travelDist); // Move DOWN (Positive Y)

        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                javafx.util.Duration.seconds(duration), streak);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(move, fade);
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
        pt.setOnFinished(e -> backgroundAnimationPane.getChildren().remove(streak));
        pt.play();
    }

    public void setView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newView = loader.load();

            if (!contentArea.getChildren().isEmpty()) {
                javafx.scene.Node currentView = contentArea.getChildren().get(0);
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(300), currentView);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(newView); // clear and add
                    fadeIn(newView);
                });
                fadeOut.play();
            } else {
                contentArea.getChildren().add(newView);
                fadeIn(newView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fadeIn(javafx.scene.Node node) {
        node.setOpacity(0);
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleMaximize() {
        maximizeWindow();
    }

    public void maximizeWindow() {
        Stage stage = (Stage) titleBar.getScene().getWindow();

        if (isMaximized) {
            // Restore
            if (savedBounds != null) {
                stage.setX(savedBounds.getMinX());
                stage.setY(savedBounds.getMinY());
                stage.setWidth(savedBounds.getWidth());
                stage.setHeight(savedBounds.getHeight());
            }

            // Restore styles (padding for shadow, rounded corners)
            mainContainer.setStyle("-fx-background-color: transparent; -fx-padding: 10;");
            innerContainer.setStyle(
                    "-fx-background-color: -color-bg; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

            isMaximized = false;
        } else {
            // Maximize
            // Save current bounds
            savedBounds = new javafx.geometry.Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(),
                    stage.getHeight());

            // Get screen bounds (respects taskbar)
            javafx.geometry.Rectangle2D visualBounds = javafx.stage.Screen.getPrimary().getVisualBounds();

            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());

            // Remove padding and radius so it fills the screen
            mainContainer.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            innerContainer.setStyle("-fx-background-color: -color-bg; -fx-background-radius: 0; -fx-effect: null;");

            isMaximized = true;
        }
    }

    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleWindowDrag(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleWindowMove(MouseEvent event) {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}
