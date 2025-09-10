package com.asos;

import java.util.List;
import java.util.Map;

/**
 * Enhanced event detector that validates user actions against learning step requirements
 */
public class EventValidator {
    
    public enum ValidationResult {
        SUCCESS,
        PARTIAL_MATCH,
        NO_MATCH,
        ERROR
    }
    
    public static class ValidationResponse {
        private ValidationResult result;
        private String message;
        private double confidence;
        private String suggestion;
        
        public ValidationResponse(ValidationResult result, String message, double confidence) {
            this.result = result;
            this.message = message;
            this.confidence = confidence;
        }
        
        public ValidationResponse(ValidationResult result, String message, double confidence, String suggestion) {
            this(result, message, confidence);
            this.suggestion = suggestion;
        }
        
        // Getters and setters
        public ValidationResult getResult() { return result; }
        public void setResult(ValidationResult result) { this.result = result; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
    
    /**
     * Validates a file system event against step configuration
     */
    public static ValidationResponse validateFileSystemEvent(String event, PathwayConfig.StepConfig stepConfig) {
        if (stepConfig == null || stepConfig.getValidation() == null) {
            return new ValidationResponse(ValidationResult.NO_MATCH, "No validation criteria defined", 0.0);
        }
        
        PathwayConfig.ValidationConfig validation = stepConfig.getValidation();
        
        // Check file creation events
        if (event.contains("ENTRY_CREATE")) {
            if (validation.getFileExists() != null) {
                for (String expectedFile : validation.getFileExists()) {
                    if (event.toLowerCase().contains(expectedFile.toLowerCase().replace("**/", ""))) {
                        return new ValidationResponse(ValidationResult.SUCCESS, 
                            "File created successfully: " + extractFileName(event), 0.9);
                    }
                }
            }
            
            if (validation.getFilePatterns() != null) {
                for (String pattern : validation.getFilePatterns()) {
                    if (matchesPattern(event, pattern)) {
                        return new ValidationResponse(ValidationResult.SUCCESS, 
                            "File matches expected pattern", 0.8);
                    }
                }
            }
        }
        
        // Check file modification events
        if (event.contains("ENTRY_MODIFY")) {
            if (stepConfig.getExpectedActions().contains("written") || 
                stepConfig.getExpectedActions().contains("modified")) {
                return new ValidationResponse(ValidationResult.SUCCESS, 
                    "File modified as expected", 0.8);
            }
        }
        
        return new ValidationResponse(ValidationResult.NO_MATCH, 
            "File system event doesn't match expected action", 0.1,
            "Expected: " + String.join(", ", stepConfig.getExpectedActions()));
    }
    
    /**
     * Validates a process event against step configuration
     */
    public static ValidationResponse validateProcessEvent(String event, PathwayConfig.StepConfig stepConfig) {
        if (stepConfig == null || stepConfig.getValidation() == null) {
            return new ValidationResponse(ValidationResult.NO_MATCH, "No validation criteria defined", 0.0);
        }
        
        PathwayConfig.ValidationConfig validation = stepConfig.getValidation();
        
        if (event.contains("Started")) {
            // Extract process name from event
            String processName = extractProcessName(event);
            
            if (validation.getProcessNames() != null) {
                for (String expectedProcess : validation.getProcessNames()) {
                    if (processName.toLowerCase().contains(expectedProcess.toLowerCase().replace(".exe", ""))) {
                        return new ValidationResponse(ValidationResult.SUCCESS, 
                            "Correct application started: " + processName, 0.9);
                    }
                }
            }
            
            if (validation.getWindowTitles() != null) {
                for (String expectedTitle : validation.getWindowTitles()) {
                    if (event.toLowerCase().contains(expectedTitle.toLowerCase())) {
                        return new ValidationResponse(ValidationResult.PARTIAL_MATCH, 
                            "Window with expected title detected", 0.7);
                    }
                }
            }
            
            // Check if it's a browser-related process for web steps
            if (isBrowserProcess(processName) && stepConfig.getInstruction().toLowerCase().contains("browser")) {
                return new ValidationResponse(ValidationResult.SUCCESS, 
                    "Browser opened as requested", 0.8);
            }
            
            // Check if it's a terminal/command prompt for command line steps
            if (isTerminalProcess(processName) && 
                (stepConfig.getInstruction().toLowerCase().contains("terminal") || 
                 stepConfig.getInstruction().toLowerCase().contains("command"))) {
                return new ValidationResponse(ValidationResult.SUCCESS, 
                    "Command line interface opened", 0.8);
            }
        }
        
        return new ValidationResponse(ValidationResult.NO_MATCH, 
            "Process event doesn't match expected action", 0.1,
            "Expected one of: " + String.join(", ", 
                validation.getProcessNames() != null ? validation.getProcessNames() : List.of("specific process")));
    }
    
    /**
     * Validates user progress based on timing and error patterns
     */
    public static ValidationResponse validateUserProgress(long timeElapsed, int errorCount, 
                                                        PathwayConfig.StepConfig stepConfig) {
        if (stepConfig == null || stepConfig.getValidation() == null) {
            return new ValidationResponse(ValidationResult.NO_MATCH, "No timing criteria defined", 0.0);
        }
        
        int timeout = stepConfig.getValidation().getTimeout();
        
        // Check if user is taking too long
        if (timeElapsed > timeout * 1000) { // Convert to milliseconds
            return new ValidationResponse(ValidationResult.ERROR, 
                "Step is taking longer than expected", 0.3,
                "Try following the hints or ask for help");
        }
        
        // Check if user is making too many errors
        if (errorCount > 3) {
            return new ValidationResponse(ValidationResult.ERROR, 
                "Multiple errors detected", 0.2,
                "Let's break this down into smaller steps");
        }
        
        // User is progressing well
        if (timeElapsed < timeout * 0.5 && errorCount == 0) {
            return new ValidationResponse(ValidationResult.SUCCESS, 
                "Great progress! You're learning quickly", 0.9);
        }
        
        return new ValidationResponse(ValidationResult.PARTIAL_MATCH, 
            "Normal progress", 0.6);
    }
    
    private static String extractFileName(String event) {
        // Extract filename from file system event
        String[] parts = event.split(" on ");
        if (parts.length > 1) {
            String path = parts[1];
            String[] pathParts = path.split("[/\\\\]");
            return pathParts[pathParts.length - 1];
        }
        return "file";
    }
    
    private static String extractProcessName(String event) {
        // Extract process name from process event
        if (event.contains("App=")) {
            String[] parts = event.split("App=");
            if (parts.length > 1) {
                String appPart = parts[1];
                String[] appParts = appPart.split(",");
                return appParts[0].trim();
            }
        }
        return "unknown";
    }
    
    private static boolean matchesPattern(String event, String pattern) {
        // Simple pattern matching - can be enhanced with regex
        String cleanPattern = pattern.replace("*", "").toLowerCase();
        return event.toLowerCase().contains(cleanPattern);
    }
    
    private static boolean isBrowserProcess(String processName) {
        String lowerName = processName.toLowerCase();
        return lowerName.contains("chrome") || 
               lowerName.contains("firefox") || 
               lowerName.contains("edge") || 
               lowerName.contains("safari") || 
               lowerName.contains("opera") ||
               lowerName.contains("browser");
    }
    
    private static boolean isTerminalProcess(String processName) {
        String lowerName = processName.toLowerCase();
        return lowerName.contains("cmd") || 
               lowerName.contains("powershell") || 
               lowerName.contains("terminal") || 
               lowerName.contains("bash") ||
               lowerName.contains("zsh");
    }
    
    /**
     * Validates registry changes against step configuration
     */
    public static ValidationResponse validateRegistryEvent(String event, PathwayConfig.StepConfig stepConfig) {
        if (stepConfig == null || stepConfig.getValidation() == null) {
            return new ValidationResponse(ValidationResult.NO_MATCH, "No validation criteria defined", 0.0);
        }
        
        PathwayConfig.ValidationConfig validation = stepConfig.getValidation();
        
        // Check for expected registry keys
        if (validation.getRegistryKeys() != null) {
            for (String registryKey : validation.getRegistryKeys()) {
                if (event.contains(registryKey)) {
                    return new ValidationResponse(ValidationResult.SUCCESS, 
                        "Registry key change matches expectation", 0.9);
                }
            }
        }
        
        return new ValidationResponse(ValidationResult.NO_MATCH, 
            "Registry event doesn't match expected changes", 0.1,
            "Expected registry keys: " + validation.getRegistryKeys());
    }
}
