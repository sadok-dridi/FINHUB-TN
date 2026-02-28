package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.finhub.model.KnowledgeBase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import javafx.embed.swing.SwingFXUtils;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

public class SupportKbController {

    @FXML
    private TextField searchField;
    @FXML
    private VBox kbContainer;

    @FXML
    private Button exportPdfButton;

    private final tn.finhub.model.KnowledgeBaseModel kbModel = new tn.finhub.model.KnowledgeBaseModel();
    private KnowledgeBase selectedArticle;

    @FXML
    public void initialize() {
        // Dynamic search: filter articles as the user types
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> performSearch(newValue));
        }
        if (exportPdfButton != null) {
            exportPdfButton.setDisable(true); // no article selected yet
        }
        loadAllArticles();
    }

    @FXML
    private void handleSearch() {
        performSearch(searchField.getText());
    }

    private void performSearch(String rawQuery) {
        String query = rawQuery != null ? rawQuery.trim() : "";
        if (query.isEmpty()) {
            loadAllArticles();
        } else {
            List<KnowledgeBase> results = kbModel.searchArticles(query);
            displayArticles(results);
        }
    }

    private void loadAllArticles() {
        List<KnowledgeBase> articles = kbModel.getAllArticles();
        displayArticles(articles);
    }

    private void displayArticles(List<KnowledgeBase> articles) {
        kbContainer.getChildren().clear();
        if (articles.isEmpty()) {
            Label emptyLabel = new Label("No articles found.");
            emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
            kbContainer.getChildren().add(emptyLabel);
        } else {
            for (KnowledgeBase article : articles) {
                kbContainer.getChildren().add(createArticleCard(article));
            }
        }
    }

    private VBox createArticleCard(KnowledgeBase article) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20; -fx-cursor: hand;");

        // Title (Header)
        Label title = new Label(article.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-primary;");

        Label category = new Label(article.getCategory());
        category.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: -color-text-secondary; -fx-border-color: -color-text-secondary; -fx-border-radius: 4; -fx-padding: 2 6;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(category, title);

        // Content (Body)
        Label content = new Label(article.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-text-primary; -fx-line-spacing: 4;");

        card.getChildren().addAll(header, content);
        // When the card is clicked, remember the article, enable export button and show QR dialog
        card.setOnMouseClicked(e -> {
            selectedArticle = article;
            if (exportPdfButton != null) {
                exportPdfButton.setDisable(false);
            }
            openArticleDialog(article);
        });
        return card;
    }

    private void openArticleDialog(KnowledgeBase article) {

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);

        if (kbContainer != null && kbContainer.getScene() != null) {
            dialog.initOwner(kbContainer.getScene().getWindow());
        }

        dialog.setTitle(article.getTitle());

        VBox root = new VBox(25);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: #050816;");

        // ===== TITLE =====
        Label title = new Label(article.getTitle());
        title.setWrapText(true);
        title.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #38BDF8;"
        );

        // ===== QR CARD =====
        VBox qrCard = new VBox(15);
        qrCard.setAlignment(Pos.CENTER);
        qrCard.setStyle(
                "-fx-background-color: rgba(15,23,42,0.95);" +
                        "-fx-background-radius: 18;" +
                        "-fx-padding: 20;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 0);"
        );

        Label qrTitle = new Label("Visualiser ce sujet en images");
        qrTitle.setStyle(
                "-fx-text-fill: #E2E8F0;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );

        Label qrHint = new Label(
                "Scannez le QR code avec votre téléphone\n" +
                        "pour ouvrir Google Images sur ce thème."
        );
        qrHint.setWrapText(true);
        qrHint.setStyle(
                "-fx-text-fill: #94A3B8;" +
                        "-fx-font-size: 12px;"
        );

        ImageView qrView = new ImageView();
        qrView.setFitWidth(220);
        qrView.setFitHeight(220);

        try {
            Image qrImage = generateQrImage(article);
            qrView.setImage(qrImage);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Button exportPdfBtn = new Button("Export PDF");
        exportPdfBtn.getStyleClass().add("button-primary");
        exportPdfBtn.setOnAction(e -> exportArticleToPdf(article));

        qrCard.getChildren().addAll(qrTitle, qrHint, qrView, exportPdfBtn);

        root.getChildren().addAll(title, qrCard);

        Scene scene = new Scene(root, 520, 520);
        dialog.setScene(scene);
        dialog.show();
    }

    private Image generateQrImage(KnowledgeBase article) throws WriterException {
        // QR code points to Google Images search for the article title
        String encodedTitle;
        try {
            encodedTitle = java.net.URLEncoder.encode(article.getTitle(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            encodedTitle = article.getTitle();
        }
        String qrText = "https://www.google.com/search?tbm=isch&q=" + encodedTitle;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        int size = 220;
        BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, size, size);

        java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int color = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                bufferedImage.setRGB(x, y, color);
            }
        }
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private void exportArticleToPdf(KnowledgeBase article) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Knowledge Base Article");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName(article.getTitle().replaceAll("[^a-zA-Z0-9-_]+", "_") + ".pdf");

        File target = fileChooser.showSaveDialog(
                kbContainer != null && kbContainer.getScene() != null ? kbContainer.getScene().getWindow() : null);
        if (target == null) {
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(target)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();
            document.add(new Paragraph(article.getTitle()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(article.getContent()));
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExportSelectedArticle() {
        if (selectedArticle != null) {
            exportArticleToPdf(selectedArticle);
        }
    }
}
