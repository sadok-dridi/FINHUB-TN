package tn.finhub.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnection {

    private static Connection hostedInstance;
    private static Connection localInstance;

    private static final io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
            .ignoreIfMissing().load();

    // Hosted Credentials
    private static final String URL_HOSTED = dotenv.get("DB_HOSTED_URL");
    private static final String USER_HOSTED = dotenv.get("DB_HOSTED_USER");
    private static final String PASSWORD_HOSTED = dotenv.get("DB_HOSTED_PASSWORD");

    // Local Credentials
    private static final String URL_LOCAL = dotenv.get("DB_LOCAL_URL");
    private static final String USER_LOCAL = dotenv.get("DB_LOCAL_USER");
    private static final String PASSWORD_LOCAL = dotenv.get("DB_LOCAL_PASSWORD");

    private DBConnection() {
    }

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);

    /**
     * Legacy method: Returns the Hosted connection by default.
     * Kept for backward compatibility with existing DAOs.
     */
    public static Connection getInstance() {
        return getHostedConnection();
    }

    public static synchronized Connection getHostedConnection() {
        try {
            if (isValid(hostedInstance)) {
                return hostedInstance;
            }
            logger.info("Connecting to HOSTED database...");
            hostedInstance = DriverManager.getConnection(URL_HOSTED, USER_HOSTED, PASSWORD_HOSTED);
            logger.info("HOSTED connection established.");
        } catch (SQLException e) {
            logger.error("Failed to connect to HOSTED database", e);
        }
        return hostedInstance;
    }

    public static synchronized Connection getLocalConnection() {
        try {
            if (isValid(localInstance)) {
                return localInstance;
            }
            logger.info("Connecting to LOCAL database...");
            localInstance = DriverManager.getConnection(URL_LOCAL, USER_LOCAL, PASSWORD_LOCAL);
            logger.info("LOCAL connection established.");
        } catch (SQLException e) {
            logger.error("Failed to connect to LOCAL database", e);
            // Fallback: If local fails, try hosted? Or just fail?
            // For now, let's just return null or allow failure, as Market data is
            // non-critical (can show error)
        }
        return localInstance;
    }

    private static boolean isValid(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public static void resetConnection() {
        closeQuietly(hostedInstance);
        closeQuietly(localInstance);
        hostedInstance = null;
        localInstance = null;
    }

    private static void closeQuietly(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {
        }
    }

}
