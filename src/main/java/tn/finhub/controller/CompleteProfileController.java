package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tn.finhub.model.FinancialProfile;
<<<<<<< HEAD
import tn.finhub.model.FinancialProfileModel;
=======
import tn.finhub.service.FinancialProfileService;
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
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
<<<<<<< HEAD
=======
    private ComboBox<String> currencyBox;

    @FXML
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
    private Label errorLabel;

    @FXML
    public void initialize() {
        riskBox.getItems().addAll("LOW", "MEDIUM", "HIGH");
<<<<<<< HEAD
=======
        currencyBox.getItems().addAll("TND", "EUR", "USD");
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
    }

    @FXML
    public void handleSaveProfile() {
        try {
            if (incomeField.getText().isEmpty() || expensesField.getText().isEmpty() ||
<<<<<<< HEAD
                    savingsField.getText().isEmpty() || riskBox.getValue() == null) {
=======
                    savingsField.getText().isEmpty() || riskBox.getValue() == null || currencyBox.getValue() == null) {
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853
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
<<<<<<< HEAD
                    "TND",
                    true);

            FinancialProfileModel model = new FinancialProfileModel();
            model.update(profile);
=======
                    currencyBox.getValue(),
                    true);

            FinancialProfileService service = new FinancialProfileService();
            service.updateProfile(profile);
>>>>>>> 3239865d261585c607c2f3379522c60b1fede853

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
