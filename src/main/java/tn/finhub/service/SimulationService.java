package tn.finhub.service;

import tn.finhub.dao.MarketDAO;
import tn.finhub.model.MarketPrice;
import tn.finhub.model.PortfolioItem;
import tn.finhub.model.SimulatedTrade;
import tn.finhub.model.Wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SimulationService {

    private final tn.finhub.service.WalletService walletService = new tn.finhub.service.WalletService();
    private final tn.finhub.dao.MarketDAO marketDAO = new tn.finhub.dao.MarketDAO();
    private final tn.finhub.service.MarketDataService marketDataService = new tn.finhub.service.MarketDataService();

    public String buyAsset(int userId, String symbol, BigDecimal quantity) throws Exception {
        MarketPrice currentPrice = marketDataService.getPrice(symbol);
        if (currentPrice == null)
            return "Market closed or asset unavailable.";

        BigDecimal exchangeRate = marketDataService.getUsdToTndRate();
        BigDecimal priceInTnd = currentPrice.getPrice().multiply(exchangeRate);
        BigDecimal cost = priceInTnd.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

        // Balance Check & Debit via WalletService (Handles Security & Ledger)
        try {
            // Find wallet first to ensure it exists
            Wallet wallet = walletService.getWallet(userId);
            if (wallet == null)
                return "No wallet found.";

            walletService.debit(wallet.getId(), cost, "MARKET BUY " + symbol.toUpperCase());
        } catch (RuntimeException e) {
            return "Transaction Failed: " + e.getMessage();
        }

        // Update Portfolio
        PortfolioItem item = marketDAO.getPortfolioItem(userId, symbol);
        if (item == null) {
            item = new PortfolioItem(0, userId, symbol, quantity, currentPrice.getPrice());
        } else {
            BigDecimal oldTotalUsd = item.getQuantity().multiply(item.getAverageCost());
            BigDecimal newCostUsd = quantity.multiply(currentPrice.getPrice());
            BigDecimal newTotalUsd = oldTotalUsd.add(newCostUsd);
            BigDecimal newQty = item.getQuantity().add(quantity);

            item.setQuantity(newQty);
            item.setAverageCost(newTotalUsd.divide(newQty, 4, RoundingMode.HALF_UP));
        }
        marketDAO.updatePortfolioItem(item);

        SimulatedTrade trade = new SimulatedTrade(0, userId, symbol, "BUY", quantity, currentPrice.getPrice(), cost,
                null);
        marketDAO.recordTrade(trade);

        return "SUCCESS";
    }

    public String sellAsset(int userId, String symbol, BigDecimal quantity) throws Exception {
        PortfolioItem item = marketDAO.getPortfolioItem(userId, symbol);
        if (item == null || item.getQuantity().compareTo(quantity) < 0) {
            return "Insufficient assets to sell.";
        }

        MarketPrice currentPrice = marketDataService.getPrice(symbol);

        BigDecimal exchangeRate = marketDataService.getUsdToTndRate();
        BigDecimal priceInTnd = currentPrice.getPrice().multiply(exchangeRate);
        BigDecimal value = priceInTnd.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

        Wallet wallet = walletService.getWallet(userId);
        if (wallet == null)
            return "No wallet found.";

        // Credit via WalletService
        walletService.credit(wallet.getId(), value, "MARKET SELL " + symbol.toUpperCase());

        BigDecimal newQty = item.getQuantity().subtract(quantity);
        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            marketDAO.deletePortfolioItem(item.getId());
        } else {
            item.setQuantity(newQty);
            marketDAO.updatePortfolioItem(item);
        }

        SimulatedTrade trade = new SimulatedTrade(0, userId, symbol, "SELL", quantity, currentPrice.getPrice(), value,
                null);
        marketDAO.recordTrade(trade);

        return "SUCCESS";
    }
}
