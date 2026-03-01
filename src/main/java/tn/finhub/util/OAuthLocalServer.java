package tn.finhub.util;

import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import org.json.JSONObject;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class OAuthLocalServer {

    // IMPORTANT: Replace with your actual Desktop OAuth App Client ID from Google
    // Cloud Console
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String GOOGLE_CLIENT_ID = dotenv.get("GOOGLE_CLIENT_ID", "");
    private static final String GOOGLE_CLIENT_SECRET = dotenv.get("GOOGLE_CLIENT_SECRET", "");
    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    private HttpServer server;
    private String codeVerifier;

    public CompletableFuture<String> startAndAuthenticate() {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8069), 0);
            int port = server.getAddress().getPort();
            // We'll add the trailing slash just to be perfectly identical to Google's
            // standard
            String redirectUri = "http://127.0.0.1:" + port + "/";

            server.createContext("/", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("code=")) {
                    String code = query.split("code=")[1].split("&")[0];
                    String responseHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <title>FinHub Authentication</title>
                                <style>
                                    body {
                                        background-color: #1a1625;
                                        color: #ffffff;
                                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                        display: flex;
                                        flex-direction: column;
                                        align-items: center;
                                        justify-content: center;
                                        height: 100vh;
                                        margin: 0;
                                    }
                                    .container {
                                        background-color: rgba(30, 27, 46, 0.85);
                                        border: 1px solid #2E2A45;
                                        border-radius: 16px;
                                        padding: 40px;
                                        text-align: center;
                                        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
                                        max-width: 400px;
                                    }
                                    h2 { color: #4ade80; margin-top: 0; }
                                    p { color: #9ca3af; line-height: 1.6; }
                                    .loader {
                                        border: 4px solid #2E2A45;
                                        border-top: 4px solid #c084fc;
                                        border-radius: 50%;
                                        width: 40px;
                                        height: 40px;
                                        animation: spin 1s linear infinite;
                                        margin: 20px auto;
                                    }
                                    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="loader"></div>
                                    <h2>Authentication Successful</h2>
                                    <p>You can safely close this tab and return to the FinHub application.</p>
                                </div>
                                <script>
                                    setTimeout(() => window.close(), 3000);
                                </script>
                            </body>
                            </html>
                            """;
                    exchange.sendResponseHeaders(200, responseHtml.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseHtml.getBytes());
                    os.close();

                    Platform.runLater(() -> stopServer());

                    // Proceed with code exchange asynchronously
                    CompletableFuture.runAsync(() -> {
                        try {
                            String idToken = exchangeCodeForToken(code, redirectUri);
                            future.complete(idToken);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });

                } else {
                    String responseHtml = "<html><body><h2>Authentication failed!</h2></body></html>";
                    exchange.sendResponseHeaders(400, responseHtml.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseHtml.getBytes());
                    os.close();

                    Platform.runLater(() -> stopServer());
                    future.completeExceptionally(new RuntimeException("Failed to get auth code from redirect"));
                }
            });

            server.setExecutor(null);
            server.start();

            // Generate PKCE code verifier and challenge
            codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);

            String authUrl = String.format(
                    "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=openid%%20email%%20profile&code_challenge=%s&code_challenge_method=S256",
                    AUTHORIZATION_ENDPOINT, GOOGLE_CLIENT_ID, redirectUri, codeChallenge);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(authUrl));
            } else {
                stopServer();
                future.completeExceptionally(new RuntimeException("Desktop browsing not supported"));
            }

        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String exchangeCodeForToken(String code, String redirectUri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String body = String.format(
                "client_id=%s&client_secret=%s&code=%s&code_verifier=%s&redirect_uri=%s&grant_type=authorization_code",
                GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, code, codeVerifier, redirectUri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            if (json.has("id_token")) {
                return json.getString("id_token");
            } else {
                throw new RuntimeException("Google Response didn't contain an id_token");
            }
        } else {
            throw new RuntimeException("Token exchange failed: " + response.statusCode() + " " + response.body());
        }
    }

    private String generateCodeVerifier() {
        SecureRandom sr = new SecureRandom();
        byte[] code = new byte[32];
        sr.nextBytes(code);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(code);
    }

    private String generateCodeChallenge(String verifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
