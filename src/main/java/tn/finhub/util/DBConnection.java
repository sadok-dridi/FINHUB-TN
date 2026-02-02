package tn.finhub.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnection {

    private static Connection instance;

    private static final String URL = "jdbc:mariadb://localhost:3306/finhub";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // put password if you have one

        private DBConnection() {
    }
    private static final Logger logger =
            LoggerFactory.getLogger(DBConnection.class);


    public static Connection getInstance() {
        try {
            if (instance == null || instance.isClosed()) {
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                logger.info("Database connection established");
            }
        } catch (SQLException e) {
            logger.error("Database connection failed", e);
        }
        return instance;
    }

}
