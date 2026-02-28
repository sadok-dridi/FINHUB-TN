package tn.finhub.model;

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
        return getHostedConnection();
    }

    public static synchronized Connection getHostedConnection() {
        try {
            if (isValid(instance)) {
                return instance;
            }
            logger.info("Connecting to HOSTED database...");
            instance = DriverManager.getConnection(URL_HOSTED, USER_HOSTED, PASSWORD_HOSTED);
            logger.info("HOSTED connection established.");
        } catch (SQLException e) {
            logger.error("Failed to connect to HOSTED database", e);
        }
        return instance;
    }

    private static boolean isValid(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public static void resetConnection() {
        closeQuietly(instance);
        instance = null;
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
