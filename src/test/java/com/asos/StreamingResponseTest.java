package com.asos;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration test of the streaming pipeline against the local Ollama
 * server. Skipped automatically when Ollama isn't running, so CI/machines
 * without the model still pass.
 */
class StreamingResponseTest {

    @Test
    @DisplayName("Streaming delivers tokens that reassemble into the final response")
    void streamedTokensMatchFinalResponse() throws Exception {
        Assumptions.assumeTrue(isOllamaReachable(), "Ollama not running - skipping");

        LocalAIEngine engine = new LocalAIEngine();
        // The constructor already starts async initialization - poll until it
        // finishes (the same way the app itself checks readiness)
        long deadline = System.currentTimeMillis() + 30_000;
        while (!engine.isReady() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        Assumptions.assumeTrue(engine.isReady(), "Model not installed - skipping");

        List<String> tokens = new CopyOnWriteArrayList<>();
        LocalAIEngine.AIContext context = new LocalAIEngine.AIContext("VISUAL", "testing");

        LocalAIEngine.AIResponse response = engine
                .generateResponseStreaming("Reply with a short greeting.", context, tokens::add)
                .get(120, TimeUnit.SECONDS);

        assertTrue(response.isSuccess(), "Streaming response should succeed");
        assertFalse(response.getText().isBlank(), "Final text should not be empty");
        assertFalse(tokens.isEmpty(), "At least one token should have been streamed");

        // The streamed pieces must reassemble into the final text
        String reassembled = String.join("", tokens).trim();
        assertEquals(response.getText(), reassembled,
                "Streamed tokens should concatenate to exactly the final response");

        // A model-generated reply arrives as many small pieces, not one blob
        assertTrue(tokens.size() > 1,
                "Model responses should stream as multiple pieces (got " + tokens.size() + ")");

        // Conversation history must record the exchange like the non-streaming path
        List<String> history = engine.getConversationHistory();
        assertEquals(2, history.size());
        assertTrue(history.get(0).startsWith("User: "));
        assertTrue(history.get(1).startsWith("Assistant: "));

        engine.cleanup();
    }

    private boolean isOllamaReachable() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LocalAIEngine.OLLAMA_BASE_URL + "/api/tags"))
                    .timeout(Duration.ofSeconds(2)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
