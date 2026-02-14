package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import tn.finhub.model.FinancialProfile;
import tn.finhub.model.User;
<<<<<<< HEAD
import tn.finhub.service.FinancialProfileService;
import tn.finhub.util.SessionManager;
import tn.finhub.util.UserSession;
=======
import tn.finhub.model.FinancialProfileModel; // Added import

import tn.finhub.util.SessionManager;
import tn.finhub.util.UserSession;
import tn.finhub.util.LanguageManager;
>>>>>>> cd680ce (crud+controle de saisie)

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
<<<<<<< HEAD
    @FXML
    private ComboBox<String> currencyBox;
=======
    // @FXML private ComboBox<String> currencyBox; // Removed

>>>>>>> cd680ce (crud+controle de saisie)
    @FXML
    private ComboBox<String> dbModeBox;
    @FXML
    private TextField apiKeyField;

    @FXML
    private Label statusLabel;

<<<<<<< HEAD
    private final FinancialProfileService profileService = new FinancialProfileService();
=======
    @FXML
    private ComboBox<String> languageBox;

    private final FinancialProfileModel profileModel = new FinancialProfileModel(); // Changed to Model
>>>>>>> cd680ce (crud+controle de saisie)
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
<<<<<<< HEAD
        currencyBox.getItems().addAll("TND", "EUR", "USD");
    }

    private void setupSettings() {
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
    }

    private void setupSettings() {
        // Language Settings
        languageBox.getItems().addAll(LanguageManager.getAvailableLanguages());
        String currentLang = LanguageManager.getInstance().getCurrentLanguageName();
        languageBox.setValue(currentLang);

        // Database Settings
>>>>>>> cd680ce (crud+controle de saisie)
        dbModeBox.getItems().addAll("Hosted", "Local");
        java.util.prefs.Preferences dbPrefs = java.util.prefs.Preferences
                .userNodeForPackage(tn.finhub.util.DBConnection.class);
        String currentMode = dbPrefs.get(PREF_DB_MODE, "Hosted");
        dbModeBox.setValue(currentMode);

        java.util.prefs.Preferences apiPrefs = java.util.prefs.Preferences
<<<<<<< HEAD
                .userNodeForPackage(tn.finhub.service.MarketDataService.class);
=======
                .userNodeForPackage(tn.finhub.model.MarketModel.class);
>>>>>>> cd680ce (crud+controle de saisie)
        String currentKey = apiPrefs.get(PREF_API_KEY, "");
        apiKeyField.setText(currentKey);
    }

<<<<<<< HEAD
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
=======
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
>>>>>>> cd680ce (crud+controle de saisie)
    }

    @FXML
    private void handleUpdateProfile() {
        try {
            int userId = SessionManager.getUserId();
            double income = Double.parseDouble(incomeField.getText());
            double expenses = Double.parseDouble(expensesField.getText());
            double savings = Double.parseDouble(savingsField.getText());
            String risk = riskBox.getValue();
<<<<<<< HEAD
            String currency = currencyBox.getValue();
=======
            String currency = "TND"; // Hardcoded default
>>>>>>> cd680ce (crud+controle de saisie)

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

<<<<<<< HEAD
            profileService.updateProfile(profile);
=======
            profileModel.update(profile);
>>>>>>> cd680ce (crud+controle de saisie)

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
<<<<<<< HEAD
=======
            // Language Settings
            String selectedLanguage = languageBox.getValue();
            if (selectedLanguage != null) {
                String currentLang = LanguageManager.getInstance().getCurrentLanguageName();
                if (!selectedLanguage.equals(currentLang)) {
                    // Set language based on selection
                    if (selectedLanguage.equals("Français")) {
                        LanguageManager.getInstance().setLanguage(LanguageManager.FRENCH);
                    } else {
                        LanguageManager.getInstance().setLanguage(LanguageManager.ENGLISH);
                    }

                    // Reload the current view to apply language changes
                    javafx.application.Platform.runLater(() -> {
                        try {
                            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                                    getClass().getResource("/view/profile.fxml"),
                                    LanguageManager.getInstance().getResourceBundle());
                            javafx.scene.Parent newView = loader.load();
                            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) languageBox
                                    .getScene().lookup("#dashboardContent");
                            if (contentArea != null) {
                                contentArea.getChildren().setAll(newView);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }

>>>>>>> cd680ce (crud+controle de saisie)
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
<<<<<<< HEAD
                    .userNodeForPackage(tn.finhub.service.MarketDataService.class);
=======
                    .userNodeForPackage(tn.finhub.model.MarketModel.class);
>>>>>>> cd680ce (crud+controle de saisie)
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
