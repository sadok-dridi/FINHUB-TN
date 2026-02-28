package tn.finhub.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class MarketPrice {
    private String symbol;
    private BigDecimal price;
    private BigDecimal changePercent;
    private Timestamp lastUpdated;

    public MarketPrice(String symbol, BigDecimal price, BigDecimal changePercent, Timestamp lastUpdated) {
        this.symbol = symbol;
        this.price = price;
        this.changePercent = changePercent;
        this.lastUpdated = lastUpdated;
    }

    public MarketPrice() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
