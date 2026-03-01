package tn.finhub.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class CandleData {
    private Timestamp timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;

    public CandleData(long timestamp, double open, double high, double low, double close) {
        this.timestamp = new Timestamp(timestamp);
        this.open = BigDecimal.valueOf(open);
        this.high = BigDecimal.valueOf(high);
        this.low = BigDecimal.valueOf(low);
        this.close = BigDecimal.valueOf(close);
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }
}
