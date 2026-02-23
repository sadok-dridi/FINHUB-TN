package tn.finhub.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import tn.finhub.model.User;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.ViewUtils;

import java.io.File;
import java.io.IOException;
import java.awt.Desktop;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @FXML
    private void handleBack() {
        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) nameLabel.getScene()
                .lookup("#adminContentArea");
        if (contentArea != null) {
            ViewUtils.loadContent(contentArea, "/view/admin_users.fxml");
        }
    }

    @FXML
    private void handleExportPdf() {
        if (currentUser == null) {
            DialogUtil.showError("Error", "No user selected.");
            return;
        }

        String safeName = (currentUser.getFullName() != null ? currentUser.getFullName() : "User")
                .replaceAll("[^a-zA-Z0-9_-]", "_");
        String defaultFileName = "UserDetails_" + currentUser.getId() + "_" + safeName + ".pdf";

        javafx.stage.Window window = nameLabel.getScene() != null ? nameLabel.getScene().getWindow() : null;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save user details as PDF");
        chooser.setInitialFileName(defaultFileName);
        File file = chooser.showSaveDialog(window);
        if (file == null)
            return;

        File targetFile = file;
        if (!targetFile.getName().toLowerCase().endsWith(".pdf")) {
            targetFile = new File(targetFile.getParent(), targetFile.getName() + ".pdf");
        }

        final File toSave = targetFile;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Wallet wallet = walletModel.findByUserId(currentUser.getId());

                // FinHub theme colors (RGB 0-1 for PDFBox)
                final float[] colorPrimary = { 139f/255f, 92f/255f, 246f/255f };      // #8B5CF6 violet
                final float[] colorSecondary = { 217f/255f, 70f/255f, 239f/255f };     // #D946EF fuchsia
                final float[] colorCardBg = { 30f/255f, 27f/255f, 46f/255f };          // #1E1B2E
                final float[] colorCardBorder = { 46f/255f, 42f/255f, 69f/255f };      // #2E2A45
                final float[] colorTextPrimary = { 243f/255f, 244f/255f, 246f/255f }; // #F3F4F6
                final float[] colorTextMuted = { 156f/255f, 163f/255f, 175f/255f };    // #9CA3AF
                final float[] colorSuccess = { 16f/255f, 185f/255f, 129f/255f };      // #10B981
                final float[] colorError = { 239f/255f, 68f/255f, 68f/255f };          // #EF4444

                try (PDDocument doc = new PDDocument()) {
                    PDPage page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);

                    float pageWidth = page.getMediaBox().getWidth();
                    float pageHeight = page.getMediaBox().getHeight();
                    float margin = 50f;

                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        // ----- Header band (violet gradient effect: solid primary) -----
                        float headerH = 72f;
                        float headerY = pageHeight - headerH;
                        cs.setNonStrokingColor(colorPrimary[0], colorPrimary[1], colorPrimary[2]);
                        cs.addRect(0, headerY, pageWidth, headerH);
                        cs.fill();

                        // Accent line (secondary) under header
                        cs.setNonStrokingColor(colorSecondary[0], colorSecondary[1], colorSecondary[2]);
                        cs.addRect(0, headerY - 4f, pageWidth, 4f);
                        cs.fill();

                        // Header text
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 24);
                        cs.setNonStrokingColor(1f, 1f, 1f);
                        cs.newLineAtOffset(margin, headerY + 26f);
                        cs.showText("FinHub");
                        cs.setFont(PDType1Font.HELVETICA, 14);
                        cs.newLineAtOffset(0, -12f);
                        cs.showText("User Details Report");
                        cs.endText();

                        float y = headerY - 40f;
                        float cardWidth = pageWidth - 2f * margin;
                        float cardPadding = 16f;
                        float lineH = 16f;

                        // ----- Card 1: Personal information -----
                        float card1H = 118f;
                        float card1Y = y - card1H;

                        cs.setNonStrokingColor(colorCardBg[0], colorCardBg[1], colorCardBg[2]);
                        cs.addRect(margin, card1Y, cardWidth, card1H);
                        cs.fill();
                        cs.setStrokingColor(colorCardBorder[0], colorCardBorder[1], colorCardBorder[2]);
                        cs.setLineWidth(1.5f);
                        cs.addRect(margin, card1Y, cardWidth, card1H);
                        cs.stroke();

                        // Card title with violet left accent
                        cs.setNonStrokingColor(colorPrimary[0], colorPrimary[1], colorPrimary[2]);
                        cs.addRect(margin, card1Y + card1H - 28f, 4f, 20f);
                        cs.fill();
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                        cs.setNonStrokingColor(colorTextPrimary[0], colorTextPrimary[1], colorTextPrimary[2]);
                        cs.newLineAtOffset(margin + 14f, card1Y + card1H - 22f);
                        cs.showText("Personal information");
                        cs.endText();

                        float textX = margin + cardPadding;
                        float textY = card1Y + card1H - 52f;
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 11);
                        cs.setNonStrokingColor(colorTextMuted[0], colorTextMuted[1], colorTextMuted[2]);
                        cs.newLineAtOffset(textX, textY);
                        cs.showText("Full name");
                        cs.setNonStrokingColor(colorTextPrimary[0], colorTextPrimary[1], colorTextPrimary[2]);
                        cs.newLineAtOffset(0, -lineH);
                        cs.showText(currentUser.getFullName() != null ? currentUser.getFullName() : "N/A");
                        cs.setNonStrokingColor(colorTextMuted[0], colorTextMuted[1], colorTextMuted[2]);
                        cs.newLineAtOffset(0, -lineH);
                        cs.showText("Email");
                        cs.setNonStrokingColor(colorTextPrimary[0], colorTextPrimary[1], colorTextPrimary[2]);
                        cs.newLineAtOffset(0, -lineH);
                        cs.showText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");
                        cs.setNonStrokingColor(colorTextMuted[0], colorTextMuted[1], colorTextMuted[2]);
                        cs.newLineAtOffset(0, -lineH);
                        cs.showText("Role  •  User ID");
                        cs.setNonStrokingColor(colorPrimary[0], colorPrimary[1], colorPrimary[2]);
                        cs.newLineAtOffset(0, -lineH);
                        cs.showText((currentUser.getRole() != null ? currentUser.getRole() : "USER") + "  #" + currentUser.getId());
                        cs.endText();

                        // ----- Card 2: Wallet status -----
                        y = card1Y - 24f;
                        float card2H = 110f;
                        float card2Y = y - card2H;

                        cs.setNonStrokingColor(colorCardBg[0], colorCardBg[1], colorCardBg[2]);
                        cs.addRect(margin, card2Y, cardWidth, card2H);
                        cs.fill();
                        cs.setStrokingColor(colorCardBorder[0], colorCardBorder[1], colorCardBorder[2]);
                        cs.addRect(margin, card2Y, cardWidth, card2H);
                        cs.stroke();

                        boolean isFrozen = wallet != null && "FROZEN".equals(wallet.getStatus());
                        float[] statusColor = isFrozen ? colorError : colorSuccess;

                        cs.setNonStrokingColor(statusColor[0], statusColor[1], statusColor[2]);
                        cs.addRect(margin, card2Y + card2H - 28f, 4f, 20f);
                        cs.fill();
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                        cs.setNonStrokingColor(colorTextPrimary[0], colorTextPrimary[1], colorTextPrimary[2]);
                        cs.newLineAtOffset(margin + 14f, card2Y + card2H - 22f);
                        cs.showText("Wallet status");
                        cs.endText();

                        textX = margin + cardPadding;
                        textY = card2Y + card2H - 50f;
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 11);
                        cs.setNonStrokingColor(colorTextMuted[0], colorTextMuted[1], colorTextMuted[2]);
                        cs.newLineAtOffset(textX, textY);
                        cs.showText("Balance");
                        cs.setNonStrokingColor(colorPrimary[0], colorPrimary[1], colorPrimary[2]);
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                        cs.newLineAtOffset(0, -lineH - 2f);
                        if (wallet != null) {
                            String curr = wallet.getCurrency() != null ? wallet.getCurrency() : "TND";
                            cs.showText(wallet.getBalance() + " " + curr);
                        } else {
                            cs.showText("N/A");
                        }
                        cs.setFont(PDType1Font.HELVETICA, 11);
                        cs.setNonStrokingColor(colorTextMuted[0], colorTextMuted[1], colorTextMuted[2]);
                        cs.newLineAtOffset(0, -lineH);
                        cs.showText("Status");
                        cs.setNonStrokingColor(statusColor[0], statusColor[1], statusColor[2]);
                        cs.newLineAtOffset(0, -lineH);
                        cs.showText(wallet != null && wallet.getStatus() != null ? wallet.getStatus() : "No wallet");
                        cs.endText();

                        // ----- Footer -----
                        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 9);
                        cs.setNonStrokingColor(colorTextMuted[0], colorTextMuted[1], colorTextMuted[2]);
                        cs.newLineAtOffset(margin, 28f);
                        cs.showText("Generated on " + generatedAt + "  •  FinHub Admin");
                        cs.endText();
                    }

                    doc.save(toSave);
                }

                Platform.runLater(() -> {
                    DialogUtil.showInfo("PDF exported", "File saved: " + toSave.getAbsolutePath());
                    try {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                            Desktop.getDesktop().open(toSave);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> DialogUtil.showError("Export failed", "Could not create PDF: " + e.getMessage()));
                e.printStackTrace();
            }
        });
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
