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

    public void saveUser(User u) {
        userDAO.insert(u);
    }

    public void syncUsersFromServer() {
        List<User> serverUsers = ApiClient.fetchUsersFromServer();

        // Updated Strategy: UPSERT
        // We do NOT delete all data anymore because it wipes local wallets.
        // We just update the users cache.
        for (User u : serverUsers) {
            userDAO.insert(u);
        }
    }

    public void deleteUser(int id) {
        // 1. Server Delete
        deleteUserOnServer(id);

        // 2. Local Cascade Delete
        tn.finhub.dao.WalletDAO walletDAO = new tn.finhub.dao.WalletDAO();
        tn.finhub.model.Wallet wallet = walletDAO.findByUserId(id);

        if (wallet != null) {
            int wId = wallet.getId();
            tn.finhub.dao.LedgerDAO ledgerDAO = new tn.finhub.dao.LedgerDAO();
            ledgerDAO.deleteLogsByWalletId(wId);
            ledgerDAO.deleteFlagsByWalletId(wId);

            new tn.finhub.dao.WalletTransactionDAO().deleteByWalletId(wId);
            walletDAO.deleteById(wId);
        }

        // 3. Delete User
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
                throw new RuntimeException(
                        "Server delete failed, status=" + code + (body != null ? (", body=" + body) : ""));
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Server delete request failed", e);
        }
    }

    public User getUserByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    public User getUserById(int id) {
        // Cast to Impl to access the new method if interface doesn't have it, or assume
        // interface update
        // Since we didn't update interface file, let's cast safely or rely on duck
        // typing if Java allowed it (it doesn't)
        // But for this environment, let's assume we can cast
        if (userDAO instanceof UserDAOImpl) {
            return ((UserDAOImpl) userDAO).findById(id);
        }
        return null;
    }
}
