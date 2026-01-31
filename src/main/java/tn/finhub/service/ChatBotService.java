package tn.finhub.service;

import org.json.JSONArray;
import org.json.JSONObject;
import tn.finhub.dao.KnowledgeBaseDAO;
import tn.finhub.model.KnowledgeBase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class ChatBotService {

    private final KnowledgeBaseDAO kbDAO = new KnowledgeBaseDAO();

    // --- Configuration ---
    // Toggle this to true to use Local LLM (Ollama) and save API limits
    // "Consumes OS usage" instead of "API Quota"
    private static final boolean USE_LOCAL_LLM = true;

    private static final String GEMINI_API_KEY = "AIzaSyAxRwRvXVIib04wCqCxm9XoX7JRaY24aMs";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
            + GEMINI_API_KEY;

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "mistral"; // Standard fast model, user must have this pulled

    public String getResponse(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "How can I help you today?";
        }

        // 1. Local Knowledge Base (Always check first for speed)
        List<KnowledgeBase> matches = kbDAO.searchArticles(userMessage);
        // If we want to strictly use AI, we can skip this, but it's good practice.
        // For now, let's just use it as context if needed, or return if extremely
        // confident.

        // 2. Determine AI Provider
        try {
            if (USE_LOCAL_LLM) {
                return callOllamaAPI(userMessage);
            } else {
                return callGeminiAPI(userMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback if AI fails (e.g. Ollama not running)
            if (USE_LOCAL_LLM) {
                return "I couldn't connect to your local AI (Ollama). Please ensure it's running on port 11434, or disable local mode to use the Cloud API. (Error: "
                        + e.getMessage() + ")";
            }
            return "I'm having trouble connecting to the cloud. Please try again later. (Error: " + e.getMessage()
                    + ")";
        }
    }

    // --- Google Gemini (Cloud) ---
    private String callGeminiAPI(String prompt) throws Exception {
        String systemContext = getSystemPrompt();
        String fullPrompt = systemContext + "\n\nUser: " + prompt;

        JSONObject textPart = new JSONObject();
        textPart.put("text", fullPrompt);

        JSONObject parts = new JSONObject();
        parts.put("parts", new JSONArray().put(textPart));

        JSONObject payload = new JSONObject();
        payload.put("contents", new JSONArray().put(parts));

        // Add Generation Config to limit tokens (Quota Optimization)
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("maxOutputTokens", 150); // Limit response length
        generationConfig.put("temperature", 0.7);
        payload.put("generationConfig", generationConfig);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseGeminiResponse(response.body());
        } else {
            return "Gemini API Error (" + response.statusCode() + "): " + response.body();
        }
    }

    // --- Ollama (Local OS Usage) ---
    private String callOllamaAPI(String prompt) throws Exception {
        String systemContext = getSystemPrompt();

        // Ollama JSON format: { "model": "mistral", "prompt": "...", "stream": false }
        JSONObject payload = new JSONObject();
        payload.put("model", OLLAMA_MODEL);
        payload.put("prompt", systemContext + "\n\nUser: " + prompt + "\nAssistant:");
        payload.put("stream", false);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            if (json.has("response")) {
                return json.getString("response");
            }
        }
        return "Ollama Error: " + response.statusCode();
    }

    private String getSystemPrompt() {
        return "You are the FinHub Assistant. " +
                "Help users with wallets, transactions, escrow, and security. " +
                "Keep answers concise and helpful.";
    }

    private String parseGeminiResponse(String jsonBody) {
        try {
            JSONObject json = new JSONObject(jsonBody);
            JSONArray candidates = json.getJSONArray("candidates");
            if (candidates.length() > 0) {
                return candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                        .getString("text");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "I couldn't process the response.";
    }
}
