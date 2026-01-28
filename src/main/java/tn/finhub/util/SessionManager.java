package tn.finhub.util;

public class SessionManager {

    private static int userId;
    private static String fullName;
    private static String email;
    private static String role;
    private static String token;

    public static void login(int id, String name, String e, String r, String t) {
        userId = id;
        fullName = name;
        email = e;
        role = r;
        token = t;

        // Populate UserSession
        tn.finhub.model.User user = new tn.finhub.model.User(id, e, r, name);
        UserSession.getInstance().setUser(user);
    }

    public static int getUserId() {
        return userId;
    }

    public static String getFullName() {
        return fullName;
    }

    public static String getEmail() {
        return email;
    }

    public static String getRole() {
        return role;
    }

    public static String getToken() {
        return token;
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public static void logout() {
        userId = 0;
        fullName = null;
        email = null;
        role = null;
        token = null;
        UserSession.getInstance().cleanUserSession();
    }
}
