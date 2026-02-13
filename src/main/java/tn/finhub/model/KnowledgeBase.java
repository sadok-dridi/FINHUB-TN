package tn.finhub.model;

import java.sql.Timestamp;

public class KnowledgeBase {
    private int id;
    private String category; // WALLET, ESCROW, TRANSACTION, SECURITY
    private String question;
    private String answer;
    private Timestamp createdAt;

    public KnowledgeBase() {
    }

    public KnowledgeBase(String category, String question, String answer) {
        this.category = category;
        this.question = question;
        this.answer = answer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
