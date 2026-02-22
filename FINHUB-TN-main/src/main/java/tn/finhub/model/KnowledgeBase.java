package tn.finhub.model;

import java.sql.Timestamp;

public class KnowledgeBase {
    private int id;
    private String title;
    private String category;
    private String content;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public KnowledgeBase() {
    }

    public KnowledgeBase(int id, String title, String category, String content, Timestamp createdAt,
            Timestamp updatedAt) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
