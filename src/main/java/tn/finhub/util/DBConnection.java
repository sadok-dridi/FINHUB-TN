package tn.finhub.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnection {

    private static Connection instance;

    private static final io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
            .ignoreIfMissing().load();

    // Hosted Credentials
    private static final String URL_HOSTED = dotenv.get("DB_HOSTED_URL");
    private static final String USER_HOSTED = dotenv.get("DB_HOSTED_USER");
    private static final String PASSWORD_HOSTED = dotenv.get("DB_HOSTED_PASSWORD");

    // Local Credentials (Default)
    private static final String URL_LOCAL = dotenv.get("DB_LOCAL_URL");
    private static final String USER_LOCAL = dotenv.get("DB_LOCAL_USER");
    private static final String PASSWORD_LOCAL = dotenv.get("DB_LOCAL_PASSWORD");

<<<<<<< HEAD
    private static final String PREF_DB_MODE = "db_mode";

=======
>>>>>>> cd680ce (crud+controle de saisie)
    private DBConnection() {
    }

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);

    public static Connection getInstance() {
        try {
            boolean isValid = false;
            try {
                if (instance != null && !instance.isClosed()) {
                    isValid = instance.isValid(2); // Wait 2 seconds for validation
                }
            } catch (SQLException ignored) {
                // If validation fails, we assume invalid
            }

            if (!isValid) {
                // Force closure if it was somehow "open" but invalid
                if (instance != null) {
                    try {
                        instance.close();
                    } catch (Exception ignored) {
                    }
                }

<<<<<<< HEAD
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DBConnection.class);
                String mode = prefs.get(PREF_DB_MODE, "Hosted"); // Default to Hosted as per user context

                String url, user, password;

                if ("Local".equalsIgnoreCase(mode)) {
                    url = URL_LOCAL;
                    user = USER_LOCAL;
                    password = PASSWORD_LOCAL;
                    logger.info("Connecting to LOCAL database...");
                } else {
                    url = URL_HOSTED;
                    user = USER_HOSTED;
                    password = PASSWORD_HOSTED;
                    logger.info("Connecting to HOSTED database...");
                }
=======
                // FORCE HOSTED MODE
                String mode = "Hosted";
                String url = URL_HOSTED;
                String user = USER_HOSTED;
                String password = PASSWORD_HOSTED;
                logger.info("Connecting to HOSTED database (Enforced)...");
>>>>>>> cd680ce (crud+controle de saisie)

                instance = DriverManager.getConnection(url, user, password);
                logger.info("Database connection established ({})", mode);
            }
        } catch (SQLException e) {
            logger.error("Database connection failed", e);
        }
        return instance;
    }

<<<<<<< HEAD
=======
    public static Connection getLocalConnection() {
        try {
            return DriverManager.getConnection(URL_LOCAL, USER_LOCAL, PASSWORD_LOCAL);
        } catch (SQLException e) {
            logger.error("Failed to connect to LOCAL DB", e);
            return null;
        }
    }

    public static Connection getHostedConnection() {
        try {
            return DriverManager.getConnection(URL_HOSTED, USER_HOSTED, PASSWORD_HOSTED);
        } catch (SQLException e) {
            logger.error("Failed to connect to HOSTED DB", e);
            throw new RuntimeException("Hosted DB Connection failed", e);
        }
    }

>>>>>>> cd680ce (crud+controle de saisie)
    public static void resetConnection() {
        try {
            if (instance != null && !instance.isClosed()) {
                instance.close();
                logger.info("Database connection closed for reset.");
            }
            instance = null;
        } catch (SQLException e) {
            logger.error("Error closing connection during reset", e);
        }
    }

}
