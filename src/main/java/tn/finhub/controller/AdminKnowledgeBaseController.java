package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.finhub.model.KnowledgeBase;
import tn.finhub.model.KnowledgeBaseModel;
import tn.finhub.util.DialogUtil;

import java.util.List;

public class AdminKnowledgeBaseController {

    @FXML
    private VBox listViewContainer;
    @FXML
    private VBox editorContainer;

    @FXML
    private TextField searchField;
    @FXML
    private ListView<KnowledgeBase> articlesListView;

    @FXML
    private Label editorTitleLabel;
    @FXML
    private TextField titleField;
    @FXML
    private TextField categoryField;
    @FXML
    private TextArea contentArea;
    @FXML
    private Button deleteButton;

    private final KnowledgeBaseModel model = new KnowledgeBaseModel();
    private KnowledgeBase currentArticle;

    private static List<KnowledgeBase> cachedArticles;

    public static void setCachedArticles(List<KnowledgeBase> articles) {
        cachedArticles = articles;
    }

    @FXML
    public void initialize() {
        // Setup ListView
        articlesListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(KnowledgeBase item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("article-cell");
                } else {
                    setText(item.getTitle() + " (" + item.getCategory() + ")");
                    if (!getStyleClass().contains("article-cell")) {
                        getStyleClass().add("article-cell");
                    }
                }
            }
        });

        articlesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectArticle(newVal);
            }
        });

        loadArticles();
        showList(); // Default view
    }

    private void showList() {
        listViewContainer.setVisible(true);
        editorContainer.setVisible(false);
        articlesListView.getSelectionModel().clearSelection();
    }

    private void showEditor() {
        listViewContainer.setVisible(false);
        editorContainer.setVisible(true);
    }

    private void loadArticles() {
        List<KnowledgeBase> articles;
        if (cachedArticles != null) {
            articles = cachedArticles;
            cachedArticles = null;
        } else {
            articles = model.getAllArticles();
        }
        articlesListView.getItems().setAll(articles);
    }

    private void selectArticle(KnowledgeBase article) {
        this.currentArticle = article;
        titleField.setText(article.getTitle());
        categoryField.setText(article.getCategory());
        contentArea.setText(article.getContent());

        editorTitleLabel.setText("Edit Article");
        deleteButton.setVisible(true);

        showEditor();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadArticles();
        } else {
            List<KnowledgeBase> results = model.searchArticles(query);
            articlesListView.getItems().setAll(results);
        }
    }

    @FXML
    private void handleNewArticle() {
        currentArticle = null;
        titleField.clear();
        categoryField.clear();
        contentArea.clear();

        editorTitleLabel.setText("New Article");
        deleteButton.setVisible(false);

        showEditor();
    }

    @FXML
    private void handleBack() {
        showList();
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText().trim();
        String category = categoryField.getText().trim();
        String content = contentArea.getText().trim();

        if (title.isEmpty() || category.isEmpty() || content.isEmpty()) {
            DialogUtil.showError("Validation Error", "All fields are required.");
            return;
        }

        try {
            if (currentArticle == null) {
                model.addArticle(title, category, content);
                DialogUtil.showInfo("Success", "Article created successfully.");
            } else {
                model.updateArticle(currentArticle.getId(), title, category, content);
                DialogUtil.showInfo("Success", "Article updated successfully.");
            }
            loadArticles(); // Refresh list
            showList(); // Return to list view
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Failed to save article: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (currentArticle == null)
            return;

        boolean confirm = DialogUtil.showConfirmation("Delete Article",
                "Are you sure you want to delete this article?");
        if (confirm) {
            try {
                model.deleteArticle(currentArticle.getId());
                loadArticles();
                DialogUtil.showInfo("Success", "Article deleted.");
                showList(); // Return to list view
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Error", "Failed to delete article.");
            }
        }
    }
}
