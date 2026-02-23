package tn.finhub.util;

import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class RecaptchaService {

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String SECRET_KEY = dotenv.get("RECAPTCHA_SECRET_KEY");
    // Public site key used by the client-side widget
    private static final String SITE_KEY = dotenv.get("RECAPTCHA_SITE_KEY");
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private RecaptchaService() {
    }

    public static String getSiteKey() {
        return SITE_KEY;
    }

    public static boolean isConfigured() {
        return SECRET_KEY != null && !SECRET_KEY.isBlank()
                && SITE_KEY   != null && !SITE_KEY.isBlank();
    }

    public static boolean verify(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (!isConfigured()) {
            // If not configured, treat as failure to avoid giving a false sense of security.
            return false;
        }
        try {
            String formData = "secret=" + URLEncoder.encode(SECRET_KEY, StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERIFY_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }

            JSONObject json = new JSONObject(response.body());
            return json.optBoolean("success", false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

