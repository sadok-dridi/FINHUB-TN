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

                logger.info("Connecting to HOSTED database...");
                instance = DriverManager.getConnection(URL_HOSTED, USER_HOSTED, PASSWORD_HOSTED);
                logger.info("Database connection established (Hosted)");
            }
        } catch (SQLException e) {
            logger.error("Database connection failed", e);
        }
        return instance;
    }

    public static Connection getHostedConnection() {
        return getInstance();
    }

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
