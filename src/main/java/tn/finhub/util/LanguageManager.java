package tn.finhub.util;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Manages application language settings and provides access to localized
 * strings.
 * Supports English and French languages with persistence of user preference.
 */
public class LanguageManager {

    private static LanguageManager instance;
    private ResourceBundle resourceBundle;
    private Locale currentLocale;
    private static final String PREF_KEY_LANGUAGE = "app.language";
    private static final Preferences prefs = Preferences.userNodeForPackage(LanguageManager.class);

    // Supported locales
    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale FRENCH = Locale.FRENCH;

    // Language change listeners
    private final List<LanguageChangeListener> listeners = new ArrayList<>();

    private LanguageManager() {
        // Load saved language preference or default to English
        String savedLang = prefs.get(PREF_KEY_LANGUAGE, "en");
        currentLocale = savedLang.equals("fr") ? FRENCH : ENGLISH;
        loadResourceBundle();
    }

    /**
     * Get the singleton instance of LanguageManager
     */
    public static synchronized LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    /**
     * Load the resource bundle for the current locale
     */
    private void loadResourceBundle() {
        try {
            resourceBundle = ResourceBundle.getBundle("messages", currentLocale);
        } catch (MissingResourceException e) {
            System.err.println("Resource bundle not found for locale: " + currentLocale);
            // Fallback to English
            resourceBundle = ResourceBundle.getBundle("messages", ENGLISH);
        }
    }

    /**
     * Get a localized string by key
     * 
     * @param key The resource bundle key
     * @return The localized string, or the key itself if not found
     */
    public String getString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            System.err.println("Missing resource key: " + key);
            return key;
        }
    }

    /**
     * Get a localized string with parameter substitution
     * 
     * @param key    The resource bundle key
     * @param params Parameters to substitute in the string
     * @return The formatted localized string
     */
    public String getString(String key, Object... params) {
        try {
            String pattern = resourceBundle.getString(key);
            return String.format(pattern, params);
        } catch (MissingResourceException e) {
            System.err.println("Missing resource key: " + key);
            return key;
        }
    }

    /**
     * Get the current locale
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Get the current language code (en or fr)
     */
    public String getCurrentLanguageCode() {
        return currentLocale.getLanguage();
    }

    /**
     * Get the current language display name
     */
    public String getCurrentLanguageName() {
        return currentLocale.equals(FRENCH) ? "Français" : "English";
    }

    /**
     * Set the application language
     * 
     * @param locale The locale to set (ENGLISH or FRENCH)
     */
    public void setLanguage(Locale locale) {
        if (!currentLocale.equals(locale)) {
            currentLocale = locale;
            loadResourceBundle();

            // Save preference
            prefs.put(PREF_KEY_LANGUAGE, locale.getLanguage());

            // Notify listeners
            notifyLanguageChanged();
        }
    }

    /**
     * Set the application language by language code
     * 
     * @param languageCode "en" for English, "fr" for French
     */
    public void setLanguage(String languageCode) {
        Locale locale = languageCode.equals("fr") ? FRENCH : ENGLISH;
        setLanguage(locale);
    }

    /**
     * Get the resource bundle (for JavaFX FXML usage)
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Add a language change listener
     */
    public void addLanguageChangeListener(LanguageChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a language change listener
     */
    public void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners that the language has changed
     */
    private void notifyLanguageChanged() {
        for (LanguageChangeListener listener : listeners) {
            listener.onLanguageChanged(currentLocale);
        }
    }

    /**
     * Interface for language change listeners
     */
    public interface LanguageChangeListener {
        void onLanguageChanged(Locale newLocale);
    }

    /**
     * Get list of available languages
     */
    public static List<String> getAvailableLanguages() {
        return Arrays.asList("English", "Français");
    }

    /**
     * Get list of available language codes
     */
    public static List<String> getAvailableLanguageCodes() {
        return Arrays.asList("en", "fr");
    }
}
