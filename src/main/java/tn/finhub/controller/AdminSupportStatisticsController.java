package tn.finhub.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import tn.finhub.model.SupportModel;
import tn.finhub.model.SupportTicket;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.PdfExportUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminSupportStatisticsController {

    @FXML
    private Label totalTicketsLabel;
    @FXML
    private Label openTicketsLabel;
    @FXML
    private Label resolvedTicketsLabel;

    @FXML
    private PieChart statusPieChart;
    @FXML
    private BarChart<String, Number> categoryBarChart;

    private final SupportModel supportModel = new SupportModel();

    @FXML
    public void initialize() {
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            List<SupportTicket> allTickets = supportModel.getAllTickets();

            if (allTickets == null || allTickets.isEmpty()) {
                totalTicketsLabel.setText("0");
                openTicketsLabel.setText("0");
                resolvedTicketsLabel.setText("0");
                return;
            }

            int total = allTickets.size();
            int open = 0;
            int resolved = 0;

            // Map for Category Bar Chart
            Map<String, Integer> categoryCounts = new HashMap<>();

            for (SupportTicket ticket : allTickets) {
                // Count Statuses
                if ("OPEN".equals(ticket.getStatus())) {
                    open++;
                } else if ("RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus())) {
                    resolved++;
                }

                // Count Categories
                String cat = ticket.getCategory() != null ? ticket.getCategory() : "Uncategorized";
                categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
            }

            // 1. Update KPIs
            totalTicketsLabel.setText(String.valueOf(total));
            openTicketsLabel.setText(String.valueOf(open));
            resolvedTicketsLabel.setText(String.valueOf(resolved));

            // 2. Populate Status Pie Chart
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Open", open),
                    new PieChart.Data("Resolved/Closed", resolved));
            statusPieChart.setData(pieChartData);

            // Apply PieChart styling
            for (PieChart.Data data : statusPieChart.getData()) {
                if (data.getName().equals("Open")) {
                    data.getNode().setStyle("-fx-pie-color: #ef4444;"); // Red
                } else {
                    data.getNode().setStyle("-fx-pie-color: #10b981;"); // Green
                }
            }

            // 3. Populate Category Bar Chart
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Tickets by Category");

            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            categoryBarChart.getData().clear();
            categoryBarChart.getData().add(series);

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Data Error", "Could not load support ticket statistics.");
        }
    }

    @FXML
    public void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_support.fxml"));
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
            DialogUtil.showError("Navigation Error", "Could not go back to ticket ledger.");
        }
    }

    @FXML
    public void handleExportPDF() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save Support Statistics Report");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Global_Support_Stats.pdf");

        java.io.File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                javafx.scene.image.WritableImage statusChartSnapshot = null;
                javafx.scene.image.WritableImage categoryChartSnapshot = null;

                if (statusPieChart.getData().size() > 0) {
                    statusChartSnapshot = statusPieChart.snapshot(new javafx.scene.SnapshotParameters(), null);
                }

                if (!categoryBarChart.getData().isEmpty() && !categoryBarChart.getData().get(0).getData().isEmpty()) {
                    categoryChartSnapshot = categoryBarChart.snapshot(new javafx.scene.SnapshotParameters(), null);
                }

                PdfExportUtil.generateSupportStatisticsReport( // Changed to use the imported class directly
                        file,
                        totalTicketsLabel.getText(),
                        openTicketsLabel.getText(),
                        resolvedTicketsLabel.getText(),
                        statusChartSnapshot,
                        categoryChartSnapshot);

                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("PDF Exported Successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Export Error", "Failed to export PDF: " + e.getMessage());
            }
        }
    }
}
