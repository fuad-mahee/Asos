package com.asos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Data classes for JSON pathway configuration
 */
public class PathwayConfig {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("difficulty")
    private String difficulty;
    
    @JsonProperty("estimatedTime")
    private String estimatedTime;
    
    @JsonProperty("prerequisites")
    private List<String> prerequisites;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("character")
    private CharacterConfig character;
    
    @JsonProperty("steps")
    private List<StepConfig> steps;
    
    @JsonProperty("adaptivity")
    private AdaptivityConfig adaptivity;
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public String getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(String estimatedTime) { this.estimatedTime = estimatedTime; }
    
    public List<String> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public CharacterConfig getCharacter() { return character; }
    public void setCharacter(CharacterConfig character) { this.character = character; }
    
    public List<StepConfig> getSteps() { return steps; }
    public void setSteps(List<StepConfig> steps) { this.steps = steps; }
    
    public AdaptivityConfig getAdaptivity() { return adaptivity; }
    public void setAdaptivity(AdaptivityConfig adaptivity) { this.adaptivity = adaptivity; }
    
    public static class CharacterConfig {
        @JsonProperty("welcomeMessage")
        private String welcomeMessage;
        
        @JsonProperty("completionMessage")
        private String completionMessage;
        
        public String getWelcomeMessage() { return welcomeMessage; }
        public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
        
        public String getCompletionMessage() { return completionMessage; }
        public void setCompletionMessage(String completionMessage) { this.completionMessage = completionMessage; }
    }
    
    public static class StepConfig {
        @JsonProperty("stepNumber")
        private int stepNumber;
        
        @JsonProperty("instruction")
        private String instruction;
        
        @JsonProperty("detailedInstruction")
        private String detailedInstruction;
        
        @JsonProperty("expectedActions")
        private List<String> expectedActions;
        
        @JsonProperty("validation")
        private ValidationConfig validation;
        
        @JsonProperty("successMessage")
        private String successMessage;
        
        @JsonProperty("errorMessages")
        private Map<String, String> errorMessages;
        
        @JsonProperty("hints")
        private List<String> hints;
        
        // Getters and setters
        public int getStepNumber() { return stepNumber; }
        public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }
        
        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }
        
        public String getDetailedInstruction() { return detailedInstruction; }
        public void setDetailedInstruction(String detailedInstruction) { this.detailedInstruction = detailedInstruction; }
        
        public List<String> getExpectedActions() { return expectedActions; }
        public void setExpectedActions(List<String> expectedActions) { this.expectedActions = expectedActions; }
        
        public ValidationConfig getValidation() { return validation; }
        public void setValidation(ValidationConfig validation) { this.validation = validation; }
        
        public String getSuccessMessage() { return successMessage; }
        public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }
        
        public Map<String, String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(Map<String, String> errorMessages) { this.errorMessages = errorMessages; }
        
        public List<String> getHints() { return hints; }
        public void setHints(List<String> hints) { this.hints = hints; }
    }
    
    public static class ValidationConfig {
        @JsonProperty("processNames")
        private List<String> processNames;
        
        @JsonProperty("windowTitles")
        private List<String> windowTitles;
        
        @JsonProperty("filePatterns")
        private List<String> filePatterns;
        
        @JsonProperty("fileExists")
        private List<String> fileExists;
        
        @JsonProperty("fileContent")
        private List<String> fileContent;
        
        @JsonProperty("commandOutput")
        private List<String> commandOutput;
        
        @JsonProperty("commandHistory")
        private List<String> commandHistory;
        
        @JsonProperty("downloadFolder")
        private Boolean downloadFolder;
        
        @JsonProperty("timeout")
        private int timeout;
        
        @JsonProperty("registryKeys")
        private List<String> registryKeys;
        
        @JsonProperty("fileExtension")
        private List<String> fileExtension;
        
        @JsonProperty("fileSaved")
        private Boolean fileSaved;
        
        @JsonProperty("currentPath")
        private List<String> currentPath;
        
        // Getters and setters
        public List<String> getProcessNames() { return processNames; }
        public void setProcessNames(List<String> processNames) { this.processNames = processNames; }
        
        public List<String> getWindowTitles() { return windowTitles; }
        public void setWindowTitles(List<String> windowTitles) { this.windowTitles = windowTitles; }
        
        public List<String> getFilePatterns() { return filePatterns; }
        public void setFilePatterns(List<String> filePatterns) { this.filePatterns = filePatterns; }
        
        public List<String> getFileExists() { return fileExists; }
        public void setFileExists(List<String> fileExists) { this.fileExists = fileExists; }
        
        public List<String> getFileContent() { return fileContent; }
        public void setFileContent(List<String> fileContent) { this.fileContent = fileContent; }
        
        public List<String> getCommandOutput() { return commandOutput; }
        public void setCommandOutput(List<String> commandOutput) { this.commandOutput = commandOutput; }
        
        public List<String> getCommandHistory() { return commandHistory; }
        public void setCommandHistory(List<String> commandHistory) { this.commandHistory = commandHistory; }
        
        public Boolean getDownloadFolder() { return downloadFolder; }
        public void setDownloadFolder(Boolean downloadFolder) { this.downloadFolder = downloadFolder; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
        public List<String> getRegistryKeys() { return registryKeys; }
        public void setRegistryKeys(List<String> registryKeys) { this.registryKeys = registryKeys; }
        
        public List<String> getFileExtension() { return fileExtension; }
        public void setFileExtension(List<String> fileExtension) { this.fileExtension = fileExtension; }
        
        public Boolean getFileSaved() { return fileSaved; }
        public void setFileSaved(Boolean fileSaved) { this.fileSaved = fileSaved; }
        
        public List<String> getCurrentPath() { return currentPath; }
        public void setCurrentPath(List<String> currentPath) { this.currentPath = currentPath; }
    }
    
    public static class AdaptivityConfig {
        @JsonProperty("fastLearner")
        private LearnerConfig fastLearner;
        
        @JsonProperty("slowLearner")
        private LearnerConfig slowLearner;
        
        @JsonProperty("errorProne")
        private LearnerConfig errorProne;
        
        public LearnerConfig getFastLearner() { return fastLearner; }
        public void setFastLearner(LearnerConfig fastLearner) { this.fastLearner = fastLearner; }
        
        public LearnerConfig getSlowLearner() { return slowLearner; }
        public void setSlowLearner(LearnerConfig slowLearner) { this.slowLearner = slowLearner; }
        
        public LearnerConfig getErrorProne() { return errorProne; }
        public void setErrorProne(LearnerConfig errorProne) { this.errorProne = errorProne; }
        
        public static class LearnerConfig {
            @JsonProperty("threshold")
            private int threshold;
            
            @JsonProperty("actions")
            private List<String> actions;
            
            public int getThreshold() { return threshold; }
            public void setThreshold(int threshold) { this.threshold = threshold; }
            
            public List<String> getActions() { return actions; }
            public void setActions(List<String> actions) { this.actions = actions; }
        }
    }
}
