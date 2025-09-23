package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Intelligent Learning Assistant that provides AI-powered educational support
 * Uses the Local AI Engine to generate personalized learning content
 */
public class IntelligentLearningAssistant {
    
    private static final Logger logger = LoggerFactory.getLogger(IntelligentLearningAssistant.class);
    
    private final LocalAIEngine aiEngine;
    private final UserProfileManager profileManager;
    private final LearningAnalytics analytics;
    
    // Learning context management
    private String currentTopic;
    private LearningMode currentMode;
    private final Map<String, Object> learningContext;
    
    // Response templates for different learning scenarios
    private final Map<LearningScenario, List<String>> responseTemplates;
    
    public enum LearningMode {
        TUTORIAL, EXPLANATION, PRACTICE, ASSESSMENT, CONVERSATION
    }
    
    public enum LearningScenario {
        CONCEPT_EXPLANATION, STEP_BY_STEP_GUIDE, ERROR_CORRECTION, 
        ENCOURAGEMENT, HINT_GENERATION, SUMMARY, NEXT_STEPS
    }
    
    public IntelligentLearningAssistant(LocalAIEngine aiEngine, 
                                      UserProfileManager profileManager,
                                      LearningAnalytics analytics) {
        this.aiEngine = aiEngine;
        this.profileManager = profileManager;
        this.analytics = analytics;
        this.learningContext = new HashMap<>();
        this.responseTemplates = initializeResponseTemplates();
        this.currentMode = LearningMode.CONVERSATION;
    }
    
