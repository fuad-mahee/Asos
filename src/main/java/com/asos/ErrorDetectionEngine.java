package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Error Detection Engine for providing intelligent error correction suggestions
 * Copilot-like functionality for learning assistance
 */
public class ErrorDetectionEngine {
    private static final Logger logger = LoggerFactory.getLogger(ErrorDetectionEngine.class);
    
    private BiConsumer<String, String> onErrorFound;
    
    public ErrorDetectionEngine() {
        // Initialize engine
    }
    
    /**
     * Detect errors in code and provide suggestions
     */
    public void detectErrors(String filePath, String content) {
        List<ErrorSuggestion> suggestions = analyzeForErrors(content, getLanguageFromPath(filePath));
        
        for (ErrorSuggestion suggestion : suggestions) {
            if (onErrorFound != null) {
                onErrorFound.accept(suggestion.getError(), suggestion.getSuggestion());
            }
        }
    }
    
    /**
     * Analyze content for errors and return suggestions
     */
    private List<ErrorSuggestion> analyzeForErrors(String content, String language) {
        List<ErrorSuggestion> suggestions = new ArrayList<>();
        
        switch (language.toLowerCase()) {
            case "java":
                suggestions.addAll(analyzeJavaErrors(content));
                break;
            case "python":
                suggestions.addAll(analyzePythonErrors(content));
                break;
            case "javascript":
                suggestions.addAll(analyzeJavaScriptErrors(content));
                break;
        }
        
        return suggestions;
    }
    
    /**
     * Analyze Java code for errors
     */
    private List<ErrorSuggestion> analyzeJavaErrors(String content) {
        List<ErrorSuggestion> suggestions = new ArrayList<>();
        
        // Common Java typos and errors
        if (content.contains("Systm.out.println")) {
            suggestions.add(new ErrorSuggestion(
                "Typo in 'Systm.out.println'",
                "Did you mean 'System.out.println'? (with capital S and 'e' in System)"
            ));
        }
        
        if (content.contains("sytem.out.println")) {
            suggestions.add(new ErrorSuggestion(
                "Typo in 'sytem.out.println'",
                "Did you mean 'System.out.println'? (capital S and 's' before 'y')"
            ));
        }
        
        if (content.contains("println(") && !content.contains("System.out.println")) {
            suggestions.add(new ErrorSuggestion(
                "Missing System.out prefix",
                "Use 'System.out.println()' instead of just 'println()'"
            ));
        }
        
        // Check for missing main method
        if (content.contains("public class") && !content.contains("public static void main")) {
            suggestions.add(new ErrorSuggestion(
                "Missing main method",
                "Add 'public static void main(String[] args)' method to run your program"
            ));
        }
        
        // Check for missing semicolons
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if ((line.contains("System.out.println") || line.contains("int ") || line.contains("String ")) 
                && !line.endsWith(";") && !line.endsWith("{") && !line.endsWith("}")) {
                suggestions.add(new ErrorSuggestion(
                    "Missing semicolon on line " + (i + 1),
                    "Add a semicolon (;) at the end of the statement"
                ));
            }
        }
        
        return suggestions;
    }
    
    /**
     * Analyze Python code for errors
     */
    private List<ErrorSuggestion> analyzePythonErrors(String content) {
        List<ErrorSuggestion> suggestions = new ArrayList<>();
        
        // Common Python typos
        if (content.contains("pirnt(")) {
            suggestions.add(new ErrorSuggestion(
                "Typo in 'pirnt'",
                "Did you mean 'print'? (with 'r' and 'i' swapped)"
            ));
        }
        
        if (content.contains("pyhton")) {
            suggestions.add(new ErrorSuggestion(
                "Typo in 'pyhton'",
                "Did you mean 'python'? (with 't' and 'h' swapped)"
            ));
        }
        
        // Check for semicolons (not needed in Python)
        if (content.contains(");")) {
            suggestions.add(new ErrorSuggestion(
                "Unnecessary semicolon",
                "Python doesn't require semicolons at the end of statements"
            ));
        }
        
        return suggestions;
    }
    
    /**
     * Analyze JavaScript code for errors
     */
    private List<ErrorSuggestion> analyzeJavaScriptErrors(String content) {
        List<ErrorSuggestion> suggestions = new ArrayList<>();
        
        // Common JavaScript typos
        if (content.contains("consol.log")) {
            suggestions.add(new ErrorSuggestion(
                "Typo in 'consol.log'",
                "Did you mean 'console.log'? (missing 'e' in console)"
            ));
        }
        
        if (content.contains("console.log") && content.split("console\\.log").length > 3) {
            suggestions.add(new ErrorSuggestion(
                "Multiple console.log statements",
                "Consider using a debugger or proper logging instead of multiple console.log"
            ));
        }
        
        return suggestions;
    }
    
    /**
     * Get programming language from file path
     */
    private String getLanguageFromPath(String filePath) {
        if (filePath.endsWith(".java")) return "java";
        if (filePath.endsWith(".py")) return "python";
        if (filePath.endsWith(".js") || filePath.endsWith(".ts")) return "javascript";
        return "unknown";
    }
    
    /**
     * Set callback for when errors are found
     */
    public void setOnErrorFound(BiConsumer<String, String> callback) {
        this.onErrorFound = callback;
    }
    
    /**
     * Error suggestion class
     */
    private static class ErrorSuggestion {
        private final String error;
        private final String suggestion;
        
        public ErrorSuggestion(String error, String suggestion) {
            this.error = error;
            this.suggestion = suggestion;
        }
        
        public String getError() { return error; }
        public String getSuggestion() { return suggestion; }
    }
}
