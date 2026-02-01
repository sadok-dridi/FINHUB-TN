package tn.finhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.finhub.dao.MarketDAO;
import tn.finhub.model.MarketPrice;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class MarketDataService {

    private final MarketDAO marketDAO = new MarketDAO();
    private static final long CACHE_DURATION_MS = 60 * 1000; // 1 minute cache

    // CoinGecko API
    private static final String API_URL = "https://api.coingecko.com/api/v3/simple/price";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Circuit Breaker & Rate Limiting
    private static long apiBlockedUntil = 0;
    private static final long BLOCK_DURATION_MS = 60 * 1000; // Block for 1 min if 429 occurs

    public MarketPrice getPrice(String symbol) {
        // CoinGecko uses IDs (e.g., "bitcoin"), assume symbol is the ID here
        // 1. Check local DB/Cache
        MarketPrice cached = marketDAO.getPrice(symbol);
        boolean isCacheValid = false;

        if (cached != null) {
            long age = System.currentTimeMillis() - cached.getLastUpdated().getTime();
            if (age < CACHE_DURATION_MS) {
                isCacheValid = true;
            }
        }

        // Return cached if valid OR if API is blocked
        if (isCacheValid || System.currentTimeMillis() < apiBlockedUntil) {
            return cached;
        }

        // 2. Fetch from API (Individual fetch is inefficient on CoinGecko, but
        // supported)
        return fetchFromApi(new String[] { symbol }).get(symbol);
    }

    public Map<String, MarketPrice> getPrices(String[] symbols) {
        Map<String, MarketPrice> results = new HashMap<>();

        // 1. Check if we really need to fetch
        // Collect symbols that need updating
        StringBuilder idsToFetch = new StringBuilder();
        boolean needsFetch = false;

        for (String s : symbols) {
            MarketPrice cached = marketDAO.getPrice(s);
            boolean valid = false;
            if (cached != null) {
                results.put(s, cached);
                long age = System.currentTimeMillis() - cached.getLastUpdated().getTime();
                if (age < CACHE_DURATION_MS) {
                    valid = true;
                }
            }

            if (!valid) {
                if (idsToFetch.length() > 0)
                    idsToFetch.append(",");
                idsToFetch.append(s);
                needsFetch = true;
            }
        }

        if (!needsFetch || System.currentTimeMillis() < apiBlockedUntil) {
            return results;
        }

        // 2. Fetch from API
        Map<String, MarketPrice> freshPrices = fetchFromApi(idsToFetch.toString().split(","));
        results.putAll(freshPrices);

        return results;
    }

    private Map<String, MarketPrice> fetchFromApi(String[] symbols) {
        Map<String, MarketPrice> fetched = new HashMap<>();
        if (symbols.length == 0)
            return fetched;

        String ids = String.join(",", symbols);
        String uri = API_URL + "?ids=" + ids + "&vs_currencies=usd&include_24hr_change=true";

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
                        // 24h change might be null
                        BigDecimal change = BigDecimal.ZERO;
                        if (data.has("usd_24h_change") && !data.get("usd_24h_change").isNull()) {
                            change = BigDecimal.valueOf(data.get("usd_24h_change").asDouble());
                        }

                        MarketPrice mp = new MarketPrice(symbol, price, change, Timestamp.from(Instant.now()));
                        marketDAO.saveOrUpdatePrice(mp);
                        fetched.put(symbol, mp);
                    } else {
                        // Skip specific asset if no price data but don't crash
                        System.err.println("Warning: No 'usd' price for asset: " + symbol);
                    }
                }
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Validation Error: " + e.getMessage());
            return fetched;
        }
        return fetched;
    }

    public java.util.List<MarketPrice> getHistory(String symbol) {
        // Fetch 24h history for sparkline/chart
        // Endpoint: /coins/{id}/market_chart?vs_currency=usd&days=1
        java.util.List<MarketPrice> history = new java.util.ArrayList<>();
        if (System.currentTimeMillis() < apiBlockedUntil)
            return history;

        String uri = "https://api.coingecko.com/api/v3/coins/" + symbol + "/market_chart?vs_currency=usd&days=1";

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
                // Reduce data points for performance (take every 12th point ~ every hour)
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

    public java.util.List<tn.finhub.model.CandleData> getOHLC(String symbol) {
        java.util.List<tn.finhub.model.CandleData> ohlcList = new java.util.ArrayList<>();
        if (System.currentTimeMillis() < apiBlockedUntil)
            return ohlcList;

        String uri = "https://api.coingecko.com/api/v3/coins/" + symbol + "/ohlc?vs_currency=usd&days=1";

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

    // Exchange Rate Cache
    private static java.math.BigDecimal cachedUsdToTndRate = java.math.BigDecimal.valueOf(3.15); // Default fallback
    private static long rateLastUpdated = 0;
    private static final long RATE_CACHE_DURATION = 3600 * 1000; // 1 Hour

    public java.math.BigDecimal getUsdToTndRate() {
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
                    cachedUsdToTndRate = java.math.BigDecimal.valueOf(rate);
                    rateLastUpdated = System.currentTimeMillis();
                    System.out.println("Updated Exchange Rate: 1 USD = " + rate + " TND");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch exchange rate: " + e.getMessage());
            // Keep using cached/fallback rate
        }

        return cachedUsdToTndRate;
    }
}
