package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.finhub.model.KnowledgeBase;

import java.util.List;

public class SupportKbController {

    @FXML
    private TextField searchField;
    @FXML
    private VBox kbContainer;
    @FXML
    private HBox loadingIndicator;

    private final tn.finhub.model.KnowledgeBaseModel kbModel = new tn.finhub.model.KnowledgeBaseModel();

    @FXML
    public void initialize() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
        loadAllArticles();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadAllArticles();
        } else {
            // 1. Local Search (Limit to 1 top result visually)
            List<KnowledgeBase> allLocalResults = kbModel.searchArticles(query);
            List<KnowledgeBase> topLocalResult = allLocalResults.isEmpty() ? new java.util.ArrayList<>()
                    : allLocalResults.subList(0, 1);

            kbContainer.getChildren().clear();
            if (!topLocalResult.isEmpty()) {
                kbContainer.getChildren().add(createArticleCard(topLocalResult.get(0)));
            }

            // 2. Show loading spinner
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(true);
                loadingIndicator.setManaged(true);
            }

            // 3. Fetch Web Results asynchronously via n8n
            tn.finhub.util.KbWebSearchUtil.fetchArticlesAsync(query, webArticles -> {
                // Success Callback: Hide spinner and append results
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                    loadingIndicator.setManaged(false);
                }

                if (webArticles != null && !webArticles.isEmpty()) {
                    for (KnowledgeBase article : webArticles) {
                        kbContainer.getChildren().add(createWebArticleCard(article));
                    }
                } else if (topLocalResult.isEmpty()) {
                    Label emptyLabel = new Label("No local or web articles found.");
                    emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-padding: 20;");
                    kbContainer.getChildren().add(emptyLabel);
                }
            }, errorMsg -> {
                // Error Callback
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                    loadingIndicator.setManaged(false);
                }
                System.err.println("Web Search Error: " + errorMsg);
                if (topLocalResult.isEmpty()) {
                    Label errorLabel = new Label("Search failed: " + errorMsg);
                    errorLabel.setStyle("-fx-text-fill: -color-error; -fx-font-size: 14px; -fx-padding: 20;");
                    kbContainer.getChildren().add(errorLabel);
                }
            });
        }
    }

    private void loadAllArticles() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
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
        return card;
    }

    private VBox createWebArticleCard(KnowledgeBase article) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        // Distinguish web results with a slight primary color border
        card.setStyle(
                "-fx-padding: 20; -fx-cursor: hand; -fx-border-color: -color-primary; -fx-border-width: 1px; -fx-border-radius: 8px;");

        // Title (Header)
        Label title = new Label(article.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-primary;");

        Label category = new Label("🌐 Web Result");
        category.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: -color-primary; -fx-border-color: -color-primary; -fx-border-radius: 4; -fx-padding: 2 6;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(category, title);

        // Content (Body preview)
        Label content = new Label(article.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-text-primary; -fx-line-spacing: 4;");

        // Source footer (We stored the source URL in the entity's category property
        // temporarily in the Util)
        Label source = new Label("Source/Link: " + article.getCategory());
        source.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-padding: 5 0 0 0;");

        card.getChildren().addAll(header, content, source);

        // Attempt to open link if valid
        card.setOnMouseClicked(e -> {
            String url = article.getCategory();
            if (url != null && url.contains("http")) {
                try {
                    // Extract just the URL if prefixed with something like "Wikipedia -
                    // https://..."
                    String cleanUrl = url.substring(url.indexOf("http")).trim();
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(cleanUrl));
                } catch (Exception ex) {
                    System.err.println("Could not open browser: " + ex.getMessage());
                }
            }
        });

        return card;
    }
}
