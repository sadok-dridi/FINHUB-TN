package tn.finhub.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WebhookUtil {

    private static final String TRANSFER_WEBHOOK_URL = io.github.cdimascio.dotenv.Dotenv.configure()
            .ignoreIfMissing().load()
            .get("N8N_WEBHOOK_URL_TRANSFER", "https://crashinburn.work.gd/webhook/payment-success");

    private static final String ESCROW_WEBHOOK_URL = io.github.cdimascio.dotenv.Dotenv.configure()
            .ignoreIfMissing().load()
            .get("N8N_WEBHOOK_URL_ESCROW", "https://crashinburn.work.gd/webhook/escrow-events");

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Sends an asynchronous webhook notification for standard transactions.
     */
    public static void sendTransferNotification(String senderName, String senderEmail, String recipientEmail,
            String amount) {
        try {
            // Encode parameters for the URL
            String urlParameters = String.format("?sender=%s&sender_email=%s&receiver_email=%s&amount=%s",
                    java.net.URLEncoder.encode(senderName != null ? senderName : "Unknown", "UTF-8"),
                    java.net.URLEncoder.encode(senderEmail != null ? senderEmail : "Unknown", "UTF-8"),
                    java.net.URLEncoder.encode(recipientEmail != null ? recipientEmail : "Unknown", "UTF-8"),
                    java.net.URLEncoder.encode(amount != null ? amount : "0", "UTF-8"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TRANSFER_WEBHOOK_URL + urlParameters))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            // Send async so UI thread isn't blocked
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Transfer Webhook sent with status: " + response.statusCode());
                    })
                    .exceptionally(ex -> {
                        System.err.println("Failed to send Transfer Webhook: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an asynchronous webhook notification for Escrow events.
     */
    public static void sendEscrowNotification(String senderName, String senderEmail, String recipientEmail,
            String amount, String eventType) {
        try {
            // Encode parameters for the URL
            String urlParameters = String.format("?event=%s&sender=%s&sender_email=%s&receiver_email=%s&amount=%s",
                    java.net.URLEncoder.encode(eventType != null ? eventType : "UNKNOWN", "UTF-8"),
                    java.net.URLEncoder.encode(senderName != null ? senderName : "Unknown", "UTF-8"),
                    java.net.URLEncoder.encode(senderEmail != null ? senderEmail : "Unknown", "UTF-8"),
                    java.net.URLEncoder.encode(recipientEmail != null ? recipientEmail : "Unknown", "UTF-8"),
                    java.net.URLEncoder.encode(amount != null ? amount : "0", "UTF-8"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ESCROW_WEBHOOK_URL + urlParameters))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            // Send async so UI thread isn't blocked
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Escrow Webhook sent with status: " + response.statusCode());
                    })
                    .exceptionally(ex -> {
                        System.err.println("Failed to send Escrow Webhook: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
