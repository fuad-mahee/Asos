package com.asos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Local AI Engine backed by an Ollama sidecar process.
 *
 * Talks to a locally running Ollama server (http://localhost:11434) over HTTP
 * and generates responses with a small instruct model (default: qwen2.5:1.5b-instruct).
 * Everything stays on the local machine - no cloud calls, fully offline.
 *
 * If Ollama is not installed/running or the model has not been pulled yet,
 * the engine degrades gracefully to rule-based fallback responses so the
 * application remains usable.
 */
public class LocalAIEngine {

    private static final Logger logger = LoggerFactory.getLogger(LocalAIEngine.class);

    /** Base URL of the local Ollama server. Override with -Dasos.ollama.url=... */
    public static final String OLLAMA_BASE_URL =
            System.getProperty("asos.ollama.url", "http://localhost:11434");

    /** Model tag to use. Override with -Dasos.ollama.model=... */
    public static final String MODEL_NAME =
            System.getProperty("asos.ollama.model", "qwen2.5:1.5b-instruct");

    private static final String SYSTEM_PROMPT =
            "You are Asos (আছস?), a friendly, patient learning companion that runs fully " +
            "offline on the learner's computer. You help absolute beginners learn " +
            "programming and basic computer skills. Keep answers clear, encouraging and " +
            "concise (under 150 words unless the user asks for detail). Use simple language.";

    private static final Duration PING_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CHAT_TIMEOUT = Duration.ofSeconds(120);
    private static final int SERVE_STARTUP_WAIT_SECONDS = 12;
    private static final long REINIT_THROTTLE_MS = 10_000;
    private static final int MAX_HISTORY_MESSAGES = 10;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    private volatile long lastInitAttempt = 0;

    /** The actual model tag resolved from Ollama's tag list (e.g. "qwen2.5:1.5b"). */
    private volatile String resolvedModelTag = MODEL_NAME;

    /** Set when this engine launched "ollama serve" itself, so cleanup() can stop it. */
    private Process ollamaProcess;

    // Conversation context management
    private final List<String> conversationHistory =
            Collections.synchronizedList(new ArrayList<>());

    public LocalAIEngine() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(PING_TIMEOUT)
                .build();

        // Connect to Ollama in the background so app startup is never blocked
        initializeAsync();
    }

    /**
     * Initialize the engine asynchronously: find (or start) Ollama, then verify the model.
     */
    public CompletableFuture<Boolean> initializeAsync() {
        return CompletableFuture.supplyAsync(this::initialize);
    }

    private boolean initialize() {
        if (isInitialized.get() || isLoading.getAndSet(true)) {
            return isInitialized.get();
        }

        try {
            lastInitAttempt = System.currentTimeMillis();

            if (!isOllamaReachable()) {
                logger.info("Ollama not reachable at {}, attempting to start 'ollama serve'...",
                        OLLAMA_BASE_URL);
                if (!tryStartOllamaServe()) {
                    logger.warn("Ollama is not available. Install it from https://ollama.com " +
                            "and run: ollama pull {}  (falling back to rule-based responses)",
                            MODEL_NAME);
                    return false;
                }
            }

            String tag = findInstalledModelTag();
            if (tag == null) {
                logger.warn("Ollama is running but model '{}' is not installed. " +
                        "Run: ollama pull {}  (falling back to rule-based responses)",
                        MODEL_NAME, MODEL_NAME);
                return false;
            }

            resolvedModelTag = tag;
            isInitialized.set(true);
            logger.info("Local AI Engine ready - Ollama at {} with model '{}'",
                    OLLAMA_BASE_URL, resolvedModelTag);
            return true;

        } catch (Exception e) {
            logger.error("Failed to initialize AI engine", e);
            return false;
        } finally {
            isLoading.set(false);
        }
    }

    /**
     * Quick health check against the Ollama server.
     */
    private boolean isOllamaReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_BASE_URL + "/api/tags"))
                    .timeout(PING_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Try to launch "ollama serve" as a background sidecar process and wait
     * until it answers, or give up after a short timeout.
     */
    private boolean tryStartOllamaServe() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "serve");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            ollamaProcess = pb.start();
            logger.info("Launched 'ollama serve' (pid {})", ollamaProcess.pid());
        } catch (IOException e) {
            // Ollama binary not on PATH - it is simply not installed
            logger.warn("Could not launch 'ollama serve': {}", e.getMessage());
            return false;
        }

        // Poll until the server answers
        for (int i = 0; i < SERVE_STARTUP_WAIT_SECONDS * 2; i++) {
            if (isOllamaReachable()) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        logger.warn("'ollama serve' was launched but did not become reachable in time");
        return false;
    }

    /**
     * Look up the installed model list and resolve the best matching tag.
     * Accepts both "qwen2.5:1.5b-instruct" and its alias "qwen2.5:1.5b".
     */
    private String findInstalledModelTag() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_BASE_URL + "/api/tags"))
                    .timeout(PING_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode models = objectMapper.readTree(response.body()).path("models");
            String baseName = MODEL_NAME.endsWith("-instruct")
                    ? MODEL_NAME.substring(0, MODEL_NAME.length() - "-instruct".length())
                    : MODEL_NAME;

            String prefixMatch = null;
            for (JsonNode model : models) {
                String tag = model.path("name").asText("");
                if (tag.equals(MODEL_NAME)) {
                    return tag; // exact match wins
                }
                if (prefixMatch == null && (tag.startsWith(MODEL_NAME) || tag.startsWith(baseName))) {
                    prefixMatch = tag;
                }
            }
            return prefixMatch;

        } catch (Exception e) {
            logger.warn("Failed to query Ollama model list: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate a response to user input.
     */
    public CompletableFuture<AIResponse> generateResponseAsync(String input, AIContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateResponse(input, context);
            } catch (Exception e) {
                logger.error("Failed to generate response", e);
                return new AIResponse("I'm sorry, I encountered an error while processing your request.",
                        AIResponse.ResponseType.ERROR, false);
            }
        });
    }

    private AIResponse generateResponse(String input, AIContext context) {
        logger.debug("Generating response for input: {}", input);

        // If Ollama came online after startup (user installed/pulled later), pick it up
        if (!isInitialized.get()
                && System.currentTimeMillis() - lastInitAttempt > REINIT_THROTTLE_MS) {
            initialize();
        }

        conversationHistory.add("User: " + input);

        String response;
        if (isInitialized.get()) {
            response = generateModelResponse(input);
            if (response == null) {
                // Model call failed mid-session (e.g. Ollama stopped) - degrade gracefully
                isInitialized.set(false);
                response = generateEnhancedFallbackResponse(input);
            }
        } else {
            response = generateEnhancedFallbackResponse(input);
        }

        conversationHistory.add("Assistant: " + response);
        trimConversationHistory();

        return new AIResponse(response, AIResponse.ResponseType.SUCCESS, true);
    }

    /**
     * Build the /api/chat request body: system prompt (with the preferred
     * answer language stated firmly - small models ignore a single mention at
     * the top of a long English prompt, so it is also repeated at the very end
     * of the user message), recent history, and the current input.
     */
    private ObjectNode buildChatRequestBody(String input, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", resolvedModelTag);
        body.put("stream", stream);

        ObjectNode options = body.putObject("options");
        options.put("temperature", 0.7);
        options.put("num_predict", 512);

        String systemPrompt = SYSTEM_PROMPT;
        String preferredLanguage = AppSettings.getLanguageForPrompt();
        boolean nonEnglish = !AppSettings.DEFAULT_LANGUAGE.equalsIgnoreCase(preferredLanguage);
        if (nonEnglish) {
            systemPrompt += " IMPORTANT: You must write your ENTIRE reply in "
                    + preferredLanguage + ". Never reply in English.";
        }

        ArrayNode messages = body.putArray("messages");
        addMessage(messages, "system", systemPrompt);

        // Include recent conversation history (excluding the current input,
        // which was just appended by the caller)
        List<String> history = getRecentHistory();
        for (int i = 0; i < history.size() - 1; i++) {
            String entry = history.get(i);
            if (entry.startsWith("User: ")) {
                addMessage(messages, "user", entry.substring("User: ".length()));
            } else if (entry.startsWith("Assistant: ")) {
                addMessage(messages, "assistant", entry.substring("Assistant: ".length()));
            }
        }
        String outgoingInput = nonEnglish
                ? input + "\n\n(Write your entire answer in " + preferredLanguage + ".)"
                : input;
        addMessage(messages, "user", outgoingInput);

        return body;
    }

    /**
     * Call Ollama's /api/chat endpoint with the system prompt and recent history.
     * Returns null on any failure so callers can fall back.
     */
    private String generateModelResponse(String input) {
        try {
            ObjectNode body = buildChatRequestBody(input, false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_BASE_URL + "/api/chat"))
                    .timeout(CHAT_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warn("Ollama chat request failed with status {}: {}",
                        response.statusCode(), response.body());
                return null;
            }

            String content = objectMapper.readTree(response.body())
                    .path("message").path("content").asText("");
            return content.isBlank() ? null : content.trim();

        } catch (Exception e) {
            logger.warn("Ollama chat request failed: {}", e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Streaming generation (token-by-token typing effect)
    // ------------------------------------------------------------------

    /**
     * Generate a response, delivering it piece-by-piece through onToken as the
     * model produces it. The returned future completes with the full response.
     * When the model is unavailable, the rule-based fallback is delivered as a
     * single chunk, so callers behave identically in both modes.
     *
     * onToken is invoked on a background thread - UI callers must hop to the
     * FX thread themselves.
     */
    public CompletableFuture<AIResponse> generateResponseStreaming(
            String input, AIContext context, Consumer<String> onToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateStreamingInternal(input, onToken);
            } catch (Exception e) {
                logger.error("Failed to generate streaming response", e);
                return new AIResponse("I'm sorry, I encountered an error while processing your request.",
                        AIResponse.ResponseType.ERROR, false);
            }
        });
    }

    private AIResponse generateStreamingInternal(String input, Consumer<String> onToken) {
        // If Ollama came online after startup, pick it up (same as generateResponse)
        if (!isInitialized.get()
                && System.currentTimeMillis() - lastInitAttempt > REINIT_THROTTLE_MS) {
            initialize();
        }

        conversationHistory.add("User: " + input);

        String response;
        if (isInitialized.get()) {
            response = streamModelResponse(input, onToken);
            if (response == null) {
                // Model call failed before producing anything - degrade gracefully
                isInitialized.set(false);
                response = generateEnhancedFallbackResponse(input);
                onToken.accept(response);
            }
        } else {
            response = generateEnhancedFallbackResponse(input);
            onToken.accept(response);
        }

        conversationHistory.add("Assistant: " + response);
        trimConversationHistory();

        return new AIResponse(response, AIResponse.ResponseType.SUCCESS, true);
    }

    /**
     * Stream from Ollama's /api/chat (newline-delimited JSON). Emits each
     * content piece through onToken and returns the full text. Returns the
     * partial text if the stream breaks midway, or null if nothing arrived.
     */
    private String streamModelResponse(String input, Consumer<String> onToken) {
        StringBuilder full = new StringBuilder();
        try {
            ObjectNode body = buildChatRequestBody(input, true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_BASE_URL + "/api/chat"))
                    .timeout(CHAT_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                try (InputStream errorStream = response.body()) {
                    logger.warn("Ollama streaming request failed with status {}: {}",
                            response.statusCode(),
                            new String(errorStream.readNBytes(500), StandardCharsets.UTF_8));
                }
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode node = objectMapper.readTree(line);
                    String piece = node.path("message").path("content").asText("");
                    if (!piece.isEmpty()) {
                        full.append(piece);
                        onToken.accept(piece);
                    }
                    if (node.path("done").asBoolean(false)) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Ollama streaming request failed: {}", e.getMessage());
            // Keep whatever was already shown to the user; only report total
            // failure when nothing arrived at all
        }

        String result = full.toString().trim();
        return result.isBlank() ? null : result;
    }

    private void addMessage(ArrayNode messages, String role, String content) {
        ObjectNode message = messages.addObject();
        message.put("role", role);
        message.put("content", content);
    }

    private List<String> getRecentHistory() {
        synchronized (conversationHistory) {
            int from = Math.max(0, conversationHistory.size() - MAX_HISTORY_MESSAGES);
            return new ArrayList<>(conversationHistory.subList(from, conversationHistory.size()));
        }
    }

    /**
     * Rule-based fallback used when the local model is unavailable.
     */
    private String generateEnhancedFallbackResponse(String input) {
        String actualUserQuestion = extractUserQuestion(input);

        String ruleBasedResponse = FallbackResponseGenerator.generateFallbackResponse(actualUserQuestion);
        if (ruleBasedResponse != null) {
            return ruleBasedResponse;
        }

        if (actualUserQuestion.trim().length() > 10) {
            return "Thanks for your question: '" + actualUserQuestion + "'\n\n" +
                   "I'm Asos, your learning companion! আছস? 😊 My local AI model isn't running " +
                   "right now (install Ollama and run 'ollama pull " + MODEL_NAME + "' to enable it), " +
                   "but I can still guide you.\n\n" +
                   "Could you tell me more about what you'd like to learn or understand?";
        }

        return "I'm Asos, your learning companion! আছস? 😊\n\n" +
               "My local AI model isn't running right now, but I'm still here to help you learn. " +
               "To enable full AI answers, install Ollama (https://ollama.com) and run:\n" +
               "ollama pull " + MODEL_NAME + "\n\n" +
               "What can I help you discover today?";
    }

    /**
     * Extract the actual user question from the enhanced prompt built by
     * IntelligentLearningAssistant ("... User question: X").
     */
    private String extractUserQuestion(String enhancedPrompt) {
        String marker = "User question: ";
        int markerIndex = enhancedPrompt.indexOf(marker);
        if (markerIndex != -1) {
            return enhancedPrompt.substring(markerIndex + marker.length()).trim();
        }
        return enhancedPrompt.trim();
    }

    private void trimConversationHistory() {
        synchronized (conversationHistory) {
            while (conversationHistory.size() > 20) {
                conversationHistory.remove(0);
            }
        }
    }

    /**
     * Check if the engine is ready for model-backed inference.
     */
    public boolean isReady() {
        return isInitialized.get();
    }

    public List<String> getConversationHistory() {
        synchronized (conversationHistory) {
            return new ArrayList<>(conversationHistory);
        }
    }

    public void clearConversationHistory() {
        conversationHistory.clear();
        logger.debug("Conversation history cleared");
    }

    public EngineStatus getStatus() {
        if (isLoading.get()) {
            return EngineStatus.LOADING;
        } else if (isInitialized.get()) {
            return EngineStatus.READY;
        } else {
            return EngineStatus.NOT_INITIALIZED;
        }
    }

    /**
     * Release resources. Stops the Ollama sidecar only if this engine started it.
     */
    public void cleanup() {
        try {
            if (ollamaProcess != null && ollamaProcess.isAlive()) {
                logger.info("Stopping Ollama sidecar process (pid {})", ollamaProcess.pid());
                ollamaProcess.destroy();
                ollamaProcess = null;
            }
            conversationHistory.clear();
            isInitialized.set(false);
            logger.info("AI Engine cleanup completed");
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

    // Data classes and enums

    public enum EngineStatus {
        NOT_INITIALIZED, LOADING, READY, ERROR
    }

    public static class AIContext {
        private final String learningStyle;
        private final String currentTopic;
        private final Map<String, Object> metadata;

        public AIContext(String learningStyle, String currentTopic) {
            this.learningStyle = learningStyle;
            this.currentTopic = currentTopic;
            this.metadata = new HashMap<>();
        }

        public String getLearningStyle() { return learningStyle; }
        public String getCurrentTopic() { return currentTopic; }
        public Map<String, Object> getMetadata() { return metadata; }

        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }

    public static class AIResponse {
        private final String text;
        private final ResponseType type;
        private final boolean success;
        private final long timestamp;

        public enum ResponseType {
            SUCCESS, ERROR, WARNING, INFO
        }

        public AIResponse(String text, ResponseType type, boolean success) {
            this.text = text;
            this.type = type;
            this.success = success;
            this.timestamp = System.currentTimeMillis();
        }

        public String getText() { return text; }
        public ResponseType getType() { return type; }
        public boolean isSuccess() { return success; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("AIResponse{text='%s', type=%s, success=%s}",
                    text, type, success);
        }
    }
}