    /**
     * Process a learning query from the user
     */
    public CompletableFuture<LearningResponse> processLearningQuery(String query, LearningQueryContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return handleLearningQuery(query, context);
            } catch (Exception e) {
                logger.error("Error processing learning query", e);
                return new LearningResponse(
                    "I encountered an error while processing your question. Please try again.",
                    LearningResponse.ResponseType.ERROR,
                    currentMode
                );
            }
        });
    }
    
    /**
     * Handle learning query with intelligent processing
     */
    private LearningResponse handleLearningQuery(String query, LearningQueryContext context) {
        logger.debug("Processing learning query: {}", query);
        
        // Analyze query intent
        QueryIntent intent = analyzeQueryIntent(query);
        
        // Get user profile for personalization
        UserProfileManager.UserProfile userProfile = profileManager.getCurrentProfile();
        
        // Create AI context
        LocalAIEngine.AIContext aiContext = new LocalAIEngine.AIContext(
            userProfile.getLearningStyle().toString(),
            context.getCurrentTopic()
        );
        
        // Add learning context metadata
        aiContext.addMetadata("learning_mode", currentMode);
        aiContext.addMetadata("difficulty_level", userProfile.getSkillLevel());
        aiContext.addMetadata("query_intent", intent);
        
        // Generate enhanced prompt based on learning scenario
        String enhancedPrompt = enhancePromptForLearning(query, intent, userProfile);
        
        // Get AI response
        try {
            LocalAIEngine.AIResponse aiResponse = aiEngine.generateResponseAsync(enhancedPrompt, aiContext).get();
            
            if (aiResponse.isSuccess()) {
                // Post-process response for educational value
                String processedResponse = postProcessEducationalResponse(aiResponse.getText(), intent, userProfile);
                
                // Generate learning suggestions
                List<String> suggestions = generateLearningSuggestions(query, intent, userProfile);
                
                // Determine response type based on intent
                LearningResponse.ResponseType responseType = mapIntentToResponseType(intent);
                
                return new LearningResponse(processedResponse, responseType, currentMode, suggestions);
            } else {
                return handleAIResponseError(aiResponse);
            }
            
        } catch (Exception e) {
            logger.error("Error getting AI response", e);
            return generateFallbackResponse(query, intent, userProfile);
        }
    }
    
    /**
     * Analyze the intent behind a user's query
     */
    private QueryIntent analyzeQueryIntent(String query) {
        String lowerQuery = query.toLowerCase().trim();
        
        // Question patterns
        if (lowerQuery.startsWith("what") || lowerQuery.startsWith("how") || 
            lowerQuery.startsWith("why") || lowerQuery.startsWith("when") ||
            lowerQuery.startsWith("where") || lowerQuery.contains("explain")) {
            return QueryIntent.EXPLANATION_REQUEST;
        }
        
        // Help patterns
        if (lowerQuery.contains("help") || lowerQuery.contains("stuck") || 
            lowerQuery.contains("don't understand") || lowerQuery.contains("confused")) {
            return QueryIntent.HELP_REQUEST;
        }
        
        // Practice patterns
        if (lowerQuery.contains("practice") || lowerQuery.contains("exercise") || 
            lowerQuery.contains("example") || lowerQuery.contains("try")) {
            return QueryIntent.PRACTICE_REQUEST;
        }
        
        // Error/correction patterns
        if (lowerQuery.contains("error") || lowerQuery.contains("wrong") || 
            lowerQuery.contains("mistake") || lowerQuery.contains("fix")) {
            return QueryIntent.ERROR_CORRECTION;
        }
        
        // Step-by-step patterns
        if (lowerQuery.contains("step") || lowerQuery.contains("guide") || 
            lowerQuery.contains("tutorial") || lowerQuery.contains("walk me through")) {
            return QueryIntent.STEP_BY_STEP_GUIDE;
        }
        
        // Summary patterns
        if (lowerQuery.contains("summary") || lowerQuery.contains("recap") || 
            lowerQuery.contains("review") || lowerQuery.contains("summarize")) {
            return QueryIntent.SUMMARY_REQUEST;
        }
        
        // Default to general conversation
        return QueryIntent.GENERAL_CONVERSATION;
    }
    
    /**
     * Enhance the user's prompt with educational context
     */
    private String enhancePromptForLearning(String originalQuery, QueryIntent intent, 
                                          UserProfileManager.UserProfile userProfile) {
        StringBuilder enhancedPrompt = new StringBuilder();
        
        // Add learning context
        enhancedPrompt.append("You are Asos, a friendly and patient learning companion. ");
        
        // Add user profile context
        enhancedPrompt.append(String.format("The user's learning style is %s and skill level is %s. ", 
            userProfile.getLearningStyle(), userProfile.getSkillLevel()));
        
        // Add intent-specific instructions
        switch (intent) {
            case EXPLANATION_REQUEST:
                enhancedPrompt.append("Provide a clear, beginner-friendly explanation. ");
                enhancedPrompt.append("Use simple language and include examples. ");
                break;
                
            case STEP_BY_STEP_GUIDE:
                enhancedPrompt.append("Provide a detailed step-by-step guide. ");
                enhancedPrompt.append("Number each step and explain what to do clearly. ");
                break;
                
            case PRACTICE_REQUEST:
                enhancedPrompt.append("Suggest practical exercises or examples. ");
                enhancedPrompt.append("Make it interactive and engaging. ");
                break;
                
            case ERROR_CORRECTION:
                enhancedPrompt.append("Help identify and fix the problem. ");
                enhancedPrompt.append("Explain what went wrong and how to correct it. ");
                break;
                
            case HELP_REQUEST:
                enhancedPrompt.append("Provide encouraging and supportive help. ");
                enhancedPrompt.append("Break down complex concepts into simpler parts. ");
                break;
                
            case SUMMARY_REQUEST:
                enhancedPrompt.append("Provide a concise summary of key points. ");
                enhancedPrompt.append("Highlight the most important concepts. ");
                break;
                
            default:
                enhancedPrompt.append("Respond in a helpful and educational manner. ");
        }
        
        // Add the original query
        enhancedPrompt.append("\n\nUser question: ").append(originalQuery);
        
        return enhancedPrompt.toString();
    }
    
    /**
     * Post-process AI response to enhance educational value
     */
    private String postProcessEducationalResponse(String response, QueryIntent intent, 
                                                UserProfileManager.UserProfile userProfile) {
        // Add encouraging elements based on learning style
        String processedResponse = response;
        
        // Add visual elements for visual learners
        if (userProfile.getLearningStyle() == UserProfileManager.LearningStyle.VISUAL) {
            processedResponse = addVisualElements(processedResponse);
        }
        
        // Add practical elements for kinesthetic learners
        if (userProfile.getLearningStyle() == UserProfileManager.LearningStyle.KINESTHETIC) {
            processedResponse = addPracticalElements(processedResponse);
        }
        
        // Add encouragement based on skill level
        if (userProfile.getSkillLevel() == UserProfileManager.SkillLevel.BEGINNER) {
            processedResponse += "\n\nRemember, everyone starts as a beginner. You're doing great by asking questions!";
        }
        
        return processedResponse;
    }
    
    /**
     * Add visual elements for visual learners
     */
    private String addVisualElements(String response) {
        // Remove emojis for clean text interface
        response = response.replaceAll("\\b(step \\d+)", "$1");
        response = response.replaceAll("\\b(important|note|remember)", "$1");
        response = response.replaceAll("\\b(example|for instance)", "$1");
        response = response.replaceAll("\\b(tip|hint)", "$1");
        
        return response;
    }
    
    /**
     * Add practical elements for kinesthetic learners
     */
    private String addPracticalElements(String response) {
        if (!response.contains("try") && !response.contains("practice")) {
            response += "\n\n🔧 Try practicing this concept hands-on to reinforce your learning!";
        }
        
        return response;
    }
    
    /**
     * Generate learning suggestions based on the query and user profile
     */
    private List<String> generateLearningSuggestions(String query, QueryIntent intent, 
                                                   UserProfileManager.UserProfile userProfile) {
        List<String> suggestions = new ArrayList<>();
        
        switch (intent) {
            case EXPLANATION_REQUEST:
                suggestions.add("Would you like me to provide some examples?");
                suggestions.add("Should we try a hands-on exercise?");
                break;
                
            case STEP_BY_STEP_GUIDE:
                suggestions.add("Let me know if any step is unclear");
                suggestions.add("Would you like more detail on any particular step?");
                break;
                
            case PRACTICE_REQUEST:
                suggestions.add("Try this example and let me know how it goes");
                suggestions.add("I can provide more practice problems if needed");
                break;
                
            case ERROR_CORRECTION:
                suggestions.add("Let's try the corrected approach step by step");
                suggestions.add("I can watch for similar issues in the future");
                break;
                
            default:
                suggestions.add("Ask me anything else you'd like to know");
                suggestions.add("I'm here to help you learn!");
        }
        
        return suggestions;
    }
    
    /**
     * Map query intent to response type
     */
    private LearningResponse.ResponseType mapIntentToResponseType(QueryIntent intent) {
        switch (intent) {
            case EXPLANATION_REQUEST:
                return LearningResponse.ResponseType.EXPLANATION;
            case STEP_BY_STEP_GUIDE:
                return LearningResponse.ResponseType.TUTORIAL;
            case PRACTICE_REQUEST:
                return LearningResponse.ResponseType.EXERCISE;
            case ERROR_CORRECTION:
                return LearningResponse.ResponseType.CORRECTION;
            case HELP_REQUEST:
                return LearningResponse.ResponseType.HELP;
            case SUMMARY_REQUEST:
                return LearningResponse.ResponseType.SUMMARY;
            default:
                return LearningResponse.ResponseType.CONVERSATION;
        }
    }
    
    /**
     * Handle AI response errors gracefully
     */
    private LearningResponse handleAIResponseError(LocalAIEngine.AIResponse aiResponse) {
        String fallbackMessage = "I'm having trouble processing that right now. Let me try to help in a different way.";
        
        return new LearningResponse(
            fallbackMessage,
            LearningResponse.ResponseType.ERROR,
            currentMode
        );
    }
    
    /**
     * Generate fallback response when AI is unavailable
     */
    private LearningResponse generateFallbackResponse(String query, QueryIntent intent, 
                                                    UserProfileManager.UserProfile userProfile) {
        String fallbackResponse = generateRuleBasedResponse(query, intent);
        
        return new LearningResponse(
            fallbackResponse,
            mapIntentToResponseType(intent),
            currentMode,
            Arrays.asList("I'm working on improving my responses", "Feel free to ask more questions")
        );
    }
    
    /**
     * Generate rule-based response as fallback
     */
    private String generateRuleBasedResponse(String query, QueryIntent intent) {
        List<String> templates = responseTemplates.getOrDefault(
            mapIntentToScenario(intent), 
            responseTemplates.get(LearningScenario.ENCOURAGEMENT)
        );
        
        return templates.get(new Random().nextInt(templates.size()));
    }
    
    /**
     * Map query intent to learning scenario
     */
    private LearningScenario mapIntentToScenario(QueryIntent intent) {
        switch (intent) {
            case EXPLANATION_REQUEST:
                return LearningScenario.CONCEPT_EXPLANATION;
            case STEP_BY_STEP_GUIDE:
                return LearningScenario.STEP_BY_STEP_GUIDE;
            case ERROR_CORRECTION:
                return LearningScenario.ERROR_CORRECTION;
            case HELP_REQUEST:
                return LearningScenario.HINT_GENERATION;
            case SUMMARY_REQUEST:
                return LearningScenario.SUMMARY;
            default:
                return LearningScenario.ENCOURAGEMENT;
        }
    }
    
    /**
     * Initialize response templates for fallback scenarios
     */
    private Map<LearningScenario, List<String>> initializeResponseTemplates() {
        Map<LearningScenario, List<String>> templates = new HashMap<>();
        
        templates.put(LearningScenario.CONCEPT_EXPLANATION, Arrays.asList(
            "Let me explain this concept in simple terms...",
            "This is an important topic. Here's how I understand it...",
            "Great question! Let me break this down for you..."
        ));
        
        templates.put(LearningScenario.STEP_BY_STEP_GUIDE, Arrays.asList(
            "I'll walk you through this step by step...",
            "Let's tackle this one step at a time...",
            "Here's a systematic approach to this..."
        ));
        
        templates.put(LearningScenario.ERROR_CORRECTION, Arrays.asList(
            "I can help you identify what went wrong...",
            "Let's debug this together...",
            "Mistakes are part of learning! Let's fix this..."
        ));
        
        templates.put(LearningScenario.ENCOURAGEMENT, Arrays.asList(
            "You're doing great! Keep asking questions...",
            "Learning takes time, and you're on the right track...",
            "I'm here to help you understand this better..."
        ));
        
        templates.put(LearningScenario.HINT_GENERATION, Arrays.asList(
            "Here's a hint to get you started...",
            "Try thinking about it this way...",
            "Consider this approach..."
        ));
        
        templates.put(LearningScenario.SUMMARY, Arrays.asList(
            "Let me summarize the key points...",
            "Here are the main takeaways...",
            "To recap what we've covered..."
        ));
        
        return templates;
    }
    
    /**
     * Set the current learning mode
     */
    /**
     * Simple query processing method for conversational interface
     * Converts Map context to LearningQueryContext and processes the query synchronously
     */
    public String processQuery(String query, Map<String, String> contextMap) {
        try {
            // Extract context information from map
            String currentTopic = contextMap.getOrDefault("current_pathway", "general");
            String mode = contextMap.getOrDefault("mode", "General Learning");
            
            // Create learning query context
            LearningQueryContext context = new LearningQueryContext(currentTopic, "");
            
            // Add mode information to metadata
            context.getMetadata().put("conversation_mode", mode);
            context.getMetadata().put("timestamp", contextMap.get("timestamp"));
            
            // Set learning mode based on conversation mode
            setLearningModeFromString(mode);
            
            // Process query and get result
            CompletableFuture<LearningResponse> future = processLearningQuery(query, context);
            LearningResponse response = future.get(); // Synchronous wait
            
            // Check if response is valid and not an error
            if (response != null && response.getContent() != null && !response.getContent().trim().isEmpty()) {
                return response.getContent();
            } else {
                // Use enhanced fallback instead of generic error
                return generateErrorResponse(query);
            }
            
        } catch (Exception e) {
            logger.error("Error processing conversational query", e);
            return generateErrorResponse(query);
        }
    }
    
    /**
     * Set learning mode from string representation
     */
    private void setLearningModeFromString(String mode) {
        switch (mode) {
            case "Programming Help":
            case "Problem Solving":
                setLearningMode(LearningMode.PRACTICE);
                break;
            case "Study Planning":
            case "Career Guidance":
                setLearningMode(LearningMode.TUTORIAL);
                break;
            case "Concept Explanation":
                setLearningMode(LearningMode.EXPLANATION);
                break;
            case "General Learning":
            default:
                setLearningMode(LearningMode.CONVERSATION);
                break;
        }
    }
    
    /**
     * Generate error response for failed queries
     */
    private String generateErrorResponse(String query) {
        // Use the enhanced fallback response system instead of generic error
        try {
            // Analyze query intent for better fallback response
            QueryIntent intent = analyzeQueryIntent(query);
            UserProfileManager.UserProfile userProfile = profileManager.getCurrentProfile();
            LearningResponse fallbackResponse = generateFallbackResponse(query, intent, userProfile);
            return fallbackResponse.getContent();
        } catch (Exception e) {
            logger.error("Error generating fallback response", e);
            // Last resort fallback
            String lowerQuery = query.toLowerCase().trim();
            
            if (lowerQuery.contains("hello") || lowerQuery.contains("hi")) {
                return "আছস? Hello! I'm here to help you learn. What would you like to explore today?";
            }
            
            if (lowerQuery.contains("help")) {
                return "I'm here to help! Let's break this down together. What specific topic are you working on?";
            }
            
            return "আছস? I'm your learning companion! While my main AI is starting up, " +
                   "I'm still here to help guide your learning journey. What can I help you discover today?";
        }
    }

    public void setLearningMode(LearningMode mode) {
        this.currentMode = mode;
        logger.debug("Learning mode set to: {}", mode);
    }
    
    /**
     * Set the current topic
     */
    public void setCurrentTopic(String topic) {
        this.currentTopic = topic;
        logger.debug("Current topic set to: {}", topic);
    }
    
    /**
     * Check if AI engine is ready
     */
    public boolean isReady() {
        return aiEngine.isReady();
    }
    
    // Enums and data classes
    
    public enum QueryIntent {
        EXPLANATION_REQUEST, HELP_REQUEST, PRACTICE_REQUEST, ERROR_CORRECTION,
        STEP_BY_STEP_GUIDE, SUMMARY_REQUEST, GENERAL_CONVERSATION
    }
    
    public static class LearningQueryContext {
        private final String currentTopic;
        private final String previousContext;
        private final Map<String, Object> metadata;
        
        public LearningQueryContext(String currentTopic, String previousContext) {
            this.currentTopic = currentTopic;
            this.previousContext = previousContext;
            this.metadata = new HashMap<>();
        }
        
        public String getCurrentTopic() { return currentTopic; }
        public String getPreviousContext() { return previousContext; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    public static class LearningResponse {
        private final String content;
        private final ResponseType type;
        private final LearningMode mode;
        private final List<String> suggestions;
        private final long timestamp;
        
        public enum ResponseType {
            EXPLANATION, TUTORIAL, EXERCISE, CORRECTION, HELP, SUMMARY, CONVERSATION, ERROR
        }
        
        public LearningResponse(String content, ResponseType type, LearningMode mode) {
            this(content, type, mode, Collections.emptyList());
        }
        
        public LearningResponse(String content, ResponseType type, LearningMode mode, List<String> suggestions) {
            this.content = content;
            this.type = type;
            this.mode = mode;
            this.suggestions = new ArrayList<>(suggestions);
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getContent() { return content; }
        public ResponseType getType() { return type; }
        public LearningMode getMode() { return mode; }
        public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("LearningResponse{type=%s, mode=%s, content='%s'}", 
                    type, mode, content.substring(0, Math.min(50, content.length())));
        }
    }
}
