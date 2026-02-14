package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import tn.finhub.model.User;
import tn.finhub.model.UserModel;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class SellerProfileController {

    @FXML
    private Circle trustScoreRing;
    @FXML
    private Label initialsLabel;
    @FXML
    private Label trustScoreLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private SVGPath verifiedBadge;
    @FXML
    private Label emailLabel;
    @FXML
    private Label totalDealsLabel;
    @FXML
    private Label successRateLabel;
    @FXML
    private Label volumeLabel;
    @FXML
    private Button startEscrowBtn;
    @FXML
    private Button saveContactBtn;

    private User displayedUser;
    private final UserModel userModel = new UserModel();

    public void setUser(User user) {
        this.displayedUser = user;
        populateData();
        animateTrustScore();
    }

    public void setUserId(int userId) {
        this.displayedUser = userModel.findById(userId);
        populateData();
        animateTrustScore();
    }

    private void populateData() {
        if (displayedUser == null)
            return;

        nameLabel.setText(displayedUser.getFullName());
        emailLabel.setText(displayedUser.getEmail());

        String[] names = displayedUser.getFullName().split(" ");
        String initials = "";
        if (names.length > 0)
            initials += names[0].charAt(0);
        if (names.length > 1)
            initials += names[names.length - 1].charAt(0);
        initialsLabel.setText(initials.toUpperCase());

        // Profile Photo
        if (displayedUser.getProfilePhotoUrl() != null && !displayedUser.getProfilePhotoUrl().isEmpty()) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(displayedUser.getProfilePhotoUrl());
                trustScoreRing.setFill(new javafx.scene.paint.ImagePattern(img));
                initialsLabel.setVisible(false);
            } catch (Exception e) {
                // Fallback to initials
                initialsLabel.setVisible(true);
            }
        } else {
            initialsLabel.setVisible(true);
            trustScoreRing.setFill(javafx.scene.paint.Color.TRANSPARENT);
        }

        // Trust Score Logic
        int score = displayedUser.getTrustScore();
        trustScoreLabel.setText(String.valueOf(score));

        // Verification Badge
        verifiedBadge.setVisible(displayedUser.isEmailVerified()); // Or custom logic

        // Mock Stats for now (Ideally fetch from EscrowManager)
        // In real impl: escrowManager.getStatsForUser(displayedUser.getId());
        totalDealsLabel.setText("12");
        successRateLabel.setText("100%");
        volumeLabel.setText("2.5k");
    }

    private void animateTrustScore() {
        double maxCircumference = 2 * Math.PI * 60; // radius 60
        double percentage = displayedUser.getTrustScore() / 100.0;
        double targetDash = maxCircumference * percentage;

        trustScoreRing.getStrokeDashArray().clear();
        trustScoreRing.getStrokeDashArray().addAll(0d, maxCircumference);

        DoubleProperty progressParams = new SimpleDoubleProperty(0);
        progressParams.addListener((obs, oldVal, newVal) -> {
            trustScoreRing.getStrokeDashArray().set(0, newVal.doubleValue());
        });

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressParams, 0d)),
                new KeyFrame(Duration.seconds(1.5), new KeyValue(progressParams, targetDash)));
        timeline.play();

        // Color Logic
        if (displayedUser.getTrustScore() > 80) {
            trustScoreRing.setStyle(
                    "-fx-stroke: -color-success; -fx-fill: transparent; -fx-stroke-width: 8; -fx-rotate: -90;");
            trustScoreLabel.setStyle(trustScoreLabel.getStyle() + "-fx-text-fill: -color-success;");
        } else if (displayedUser.getTrustScore() < 50) {
            trustScoreRing.setStyle(
                    "-fx-stroke: -color-danger; -fx-fill: transparent; -fx-stroke-width: 8; -fx-rotate: -90;");
            trustScoreLabel.setStyle(trustScoreLabel.getStyle() + "-fx-text-fill: -color-danger;");
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        stage.close();
    }
}
