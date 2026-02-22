package tn.finhub.model;

import java.math.BigDecimal;

public class ExpenseItem {
    private int id;
    private int expenseId;
    private String itemName;
    private BigDecimal price;

    public ExpenseItem() {
    }

    public ExpenseItem(int id, int expenseId, String itemName, BigDecimal price) {
        this.id = id;
        this.expenseId = expenseId;
        this.itemName = itemName;
        this.price = price;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
