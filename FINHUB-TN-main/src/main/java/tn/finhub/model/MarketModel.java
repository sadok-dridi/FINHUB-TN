package tn.finhub.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.finhub.util.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketModel {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache Configuration
    private static final long CACHE_DURATION_MS = 60 * 1000; // 60s Cache
    private static long apiBlockedUntil = 0;
    private static final long BLOCK_DURATION_MS = 60 * 1000;

    // Exchange Rate Cache
    private static BigDecimal cachedUsdToTndRate = BigDecimal.valueOf(3.15);
    private static long rateLastUpdated = 0;
    private static final long RATE_CACHE_DURATION = 3600 * 1000; // 1 Hour

    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3/simple/price";

    // ========================
    // DATA ACCESS (DAO Logic)
    // ========================

    private Connection getLocalConnection() {
        Connection conn = DBConnection.getLocalConnection();
        return (conn != null) ? conn : DBConnection.getHostedConnection();
    }

    private Connection getHostedConnection() {
        return DBConnection.getHostedConnection();
    }

    // --- Market Prices (LOCAL DB) ---

    private void saveOrUpdatePrice(MarketPrice price) {
        String sql = """
                    INSERT INTO market_prices (symbol, price, change_percent, last_updated)
                    VALUES (?, ?, ?, NOW())
                    ON DUPLICATE KEY UPDATE price = VALUES(price), change_percent = VALUES(change_percent), last_updated = NOW()
                """;
        try (PreparedStatement ps = getLocalConnection().prepareStatement(sql)) {
            ps.setString(1, price.getSymbol());
            ps.setBigDecimal(2, price.getPrice());
            ps.setBigDecimal(3, price.getChangePercent());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving market price to local DB: " + e.getMessage());
        }
    }

    private MarketPrice getPriceFromDb(String symbol) {
        String sql = "SELECT * FROM market_prices WHERE symbol = ?";
        try (PreparedStatement ps = getLocalConnection().prepareStatement(sql)) {
            ps.setString(1, symbol);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new MarketPrice(
                        rs.getString("symbol"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("change_percent"),
                        rs.getTimestamp("last_updated"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching market price from local DB: " + e.getMessage());
        }
        return null;
    }

    // --- Portfolio (HOSTED DB) ---

    public List<PortfolioItem> getPortfolio(int userId) {
        List<PortfolioItem> items = new ArrayList<>();
        String sql = "SELECT * FROM portfolio_items WHERE user_id = ?";
        try (PreparedStatement ps = getHostedConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new PortfolioItem(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("symbol"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("average_cost")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching portfolio", e);
        }
        return items;
    }

    public PortfolioItem getPortfolioItem(int userId, String symbol) {
        String sql = "SELECT * FROM portfolio_items WHERE user_id = ? AND symbol = ?";
        try (PreparedStatement ps = getHostedConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, symbol);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new PortfolioItem(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("symbol"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("average_cost"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching portfolio item", e);
        }
        return null;
    }

    public void updatePortfolioItem(PortfolioItem item) {
        if (item.getId() == 0) {
            // New item
            String sql = "INSERT INTO portfolio_items (user_id, symbol, quantity, average_cost) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = getHostedConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, item.getUserId());
                ps.setString(2, item.getSymbol());
                ps.setBigDecimal(3, item.getQuantity());
                ps.setBigDecimal(4, item.getAverageCost());
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next())
                    item.setId(rs.getInt(1));
            } catch (SQLException e) {
                throw new RuntimeException("Error creating portfolio item", e);
            }
        } else {
            // Update existing
            String sql = "UPDATE portfolio_items SET quantity = ?, average_cost = ? WHERE id = ?";
            try (PreparedStatement ps = getHostedConnection().prepareStatement(sql)) {
                ps.setBigDecimal(1, item.getQuantity());
                ps.setBigDecimal(2, item.getAverageCost());
                ps.setInt(3, item.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error updating portfolio item", e);
            }
        }
    }

    public void deletePortfolioItem(int itemId) {
        String sql = "DELETE FROM portfolio_items WHERE id = ?";
        try (PreparedStatement ps = getHostedConnection().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting portfolio item", e);
        }
    }

    public void deletePortfolioByUserId(int userId) {
        String sql = "DELETE FROM portfolio_items WHERE user_id = ?";
        try (PreparedStatement ps = getHostedConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting portfolio for user " + userId, e);
        }
    }

    public void liquidatePortfolioToWallet(int userId) {
        // 1. Fetch Portfolio
        List<PortfolioItem> items = getPortfolio(userId);
        if (items.isEmpty())
            return;

        WalletModel walletModel = new WalletModel();
        Wallet wallet = walletModel.findByUserId(userId);
        if (wallet == null)
            throw new RuntimeException("User has no wallet to liquidate assets into.");

        BigDecimal totalLiquidationValue = BigDecimal.ZERO;
        StringBuilder liquidationLog = new StringBuilder("Liquidated: ");

        // 2. Sell Each Item
        for (PortfolioItem item : items) {
            String symbol = item.getSymbol();
            BigDecimal quantity = item.getQuantity();

            // Get Current Price (Force Refresh to be fair)
            MarketPrice price = getPrice(symbol);
            BigDecimal currentPriceUsd = (price != null) ? price.getPrice() : BigDecimal.ZERO;

            // If price is 0 (API error), we might need to fallback or skip.
            // For now, if 0, we assume asset is worthless or data missing.
            // Better to try fetching fresh.
            if (currentPriceUsd.compareTo(BigDecimal.ZERO) == 0) {
                // Try one last fetch
                Map<String, MarketPrice> fresh = fetchFromApi(new String[] { symbol });
                if (fresh.containsKey(symbol)) {
                    currentPriceUsd = fresh.get(symbol).getPrice();
                }
            }

            BigDecimal exchangeRate = getUsdToTndRate();
            BigDecimal priceInTnd = currentPriceUsd.multiply(exchangeRate);
            BigDecimal value = priceInTnd.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

            totalLiquidationValue = totalLiquidationValue.add(value);
            liquidationLog.append(symbol).append("(").append(quantity).append(") @ ").append(priceInTnd)
                    .append(" TND; ");

            // Record Trade
            SimulatedTrade trade = new SimulatedTrade(0, userId, symbol, "SELL", quantity, currentPriceUsd,
                    value, null);
            recordTrade(trade);
        }

        // 3. Credit Wallet
        if (totalLiquidationValue.compareTo(BigDecimal.ZERO) > 0) {
            walletModel.credit(wallet.getId(), totalLiquidationValue, "Asset Liquidation before Deletion");
            System.out.println("Liquidated Portfolio for User " + userId + ": " + totalLiquidationValue + " TND");
        }

        // 4. Clear Portfolio (Delete items)
        deletePortfolioByUserId(userId);
    }

    public void deleteTradesByUserId(int userId) {
        String sql = "DELETE FROM simulated_trades WHERE user_id = ?";
        try (PreparedStatement ps = getHostedConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting trades for user " + userId, e);
        }
    }

    // --- Trades (HOSTED DB) ---

    public void recordTrade(SimulatedTrade trade) {
        String sql = "INSERT INTO simulated_trades (user_id, asset_symbol, action, quantity, price_at_transaction, transaction_date) VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = getHostedConnection().prepareStatement(sql)) {
            ps.setInt(1, trade.getUserId());
            ps.setString(2, trade.getAssetSymbol());
            ps.setString(3, trade.getAction());
            ps.setBigDecimal(4, trade.getQuantity());
            ps.setBigDecimal(5, trade.getPriceAtTransaction());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error recording trade", e);
        }
    }

    public List<SimulatedTrade> getTradeHistory(int userId) {
        List<SimulatedTrade> trades = new ArrayList<>();
        String sql = "SELECT * FROM simulated_trades WHERE user_id = ? ORDER BY transaction_date DESC";
        try (PreparedStatement ps = getHostedConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                trades.add(new SimulatedTrade(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("asset_symbol"),
                        rs.getString("action"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("price_at_transaction"),
                        rs.getBigDecimal("total_cost"),
                        rs.getTimestamp("transaction_date")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching trade history", e);
        }
        return trades;
    }

    // ========================
    // AI ADVISORY LOGIC
    // ========================
    public String getRecommendation(MarketPrice price, String riskProfile) {
        if (price == null)
            return "No Data";

        java.math.BigDecimal change = price.getChangePercent();
        // Simple Heuristics for Simulation
        boolean highVol = change.abs().compareTo(java.math.BigDecimal.valueOf(2.0)) > 0; // > 2% swing
        boolean upTrend = change.compareTo(java.math.BigDecimal.ZERO) > 0;

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

    // --- Trading Logic (Moved from SimulationService) ---

    public String buyAsset(int userId, String symbol, BigDecimal quantity) {
        try {
            MarketPrice currentPrice = getPrice(symbol);
            if (currentPrice == null)
                return "Market closed or asset unavailable.";

            BigDecimal exchangeRate = getUsdToTndRate();
            BigDecimal priceInTnd = currentPrice.getPrice().multiply(exchangeRate);
            BigDecimal cost = priceInTnd.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

            // Balance Check & Debit via WalletModel
            WalletModel walletModel = new WalletModel();
            Wallet wallet = walletModel.getWallet(userId);
            if (wallet == null)
                return "No wallet found.";

            if ("FROZEN".equals(wallet.getStatus())) {
                return "Wallet is FROZEN. Trading disabled.";
            }

            walletModel.debit(wallet.getId(), cost, "MARKET BUY " + symbol.toUpperCase());

            // Update Portfolio
            PortfolioItem item = getPortfolioItem(userId, symbol);
            if (item == null) {
                item = new PortfolioItem(0, userId, symbol, quantity, currentPrice.getPrice());
                updatePortfolioItem(item); // Insert
            } else {
                BigDecimal oldTotalUsd = item.getQuantity().multiply(item.getAverageCost());
                BigDecimal newCostUsd = quantity.multiply(currentPrice.getPrice());
                BigDecimal newTotalUsd = oldTotalUsd.add(newCostUsd);
                BigDecimal newQty = item.getQuantity().add(quantity);

                item.setQuantity(newQty);
                item.setAverageCost(newTotalUsd.divide(newQty, 4, RoundingMode.HALF_UP));
                updatePortfolioItem(item); // Update
            }

            SimulatedTrade trade = new SimulatedTrade(0, userId, symbol, "BUY", quantity, currentPrice.getPrice(), cost,
                    null);
            recordTrade(trade);

            return "SUCCESS";
        } catch (Exception e) {
            return "Transaction Failed: " + e.getMessage();
        }
    }

    public String sellAsset(int userId, String symbol, BigDecimal quantity) {
        try {
            PortfolioItem item = getPortfolioItem(userId, symbol);
            if (item == null || item.getQuantity().compareTo(quantity) < 0) {
                return "Insufficient assets to sell.";
            }

            MarketPrice currentPrice = getPrice(symbol);
            BigDecimal exchangeRate = getUsdToTndRate();
            BigDecimal priceInTnd = currentPrice.getPrice().multiply(exchangeRate);
            BigDecimal value = priceInTnd.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

            WalletModel walletModel = new WalletModel();
            Wallet wallet = walletModel.getWallet(userId);
            if (wallet == null)
                return "No wallet found.";

            if ("FROZEN".equals(wallet.getStatus())) {
                return "Wallet is FROZEN. Trading disabled.";
            }

            // Credit via WalletModel
            walletModel.credit(wallet.getId(), value, "MARKET SELL " + symbol.toUpperCase());

            BigDecimal newQty = item.getQuantity().subtract(quantity);
            if (newQty.compareTo(BigDecimal.ZERO) == 0) {
                deletePortfolioItem(item.getId());
            } else {
                item.setQuantity(newQty);
                updatePortfolioItem(item);
            }

            SimulatedTrade trade = new SimulatedTrade(0, userId, symbol, "SELL", quantity, currentPrice.getPrice(),
                    value, null);
            recordTrade(trade);

            return "SUCCESS";
        } catch (Exception e) {
            return "Transaction Failed: " + e.getMessage();
        }
    }

    // ========================
    // MARKET DATA LOGIC (Service Logic)
    // ========================

    private String getApiKey() {
        // 1. Check User Preferences (Overridden by UI)
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(MarketModel.class);
        String prefKey = prefs.get("market_api_key", null);
        if (prefKey != null && !prefKey.isBlank()) {
            return prefKey;
        }

        // 2. Check .env
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing()
                .load();
        return dotenv.get("MARKET_API_KEY");
    }

    public void forceRefreshPrices(String[] symbols) {
        if (System.currentTimeMillis() < apiBlockedUntil)
            return;
        fetchFromApi(symbols);
    }

    public MarketPrice getPrice(String symbol) {
        // 1. Check local DB/Cache
        MarketPrice cached = getPriceFromDb(symbol);

        if (cached != null) {
            return cached;
        }

        // Return cached if valid OR if API is blocked
        if (System.currentTimeMillis() < apiBlockedUntil) {
            return cached;
        }

        // 2. Fetch from API (Fallback for first load)
        Map<String, MarketPrice> fetched = fetchFromApi(new String[] { symbol });
        return fetched.get(symbol);
    }

    public Map<String, MarketPrice> getPrices(String[] symbols) {
        return getPrices(symbols, false);
    }

    public Map<String, MarketPrice> getPrices(String[] symbols, boolean forceRefresh) {
        Map<String, MarketPrice> results = new HashMap<>();
        StringBuilder idsToFetch = new StringBuilder();
        boolean needsFetch = forceRefresh;

        for (String s : symbols) {
            MarketPrice cached = getPriceFromDb(s);
            boolean valid = false;
            if (cached != null) {
                results.put(s, cached);
                long age = System.currentTimeMillis() - cached.getLastUpdated().getTime();
                if (age < CACHE_DURATION_MS) {
                    valid = true;
                }
            }

            if (!valid && !forceRefresh) {
                if (idsToFetch.length() > 0)
                    idsToFetch.append(",");
                idsToFetch.append(s);
                needsFetch = true;
            }
        }

        if (forceRefresh) {
            Map<String, MarketPrice> fresh = fetchFromApi(symbols);
            results.putAll(fresh);
            return results;
        }

        if (!needsFetch || System.currentTimeMillis() < apiBlockedUntil) {
            return results;
        }

        Map<String, MarketPrice> freshPrices = fetchFromApi(idsToFetch.toString().split(","));
        results.putAll(freshPrices);

        return results;
    }

    private Map<String, MarketPrice> fetchFromApi(String[] symbols) {
        Map<String, MarketPrice> fetched = new HashMap<>();
        if (symbols.length == 0)
            return fetched;

        String ids = String.join(",", symbols);
        String uri = COINGECKO_API_URL + "?ids=" + ids + "&vs_currencies=usd&include_24hr_change=true"
                + "&x_cg_demo_api_key=" + getApiKey();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                System.err.println("CoinGecko API Rate Limit. Blocking for 60s.");
                apiBlockedUntil = System.currentTimeMillis() + BLOCK_DURATION_MS;
                return fetched;
            }

            if (response.statusCode() != 200) {
                System.err.println("CoinGecko API Error: " + response.statusCode());
                return fetched;
            }

            JsonNode root = objectMapper.readTree(response.body());

            for (String symbol : symbols) {
                if (root.has(symbol)) {
                    JsonNode data = root.get(symbol);
                    if (data.has("usd") && !data.get("usd").isNull()) {
                        BigDecimal price = BigDecimal.valueOf(data.get("usd").asDouble());
                        BigDecimal change = BigDecimal.ZERO;
                        if (data.has("usd_24h_change") && !data.get("usd_24h_change").isNull()) {
                            change = BigDecimal.valueOf(data.get("usd_24h_change").asDouble());
                        }

                        MarketPrice mp = new MarketPrice(symbol, price, change, Timestamp.from(Instant.now()));
                        saveOrUpdatePrice(mp);
                        fetched.put(symbol, mp);
                    } else {
                        System.err.println("Warning: No 'usd' price for asset: " + symbol);
                    }
                }
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Validation Error: " + e.getMessage());
        }
        return fetched;
    }

    public List<MarketPrice> getHistory(String symbol) {
        List<MarketPrice> history = new ArrayList<>();
        if (System.currentTimeMillis() < apiBlockedUntil)
            return history;

        String uri = "https://api.coingecko.com/api/v3/coins/" + symbol + "/market_chart?vs_currency=usd&days=1"
                + "&x_cg_demo_api_key=" + getApiKey();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                System.err.println("CoinGecko History Rate Limit.");
                apiBlockedUntil = System.currentTimeMillis() + BLOCK_DURATION_MS;
                return history;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("prices")) {
                JsonNode prices = root.get("prices");
                int count = 0;
                for (JsonNode point : prices) {
                    if (count++ % 12 != 0)
                        continue;

                    long timestamp = point.get(0).asLong();
                    double val = point.get(1).asDouble();
                    MarketPrice mp = new MarketPrice(symbol, BigDecimal.valueOf(val), BigDecimal.ZERO,
                            new Timestamp(timestamp));
                    history.add(mp);
                }
            }
        } catch (Exception e) {
            System.err.println("History Fetch Error: " + e.getMessage());
        }
        return history;
    }

    public List<tn.finhub.model.CandleData> getOHLC(String symbol) {
        List<tn.finhub.model.CandleData> ohlcList = new ArrayList<>();
        if (System.currentTimeMillis() < apiBlockedUntil)
            return ohlcList;

        String uri = "https://api.coingecko.com/api/v3/coins/" + symbol + "/ohlc?vs_currency=usd&days=1"
                + "&x_cg_demo_api_key=" + getApiKey();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                System.err.println("CoinGecko OHLC Rate Limit.");
                apiBlockedUntil = System.currentTimeMillis() + BLOCK_DURATION_MS;
                return ohlcList;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.isArray()) {
                for (JsonNode point : root) {
                    long timestamp = point.get(0).asLong();
                    double open = point.get(1).asDouble();
                    double high = point.get(2).asDouble();
                    double low = point.get(3).asDouble();
                    double close = point.get(4).asDouble();
                    ohlcList.add(new tn.finhub.model.CandleData(timestamp, open, high, low, close));
                }
            }
        } catch (Exception e) {
            System.err.println("OHLC Fetch Error: " + e.getMessage());
        }
        return ohlcList;
    }

    public BigDecimal getUsdToTndRate() {
        if (System.currentTimeMillis() - rateLastUpdated < RATE_CACHE_DURATION) {
            return cachedUsdToTndRate;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://open.er-api.com/v6/latest/USD"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("rates") && root.get("rates").has("TND")) {
                    double rate = root.get("rates").get("TND").asDouble();
                    cachedUsdToTndRate = BigDecimal.valueOf(rate);
                    rateLastUpdated = System.currentTimeMillis();
                    System.out.println("Updated Exchange Rate: 1 USD = " + rate + " TND");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch exchange rate: " + e.getMessage());
        }

        return cachedUsdToTndRate;
    }
}
