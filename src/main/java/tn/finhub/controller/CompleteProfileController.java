package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tn.finhub.model.FinancialProfile;
import tn.finhub.model.FinancialProfileModel;
import tn.finhub.util.SessionManager;

public class CompleteProfileController {

    @FXML
    private TextField incomeField;

    @FXML
    private TextField expensesField;

    @FXML
    private TextField savingsField;

    @FXML
    private ComboBox<String> riskBox;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        riskBox.getItems().addAll("LOW", "MEDIUM", "HIGH");
        setupDecimalValidation(incomeField, expensesField, savingsField);
    }

    private void setupDecimalValidation(TextField... fields) {
        for (TextField field : fields) {
            java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> filter = change -> {
                String text = change.getControlNewText();
                if (text.matches("[0-9]*\\.?[0-9]*")) {
                    return change;
                }
                return null;
            };
            field.setTextFormatter(new javafx.scene.control.TextFormatter<>(filter));
        }
    }

    @FXML
    public void handleSaveProfile() {
        try {
            if (incomeField.getText().isEmpty() || expensesField.getText().isEmpty() ||
                    savingsField.getText().isEmpty() || riskBox.getValue() == null) {
                errorLabel.setText("Please fill in all fields.");
                return;
            }

            double income = Double.parseDouble(incomeField.getText());
            double expenses = Double.parseDouble(expensesField.getText());
            double savings = Double.parseDouble(savingsField.getText());

            FinancialProfile profile = new FinancialProfile(
                    SessionManager.getUserId(),
                    income,
                    expenses,
                    savings,
                    riskBox.getValue(),
                    "TND",
                    true);

            FinancialProfileModel model = new FinancialProfileModel();
            model.update(profile);

            // Navigate to Dashboard with fade animation
            tn.finhub.util.ViewUtils.setView(incomeField, "/view/user_dashboard.fxml");

        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter valid numbers for financial fields.");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error saving profile: " + e.getMessage());
        }
    }
}
