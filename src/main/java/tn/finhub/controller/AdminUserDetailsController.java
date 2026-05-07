package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart;
import java.util.List;

import tn.finhub.model.User;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.ViewUtils;

public class AdminUserDetailsController {

    // Personal Information fields
    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label idLabel;

    // Wallet Status fields
    @FXML
    private Label balanceLabel;
    @FXML
    private Label currencyLabel;
    @FXML
    private Label walletStatusLabel;

    @FXML
    private LineChart<Number, Number> activitySparkline;

    // Buttons
    @FXML
    private Button freezeBtn;

    private User currentUser;
    private final WalletModel walletModel = new WalletModel();

    @FXML
    public void initialize() {
        // Initialization logic if needed
    }

    public void setUser(User user) {
        if (user == null)
            return;

        this.currentUser = user;

        // Set personal information
        nameLabel.setText(user.getFullName() != null ? user.getFullName() : "N/A");
        emailLabel.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        roleLabel.setText(user.getRole() != null ? user.getRole() : "USER");
        idLabel.setText("User ID: " + user.getId());

        // Refresh wallet info
        refreshWalletInfo();
    }

    @FXML
    private void handleViewPortfolio() {
        if (currentUser == null)
            return;

        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameLabel.getScene()
                .lookup("#adminContentArea");
        if (contentArea != null) {
            javafx.fxml.FXMLLoader loader = ViewUtils.loadContent(contentArea, "/view/admin_user_portfolio.fxml");
            if (loader != null) {
                Object controller = loader.getController();
                if (controller instanceof AdminUserPortfolioController portfolioController) {
                    portfolioController.setUser(currentUser);
                }
            }
        }
    }

