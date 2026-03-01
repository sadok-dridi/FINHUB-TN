package tn.finhub.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import tn.finhub.model.KnowledgeBase;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class KbWebSearchUtil {

    private static final String N8N_KB_WEBHOOK = io.github.cdimascio.dotenv.Dotenv.configure()
            .ignoreIfMissing().load()
            .get("N8N_KB_WEBHOOK_URL", "https://crashinburn.work.gd/webhook/kb-search");

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Fetches up to 3 articles from the n8n webhook asynchronously.
     * Uses Callbacks to return data to the JavaFX Application thread safely.
     */
    public static void fetchArticlesAsync(String query, Consumer<List<KnowledgeBase>> onSuccess,
            Consumer<String> onError) {
        if (query == null || query.isBlank()) {
            return;
        }

        try {
            // Encode the query cleanly for URL usage
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            URI uri = URI.create(N8N_KB_WEBHOOK + "?q=" + encodedQuery);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                List<KnowledgeBase> webArticles = new ArrayList<>();
                                JsonNode rootNode = mapper.readTree(response.body());

                                if (rootNode.isArray()) {
                                    for (JsonNode node : rootNode) {
                                        String title = node.has("title") ? node.get("title").asText()
                                                : "Untitled Web Result";
                                        String snippet = node.has("snippet") ? node.get("snippet").asText() : "";
                                        String source = node.has("source") ? node.get("source").asText() : "Web Search";

                                        KnowledgeBase kb = new KnowledgeBase();
                                        kb.setTitle(title);
                                        kb.setContent(snippet);
                                        // We reuse Category to store the URL or Source label locally
                                        kb.setCategory(source);
                                        webArticles.add(kb);
                                    }
                                }

                                // Safely return data to JavaFX UI thread
                                Platform.runLater(() -> onSuccess.accept(webArticles));

                            } catch (Exception e) {
                                e.printStackTrace();
                                System.err.println("JSON Parse Error: " + response.body());
                                Platform.runLater(() -> onError.accept(
                                        "Failed to parse web results. Please ensure n8n returns standard JSON."));
                            }
                        } else {
                            System.err.println("N8N KB Search failed with code: " + response.statusCode());
                            Platform.runLater(
                                    () -> onError.accept("Web search returned HTTP " + response.statusCode()));
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        Platform.runLater(() -> onError.accept("Network error targeting n8n. Is it running?"));
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> onError.accept(e.getMessage()));
        }
    }
}
