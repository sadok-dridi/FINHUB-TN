package tn.finhub.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import tn.finhub.model.Escrow;
import tn.finhub.model.EscrowManager;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.PdfExportUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminEscrowStatisticsController {

    @FXML
    private Label totalEscrowsLabel;
    @FXML
    private Label lockedEscrowsLabel;
    @FXML
    private Label disputedEscrowsLabel;
    @FXML
    private Label completedEscrowsLabel;

    @FXML
    private PieChart statusPieChart;
    @FXML
    private BarChart<String, Number> typeBarChart;

    private final EscrowManager escrowManager = new EscrowManager();

    @FXML
    public void initialize() {
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            List<Escrow> allEscrows = escrowManager.getEscrowsForAdmin();

            if (allEscrows == null || allEscrows.isEmpty()) {
                totalEscrowsLabel.setText("0");
                lockedEscrowsLabel.setText("0");
                disputedEscrowsLabel.setText("0");
                completedEscrowsLabel.setText("0");
                return;
            }

            int total = allEscrows.size();
            int locked = 0;
            int disputed = 0;
            int completed = 0;

            // Map for Escrows By Type Bar Chart
            Map<String, Integer> typeCounts = new HashMap<>();

            for (Escrow e : allEscrows) {
                // Count Statuses
                if ("LOCKED".equals(e.getStatus())) {
                    locked++;
                } else if ("DISPUTED".equals(e.getStatus())) {
                    disputed++;
                } else if ("RELEASED".equals(e.getStatus()) || "REFUNDED".equals(e.getStatus())) {
                    completed++;
                }

                // Count Types
                String type = e.getEscrowType() != null ? e.getEscrowType() : "Unknown";
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            }

            // 1. Update KPIs
            totalEscrowsLabel.setText(String.valueOf(total));
            lockedEscrowsLabel.setText(String.valueOf(locked));
            disputedEscrowsLabel.setText(String.valueOf(disputed));
            completedEscrowsLabel.setText(String.valueOf(completed));

            // 2. Populate Status Pie Chart
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Active Locked", locked),
                    new PieChart.Data("Disputed", disputed),
                    new PieChart.Data("Completed", completed));
            statusPieChart.setData(pieChartData);

            // Apply PieChart styling via looking up the nodes after rendering
            for (PieChart.Data data : statusPieChart.getData()) {
                if (data.getName().equals("Active Locked")) {
                    data.getNode().setStyle("-fx-pie-color: #3b82f6;"); // Blue
                } else if (data.getName().equals("Disputed")) {
                    data.getNode().setStyle("-fx-pie-color: #ef4444;"); // Red
                } else {
                    data.getNode().setStyle("-fx-pie-color: #10b981;"); // Green
                }
            }

            // 3. Populate Category Bar Chart
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Transaction Types Volume");

            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            typeBarChart.getData().clear();
            typeBarChart.getData().add(series);

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Data Error", "Could not load Escrow statistics.");
        }
    }

    @FXML
    public void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_escrow.fxml"));
            loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
            javafx.scene.Parent view = loader.load();

            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) statusPieChart.getScene()
                    .lookup("#adminContentArea");

            if (contentArea != null) {
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(200), contentArea);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().clear();
                    contentArea.getChildren().add(view);
                    javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                            javafx.util.Duration.millis(200), contentArea);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Navigation Error", "Could not go back to the Escrow list view.");
        }
    }

    @FXML
    public void handleExportPDF() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save Escrow Statistics Report");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Global_Escrow_Stats.pdf");

        java.io.File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                javafx.scene.image.WritableImage statusChartSnapshot = null;
                javafx.scene.image.WritableImage typeChartSnapshot = null;

                if (statusPieChart.getData().size() > 0) {
                    statusChartSnapshot = statusPieChart.snapshot(new javafx.scene.SnapshotParameters(), null);
                }

                if (!typeBarChart.getData().isEmpty() && !typeBarChart.getData().get(0).getData().isEmpty()) {
                    typeChartSnapshot = typeBarChart.snapshot(new javafx.scene.SnapshotParameters(), null);
                }

                PdfExportUtil.generateEscrowStatisticsReport(
                        file,
                        totalEscrowsLabel.getText(),
                        lockedEscrowsLabel.getText(),
                        disputedEscrowsLabel.getText(),
                        completedEscrowsLabel.getText(),
                        statusChartSnapshot,
                        typeChartSnapshot);

                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Escrow Statistics Report PDF Exported Successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Export Error", "Failed to export PDF: " + e.getMessage());
            }
        }
    }
}
