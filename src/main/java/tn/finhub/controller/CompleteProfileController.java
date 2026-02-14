package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tn.finhub.model.FinancialProfile;
<<<<<<< HEAD
import tn.finhub.service.FinancialProfileService;
=======
import tn.finhub.model.FinancialProfileModel;
>>>>>>> cd680ce (crud+controle de saisie)
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
    private ComboBox<String> currencyBox;

    @FXML
=======
>>>>>>> cd680ce (crud+controle de saisie)
    private Label errorLabel;

    @FXML
    public void initialize() {
        riskBox.getItems().addAll("LOW", "MEDIUM", "HIGH");
<<<<<<< HEAD
        currencyBox.getItems().addAll("TND", "EUR", "USD");
=======
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
>>>>>>> cd680ce (crud+controle de saisie)
    }

    @FXML
    public void handleSaveProfile() {
        try {
            if (incomeField.getText().isEmpty() || expensesField.getText().isEmpty() ||
<<<<<<< HEAD
                    savingsField.getText().isEmpty() || riskBox.getValue() == null || currencyBox.getValue() == null) {
=======
                    savingsField.getText().isEmpty() || riskBox.getValue() == null) {
>>>>>>> cd680ce (crud+controle de saisie)
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
                    currencyBox.getValue(),
                    true);

            FinancialProfileService service = new FinancialProfileService();
            service.updateProfile(profile);
=======
                    "TND",
                    true);

            FinancialProfileModel model = new FinancialProfileModel();
            model.update(profile);
>>>>>>> cd680ce (crud+controle de saisie)

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
