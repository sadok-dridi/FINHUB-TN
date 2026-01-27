package tn.finhub.service;

import tn.finhub.dao.UserDAO;
import tn.finhub.dao.impl.UserDAOImpl;
import tn.finhub.model.User;
import tn.finhub.util.ApiClient;
import tn.finhub.util.SessionManager;
import java.io.IOException;
import tn.finhub.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.*;

import java.util.List;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;



public class UserService {

    private UserDAO userDAO = new UserDAOImpl();



    public List<User> getAllUsers() {
        return userDAO.findAll();
    }
    public void syncUsersFromServer() {
        List<User> serverUsers = ApiClient.fetchUsersFromServer();

        // Clear local table
        userDAO.deleteAll();

        // Insert fresh data
        for (User u : serverUsers) {
            userDAO.insert(u);
        }
    }



    public void deleteUser(int id) {
        deleteUserOnServer(id);
        userDAO.delete(id);
    }
    private void deleteUserOnServer(int userId) {
        String token = SessionManager.getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing JWT token in session");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiClient.BASE_URL + "/admin/users/" + userId))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .DELETE()
                .build();

        try {
            HttpResponse<String> response = ApiClient.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            if (code < 200 || code >= 300) {
                String body = response.body();
                throw new RuntimeException("Server delete failed, status=" + code + (body != null ? (", body=" + body) : ""));
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Server delete request failed", e);
        }
    }
}
