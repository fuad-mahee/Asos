package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * Code Analyzer for detecting common programming errors and providing suggestions
 * Similar to GitHub Copilot's error detection but focused on learning
 */
public class CodeAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalyzer.class);
    
    private BiConsumer<String, String> onErrorDetected;
    
    // Common error patterns for different languages
    private static final List<ErrorPattern> JAVA_ERROR_PATTERNS = new ArrayList<>();
    private static final List<ErrorPattern> PYTHON_ERROR_PATTERNS = new ArrayList<>();
    private static final List<ErrorPattern> JAVASCRIPT_ERROR_PATTERNS = new ArrayList<>();
    
    static {
        initializeErrorPatterns();
    }
    
    public CodeAnalyzer() {
        // Initialize with default settings
    }
    
    /**
     * Analyze code for common errors and learning opportunities
     */
    public void analyzeCode(String filePath, String content) {
        if (content == null || content.trim().isEmpty()) return;
        
        String extension = getFileExtension(filePath);
        List<String> errors = new ArrayList<>();
        
        switch (extension.toLowerCase()) {
            case "java":
                errors.addAll(analyzeJavaCode(content));
                break;
            case "py":
                errors.addAll(analyzePythonCode(content));
                break;
            case "js":
            case "ts":
                errors.addAll(analyzeJavaScriptCode(content));
                break;
        }
        
        // Report errors
        for (String error : errors) {
            if (onErrorDetected != null) {
                onErrorDetected.accept(filePath, error);
            }
        }
        
        if (!errors.isEmpty()) {
            logger.debug("Found {} potential issues in {}", errors.size(), filePath);
        }
    }
    
    /**
     * Analyze Java code for common errors
     */
    private List<String> analyzeJavaCode(String content) {
        List<String> errors = new ArrayList<>();
        
        // Check for common Java mistakes
        for (ErrorPattern pattern : JAVA_ERROR_PATTERNS) {
            if (Pattern.compile(pattern.getPattern(), Pattern.CASE_INSENSITIVE).matcher(content).find()) {
                errors.add(pattern.getErrorMessage());
            }
        }
        
        // Additional Java-specific checks
        if (content.contains("Systm.out.println")) {
            errors.add("Typo detected: 'Systm' should be 'System'");
        }
        
        if (content.contains("public class") && !content.contains("public static void main")) {
            if (content.contains("main(")) {
                errors.add("Main method signature might be incorrect. Use: public static void main(String[] args)");
            }
        }
        
        // Check for missing semicolons (basic check)
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("System.out.println") && !line.endsWith(";") && !line.endsWith("{")) {
                errors.add("Line " + (i + 1) + ": Missing semicolon after System.out.println");
            }
        }
        
        return errors;
    }
    
    /**
     * Analyze Python code for common errors
     */
    private List<String> analyzePythonCode(String content) {
        List<String> errors = new ArrayList<>();
        
        for (ErrorPattern pattern : PYTHON_ERROR_PATTERNS) {
            if (Pattern.compile(pattern.getPattern(), Pattern.CASE_INSENSITIVE).matcher(content).find()) {
                errors.add(pattern.getErrorMessage());
            }
        }
        
        // Python-specific checks
        if (content.contains("pyhton")) {
            errors.add("Typo detected: 'pyhton' should be 'python'");
        }
        
        if (content.contains("pirnt")) {
            errors.add("Typo detected: 'pirnt' should be 'print'");
        }
        
        return errors;
    }
    
    /**
     * Analyze JavaScript code for common errors
     */
    private List<String> analyzeJavaScriptCode(String content) {
        List<String> errors = new ArrayList<>();
        
        for (ErrorPattern pattern : JAVASCRIPT_ERROR_PATTERNS) {
            if (Pattern.compile(pattern.getPattern()).matcher(content).find()) {
                errors.add(pattern.getErrorMessage());
            }
        }
        
        // JavaScript-specific checks
        if (content.contains("consol.log")) {
            errors.add("Typo detected: 'consol.log' should be 'console.log'");
        }
        
        return errors;
    }
    
    /**
     * Get file extension from file path
     */
    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1) : "";
    }
    
    /**
     * Initialize error patterns for different languages
     */
    private static void initializeErrorPatterns() {
        // Java error patterns
        JAVA_ERROR_PATTERNS.add(new ErrorPattern(
            "class\\s+\\w+\\s*\\{[^}]*public\\s+static\\s+void\\s+main\\s*\\([^)]*\\)\\s*\\{[^}]*\\}[^}]*\\}",
            "Consider adding some actual code in your main method"
        ));
        
        JAVA_ERROR_PATTERNS.add(new ErrorPattern(
            "System\\.out\\.print(?!ln)",
            "Consider using System.out.println() instead of System.out.print() for better output formatting"
        ));
        
        // Python error patterns
        PYTHON_ERROR_PATTERNS.add(new ErrorPattern(
            "print\\s*\\([^)]*\\)\\s*;",
            "Python doesn't need semicolons at the end of statements"
        ));
        
        PYTHON_ERROR_PATTERNS.add(new ErrorPattern(
            "==\\s*None|None\\s*==",
            "Use 'is None' instead of '== None' in Python"
        ));
        
        // JavaScript error patterns
        JAVASCRIPT_ERROR_PATTERNS.add(new ErrorPattern(
            "var\\s+\\w+",
            "Consider using 'let' or 'const' instead of 'var' for better scoping"
        ));
        
        JAVASCRIPT_ERROR_PATTERNS.add(new ErrorPattern(
            "console\\.log\\s*\\([^)]*\\)\\s*;?.*console\\.log",
            "Multiple console.log statements - consider using a debugger or proper logging"
        ));
    }
    
    /**
     * Set callback for error detection
     */
    public void setOnErrorDetected(BiConsumer<String, String> callback) {
        this.onErrorDetected = callback;
    }
    
    /**
     * Error pattern class for storing regex patterns and error messages
     */
    private static class ErrorPattern {
        private final String pattern;
        private final String errorMessage;
        
        public ErrorPattern(String pattern, String errorMessage) {
            this.pattern = pattern;
            this.errorMessage = errorMessage;
        }
        
        public String getPattern() { return pattern; }
        public String getErrorMessage() { return errorMessage; }
    }
}
