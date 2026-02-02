package tn.finhub.service;

import tn.finhub.model.MarketPrice;
import java.math.BigDecimal;

public class AIAdvisoryService {

    public String getRecommendation(MarketPrice price, String riskProfile) {
        if (price == null)
            return "No Data";

        BigDecimal change = price.getChangePercent();
        // Simple Heuristics for Simulation
        boolean highVol = change.abs().compareTo(BigDecimal.valueOf(2.0)) > 0; // > 2% swing
        boolean upTrend = change.compareTo(BigDecimal.ZERO) > 0;

        if (riskProfile.equalsIgnoreCase("LOW")) {
            if (highVol)
                return " High Volatility. WAIT.";
            if (upTrend)
                return " Steady Growth. BUY.";
            return "HOLD";
        } else if (riskProfile.equalsIgnoreCase("HIGH")) {
            if (highVol && !upTrend)
                return " Dip Opportunity. BUY.";
            if (upTrend)
                return "takeprofit? CONSIDER SELL.";
            return "HOLD";
        }

        // Medium/Default
        if (upTrend)
            return "Simulated Buy Signal";
        return "Simulated Hold Signal";
    }
}
