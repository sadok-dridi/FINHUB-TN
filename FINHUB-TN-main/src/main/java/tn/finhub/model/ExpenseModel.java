package tn.finhub.model;

import tn.finhub.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExpenseModel {

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    public boolean isTelegramLinked(int userId) {
        String sql = "SELECT telegram_chat_id FROM users_local WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String chatId = rs.getString("telegram_chat_id");
                    return chatId != null && !chatId.trim().isEmpty();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Expense> getExpenses(int userId) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM expenses WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Expense e = new Expense();
                    e.setId(rs.getInt("id"));
                    e.setUserId(rs.getInt("user_id"));
                    e.setMerchant(rs.getString("merchant"));
                    e.setTotalAmount(rs.getBigDecimal("total_amount"));
                    e.setCreatedAt(rs.getTimestamp("created_at"));
                    // Items are fetched lazily or via a separate call if needed,
                    // but for the list view we just need totals.
                    expenses.add(e);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }

    public List<ExpenseItem> getExpenseItems(int expenseId) {
        List<ExpenseItem> items = new ArrayList<>();
        String sql = "SELECT * FROM expense_items WHERE expense_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExpenseItem item = new ExpenseItem();
                    item.setId(rs.getInt("id"));
                    item.setExpenseId(rs.getInt("expense_id"));
                    item.setItemName(rs.getString("item_name"));
                    item.setPrice(rs.getBigDecimal("price"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public double getTotalExpenses(int userId) {
        String sql = "SELECT SUM(total_amount) FROM expenses WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
