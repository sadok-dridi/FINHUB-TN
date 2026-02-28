package tn.finhub.model;

import tn.finhub.util.DBConnection;
import java.sql.*;

public class FinancialProfileModel {

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    public FinancialProfile findByUserId(int userId) {
        String sql = """
                    SELECT * FROM financial_profiles_local
                    WHERE user_id = ?
                """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                FinancialProfile p = new FinancialProfile();
                p.setId(rs.getInt("id"));
                p.setUserId(rs.getInt("user_id"));
                p.setMonthlyIncome(rs.getDouble("monthly_income"));
                p.setMonthlyExpenses(rs.getDouble("monthly_expenses"));
                p.setSavingsGoal(rs.getDouble("savings_goal"));
                p.setRiskTolerance(rs.getString("risk_tolerance"));
                p.setCurrency(rs.getString("currency"));
                p.setProfileCompleted(rs.getBoolean("profile_completed"));
                return p;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void create(FinancialProfile p) {
        String sql = """
                    INSERT INTO financial_profiles_local
                    (user_id, monthly_income, monthly_expenses, savings_goal,
                     risk_tolerance, currency, profile_completed)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, p.getUserId());
            ps.setDouble(2, p.getMonthlyIncome());
            ps.setDouble(3, p.getMonthlyExpenses());
            ps.setDouble(4, p.getSavingsGoal());
            ps.setString(5, p.getRiskTolerance());
            ps.setString(6, p.getCurrency());
            ps.setBoolean(7, p.isProfileCompleted());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(FinancialProfile p) {
        String sql = """
                    UPDATE financial_profiles_local
                    SET monthly_income = ?,
                        monthly_expenses = ?,
                        savings_goal = ?,
                        risk_tolerance = ?,
                        currency = ?,
                        profile_completed = ?
                    WHERE user_id = ?
                """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setDouble(1, p.getMonthlyIncome());
            ps.setDouble(2, p.getMonthlyExpenses());
            ps.setDouble(3, p.getSavingsGoal());
            ps.setString(4, p.getRiskTolerance());
            ps.setString(5, p.getCurrency());
            ps.setBoolean(6, p.isProfileCompleted());
            ps.setInt(7, p.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUserId(int currentUserId, int newUserId) {
        String sql = "UPDATE financial_profiles_local SET user_id = ? WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, newUserId);
            ps.setInt(2, currentUserId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void ensureProfile(int userId) {
        FinancialProfile profile = findByUserId(userId);
        if (profile == null) {
            profile = new FinancialProfile(
                    userId,
                    0.0,
                    0.0,
                    0.0,
                    "LOW",
                    "TND",
                    false);
            create(profile);
        }
    }

    public boolean isProfileCompleted(int userId) {
        FinancialProfile profile = findByUserId(userId);
        return profile != null && profile.isProfileCompleted();
    }

    // ==========================================
    // BUSINESS LOGIC (Migrated from Service)
    // ==========================================

    /**
     * Calculates the Financial Health Score (0-100).
     */
    public int calculateHealthScore(double income, double expenses) {
        if (income <= 0)
            return 0;

        double savings = income - expenses;
        if (savings < 0)
            return 0; // Debt scenario

        int score = (int) ((savings / income) * 100);

        // Bonus points for low expense ratio (living below means)
        if (expenses < (income * 0.5)) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    public String getHealthStatus(int score) {
        if (score >= 60)
            return "Healthy";
        if (score >= 30)
            return "Medium Risk";
        return "High Risk";
    }

    public String getHealthColor(int score) {
        if (score >= 60)
            return "-color-success";
        if (score >= 30)
            return "-color-warning";
        return "-color-error";
    }

    /**
     * Generates a "What-if" simulation for N months.
     */
    public java.util.List<Double> runSimulation(double income, double expenses, int months) {
        java.util.List<Double> projection = new java.util.ArrayList<>();
        double currentSavings = 0; // Start fresh or could load existing wallet balance
        double monthlySavings = income - expenses;

        for (int i = 0; i < months; i++) {
            currentSavings += monthlySavings;
            projection.add(currentSavings);
        }
        return projection;
    }

    /**
     * Generates AI-like educational feedback based on the profile.
     */
    public String generateKeyInsights(double income, double expenses, double savingsGoal) {
        StringBuilder insight = new StringBuilder();
        double savings = income - expenses;
        double savingsRatio = (income > 0) ? (savings / income) * 100 : 0;

        insight.append("🧠 **Financial Twin Analysis**\n\n");

        // 1. Savings Analysis
        if (savings <= 0) {
            insight.append("⚠️ **Critical Warning**: You are spending more than you earn. ");
            insight.append("Immediate action required: Reduce discretionary spending or find new income sources.\n\n");
        } else if (savingsRatio < 20) {
            insight.append("⚠️ **Low Savings Rate**: You're saving only ").append(String.format("%.1f", savingsRatio))
                    .append("% of your income. ");
            insight.append("Try to aim for at least 20% by cutting small daily expenses.\n\n");
        } else {
            insight.append("✅ **Good Savings Habit**: You are saving ").append(String.format("%.1f", savingsRatio))
                    .append("% of your income. Keep it up!\n\n");
        }

        // 2. Goal Feasibility
        if (savingsGoal > 0 && savings > 0) {
            int monthsToGoal = (int) Math.ceil(savingsGoal / savings);
            if (monthsToGoal > 60) { // 5 years
                insight.append("📉 **Long Road Ahead**: At this rate, it will take ").append(monthsToGoal)
                        .append(" months to reach your goal. ");
                insight.append("Consider adjusting your goal or increasing your monthly savings.\n\n");
            } else {
                insight.append("🎯 **Goal Within Reach**: You can reach your savings goal in approx. ")
                        .append(monthsToGoal).append(" months.\n\n");
            }
        }

        // 3. Actionable Tip
        if (expenses > (income * 0.7)) {
            insight.append("💡 **Tip**: Your recurring expenses are quite high (")
                    .append((int) ((expenses / income) * 100)).append("%). ");
            insight.append("Review your subscriptions and rent/utilities to find potential savings.");
        } else {
            insight.append(
                    "💡 **Tip**: Your expense management is solid. Consider investing your surplus savings for better growth.");
        }

        return insight.toString();
    }
}
