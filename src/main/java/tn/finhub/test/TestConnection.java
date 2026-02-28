package tn.finhub.test;

import tn.finhub.util.DBConnection;

public class TestConnection {

    public static void main(String[] args) {

        if (DBConnection.getInstance() != null) {
            System.out.println("✅ DATABASE CONNECTED SUCCESSFULLY");
        } else {
            System.out.println("❌ CONNECTION FAILED");
        }
    }
}
