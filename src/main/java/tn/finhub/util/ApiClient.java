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
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

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
            HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/admin/users").openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization",
                    "Bearer " + SessionManager.getToken());
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Server error");
            }

            String json = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())).lines().collect(Collectors.joining());

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                    json,
                    new TypeReference<List<User>>() {
                    });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String signup(String fullName, String email, String password) throws Exception {
        String json = """
                {
                  "full_name": "%s",
                  "email": "%s",
                  "password": "%s",
                  "role": "CLIENT"
                }
                """.formatted(fullName, email, password);

        URL url = new URL(BASE_URL + "/signup");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int status = conn.getResponseCode();
        InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

        String responseBody = new BufferedReader(new InputStreamReader(stream))
                .lines().collect(Collectors.joining("\n"));

        if (status < 200 || status >= 300) {
            throw new RuntimeException("Signup failed: " + responseBody);
        }

        // Parse verification link
        JSONObject body = new JSONObject(responseBody);
        return body.getString("verification_link");
    }

    public static User login(String email, String password) throws Exception {
        String json = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Login failed: " + response.body());
        }

        JSONObject body = new JSONObject(response.body());
        JSONObject userJson = body.getJSONObject("user");

        return new User(
                userJson.getInt("id"),
                userJson.getString("email"),
                userJson.getString("role"),
                userJson.optString("full_name", ""));
    }

    public static String sendForgotPasswordRequest(String email) throws Exception {
        String json = """
                {
                  "email": "%s"
                }
                """.formatted(email);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/forgot-password"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Request failed: " + response.body());
        }

        JSONObject body = new JSONObject(response.body());
        return body.getString("reset_link");
    }
}
