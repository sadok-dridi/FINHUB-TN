package tn.finhub.model;

import org.json.JSONArray;
import org.json.JSONObject;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class ChatAssistantModel {

    private final KnowledgeBaseModel kbModel = new KnowledgeBaseModel();

    // --- Configuration ---
    // Toggle this to true to use Local LLM (Ollama) and save API limits
    // "Consumes OS usage" instead of "API Quota"
    private static final boolean USE_LOCAL_LLM = true;

    private static final Dotenv dotenv = Dotenv.load();
    private static final String GEMINI_API_KEY = dotenv.get("GEMINI_API_KEY");
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
            + GEMINI_API_KEY;

    // Ollama Configuration
    private static final String OLLAMA_CHAT_URL = "http://localhost:11434/api/chat";
    private static final String OLLAMA_MODEL = "mistral:7b-instruct"; // Optimized model for 3060 6GB
    private static final int MAX_HISTORY = 10; // Keep last 10 messages for context
    private static final int CONTEXT_WINDOW = 4096; // 4k context fits well in 6GB VRAM with minimal offloading issues

    // Conversation History (in-memory per session)
    private final JSONArray conversationHistory;

    public ChatAssistantModel() {
        this.conversationHistory = new JSONArray();
        // Initialize with system prompt
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", getSystemPrompt());
        this.conversationHistory.put(systemMessage);
    }

    public String getResponse(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "How can I help you today?";
        }

        // 1. Local Knowledge Base (Always check first for speed)
        List<KnowledgeBase> matches = kbModel.searchArticles(userMessage);

        // Append context from KB if found (Optional enhancement: inject KB results into
        // system prompt)
        String finalPrompt = userMessage;
        if (!matches.isEmpty()) {
            finalPrompt += "\n\n(Relevant Context from FinHub Knowledge Base: " + matches.get(0).getAnswer() + ")";
        }

        // 2. Determine AI Provider
        try {
            if (USE_LOCAL_LLM) {
                return callOllamaChatAPI(finalPrompt);
            } else {
                return callGeminiAPI(finalPrompt);
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
        // Gemini REST API is stateless, so we just send the current prompt (unless we
        // implement full history there too)
        // For now maintaining statelessness for Cloud to save tokens.
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
    private String callOllamaChatAPI(String userText) throws Exception {
        // 1. Add User Message to History
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userText);
        conversationHistory.put(userMsg);

        // 2. Prune History if too long (Keep System Prompt at index 0)
        while (conversationHistory.length() > MAX_HISTORY) {
            // Remove the second item (index 1), keeping the System Prompt (index 0)
            conversationHistory.remove(1);
        }

        // 3. Build Payload
        JSONObject payload = new JSONObject();
        payload.put("model", OLLAMA_MODEL);
        payload.put("messages", conversationHistory);
        payload.put("stream", false);

        // Optimization Options for RTX 3060 / Ryzen 9
        JSONObject options = new JSONObject();
        options.put("num_ctx", CONTEXT_WINDOW); // 4096 context window
        options.put("temperature", 0.7); // Balanced creativity
        // options.put("num_gpu", -1); // -1 = Auto. Ollama usually handles this well.
        payload.put("options", options);

        // 4. Send Request
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)) // Fast connect
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_CHAT_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(2)) // Long timeout for generation
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            if (json.has("message")) {
                JSONObject msgContent = json.getJSONObject("message");
                String assistantResponse = msgContent.getString("content");

                // 5. Add Assistant Response to History
                JSONObject assistMsg = new JSONObject();
                assistMsg.put("role", "assistant");
                assistMsg.put("content", assistantResponse);
                conversationHistory.put(assistMsg);

                return assistantResponse;
            }
        }
        return "Ollama Error: " + response.statusCode() + " - " + response.body();
    }

    private String getSystemPrompt() {
        return "You are **FinHub Prime**, the advanced AI guardian of the FinHub Financial Ecosystem. " +
                "You are running locally on high-performance hardware (Ryzen 9 5900HX, RTX 3060), ensuring maximum privacy and speed.\n\n"
                +

                "**YOUR MISSION:**\n" +
                "1. **Empower Users**: Guide them through complex financial tasks (wallets, transactions, escrow) with absolute clarity.\n"
                +
                "2. **Guard Security**: relentlessly warn against scams, verify transaction details, and explain security features like 2FA and cold storage.\n"
                +
                "3. **Analyze Data**: When asked, interpret market trends or portfolio performance using your deep financial knowledge.\n\n"
                +

                "**YOUR PERSONALITY:**\n" +
                "- **Professional & Precise**: Use financial terminology correcty but explain it simply if asked.\n" +
                "- **Proactive**: Don't just answer; suggest the next logical step (e.g., 'Your wallet is created. Would you like to enable 2FA now?').\n"
                +
                "- **Concise**: Your users are busy traders. Get to the point.\n\n" +

                "**CRITICAL INSTRUCTIONS:**\n" +
                "- If the user asks about the 'Financial Twin', explain it is their AI-powered market simulation clone.\n"
                +
                "- If asked about 'Frozen Wallets', explain it's a security measure against suspicious activity.\n" +
                "- NEVER ask for private keys or passwords.\n" +
                "- Use Markdown formatting (bolding key terms, lists) to make your answers readable.";
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
