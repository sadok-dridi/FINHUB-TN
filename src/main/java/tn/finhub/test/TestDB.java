package tn.finhub.test;

import tn.finhub.util.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        try {
            Connection conn = DBConnection.getInstance();
            System.out.println("Connected.");

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM users_local ORDER BY user_id DESC LIMIT 5");
            System.out.println("--- USERS LOCAL ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("user_id") + " | " + rs.getString("email"));
            }

            rs = st.executeQuery("SELECT * FROM wallets ORDER BY id DESC LIMIT 5");
            System.out.println("--- WALLETS ---");
            while (rs.next()) {
                System.out.println("WalletID: " + rs.getInt("id") + " | UserID: " + rs.getInt("user_id") + " | Bal: "
                        + rs.getBigDecimal("balance"));
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
