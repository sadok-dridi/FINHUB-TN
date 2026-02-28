package tn.finhub.model;

public class FinancialProfile {

    private int id;
    private int userId;

    private double monthlyIncome;
    private double monthlyExpenses;
    private double savingsGoal;

    private String riskTolerance; // LOW / MEDIUM / HIGH
    private String currency; // TND, EUR, USD

    private boolean profileCompleted;

    public FinancialProfile() {
    }

    public FinancialProfile(
            int userId,
            double monthlyIncome,
            double monthlyExpenses,
            double savingsGoal,
            String riskTolerance,
            String currency,
            boolean profileCompleted) {
        this.userId = userId;
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.savingsGoal = savingsGoal;
        this.riskTolerance = riskTolerance;
        this.currency = currency;
        this.profileCompleted = profileCompleted;
    }

    // ---------- Getters ----------
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public double getMonthlyIncome() {
        return monthlyIncome;
    }

    public double getMonthlyExpenses() {
        return monthlyExpenses;
    }

    public double getSavingsGoal() {
        return savingsGoal;
    }

    public String getRiskTolerance() {
        return riskTolerance;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    // ---------- Setters ----------
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setMonthlyIncome(double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public void setMonthlyExpenses(double monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses;
    }

    public void setSavingsGoal(double savingsGoal) {
        this.savingsGoal = savingsGoal;
    }

    public void setRiskTolerance(String riskTolerance) {
        this.riskTolerance = riskTolerance;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
}
