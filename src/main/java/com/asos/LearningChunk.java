package com.asos;

import java.util.List;
import java.util.Map;

/**
 * Represents a single learning chunk in the teaching system
 * Each chunk is a finite state with specific instructions and expected actions
 */
public class LearningChunk {
    private int chunkId;
    private String instruction;
    private String detailedInstruction;
    private List<ExpectedAction> expectedActions;
    private String hint;
    private String errorCorrection;
    private int timeoutSeconds;
    private ChunkDifficulty difficulty;
    
    public enum ChunkDifficulty {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
    
    public static class ExpectedAction {
        private ActionType type;
        private String target;
        private String pattern;
        private Map<String, String> parameters;
        
        public enum ActionType {
            FILE_CREATED, FILE_MODIFIED, CODE_CONTAINS, TERMINAL_OUTPUT, 
            FOLDER_OPENED, COMPILE_SUCCESS, RUN_SUCCESS, ERROR_FIXED
        }
        
        public ExpectedAction() {}
        
        public ExpectedAction(ActionType type, String target, String pattern) {
            this.type = type;
            this.target = target;
            this.pattern = pattern;
        }
        
        // Getters and setters
        public ActionType getType() { return type; }
        public void setType(ActionType type) { this.type = type; }
        
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public Map<String, String> getParameters() { return parameters; }
        public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
    }
    
    // Constructors
    public LearningChunk() {}
    
    public LearningChunk(int chunkId, String instruction, List<ExpectedAction> expectedActions) {
        this.chunkId = chunkId;
        this.instruction = instruction;
        this.expectedActions = expectedActions;
        this.timeoutSeconds = 300; // Default 5 minutes
        this.difficulty = ChunkDifficulty.BEGINNER;
    }
    
    // Getters and setters
    public int getChunkId() { return chunkId; }
    public void setChunkId(int chunkId) { this.chunkId = chunkId; }
    
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    
    public String getDetailedInstruction() { return detailedInstruction; }
    public void setDetailedInstruction(String detailedInstruction) { this.detailedInstruction = detailedInstruction; }
    
    public List<ExpectedAction> getExpectedActions() { return expectedActions; }
    public void setExpectedActions(List<ExpectedAction> expectedActions) { this.expectedActions = expectedActions; }
    
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    
    public String getErrorCorrection() { return errorCorrection; }
    public void setErrorCorrection(String errorCorrection) { this.errorCorrection = errorCorrection; }
    
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public ChunkDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(ChunkDifficulty difficulty) { this.difficulty = difficulty; }
}
