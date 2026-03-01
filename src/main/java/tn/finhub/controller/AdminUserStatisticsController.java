package tn.finhub.controller;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import tn.finhub.model.User;
import tn.finhub.model.UserModel;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.LanguageManager;
import tn.finhub.util.PdfExportUtil;
import javafx.scene.image.WritableImage;

import java.io.File;
import java.util.List;

public class AdminUserStatisticsController {

    @FXML
    private Label totalUsersLabel;

    @FXML
    private Label adminCountLabel;

    @FXML
    private Label avgTrustScoreLabel;

    @FXML
    private Label lowTrustAlertsLabel;

    @FXML
    private PieChart rolePieChart;

    @FXML
    private BarChart<String, Number> trustScoreBarChart;

    private UserModel userModel = new UserModel();

    @FXML
    public void initialize() {
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            List<User> users = userModel.findAll();

            if (users == null || users.isEmpty()) {
                System.out.println("No users found for statistics.");
                return;
            }

            // 1. Calculate KPI Metrics
            int totalUsers = users.size();
            int adminCount = 0;
            int lowTrustCount = 0;
            double totalTrustScore = 0;
            int trustedUserCount = 0; // Admins don't have trust scores in the same way, but let's count everyone who
                                      // has one

            for (User u : users) {
                if ("ADMIN".equalsIgnoreCase(u.getRole())) {
                    adminCount++;
                } else {
                    totalTrustScore += u.getTrustScore();
                    trustedUserCount++;
                    if (u.getTrustScore() < 50) {
                        lowTrustCount++;
                    }
                }
            }

            int avgTrustScore = trustedUserCount > 0 ? (int) Math.round(totalTrustScore / trustedUserCount) : 0;

            // Update Labels
            totalUsersLabel.setText(String.valueOf(totalUsers));
            adminCountLabel.setText(String.valueOf(adminCount));
            avgTrustScoreLabel.setText(String.valueOf(avgTrustScore));
            lowTrustAlertsLabel.setText(String.valueOf(lowTrustCount));

            // 2. Populate Role Pie Chart
            int regularUserCount = totalUsers - adminCount;
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Regular Users (" + regularUserCount + ")", regularUserCount),
                    new PieChart.Data("Administrators (" + adminCount + ")", adminCount));
            rolePieChart.setData(pieChartData);

            // 3. Populate Trust Score Bar Chart
            int score0to50 = 0;
            int score51to100 = 0;
            int score101to150 = 0;
            int scoreOver150 = 0;

            for (User u : users) {
                if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                    int score = u.getTrustScore();
                    if (score <= 50)
                        score0to50++;
                    else if (score <= 100)
                        score51to100++;
                    else if (score <= 150)
                        score101to150++;
                    else
                        scoreOver150++;
                }
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Users per Bracket");
            series.getData().add(new XYChart.Data<>("0 - 50", score0to50));
            series.getData().add(new XYChart.Data<>("51 - 100", score51to100));
            series.getData().add(new XYChart.Data<>("101 - 150", score101to150));
            series.getData().add(new XYChart.Data<>("> 150", scoreOver150));

            trustScoreBarChart.getData().clear();
            trustScoreBarChart.getData().add(series);

            // Optional: set custom colors for the bar chart through CSS lookups after scene
            // is rendered,
            // but default colors usually look fine.

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Data Error", "Could not load statistics data.");
        }
    }

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            Node sourceNode = (Node) event.getSource();
            Scene scene = sourceNode.getScene();
            if (scene == null)
                return;

            StackPane contentArea = (StackPane) scene.lookup("#adminContentArea");
            if (contentArea == null) {
                DialogUtil.showError("System Error", "Navigation component missing.");
                return;
            }

            Runnable loadView = () -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_users.fxml"));
                    loader.setResources(LanguageManager.getInstance().getResourceBundle());
                    Parent view = loader.load();

                    view.setOpacity(0);
                    contentArea.getChildren().setAll(view);

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(300), view);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                } catch (Exception e) {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() -> DialogUtil.showError("Navigation Error",
                            "Could not load users view."));
                }
            };

            if (!contentArea.getChildren().isEmpty()) {
                Node currentView = contentArea.getChildren().get(0);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentView);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> loadView.run());
                fadeOut.play();
            } else {
                loadView.run();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExportPDF(ActionEvent event) {
        try {
            // Setup File Chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Statistics Report");
            fileChooser.setInitialFileName("User_Statistics_Report.pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

            // Show save dialog
            File dest = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());

            if (dest != null) {
                // Collect Strings
                String total = totalUsersLabel.getText();
                String admins = adminCountLabel.getText();
                String avgTrust = avgTrustScoreLabel.getText();
                String lowTrust = lowTrustAlertsLabel.getText();

                // Snapshot charts
                WritableImage roleSnapshot = rolePieChart.snapshot(new javafx.scene.SnapshotParameters(), null);
                WritableImage trustSnapshot = trustScoreBarChart.snapshot(new javafx.scene.SnapshotParameters(), null);

                // Run generation asynchronously to avoid freezing UI
                javafx.concurrent.Task<Void> exportTask = new javafx.concurrent.Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        PdfExportUtil.generateStatisticsReport(dest, total, admins, avgTrust, lowTrust, roleSnapshot,
                                trustSnapshot);
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        DialogUtil.showInfo("Export Successful",
                                "Report saved successfully to:\n" + dest.getAbsolutePath());
                    }

                    @Override
                    protected void failed() {
                        getException().printStackTrace();
                        DialogUtil.showError("Export Failed",
                                "There was an error generating the PDF.\n" + getException().getMessage());
                    }
                };

                new Thread(exportTask).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("System Error", "Could not initialize PDF export.");
        }
    }
}
