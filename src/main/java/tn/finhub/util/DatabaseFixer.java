package tn.finhub.util;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseFixer {
    public static void fixEscrowTable() {
        try (Connection conn = DBConnection.getInstance();
                Statement stmt = conn.createStatement()) {

            System.out.println("Fixing escrow table schema...");
            String sql1 = "ALTER TABLE escrow MODIFY COLUMN qr_code_image LONGTEXT";
            stmt.executeUpdate(sql1);
            System.out.println("Successfully altered qr_code_image column to LONGTEXT.");

            try {
                String sql2 = "ALTER TABLE escrow ADD COLUMN docusign_envelope_id VARCHAR(255) DEFAULT NULL";
                stmt.executeUpdate(sql2);
                System.out.println("Successfully added docusign_envelope_id column.");
            } catch (Exception ex) {
                // Column might already exist
                System.out.println("docusign_envelope_id column might already exist: " + ex.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error fixing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        fixEscrowTable();
    }
}
