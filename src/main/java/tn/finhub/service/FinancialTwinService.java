package tn.finhub.service;

import tn.finhub.model.FinancialProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinancialTwinService {

    /**
     * Calculates the Financial Health Score (0-100).
     * Formula: (Savings / Income) * 100, capped at 100.
     * Logic:
     * - > 50: Healthy (Green)
     * - 20-50: Medium Risk (Yellow)
     * - < 20: High Risk (Red)
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
     * 
     * @param income   Projected monthly income
     * @param expenses Projected monthly expenses
     * @param months   Number of months to simulate
     * @return List of projected savings balance over time (cumulative)
     */
    public List<Double> runSimulation(double income, double expenses, int months) {
        List<Double> projection = new ArrayList<>();
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
