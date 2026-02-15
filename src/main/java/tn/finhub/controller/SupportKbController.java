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

    private final tn.finhub.model.KnowledgeBaseModel kbModel = new tn.finhub.model.KnowledgeBaseModel();

    @FXML
    public void initialize() {
        loadAllArticles();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
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
        return card;
    }
}