    private void refreshWalletInfo() {
        if (currentUser == null)
            return;

        // Show loading state
        balanceLabel.setText("Loading...");
        walletStatusLabel.setText("...");
        freezeBtn.setDisable(true);

        // Run DB fetch in background
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            return walletModel.findByUserId(currentUser.getId());
        }).thenAcceptAsync(wallet -> {
            // Update UI on JavaFX thread
            if (wallet != null) {
                balanceLabel.setText(wallet.getBalance().toString());
                currencyLabel.setText(wallet.getCurrency());

                String status = wallet.getStatus();
                walletStatusLabel.setText(status);

                if ("FROZEN".equals(status)) {
                    walletStatusLabel.setStyle("-fx-text-fill: -color-error; -fx-font-weight: bold;");
                    freezeBtn.setText("Unfreeze Wallet");
                    freezeBtn.getStyleClass().removeAll("button-warning");
                    freezeBtn.getStyleClass().add("button-success");
                } else {
                    walletStatusLabel.setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold;");
                    freezeBtn.setText("Freeze Wallet");
                    freezeBtn.getStyleClass().removeAll("button-success");
                    freezeBtn.getStyleClass().add("button-warning");
                }
                freezeBtn.setDisable(false);

                // Fetch transactions and populate sparkline
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    return walletModel.getTransactionHistory(wallet.getId());
                }).thenAcceptAsync(transactions -> {
                    populateSparkline(transactions, wallet.getBalance());
                }, javafx.application.Platform::runLater);

            } else {
                balanceLabel.setText("N/A");
                currencyLabel.setText("TND");
                walletStatusLabel.setText("No Wallet");
                walletStatusLabel.setStyle("-fx-text-fill: -color-text-muted;");
                freezeBtn.setDisable(true);
            }
        }, javafx.application.Platform::runLater).exceptionally(ex -> {
            javafx.application.Platform.runLater(() -> {
                tn.finhub.util.DialogUtil.showError("Error", "Failed to load wallet info.");
                ex.printStackTrace();
            });
            return null;
        });
    }

    private void populateSparkline(List<tn.finhub.model.WalletTransaction> transactions,
            java.math.BigDecimal currentBalance) {
        if (activitySparkline == null || transactions == null || currentBalance == null)
            return;

        activitySparkline.getData().clear();

        if (transactions.isEmpty())
            return;

        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        // Reverse engineer the balance over the last N transactions (up to 20 for a
        // clean sparkline)
        int limit = Math.min(transactions.size(), 20);
        double runningBalance = currentBalance.doubleValue();

        // We go backwards from the most recent to the older transactions to build the
        // line
        for (int i = 0; i < limit; i++) {
            series.getData().add(0, new XYChart.Data<>(limit - i, runningBalance)); // Add at beginning so X goes left
                                                                                    // to right

            // Subtract the effect of this transaction to find the *previous* balance
            tn.finhub.model.WalletTransaction tx = transactions.get(i);
            if (tx.getType().equals("CREDIT") || tx.getType().equals("DEPOSIT")
                    || tx.getType().equals("TRANSFER_RECEIVED")) {
                runningBalance -= tx.getAmount().doubleValue();
            } else if (tx.getType().equals("DEBIT") || tx.getType().equals("WITHDRAWAL")
                    || tx.getType().equals("TRANSFER_SENT")) {
                runningBalance += tx.getAmount().doubleValue();
            }
        }

        activitySparkline.getData().add(series);

        // Style the sparkline
        javafx.scene.Node line = series.getNode().lookup(".chart-series-line");
        if (line != null) {
            line.setStyle("-fx-stroke: #8b5cf6; -fx-stroke-width: 2px;");
        }
    }

    @FXML
    public void handleExportPDF() {
        if (currentUser == null)
            return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save User Details Report");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("User_Details_" + currentUser.getFullName() + ".pdf");

        java.io.File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                javafx.scene.image.WritableImage sparklineSnapshot = null;
                if (activitySparkline.getData().size() > 0) {
                    sparklineSnapshot = activitySparkline.snapshot(new javafx.scene.SnapshotParameters(), null);
                }

                tn.finhub.util.PdfExportUtil.generateUserDetailsReport(
                        file,
                        currentUser.getFullName(),
                        currentUser.getEmail(),
                        currentUser.getRole(),
                        String.valueOf(currentUser.getId()),
                        balanceLabel.getText(),
                        currencyLabel.getText(),
                        walletStatusLabel.getText(),
                        sparklineSnapshot);
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("PDF Exported Successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                tn.finhub.util.DialogUtil.showError("Export Error", "Failed to export PDF: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleBack() {
        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameLabel.getScene()
                .lookup("#adminContentArea");
        if (contentArea != null) {
            ViewUtils.loadContent(contentArea, "/view/admin_users.fxml");
        }
    }

    @FXML
    private void handleFreezeWallet() {
        if (currentUser == null) {
            DialogUtil.showError("Error", "No user selected.");
            return;
        }

        Wallet wallet = walletModel.findByUserId(currentUser.getId());
        if (wallet == null) {
            DialogUtil.showError("Error", "User has no wallet.");
            return;
        }

        boolean isFrozen = "FROZEN".equals(wallet.getStatus());
        String actionRequest = isFrozen ? "Unfreeze" : "Freeze";

        boolean confirmed = DialogUtil.showConfirmation(
                actionRequest + " Wallet",
                "Are you sure you want to " + actionRequest.toLowerCase() + " the wallet for "
                        + currentUser.getFullName() + "?");

        if (confirmed) {
            try {
                if (isFrozen) {
                    walletModel.unfreezeWallet(wallet.getId());
                    DialogUtil.showInfo("Success", "Wallet unfrozen successfully.");
                } else {
                    walletModel.freezeWallet(wallet.getId());
                    DialogUtil.showInfo("Success", "Wallet frozen successfully.");
                }
                refreshWalletInfo();
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Operation Failed", "Could not update wallet status: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteUser() {
        if (currentUser == null) {
            DialogUtil.showError("Error", "No user selected.");
            return;
        }

        // 1. Password Confirmation
        String inputPassword = DialogUtil.showPasswordInput(
                "Admin Authentication",
                "Please enter your Admin Password to confirm deletion:");

        if (inputPassword == null || inputPassword.isEmpty()) {
            if (inputPassword != null) {
                DialogUtil.showError("Error", "Password cannot be empty.");
            }
            return;
        }

        // 2. Verify Password (Re-Auth)
        if (!verifyAdminPassword(inputPassword)) {
            DialogUtil.showError("Authentication Failed", "Incorrect Admin Password.");
            return;
        }

        // 3. Final Warning
        boolean confirmed = DialogUtil.showConfirmation(
                "Delete User",
                "Are you sure you want to delete " + currentUser.getFullName() + "?\n\n" +
                        "This will:\n" +
                        "- LIQUIDATE all portfolio assets.\n" +
                        "- TRANSFER total balance to Central Bank.\n" +
                        "- PERMANENTLY DELETE all user data.\n" +
                        "- THIS CANNOT BE UNDONE.");

        if (confirmed) {
            try {
                tn.finhub.model.UserModel userModel = new tn.finhub.model.UserModel();
                userModel.deleteUser(currentUser.getId());

                DialogUtil.showInfo("Success", "User deleted successfully. Assets liquidated and transferred.");
                handleBack(); // Navigate back to users list
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Deletion Failed", e.getMessage());
            }
        }
    }

    private boolean verifyAdminPassword(String password) {
        String email = tn.finhub.util.SessionManager.getEmail();
        if (email == null)
            return false;

        try {
            String json = """
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(tn.finhub.util.ApiClient.BASE_URL + "/login"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();

            java.net.http.HttpResponse<String> response = tn.finhub.util.ApiClient.getClient().send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
