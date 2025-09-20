package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Real-time monitoring system for detecting learner actions
 * Monitors file system, code changes, terminal output, and compilation results
 */
public class RealTimeMonitor {
    private static final Logger logger = LoggerFactory.getLogger(RealTimeMonitor.class);
    
    private final FileSystemWatcher fileWatcher;
    private final TerminalMonitor terminalMonitor;
    private final CodeAnalyzer codeAnalyzer;
    private final ScheduledExecutorService scheduler;
    
    private List<LearningChunk.ExpectedAction> expectedActions;
    private final Set<LearningChunk.ExpectedAction> completedActions;
    private final Map<String, String> fileContents;
    
    // Callbacks
    private Consumer<LearningChunk.ExpectedAction> onActionDetected;
    private Consumer<String> onErrorDetected;
    
    public RealTimeMonitor() {
        this.fileWatcher = new FileSystemWatcher();
        this.terminalMonitor = new TerminalMonitor();
        this.codeAnalyzer = new CodeAnalyzer();
        this.scheduler = Executors.newScheduledThreadPool(3);
        this.completedActions = ConcurrentHashMap.newKeySet();
        this.fileContents = new ConcurrentHashMap<>();
        
        setupWatchers();
    }
    
    /**
     * Start monitoring for specific expected actions
     */
    public void startMonitoring(List<LearningChunk.ExpectedAction> expectedActions) {
        this.expectedActions = new ArrayList<>(expectedActions);
        this.completedActions.clear();
        
        // Start all watchers
        fileWatcher.startWatching();
        terminalMonitor.startMonitoring();
        
        logger.info("Started monitoring for {} expected actions", expectedActions.size());
    }
    
    /**
     * Stop all monitoring
     */
    public void stopMonitoring() {
        fileWatcher.stopWatching();
        terminalMonitor.stopMonitoring();
        completedActions.clear();
        
        logger.info("Stopped monitoring");
    }
    
    /**
     * Check if all expected actions are completed
     */
    public boolean areAllActionsCompleted() {
        return completedActions.size() == expectedActions.size();
    }
    
    /**
     * Setup watchers with callbacks
     */
    private void setupWatchers() {
        // File system watcher
        fileWatcher.setOnFileEvent((eventType, filePath) -> {
            handleFileEvent(eventType, filePath);
        });
        
        // Terminal output watcher
        terminalMonitor.setOnTerminalOutput((command, output) -> {
            handleTerminalOutput(command, output);
        });
        
        // Code analyzer for syntax/error detection
        codeAnalyzer.setOnErrorDetected((filePath, error) -> {
            if (onErrorDetected != null) {
                onErrorDetected.accept("Code error in " + filePath + ": " + error);
            }
        });
    }
    
    /**
     * Handle file system events
     */
    private void handleFileEvent(FileSystemWatcher.EventType eventType, String filePath) {
        logger.debug("File event: {} - {}", eventType, filePath);
        
        for (LearningChunk.ExpectedAction action : expectedActions) {
            if (completedActions.contains(action)) continue;
            
            boolean actionMatched = false;
            
            switch (action.getType()) {
                case FILE_CREATED:
                    if (eventType == FileSystemWatcher.EventType.CREATED && 
                        matchesPattern(filePath, action.getPattern())) {
                        actionMatched = true;
                    }
                    break;
                    
                case FILE_MODIFIED:
                    if (eventType == FileSystemWatcher.EventType.MODIFIED && 
                        matchesPattern(filePath, action.getPattern())) {
                        actionMatched = true;
                        // Update file contents for code analysis
                        updateFileContents(filePath);
                    }
                    break;
                    
                case CODE_CONTAINS:
                    if (eventType == FileSystemWatcher.EventType.MODIFIED && 
                        filePath.contains(action.getTarget())) {
                        // Check if code contains expected pattern
                        String content = readFileContent(filePath);
                        if (content != null && matchesPattern(content, action.getPattern())) {
                            actionMatched = true;
                        }
                    }
                    break;
            }
            
            if (actionMatched) {
                markActionCompleted(action);
            }
        }
    }
    
    /**
     * Handle terminal output
     */
    private void handleTerminalOutput(String command, String output) {
        logger.debug("Terminal output: {} -> {}", command, output);
        
        for (LearningChunk.ExpectedAction action : expectedActions) {
            if (completedActions.contains(action)) continue;
            
            boolean actionMatched = false;
            
            switch (action.getType()) {
                case TERMINAL_OUTPUT:
                    if (command.contains(action.getTarget()) && 
                        matchesPattern(output, action.getPattern())) {
                        actionMatched = true;
                    }
                    break;
                    
                case COMPILE_SUCCESS:
                    if (command.contains("javac") && output.isEmpty()) {
                        // Check if .class file was created
                        String classFile = action.getTarget().replace(".java", ".class");
                        if (new File(classFile).exists()) {
                            actionMatched = true;
                        }
                    }
                    break;
                    
                case RUN_SUCCESS:
                    if (command.contains("java") && command.contains(action.getTarget()) &&
                        matchesPattern(output, action.getPattern())) {
                        actionMatched = true;
                    }
                    break;
            }
            
            if (actionMatched) {
                markActionCompleted(action);
            }
        }
    }
    
    /**
     * Mark an action as completed
     */
    private void markActionCompleted(LearningChunk.ExpectedAction action) {
        completedActions.add(action);
        logger.info("Action completed: {} for {}", action.getType(), action.getTarget());
        
        if (onActionDetected != null) {
            onActionDetected.accept(action);
        }
    }
    
    /**
     * Check if text matches a pattern (regex or simple contains)
     */
    private boolean matchesPattern(String text, String pattern) {
        if (text == null || pattern == null) return false;
        
        try {
            return Pattern.matches(pattern, text);
        } catch (Exception e) {
            // Fallback to simple contains check
            return text.toLowerCase().contains(pattern.toLowerCase());
        }
    }
    
    /**
     * Read file content safely
     */
    private String readFileContent(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            logger.warn("Failed to read file: {}", filePath);
            return null;
        }
    }
    
    /**
     * Update cached file contents and trigger analysis
     */
    private void updateFileContents(String filePath) {
        String content = readFileContent(filePath);
        if (content != null) {
            fileContents.put(filePath, content);
            
            // Trigger code analysis for common programming languages
            if (filePath.endsWith(".java") || filePath.endsWith(".py") || 
                filePath.endsWith(".js") || filePath.endsWith(".ts")) {
                codeAnalyzer.analyzeCode(filePath, content);
            }
        }
    }
    
    // Setters for callbacks
    public void setOnActionDetected(Consumer<LearningChunk.ExpectedAction> callback) {
        this.onActionDetected = callback;
    }
    
    public void setOnErrorDetected(Consumer<String> callback) {
        this.onErrorDetected = callback;
    }
    
    // Getters
    public Set<LearningChunk.ExpectedAction> getCompletedActions() {
        return new HashSet<>(completedActions);
    }
    
    public int getCompletedActionCount() {
        return completedActions.size();
    }
    
    public int getTotalActionCount() {
        return expectedActions != null ? expectedActions.size() : 0;
    }
}
