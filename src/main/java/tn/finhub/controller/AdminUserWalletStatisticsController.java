package tn.finhub.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import tn.finhub.model.User;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.model.WalletTransaction;
import tn.finhub.util.DialogUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUserWalletStatisticsController {

    @FXML
    private Label headerTitleLabel;
    @FXML
    private Label totalSentLabel;
    @FXML
    private Label totalReceivedLabel;
    @FXML
    private Label txCountLabel;
    @FXML
    private PieChart flowPieChart;
    @FXML
    private BarChart<String, Number> typeBarChart;

    private User currentUser;
    private final WalletModel walletModel = new WalletModel();

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            headerTitleLabel.setText("Wallet Statistics: " + user.getFullName());
            loadStatistics();
        }
    }

    private void loadStatistics() {
        try {
            Wallet wallet = walletModel.findByUserId(currentUser.getId());
            if (wallet == null) {
                DialogUtil.showError("Error", "No wallet found for this user.");
                return;
            }

            List<WalletTransaction> transactions = walletModel.getTransactionHistory(wallet.getId());

            if (transactions == null || transactions.isEmpty()) {
                totalSentLabel.setText("0.00 TND");
                totalReceivedLabel.setText("0.00 TND");
                txCountLabel.setText("0");
                return;
            }

            double totalSent = 0.0;
            double totalReceived = 0.0;
            Map<String, Integer> typeCounts = new HashMap<>();

            for (WalletTransaction tx : transactions) {
                String type = tx.getType();

                // Count flow
                if (type.equals("CREDIT") || type.equals("DEPOSIT") || type.equals("TRANSFER_RECEIVED")) {
                    totalReceived += tx.getAmount().doubleValue();
                } else if (type.equals("DEBIT") || type.equals("WITHDRAWAL") || type.equals("TRANSFER_SENT")
                        || type.equals("HOLD")) {
                    totalSent += tx.getAmount().doubleValue();
                }

                // Count types for bar chart
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            }

            // 1. Update KPIs
            totalSentLabel.setText(String.format("%.2f TND", totalSent));
            totalReceivedLabel.setText(String.format("%.2f TND", totalReceived));
            txCountLabel.setText(String.valueOf(transactions.size()));

            // 2. Populate Flow Pie Chart
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Inflow (Received/Deposits)", totalReceived),
                    new PieChart.Data("Outflow (Sent/Withdrawals)", totalSent));
            flowPieChart.setData(pieChartData);

            // 3. Populate Type Bar Chart
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Transaction Types");

            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            typeBarChart.getData().clear();
            typeBarChart.getData().add(series);

            // Styling colors for PieChart
            for (PieChart.Data data : flowPieChart.getData()) {
                if (data.getName().startsWith("Inflow")) {
                    data.getNode().setStyle("-fx-pie-color: #10b981;"); // Emerald green
                } else {
                    data.getNode().setStyle("-fx-pie-color: #ef4444;"); // Red
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Data Error", "Could not load wallet statistics data.");
        }
    }

    @FXML
    public void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/admin_user_transactions.fxml"));
            loader.setResources(tn.finhub.util.LanguageManager.getInstance().getResourceBundle());
            javafx.scene.Parent view = loader.load();

            AdminUserTransactionsController controller = loader.getController();
            controller.setUser(currentUser); // Pass user back

            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) flowPieChart.getScene()
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
            DialogUtil.showError("Navigation Error", "Could not go back to ledger.");
        }
    }

    @FXML
    public void handleExportPDF() {
        if (currentUser == null)
            return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save Wallet Statistics Report");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Wallet_Stats_" + currentUser.getFullName() + ".pdf");

        java.io.File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                javafx.scene.image.WritableImage flowChartSnapshot = null;
                javafx.scene.image.WritableImage typeChartSnapshot = null;

                if (flowPieChart.getData().size() > 0) {
                    flowChartSnapshot = flowPieChart.snapshot(new javafx.scene.SnapshotParameters(), null);
                }

                if (!typeBarChart.getData().isEmpty() && !typeBarChart.getData().get(0).getData().isEmpty()) {
                    typeChartSnapshot = typeBarChart.snapshot(new javafx.scene.SnapshotParameters(), null);
                }

                tn.finhub.util.PdfExportUtil.generateUserWalletStatisticsReport(
                        file,
                        currentUser.getFullName(),
                        totalSentLabel.getText(),
                        totalReceivedLabel.getText(),
                        txCountLabel.getText(),
                        flowChartSnapshot,
                        typeChartSnapshot);

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
