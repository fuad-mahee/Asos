package com.asos;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Manages loading and accessing JSON-based knowledge base files
 */
public class KnowledgeBaseManager {
    
    private ObjectMapper objectMapper;
    private Map<String, PathwayConfig> pathways;
    private Map<String, Object> characterResponses;
    private Random random;
    
    public KnowledgeBaseManager() {
        this.objectMapper = new ObjectMapper();
        this.pathways = new HashMap<>();
        this.random = new Random();
        loadKnowledgeBase();
    }
    
    private void loadKnowledgeBase() {
        try {
            // Load character responses first
            loadCharacterResponses();
            
            // Load pathway configurations
            loadPathway("python-basics");
            loadPathway("file-navigation");
            
            System.out.println("Knowledge base loaded successfully!");
            System.out.println("Available pathways: " + pathways.keySet());
            
        } catch (Exception e) {
            System.err.println("Error loading knowledge base: " + e.getMessage());
            e.printStackTrace();
            
            // Initialize fallback responses if JSON loading fails
            initializeFallbackResponses();
        }
    }
    
    private void loadPathway(String pathwayId) throws IOException {
        String resourcePath = "/pathways/" + pathwayId + ".json";
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        
        if (inputStream == null) {
            throw new IOException("Could not find pathway file: " + resourcePath);
        }
        
        PathwayConfig config = objectMapper.readValue(inputStream, PathwayConfig.class);
        pathways.put(pathwayId, config);
        
        System.out.println("Loaded pathway: " + config.getName() + " (" + config.getSteps().size() + " steps)");
    }
    
    @SuppressWarnings("unchecked")
    private void loadCharacterResponses() throws IOException {
        String resourcePath = "/responses/character-responses.json";
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        
        if (inputStream == null) {
            throw new IOException("Could not find character responses file: " + resourcePath);
        }
        
        characterResponses = objectMapper.readValue(inputStream, Map.class);
        System.out.println("Loaded character responses with " + characterResponses.size() + " categories");
    }
    
    private void initializeFallbackResponses() {
        characterResponses = new HashMap<>();
        
        // Basic fallback responses
        characterResponses.put("greetings", java.util.List.of(
            "আছস? (Are you there?) 👋\nI'm here to help you learn!",
            "Hello! 😊\nReady for some fun learning?"
        ));
        
        characterResponses.put("encouragement", java.util.List.of(
            "You're doing great! Keep going! 💪",
            "Excellent work! I'm proud of you!"
        ));
        
        characterResponses.put("success", java.util.List.of(
            "Fantastic! You nailed it!",
            "✅ Perfect! You completed that step beautifully!"
        ));
        
        characterResponses.put("errors", java.util.List.of(
            "Oops! That's not quite right. Let me help you fix it. 🤔",
            "No worries! Mistakes help us learn. Let's try again! 💪"
        ));
        
        characterResponses.put("waiting", java.util.List.of(
            "Take your time! I'm here when you're ready. 😌",
            "No rush! Learning at your own pace is best. ⏰"
        ));
        
        characterResponses.put("completion", java.util.List.of(
            "Congratulations! You've completed the entire pathway!",
            "Amazing! You're now skilled in this area!"
        ));
        
        System.out.println("Initialized fallback character responses");
    }
    
    public PathwayConfig getPathway(String pathwayId) {
        return pathways.get(pathwayId);
    }
    
    public Map<String, PathwayConfig> getAllPathways() {
        return new HashMap<>(pathways);
    }
    
    public LearningPathway createLearningPathway(String pathwayId) {
        PathwayConfig config = pathways.get(pathwayId);
        if (config == null) {
            return null;
        }
        
        LearningPathway pathway = new LearningPathway(config.getId(), config.getName());
        
        // Convert JSON steps to LearningStep objects
        for (PathwayConfig.StepConfig stepConfig : config.getSteps()) {
            LearningStep step = new LearningStep(
                stepConfig.getStepNumber(),
                stepConfig.getInstruction(),
                String.join(",", stepConfig.getExpectedActions()),
                stepConfig.getSuccessMessage()
            );
            pathway.addStep(step);
        }
        
        return pathway;
    }
    
    public String getRandomResponse(String category) {
        return getRandomResponse(category, null);
    }
    
    @SuppressWarnings("unchecked")
    public String getRandomResponse(String category, String subcategory) {
        try {
            Object categoryData = characterResponses.get(category);
            if (categoryData == null) {
                return "আছস? I'm here to help! 😊";
            }
            
            List<String> responses;
            if (subcategory != null && categoryData instanceof Map) {
                Map<String, Object> categoryMap = (Map<String, Object>) categoryData;
                Object subcategoryData = categoryMap.get(subcategory);
                if (subcategoryData instanceof List) {
                    responses = (List<String>) subcategoryData;
                } else {
                    return "আছস? I'm here to help! 😊";
                }
            } else if (categoryData instanceof List) {
                responses = (List<String>) categoryData;
            } else {
                return "আছস? I'm here to help! 😊";
            }
            
            if (responses.isEmpty()) {
                return "আছস? I'm here to help! 😊";
            }
            
            return responses.get(random.nextInt(responses.size()));
            
        } catch (Exception e) {
            System.err.println("Error getting response for category " + category + ": " + e.getMessage());
            return "আছস? I'm here to help! 😊";
        }
    }
    
    public String getWelcomeMessage(String pathwayId) {
        PathwayConfig config = pathways.get(pathwayId);
        if (config != null && config.getCharacter() != null) {
            return config.getCharacter().getWelcomeMessage();
        }
        return getRandomResponse("greetings");
    }
    
    public String getCompletionMessage(String pathwayId) {
        PathwayConfig config = pathways.get(pathwayId);
        if (config != null && config.getCharacter() != null) {
            return config.getCharacter().getCompletionMessage();
        }
        return getRandomResponse("completion");
    }
    
    public PathwayConfig.StepConfig getStepConfig(String pathwayId, int stepNumber) {
        PathwayConfig config = pathways.get(pathwayId);
        if (config != null && config.getSteps() != null) {
            for (PathwayConfig.StepConfig step : config.getSteps()) {
                if (step.getStepNumber() == stepNumber) {
                    return step;
                }
            }
        }
        return null;
    }
    
    public boolean isValidPathway(String pathwayId) {
        return pathways.containsKey(pathwayId);
    }
    
    public int getPathwayStepCount(String pathwayId) {
        PathwayConfig config = pathways.get(pathwayId);
        return config != null ? config.getSteps().size() : 0;
    }
}
