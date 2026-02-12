package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import tn.finhub.model.FinancialProfile;
import tn.finhub.model.User;
import tn.finhub.model.FinancialProfileModel; // Added import

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
    // @FXML private ComboBox<String> currencyBox; // Removed

    @FXML
    private ComboBox<String> dbModeBox;
    @FXML
    private TextField apiKeyField;

    @FXML
    private Label statusLabel;

    private final FinancialProfileModel profileModel = new FinancialProfileModel(); // Changed to Model
    private static final String PREF_DB_MODE = "db_mode";
    private static final String PREF_API_KEY = "market_api_key";

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

        // Load App Settings
        setupSettings();
    }

    private void setupFinancialFields() {
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

    private void setupSettings() {
        dbModeBox.getItems().addAll("Hosted", "Local");
        java.util.prefs.Preferences dbPrefs = java.util.prefs.Preferences
                .userNodeForPackage(tn.finhub.util.DBConnection.class);
        String currentMode = dbPrefs.get(PREF_DB_MODE, "Hosted");
        dbModeBox.setValue(currentMode);

        java.util.prefs.Preferences apiPrefs = java.util.prefs.Preferences
                .userNodeForPackage(tn.finhub.model.MarketModel.class);
        String currentKey = apiPrefs.get(PREF_API_KEY, "");
        apiKeyField.setText(currentKey);
    }

    // STALE-WHILE-REVALIDATE CACHE
    private static FinancialProfile cachedProfile = null;

    public static void setCachedProfile(FinancialProfile profile) {
        cachedProfile = profile;
    }

    private void loadFinancialData() {
        int userId = SessionManager.getUserId();

        // 0. Optimistic UI Update
        if (cachedProfile != null && cachedProfile.getUserId() == userId) {
            updateUI(cachedProfile);
        }

        // Background Task
        javafx.concurrent.Task<FinancialProfile> task = new javafx.concurrent.Task<>() {
            @Override
            protected FinancialProfile call() throws Exception {
                profileModel.ensureProfile(userId); // DB Write/Check
                return profileModel.findByUserId(userId);
            }
        };

        task.setOnSucceeded(e -> {
            FinancialProfile profile = task.getValue();
            if (profile != null) {
                cachedProfile = profile;
                updateUI(profile);
            }
        });

        task.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void updateUI(FinancialProfile profile) {
        incomeField.setText(String.valueOf(profile.getMonthlyIncome()));
        expensesField.setText(String.valueOf(profile.getMonthlyExpenses()));
        savingsField.setText(String.valueOf(profile.getSavingsGoal()));

        // Ensure items exist before setting value (though setupFinancialFields runs
        // first)
        if (!riskBox.getItems().contains(profile.getRiskTolerance())) {
            riskBox.getItems().add(profile.getRiskTolerance());
        }
        riskBox.setValue(profile.getRiskTolerance());
    }

    @FXML
    private void handleUpdateProfile() {
        try {
            int userId = SessionManager.getUserId();
            double income = Double.parseDouble(incomeField.getText());
            double expenses = Double.parseDouble(expensesField.getText());
            double savings = Double.parseDouble(savingsField.getText());
            String risk = riskBox.getValue();
            String currency = "TND"; // Hardcoded default

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

            profileModel.update(profile);

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

    @FXML
    private void handleSaveSettings() {
        try {
            // DB Settings
            String selectedMode = dbModeBox.getValue();
            if (selectedMode != null) {
                java.util.prefs.Preferences dbPrefs = java.util.prefs.Preferences
                        .userNodeForPackage(tn.finhub.util.DBConnection.class);
                String currentMode = dbPrefs.get(PREF_DB_MODE, "Hosted");

                if (!selectedMode.equals(currentMode)) {
                    dbPrefs.put(PREF_DB_MODE, selectedMode);
                    // Reset connection to force reconnection with new settings
                    tn.finhub.util.DBConnection.resetConnection();
                    showStatus("Switched to " + selectedMode + " DB. Restart recommended.");
                } else {
                    showStatus("Settings saved.");
                }
            }

            // API Key Settings
            String newApiKey = apiKeyField.getText();
            java.util.prefs.Preferences apiPrefs = java.util.prefs.Preferences
                    .userNodeForPackage(tn.finhub.model.MarketModel.class);
            if (newApiKey == null || newApiKey.isBlank()) {
                apiPrefs.remove(PREF_API_KEY);
            } else {
                apiPrefs.put(PREF_API_KEY, newApiKey.trim());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Error saving settings.");
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
        }
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        if (message.contains("Successfully") || message.contains("saved") || message.contains("Switched")) {
            statusLabel.setTextFill(javafx.scene.paint.Color.web("#10B981"));
        } else {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> statusLabel.setVisible(false));
        pause.play();
    }
}
