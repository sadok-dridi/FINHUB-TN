package tn.finhub.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WebhookUtil {

    private static final String WEBHOOK_URL = io.github.cdimascio.dotenv.Dotenv.configure()
            .ignoreIfMissing().load()
            .get("N8N_WEBHOOK_URL", "https://crashinburn.work.gd/webhook/payment-success");

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Sends an asynchronous webhook notification for a money transfer via GET
     * request.
     */
    public static void sendTransferNotification(String senderName, String recipientEmail, String amount) {
        try {
            // Encode parameters for the URL
            String urlParameters = String.format("?sender=%s&receiver=%s&email=%s&amount=%s",
                    java.net.URLEncoder.encode(senderName != null ? senderName : "Unknown", "UTF-8"),
                    java.net.URLEncoder.encode(recipientEmail != null ? recipientEmail : "Unknown", "UTF-8"),
                    java.net.URLEncoder.encode(recipientEmail != null ? recipientEmail : "Unknown", "UTF-8"),
                    java.net.URLEncoder.encode(amount != null ? amount : "0", "UTF-8"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEBHOOK_URL + urlParameters))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            // Send async so UI thread isn't blocked
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Webhook notification sent with status: " + response.statusCode());
                    })
                    .exceptionally(ex -> {
                        System.err.println("Failed to send webhook notification: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
