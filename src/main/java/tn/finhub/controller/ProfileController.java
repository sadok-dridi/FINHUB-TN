package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import tn.finhub.model.FinancialProfile;
import tn.finhub.model.User;
import tn.finhub.service.FinancialProfileService;
import tn.finhub.util.SessionManager;
import tn.finhub.util.UserSession;

public class ProfileController {

    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;

    @FXML
    private TextField incomeField;
    @FXML
    private TextField expensesField;
    @FXML
    private TextField savingsField;
    @FXML
    private ComboBox<String> riskBox;
    @FXML
    private ComboBox<String> currencyBox;

    @FXML
    private Label statusLabel;

    private final FinancialProfileService profileService = new FinancialProfileService();

    @FXML
    public void initialize() {
        // Load User Info
        User user = UserSession.getInstance().getUser();
        if (user != null) {
            nameLabel.setText(user.getFullName());
            emailLabel.setText(user.getEmail());
            roleLabel.setText(user.getRole());
        }

        // Load Financial Profile
        setupFinancialFields();
        loadFinancialData();
    }

    private void setupFinancialFields() {
        riskBox.getItems().addAll("LOW", "MEDIUM", "HIGH");
        currencyBox.getItems().addAll("TND", "EUR", "USD");
    }

    private void loadFinancialData() {
        int userId = SessionManager.getUserId();
        profileService.ensureProfile(userId); // Safety check
        FinancialProfile profile = profileService.getByUserId(userId);

        if (profile != null) {
            incomeField.setText(String.valueOf(profile.getMonthlyIncome()));
            expensesField.setText(String.valueOf(profile.getMonthlyExpenses()));
            savingsField.setText(String.valueOf(profile.getSavingsGoal()));
            riskBox.setValue(profile.getRiskTolerance());
            currencyBox.setValue(profile.getCurrency());
        }
    }

    @FXML
    private void handleUpdateProfile() {
        try {
            int userId = SessionManager.getUserId();
            double income = Double.parseDouble(incomeField.getText());
            double expenses = Double.parseDouble(expensesField.getText());
            double savings = Double.parseDouble(savingsField.getText());
            String risk = riskBox.getValue();
            String currency = currencyBox.getValue();

            FinancialProfile profile = new FinancialProfile(
                    userId,
                    income,
                    expenses,
                    savings,
                    risk,
                    currency,
                    true);

            // We need the ID for the update to work correctly if the DAO uses ID,
            // but the current update implementation uses userId so this is fine.

            profileService.updateProfile(profile);

            showStatus("Profile updated successfully!");

        } catch (NumberFormatException e) {
            showStatus("Error: Invalid numbers entered.");
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Error updating profile.");
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
        }
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        if (message.contains("Successfully")) {
            statusLabel.setTextFill(javafx.scene.paint.Color.web("#10B981"));
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> statusLabel.setVisible(false));
        pause.play();
    }
}
