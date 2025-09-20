package com.asos;

// import org.apache.hc.client5.http.classic.methods.HttpGet;
// import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
// import org.apache.hc.client5.http.impl.classic.HttpClients;
// import org.apache.hc.core5.http.ClassicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages downloading, verification, and storage of AI models
 * Handles Gemma 270M model setup for offline AI features
 */
public class ModelManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelManager.class);
    
    // Model configuration
    private static final String MODEL_NAME = "gemma-270m";
    private static final String MODEL_FILE_NAME = "gemma-270m-int8.onnx";
    private static final String MODEL_BASE_URL = "https://huggingface.co/microsoft/gemma-270m-onnx/resolve/main/";
    private static final String MODEL_EXPECTED_HASH = ""; // Will be set after first successful download
    
    // Paths
    private final Path modelDirectory;
    private final Path modelFilePath;
    private final Path configFilePath;
    
    // Download progress tracking
    public interface DownloadProgressListener {
        void onProgress(long bytesDownloaded, long totalBytes, double percentage);
        void onComplete(boolean success, String message);
    }
    
    private DownloadProgressListener progressListener;
    
    public ModelManager() {
        // Initialize paths
        String userHome = System.getProperty("user.home");
        this.modelDirectory = Paths.get(userHome, ".asos", "models");
        this.modelFilePath = modelDirectory.resolve(MODEL_FILE_NAME);
        this.configFilePath = modelDirectory.resolve("model-config.json");
        
        // Create directories if they don't exist
        try {
            Files.createDirectories(modelDirectory);
        } catch (IOException e) {
            logger.error("Failed to create model directory: {}", e.getMessage());
        }
    }
    
    /**
     * Check if the model is available and ready for use
     */
    public boolean isModelAvailable() {
        return Files.exists(modelFilePath) && isModelValid();
    }
    
    /**
     * Get the path to the model file
     */
    public Path getModelPath() {
        return modelFilePath;
    }
    
    /**
     * Set progress listener for download operations
     */
    public void setProgressListener(DownloadProgressListener listener) {
        this.progressListener = listener;
    }
    
    /**
     * Download the model asynchronously
     */
    public CompletableFuture<Boolean> downloadModelAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadModel();
            } catch (Exception e) {
                logger.error("Failed to download model", e);
                if (progressListener != null) {
                    progressListener.onComplete(false, "Download failed: " + e.getMessage());
                }
                return false;
            }
        });
    }
    
    /**
     * Download the model synchronously
     */
    private boolean downloadModel() {
        logger.info("Starting model download: {}", MODEL_NAME);
        
        // For now, we'll create a placeholder model file
        // In a real implementation, this would download from HuggingFace
        return createPlaceholderModel();
    }
    
    /**
     * Create a placeholder model for testing (temporary solution)
     */
    private boolean createPlaceholderModel() {
        try {
            logger.info("Creating placeholder model for testing...");
            
            // Create a simple placeholder file
            String placeholderContent = "# Gemma 270M Placeholder Model\n" +
                    "# This is a placeholder for the actual ONNX model\n" +
                    "# In production, this would be the actual Gemma 270M model file\n" +
                    "# Model size: ~670MB when quantized to INT8\n" +
                    "version: 1.0\n" +
                    "model_type: gemma-270m\n" +
                    "quantization: int8\n";
            
            Files.write(modelFilePath, placeholderContent.getBytes());
            
            // Create model configuration
            createModelConfig();
            
            if (progressListener != null) {
                progressListener.onComplete(true, "Placeholder model created successfully");
            }
            
            logger.info("Placeholder model created at: {}", modelFilePath);
            return true;
            
        } catch (IOException e) {
            logger.error("Failed to create placeholder model", e);
            if (progressListener != null) {
                progressListener.onComplete(false, "Failed to create model: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Download actual model from HuggingFace (for future implementation)
     */
    private boolean downloadActualModel() {
        /*
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            
            String modelUrl = MODEL_BASE_URL + MODEL_FILE_NAME;
            HttpGet httpGet = new HttpGet(URI.create(modelUrl));
            
            logger.info("Downloading model from: {}", modelUrl);
            
            return httpClient.execute(httpGet, response -> {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    logger.error("Failed to download model. Status code: {}", statusCode);
                    return false;
                }
                
                long contentLength = getContentLength(response);
                AtomicLong downloadedBytes = new AtomicLong(0);
                
                try (InputStream inputStream = response.getEntity().getContent();
                     OutputStream outputStream = Files.newOutputStream(modelFilePath)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        long downloaded = downloadedBytes.addAndGet(bytesRead);
                        
                        if (progressListener != null && contentLength > 0) {
                            double percentage = (double) downloaded / contentLength * 100;
                            progressListener.onProgress(downloaded, contentLength, percentage);
                        }
                    }
                    
                    createModelConfig();
                    
                    if (progressListener != null) {
                        progressListener.onComplete(true, "Model downloaded successfully");
                    }
                    
                    logger.info("Model downloaded successfully to: {}", modelFilePath);
                    return true;
                    
                } catch (IOException e) {
                    logger.error("Error during model download", e);
                    // Clean up partial download
                    try {
                        Files.deleteIfExists(modelFilePath);
                    } catch (IOException deleteEx) {
                        logger.warn("Failed to clean up partial download", deleteEx);
                    }
                    return false;
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to download model", e);
            return false;
        }
        */
        
        // Placeholder implementation - will be enabled when HTTP client is configured
        logger.info("Actual model download not yet implemented - using placeholder");
        return createPlaceholderModel();
    }
    
    /*
    private long getContentLength(ClassicHttpResponse response) {
        try {
            String contentLengthHeader = response.getFirstHeader("Content-Length").getValue();
            return Long.parseLong(contentLengthHeader);
        } catch (Exception e) {
            return -1;
        }
    }
    */
    
    /**
     * Create model configuration file
     */
    private void createModelConfig() throws IOException {
        String config = "{\n" +
                "  \"model_name\": \"" + MODEL_NAME + "\",\n" +
                "  \"model_file\": \"" + MODEL_FILE_NAME + "\",\n" +
                "  \"model_type\": \"onnx\",\n" +
                "  \"quantization\": \"int8\",\n" +
                "  \"context_length\": 2048,\n" +
                "  \"vocabulary_size\": 256000,\n" +
                "  \"embedding_size\": 2048,\n" +
                "  \"num_layers\": 18,\n" +
                "  \"num_attention_heads\": 8,\n" +
                "  \"downloaded_at\": \"" + java.time.Instant.now().toString() + "\"\n" +
                "}";
        
        Files.write(configFilePath, config.getBytes());
    }
    
    /**
     * Validate model integrity
     */
    private boolean isModelValid() {
        try {
            if (!Files.exists(modelFilePath)) {
                return false;
            }
            
            // Check file size (basic validation)
            long fileSize = Files.size(modelFilePath);
            if (fileSize < 1000) { // Minimum reasonable size
                logger.warn("Model file seems too small: {} bytes", fileSize);
                return false;
            }
            
            // TODO: Add SHA256 hash verification for actual model
            return true;
            
        } catch (IOException e) {
            logger.error("Error validating model", e);
            return false;
        }
    }
    
    /**
     * Calculate SHA256 hash of file
     */
    private String calculateFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * Get model information
     */
    public ModelInfo getModelInfo() {
        if (!isModelAvailable()) {
            return null;
        }
        
        try {
            long fileSize = Files.size(modelFilePath);
            String lastModified = Files.getLastModifiedTime(modelFilePath).toString();
            
            return new ModelInfo(MODEL_NAME, MODEL_FILE_NAME, fileSize, lastModified, isModelValid());
            
        } catch (IOException e) {
            logger.error("Error getting model info", e);
            return null;
        }
    }
    
    /**
     * Delete the model (for cleanup or re-download)
     */
    public boolean deleteModel() {
        try {
            boolean deleted = Files.deleteIfExists(modelFilePath);
            Files.deleteIfExists(configFilePath);
            
            if (deleted) {
                logger.info("Model deleted successfully");
            }
            
            return deleted;
            
        } catch (IOException e) {
            logger.error("Failed to delete model", e);
            return false;
        }
    }
    
    /**
     * Model information data class
     */
    public static class ModelInfo {
        private final String name;
        private final String fileName;
        private final long fileSize;
        private final String lastModified;
        private final boolean isValid;
        
        public ModelInfo(String name, String fileName, long fileSize, String lastModified, boolean isValid) {
            this.name = name;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
            this.isValid = isValid;
        }
        
        public String getName() { return name; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getLastModified() { return lastModified; }
        public boolean isValid() { return isValid; }
        
        public String getFormattedFileSize() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
        
        @Override
        public String toString() {
            return String.format("ModelInfo{name='%s', fileName='%s', size=%s, valid=%s}", 
                    name, fileName, getFormattedFileSize(), isValid);
        }
    }
}
