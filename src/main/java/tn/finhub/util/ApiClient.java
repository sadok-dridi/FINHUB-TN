package tn.finhub.util;

import tn.finhub.model.User;

import java.net.http.HttpClient;
import java.time.Duration;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class ApiClient {

    public static final String BASE_URL = "https://apifinhub.work.gd";

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static HttpClient getClient() {
        return client;
    }
    public static String inviteAdmin(String email) throws Exception {

        URL url = new URL(BASE_URL + "/admin/invite?email=" + email);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + SessionManager.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        int status = conn.getResponseCode();

        InputStream stream = (status >= 200 && status < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        conn.disconnect();

        return response.toString();
    }
    public static List<User> fetchUsersFromServer() {

        try {
            HttpURLConnection conn = (HttpURLConnection)
                    new URL(BASE_URL + "/admin/users").openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization",
                    "Bearer " + SessionManager.getToken());
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Server error");
            }

            String json = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            ).lines().collect(Collectors.joining());

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                    json,
                    new TypeReference<List<User>>() {}
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
