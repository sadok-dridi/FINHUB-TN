package tn.finhub.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class SimulatedTrade {
    private int id;
    private int userId;
    private String assetSymbol;
    private String action; // BUY or SELL
    private BigDecimal quantity;
    private BigDecimal priceAtTransaction;
    private BigDecimal totalCost;
    private Timestamp transactionDate;

    public SimulatedTrade(int id, int userId, String assetSymbol, String action, BigDecimal quantity,
            BigDecimal priceAtTransaction, BigDecimal totalCost, Timestamp transactionDate) {
        this.id = id;
        this.userId = userId;
        this.assetSymbol = assetSymbol;
        this.action = action;
        this.quantity = quantity;
        this.priceAtTransaction = priceAtTransaction;
        this.totalCost = totalCost;
        this.transactionDate = transactionDate;
    }

    public SimulatedTrade() {
    }

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

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtTransaction() {
        return priceAtTransaction;
    }

    public void setPriceAtTransaction(BigDecimal priceAtTransaction) {
        this.priceAtTransaction = priceAtTransaction;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Timestamp getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Timestamp transactionDate) {
        this.transactionDate = transactionDate;
    }
}
