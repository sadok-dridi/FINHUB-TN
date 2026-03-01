package tn.finhub.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public class ZendeskUtil {

    private static final String ZENDESK_URL = io.github.cdimascio.dotenv.Dotenv.configure()
            .ignoreIfMissing().load()
            .get("ZENDESK_URL", "https://yoursubdomain.zendesk.com");

    private static final String ZENDESK_EMAIL = io.github.cdimascio.dotenv.Dotenv.configure()
            .ignoreIfMissing().load()
            .get("ZENDESK_EMAIL", "admin@example.com");

    private static final String ZENDESK_TOKEN = io.github.cdimascio.dotenv.Dotenv.configure()
            .ignoreIfMissing().load()
            .get("ZENDESK_API_TOKEN", "your_token");

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Creates a ticket in Zendesk using the free API plan features.
     * Posts asynchronously to prevent blocking the UI.
     */
    public static void createTicket(String subject, String message, String userName, String userEmail, int ticketId) {
        try {
            if (ZENDESK_URL == null || ZENDESK_URL.contains("yoursubdomain")) {
                System.out.println("Zendesk integration skipped: Missing configuration in .env");
                return;
            }

            // Simple JSON escaping
            String safeSubject = subject.replace("\"", "\\\"");
            String safeMessage = message.replace("\"", "\\\"");
            String safeName = userName != null ? userName.replace("\"", "\\\"") : "Unknown User";
            String safeEmail = userEmail != null ? userEmail.replace("\"", "\\\"") : "unknown@example.com";

            // Construct JSON payload manually compatible with Zendesk Ticket API
            String jsonPayload = "{\n" +
                    "  \"ticket\": {\n" +
                    "    \"subject\": \"" + safeSubject + "\",\n" +
                    "    \"external_id\": \"" + ticketId + "\",\n" +
                    "    \"comment\": {\n" +
                    "      \"body\": \"" + safeMessage + "\"\n" +
                    "    },\n" +
                    "    \"requester\": {\n" +
                    "      \"name\": \"" + safeName + "\",\n" +
                    "      \"email\": \"" + safeEmail + "\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            // Basic Auth format: email/token:token
            String authString = ZENDESK_EMAIL + "/token:" + ZENDESK_TOKEN;
            String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes("UTF-8"));

            String cleanUrl = ZENDESK_URL.endsWith("/") ? ZENDESK_URL.substring(0, ZENDESK_URL.length() - 1)
                    : ZENDESK_URL;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cleanUrl + "/api/v2/tickets.json"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + encodedAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Send async so UI thread isn't blocked
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Zendesk ticket created with status: " + response.statusCode());
                        if (response.statusCode() == 401) {
                            System.err.println("Zendesk 401 Error: Authentication failed.");
                            System.err.println(
                                    "-> Please check that ZENDESK_EMAIL and ZENDESK_API_TOKEN are correct in .env.");
                            System.err.println(
                                    "-> Ensure 'Token Access' is ENABLED in Zendesk Admin Center (Apps and integrations -> APIs -> Zendesk API).");
                            System.err.println("-> Try restarting the application to load the latest .env values.");
                        } else if (response.statusCode() >= 400) {
                            System.err.println("Zendesk Error payload: " + response.body());
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("Failed to reach Zendesk: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
