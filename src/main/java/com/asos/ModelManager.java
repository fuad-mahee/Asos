package com.asos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Manages the local AI model served by the Ollama sidecar.
 *
 * Checks whether Ollama is running and whether the configured model
 * (default: qwen2.5:1.5b-instruct) is installed, and can trigger a
 * one-time "ollama pull" through the local HTTP API.
 */
public class ModelManager {

    private static final Logger logger = LoggerFactory.getLogger(ModelManager.class);

    private static final Duration PING_TIMEOUT = Duration.ofSeconds(2);
    // Pulling a ~1 GB model on a slow connection can take a long time
    private static final Duration PULL_TIMEOUT = Duration.ofMinutes(60);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public interface DownloadProgressListener {
        void onProgress(long bytesDownloaded, long totalBytes, double percentage);
        void onComplete(boolean success, String message);
    }

    private DownloadProgressListener progressListener;

    public ModelManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(PING_TIMEOUT)
                .build();
    }

    public void setProgressListener(DownloadProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Check whether Ollama is running locally.
     */
    public boolean isOllamaRunning() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LocalAIEngine.OLLAMA_BASE_URL + "/api/tags"))
                    .timeout(PING_TIMEOUT)
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check whether the configured model is installed and ready for use.
     */
    public boolean isModelAvailable() {
        return getInstalledModelTag() != null;
    }

    /**
     * Return the installed tag matching the configured model, or null.
     * Accepts both "qwen2.5:1.5b-instruct" and its alias "qwen2.5:1.5b".
     */
    private String getInstalledModelTag() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LocalAIEngine.OLLAMA_BASE_URL + "/api/tags"))
                    .timeout(PING_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            String modelName = LocalAIEngine.MODEL_NAME;
            String baseName = modelName.endsWith("-instruct")
                    ? modelName.substring(0, modelName.length() - "-instruct".length())
                    : modelName;

            JsonNode models = objectMapper.readTree(response.body()).path("models");
            String prefixMatch = null;
            for (JsonNode model : models) {
                String tag = model.path("name").asText("");
                if (tag.equals(modelName)) {
                    return tag;
                }
                if (prefixMatch == null && (tag.startsWith(modelName) || tag.startsWith(baseName))) {
                    prefixMatch = tag;
                }
            }
            return prefixMatch;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Pull the configured model through Ollama (equivalent to "ollama pull <model>").
     * This is a one-time setup step; afterwards everything runs fully offline.
     */
    public CompletableFuture<Boolean> downloadModelAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isOllamaRunning()) {
                    notifyComplete(false,
                            "Ollama is not running. Install it from https://ollama.com and start it first.");
                    return false;
                }

                if (isModelAvailable()) {
                    notifyComplete(true, "Model already installed");
                    return true;
                }

                logger.info("Pulling model '{}' via Ollama (this may take a while)...",
                        LocalAIEngine.MODEL_NAME);

                String body = objectMapper.createObjectNode()
                        .put("name", LocalAIEngine.MODEL_NAME)
                        .put("stream", false)
                        .toString();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(LocalAIEngine.OLLAMA_BASE_URL + "/api/pull"))
                        .timeout(PULL_TIMEOUT)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                boolean success = response.statusCode() == 200
                        && objectMapper.readTree(response.body())
                                .path("status").asText("").contains("success");

                notifyComplete(success, success
                        ? "Model pulled successfully"
                        : "Model pull failed: " + response.body());
                return success;

            } catch (Exception e) {
                logger.error("Failed to pull model", e);
                notifyComplete(false, "Model pull failed: " + e.getMessage());
                return false;
            }
        });
    }

    private void notifyComplete(boolean success, String message) {
        logger.info("Model setup: {} ({})", message, success ? "ok" : "failed");
        if (progressListener != null) {
            progressListener.onComplete(success, message);
        }
    }

    /**
     * Get information about the installed model, or null if unavailable.
     */
    public ModelInfo getModelInfo() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LocalAIEngine.OLLAMA_BASE_URL + "/api/tags"))
                    .timeout(PING_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            String installedTag = getInstalledModelTag();
            if (installedTag == null) {
                return null;
            }

            JsonNode models = objectMapper.readTree(response.body()).path("models");
            for (JsonNode model : models) {
                if (installedTag.equals(model.path("name").asText(""))) {
                    return new ModelInfo(
                            installedTag,
                            model.path("size").asLong(0),
                            model.path("modified_at").asText(""));
                }
            }
            return null;

        } catch (Exception e) {
            logger.error("Error getting model info", e);
            return null;
        }
    }

    /**
     * Model information data class.
     */
    public static class ModelInfo {
        private final String name;
        private final long fileSize;
        private final String lastModified;

        public ModelInfo(String name, long fileSize, String lastModified) {
            this.name = name;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
        }

        public String getName() { return name; }
        public long getFileSize() { return fileSize; }
        public String getLastModified() { return lastModified; }

        public String getFormattedFileSize() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            if (fileSize < 1024L * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }

        @Override
        public String toString() {
            return String.format("ModelInfo{name='%s', size=%s, modified=%s}",
                    name, getFormattedFileSize(), lastModified);
        }
    }
}
