package tn.finhub.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Expense {
    private int id;
    private int userId;
    private String merchant;
    private BigDecimal totalAmount;
    private Timestamp createdAt;
    private List<ExpenseItem> items;

    public Expense() {
        this.items = new ArrayList<>();
    }

    public Expense(int id, int userId, String merchant, BigDecimal totalAmount, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.merchant = merchant;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.items = new ArrayList<>();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<ExpenseItem> getItems() {
        return items;
    }

    public void setItems(List<ExpenseItem> items) {
        this.items = items;
    }

    public void addItem(ExpenseItem item) {
        this.items.add(item);
    }
}
