package tn.finhub.dao.impl;

import tn.finhub.dao.FinancialProfileDAO;
import tn.finhub.model.FinancialProfile;
import tn.finhub.util.DBConnection;

import java.sql.*;

public class FinancialProfileDAOImpl implements FinancialProfileDAO {

    private final Connection connection = DBConnection.getInstance();

    @Override
    public FinancialProfile findByUserId(int userId) {

        String sql = """
                    SELECT * FROM financial_profiles_local
                    WHERE user_id = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

    @Override
    public void create(FinancialProfile p) {

        String sql = """
                    INSERT INTO financial_profiles_local
                    (user_id, monthly_income, monthly_expenses, savings_goal,
                     risk_tolerance, currency, profile_completed)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

    @Override
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

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

    @Override
    public void updateUserId(int currentUserId, int newUserId) {
        String sql = "UPDATE financial_profiles_local SET user_id = ? WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, newUserId);
            ps.setInt(2, currentUserId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
