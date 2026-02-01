package tn.finhub.dao;

import tn.finhub.model.MarketPrice;
import tn.finhub.model.PortfolioItem;
import tn.finhub.model.SimulatedTrade;
import tn.finhub.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MarketDAO {

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    // --- Market Prices ---

    public void saveOrUpdatePrice(MarketPrice price) {
        String sql = """
                    INSERT INTO market_prices (symbol, price, change_percent, last_updated)
                    VALUES (?, ?, ?, NOW())
                    ON DUPLICATE KEY UPDATE price = VALUES(price), change_percent = VALUES(change_percent), last_updated = NOW()
                """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, price.getSymbol());
            ps.setBigDecimal(2, price.getPrice());
            ps.setBigDecimal(3, price.getChangePercent());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving market price", e);
        }
    }

    public MarketPrice getPrice(String symbol) {
        String sql = "SELECT * FROM market_prices WHERE symbol = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
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
            throw new RuntimeException("Error fetching market price", e);
        }
        return null;
    }

    // --- Portfolio ---

    public List<PortfolioItem> getPortfolio(int userId) {
        List<PortfolioItem> items = new ArrayList<>();
        String sql = "SELECT * FROM portfolio_items WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
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
            try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting portfolio item", e);
        }
    }

    // --- Trades ---

    public void recordTrade(SimulatedTrade trade) {
        String sql = "INSERT INTO simulated_trades (user_id, asset_symbol, action, quantity, price_at_transaction, transaction_date) VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
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
}
