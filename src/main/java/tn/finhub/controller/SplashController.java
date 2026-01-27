package tn.finhub.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class SplashController {

    @FXML
    private Label logoText;

    @FXML
    private Label accentText;

    @FXML
    private Label sloganText;

    @FXML
    public void initialize() {
        // Initial State
        logoText.setOpacity(0);
        logoText.setTranslateY(20);
        accentText.setOpacity(0);
        accentText.setTranslateY(20);
        sloganText.setOpacity(0);

        // Animation Sequence

        // 1. Logo Text Fade In & Slide Up
        FadeTransition ft1 = new FadeTransition(Duration.millis(1200), logoText);
        ft1.setFromValue(0);
        ft1.setToValue(1);

        TranslateTransition tt1 = new TranslateTransition(Duration.millis(800), logoText);
        tt1.setFromY(20);
        tt1.setToY(0);

        // 2. Accent Text Fade In & Slide Up (Slight delay)
        FadeTransition ft2 = new FadeTransition(Duration.millis(800), accentText);
        ft2.setFromValue(0);
        ft2.setToValue(1);

        TranslateTransition tt2 = new TranslateTransition(Duration.millis(600), accentText);
        tt2.setFromY(20);
        tt2.setToY(0);

        // 3. Slogan Fade In
        FadeTransition ft3 = new FadeTransition(Duration.millis(900), sloganText);
        ft3.setFromValue(0);
        ft3.setToValue(1);

        // Parallel transition for Logo parts
        ParallelTransition ptLogo = new ParallelTransition(ft1, tt1);
        ParallelTransition ptAccent = new ParallelTransition(ft2, tt2);

        // Sequential Timeline
        SequentialTransition seq = new SequentialTransition(
                new PauseTransition(Duration.seconds(1.0)), // Initial empty background
                ptLogo,
                ptAccent,
                ft3,
                new PauseTransition(Duration.seconds(2)) // Hold for 2s
        );

        seq.setOnFinished(e -> navigateToLogin());
        seq.play();
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent loginView = loader.load();

            // Access the content area from the current scene
            StackPane contentArea = (StackPane) logoText.getScene().lookup("#contentArea");

            if (contentArea != null) {
                // Fade out Splash
                FadeTransition fadeOut = new FadeTransition(Duration.millis(500), contentArea.getChildren().get(0));
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(loginView);
                    // Fade in Login
                    loginView.setOpacity(0);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(500), loginView);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
