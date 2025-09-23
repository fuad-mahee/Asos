package com.asos;

import ai.onnxruntime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Local AI Engine using ONNX Runtime for offline inference
 * Handles Gemma 270M model loading and text generation
 */
public class LocalAIEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalAIEngine.class);
    
    // Model and session management
    private OrtEnvironment environment;
    private OrtSession session;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    
    // Model configuration
    private static final int MAX_CONTEXT_LENGTH = 2048;
    private static final int MAX_GENERATION_LENGTH = 512;
    private static final double TEMPERATURE = 0.7;
    private static final int TOP_K = 40;
    private static final double TOP_P = 0.9;
    
    // Tokenizer simulation (placeholder)
    private TokenizerSimulator tokenizer;
    
    // Conversation context management
    private final List<String> conversationHistory = new ArrayList<>();
    private final Map<String, Object> sessionContext = new HashMap<>();
    
    public interface InitializationListener {
        void onInitializationProgress(String step, double progress);
        void onInitializationComplete(boolean success, String message);
    }
    
    private InitializationListener initListener;
    
    public LocalAIEngine() {
        this.tokenizer = new TokenizerSimulator();
        
        // Auto-initialize with default model path if available
        autoInitializeIfModelExists();
    }
    
    /**
     * Auto-initialize the AI engine if model file exists
     */
    private void autoInitializeIfModelExists() {
        Path defaultModelPath = Path.of("models", "gemma-270m.onnx");
        if (defaultModelPath.toFile().exists()) {
            logger.info("Found model file at {}, auto-initializing...", defaultModelPath);
            initializeAsync(defaultModelPath, new InitializationListener() {
                @Override
                public void onInitializationProgress(String step, double progress) {
                    logger.debug("Auto-init progress: {} ({}%)", step, (int)(progress * 100));
                }
                
                @Override
                public void onInitializationComplete(boolean success, String message) {
                    if (success) {
                        logger.info("Auto-initialization completed successfully");
                    } else {
                        logger.warn("Auto-initialization failed: {}", message);
                    }
                }
            });
        } else {
            logger.info("No model file found at {}, manual initialization required", defaultModelPath);
        }
    }
    
    /**
     * Initialize the AI engine with the model
     */
    public CompletableFuture<Boolean> initializeAsync(Path modelPath, InitializationListener listener) {
        this.initListener = listener;
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return initialize(modelPath);
            } catch (Exception e) {
                logger.error("Failed to initialize AI engine", e);
                if (initListener != null) {
                    initListener.onInitializationComplete(false, "Initialization failed: " + e.getMessage());
                }
                return false;
            }
        });
    }
    
    /**
     * Initialize the AI engine synchronously
     */
    private boolean initialize(Path modelPath) {
        if (isInitialized.get() || isLoading.getAndSet(true)) {
            return isInitialized.get();
        }
        
        try {
            logger.info("Initializing Local AI Engine with model: {}", modelPath);
            
            // Step 1: Initialize ONNX Runtime environment with error handling
            if (initListener != null) {
                initListener.onInitializationProgress("Initializing AI Runtime...", 0.1);
            }
            
            try {
                environment = OrtEnvironment.getEnvironment();
                logger.info("AI Runtime environment initialized successfully");
                
            } catch (Exception e) {
                logger.error("Failed to initialize ONNX Runtime environment: {}", e.getMessage());
                throw new RuntimeException("ONNX Runtime initialization failed", e);
            }
            
            // Step 2: Load model (placeholder for now)
            if (initListener != null) {
                initListener.onInitializationProgress("Loading AI model...", 0.3);
            }
            
            // For now, we'll use the real ONNX model loading instead of placeholder
            boolean modelLoaded = loadActualModel(modelPath);
            
            if (!modelLoaded) {
                throw new RuntimeException("Failed to load model");
            }
            
            // Step 3: Initialize tokenizer
            if (initListener != null) {
                initListener.onInitializationProgress("Initializing tokenizer...", 0.7);
            }
            
            tokenizer.initialize();
            logger.info("Tokenizer initialized");
            
            // Step 4: Warm up the model
            if (initListener != null) {
                initListener.onInitializationProgress("Warming up model...", 0.9);
            }
            
            warmUpModel();
            
            // Step 5: Complete initialization
            isInitialized.set(true);
            
            if (initListener != null) {
                initListener.onInitializationProgress("Initialization complete", 1.0);
                initListener.onInitializationComplete(true, "AI Engine initialized successfully");
            }
            
            logger.info("Local AI Engine initialized successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to initialize AI engine", e);
            isInitialized.set(false);
            
            if (initListener != null) {
                initListener.onInitializationComplete(false, "Initialization failed: " + e.getMessage());
            }
            
            // Cleanup on failure
            cleanup();
            return false;
            
        } finally {
            isLoading.set(false);
        }
    }
    
    /**
     * Load model placeholder (temporary until real ONNX model is available)
     */
    private boolean loadModelPlaceholder(Path modelPath) {
        try {
            // Simulate model loading delay
            Thread.sleep(2000);
            
            logger.info("Placeholder model loaded from: {}", modelPath);
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Load actual ONNX model using ONNX Runtime with multiple fallback strategies
     */
    private boolean loadActualModel(Path modelPath) {
        try {
            logger.info("Loading ONNX model from: {}", modelPath);
            
            // Check if model file exists and is readable
            if (!modelPath.toFile().exists()) {
                logger.error("Model file does not exist: {}", modelPath);
                return false;
            }
            
            if (!modelPath.toFile().canRead()) {
                logger.error("Cannot read model file: {}", modelPath);
                return false;
            }
            
            // Check if companion .onnx_data file exists (for external data models)
            Path dataPath = Path.of(modelPath.toString() + "_data");
            if (dataPath.toFile().exists()) {
                logger.info("Found external data file: {}", dataPath);
            }
            
            // Try multiple loading strategies
            return tryLoadWithDifferentStrategies(modelPath);
            
        } catch (Exception e) {
            logger.error("Failed to load ONNX model: {}", e.getMessage());
            logger.warn("Falling back to placeholder mode");
            
            // Provide troubleshooting suggestions
            ModelCompatibilityChecker.suggestSolutions(e.getMessage());
            
            return false;
        }
    }
    
    /**
     * Try loading with different ONNX Runtime configurations
     */
    private boolean tryLoadWithDifferentStrategies(Path modelPath) {
        // First check model compatibility
        if (!ModelCompatibilityChecker.isModelCompatible(modelPath)) {
            logger.error("Model compatibility check failed");
            return false;
        }
        
        // Strategy 1: Standard loading with latest providers
        if (tryStandardLoading(modelPath)) {
            return true;
        }
        
        // Strategy 2: Loading with relaxed version checks
        if (tryRelaxedLoading(modelPath)) {
            return true;
        }
        
        // Strategy 3: Loading with specific execution providers
        if (tryProviderSpecificLoading(modelPath)) {
            return true;
        }
        
        logger.error("All loading strategies failed");
        return false;
    }
    
    /**
     * Strategy 1: Standard ONNX loading
     */
    private boolean tryStandardLoading(Path modelPath) {
        try {
            logger.info("Trying standard ONNX loading...");
            
            // Create session options
            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            
            // Optimize for CPU inference
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            sessionOptions.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors());
            
            // Add execution providers (CPU)
            sessionOptions.addCPU(false); // Use CPU provider
            
            // Load the model
            session = environment.createSession(modelPath.toString(), sessionOptions);
            
            logger.info("Standard ONNX model loaded successfully - Input count: {}, Output count: {}", 
                       session.getInputInfo().size(), session.getOutputInfo().size());
            return true;
            
        } catch (Exception e) {
            logger.warn("Standard loading failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Strategy 2: Relaxed loading with version tolerance
     */
    private boolean tryRelaxedLoading(Path modelPath) {
        try {
            logger.info("Trying relaxed ONNX loading...");
            
            // Create session options with relaxed settings
            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            
            // Less aggressive optimization
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
            sessionOptions.setIntraOpNumThreads(1); // Single thread for compatibility
            
            // Try to disable strict version checking if possible
            sessionOptions.addConfigEntry("session.disable_prepacking", "1");
            sessionOptions.addConfigEntry("session.use_env_allocators", "1");
            
            // Load the model
            session = environment.createSession(modelPath.toString(), sessionOptions);
            
            logger.info("Relaxed ONNX model loaded successfully - Input count: {}, Output count: {}", 
                       session.getInputInfo().size(), session.getOutputInfo().size());
            return true;
            
        } catch (Exception e) {
            logger.warn("Relaxed loading failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Strategy 3: Provider-specific loading
     */
    private boolean tryProviderSpecificLoading(Path modelPath) {
        try {
            logger.info("Trying provider-specific ONNX loading...");
            
            // Create session options with specific providers
            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            
            // Minimal optimization
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT);
            
            // Explicitly set CPU provider only
            sessionOptions.addCPU(true); // Allow CPU fallback
            
            // Load the model
            session = environment.createSession(modelPath.toString(), sessionOptions);
            
            logger.info("Provider-specific ONNX model loaded successfully - Input count: {}, Output count: {}", 
                       session.getInputInfo().size(), session.getOutputInfo().size());
            return true;
            
        } catch (Exception e) {
            logger.warn("Provider-specific loading failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Warm up the model with a test inference
     */
    private void warmUpModel() {
        try {
            // Simulate warmup
            String warmupText = "Hello";
            generateResponsePlaceholder(warmupText);
            logger.info("Model warmup completed");
            
        } catch (Exception e) {
            logger.warn("Model warmup failed, but continuing", e);
        }
    }
    
    /**
     * Generate a response to user input
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
    
    /**
     * Generate response synchronously
     */
    private AIResponse generateResponse(String input, AIContext context) {
        logger.debug("Generating response for input: {}", input);
        
        // Add to conversation history
        conversationHistory.add("User: " + input);
        
        String response;
        if (!isInitialized.get()) {
            // Use enhanced fallback response when model isn't loaded
            response = generateEnhancedFallbackResponse(input, context);
        } else {
            // Use model-based response when available
            response = generateModelResponse(input, context);
        }
        
        // Add to conversation history
        conversationHistory.add("Assistant: " + response);
        
        // Trim history if too long
        trimConversationHistory();
        
        return new AIResponse(response, AIResponse.ResponseType.SUCCESS, true);
    }
    
    /**
     * Enhanced fallback response when model is not available
     */
    private String generateEnhancedFallbackResponse(String input, AIContext context) {
        String lowerInput = input.toLowerCase().trim();
        
        // Greeting responses
        if (lowerInput.matches(".*\\b(hello|hi|hey|good morning|good afternoon|good evening)\\b.*")) {
            return "আছস? Hello! I'm here to help you learn. What would you like to explore today? I can help with programming, explain concepts, or guide you through tutorials! 😊";
        }
        
        // Programming-related questions
        if (lowerInput.matches(".*\\b(java|python|javascript|programming|code|coding|function|variable|loop|class)\\b.*")) {
            return "Great question about programming! While my advanced AI model is loading, I can still help you learn. " +
                   "Here are some things I can do:\n\n" +
                   "📚 Explain programming concepts step by step\n" +
                   "💡 Provide learning tips and best practices\n" +
                   "🎯 Guide you through coding exercises\n" +
                   "🐛 Help debug common issues\n\n" +
                   "What specific aspect would you like to learn about?";
        }
        
        // How-to questions
        if (lowerInput.matches(".*\\b(how|what|why|when|where)\\b.*")) {
            return "Excellent question! I love helping explain things. While I'm preparing my full knowledge base, " +
                   "I can still provide helpful guidance based on proven learning methods. " +
                   "Could you tell me more about what you're trying to understand or accomplish?";
        }
        
        // Help requests
        if (lowerInput.matches(".*\\b(help|stuck|confused|don't understand|problem)\\b.*")) {
            return "I'm here to help! Don't worry - everyone gets stuck sometimes, that's how we learn! 💪\n\n" +
                   "Let's break this down together:\n" +
                   "1. What are you working on?\n" +
                   "2. What part is confusing?\n" +
                   "3. What have you tried so far?\n\n" +
                   "I'll guide you through it step by step!";
        }
        
        // Learning/tutorial requests
        if (lowerInput.matches(".*\\b(learn|tutorial|teach|guide|example|practice)\\b.*")) {
            return "Perfect! I love helping people learn new things! 🎓\n\n" +
                   "I can help you with:\n" +
                   "• Step-by-step tutorials\n" +
                   "• Hands-on practice exercises\n" +
                   "• Real-world examples\n" +
                   "• Personalized learning paths\n\n" +
                   "What would you like to learn about today?";
        }
        
        // Default intelligent response
        return "I'm Asos, your learning companion! আছস? 😊\n\n" +
               "While my advanced AI capabilities are starting up, I'm still here to help you learn and grow. " +
               "I can provide guidance, explanations, and support for your learning journey.\n\n" +
               "What can I help you discover today?";
    }
    
    /**
     * Generate model-based response (for when model is loaded)
     */
    private String generateModelResponse(String input, AIContext context) {
        // This will be used when the ONNX model is successfully loaded
        // For now, use enhanced fallback
        return generateEnhancedFallbackResponse(input, context);
    }
    
    /**
     * Placeholder response generation (enhanced with compatibility checker)
     */
    private String generateResponsePlaceholder(String input) {
        // Use the enhanced fallback system from ModelCompatibilityChecker
        return ModelCompatibilityChecker.generateFallbackResponse(input);
    }
    
    /**
     * Actual model inference (for future implementation when ONNX Runtime is configured)
     */
    private String generateResponseWithModel(String input) throws Exception {
        if (session == null) {
            throw new IllegalStateException("Model session not initialized");
        }
        
        // This will be implemented when ONNX Runtime is properly configured
        /*
        // Tokenize input
        int[] inputTokens = tokenizer.encode(input);
        
        // Prepare input tensor
        long[] inputShape = {1, inputTokens.length};
        OnnxTensor inputTensor = OnnxTensor.createTensor(environment, 
            Arrays.stream(inputTokens).asLongStream().toArray(), inputShape);
        
        // Run inference
        Map<String, OnnxTensor> inputs = Map.of("input_ids", inputTensor);
        OrtSession.Result result = session.run(inputs);
        
        // Process output (simplified)
        OnnxTensor outputTensor = (OnnxTensor) result.get(0);
        long[][] output = (long[][]) outputTensor.getValue();
        
        // Decode response
        String response = tokenizer.decode(Arrays.stream(output[0]).mapToInt(i -> (int) i).toArray());
        
        // Cleanup
        inputTensor.close();
        result.close();
        
        return response;
        */
        
        // Placeholder implementation
        return "AI model response (placeholder): " + input;
    }
    
    /**
     * Trim conversation history to manage memory
     */
    private void trimConversationHistory() {
        while (conversationHistory.size() > 20) { // Keep last 20 exchanges
            conversationHistory.remove(0);
        }
    }
    
    /**
     * Check if the engine is ready for inference
     */
    public boolean isReady() {
        return isInitialized.get();
    }
    
    /**
     * Get conversation history
     */
    public List<String> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
    
    /**
     * Clear conversation history
     */
    public void clearConversationHistory() {
        conversationHistory.clear();
        logger.debug("Conversation history cleared");
    }
    
    /**
     * Get engine status
     */
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
     * Cleanup resources
     */
    public void cleanup() {
        try {
            /*
            if (session != null) {
                session.close();
                session = null;
            }
            
            if (environment != null) {
                environment.close();
                environment = null;
            }
            */
            
            // Placeholder cleanup
            session = null;
            environment = null;
            
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
    
    /**
     * Simple tokenizer simulator (placeholder)
     */
    private static class TokenizerSimulator {
        private final Map<String, Integer> vocab = new HashMap<>();
        private final Map<Integer, String> reverseVocab = new HashMap<>();
        private int nextTokenId = 1;
        
        public void initialize() {
            // Add some basic tokens
            addToken("<pad>");
            addToken("<unk>");
            addToken("<start>");
            addToken("<end>");
            
            // Add common words
            String[] commonWords = {"the", "a", "an", "and", "or", "but", "in", "on", "at", 
                    "to", "for", "of", "with", "by", "hello", "hi", "help", "please", "thank", "you"};
            
            for (String word : commonWords) {
                addToken(word);
            }
        }
        
        private void addToken(String token) {
            if (!vocab.containsKey(token)) {
                vocab.put(token, nextTokenId);
                reverseVocab.put(nextTokenId, token);
                nextTokenId++;
            }
        }
        
        public int[] encode(String text) {
            String[] words = text.toLowerCase().split("\\s+");
            int[] tokens = new int[words.length];
            
            for (int i = 0; i < words.length; i++) {
                tokens[i] = vocab.getOrDefault(words[i], vocab.get("<unk>"));
            }
            
            return tokens;
        }
        
        public String decode(int[] tokens) {
            StringBuilder result = new StringBuilder();
            
            for (int token : tokens) {
                String word = reverseVocab.getOrDefault(token, "<unk>");
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(word);
            }
            
            return result.toString();
        }
    }
}
