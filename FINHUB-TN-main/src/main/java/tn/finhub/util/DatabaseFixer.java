package tn.finhub.util;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseFixer {
    public static void fixEscrowTable() {
        try (Connection conn = DBConnection.getInstance();
                Statement stmt = conn.createStatement()) {

            System.out.println("Fixing escrow table schema...");
            String sql = "ALTER TABLE escrow MODIFY COLUMN qr_code_image LONGTEXT";
            stmt.executeUpdate(sql);
            System.out.println("Successfully altered qr_code_image column to LONGTEXT.");

        } catch (Exception e) {
            System.err.println("Error fixing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        fixEscrowTable();
    }
}
