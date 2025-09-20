package com.asos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Finite-State Machine Teaching Engine
 * Manages chunk-by-chunk learning with real-time monitoring and adaptive pacing
 */
public class ChunkTeachingEngine {
    private static final Logger logger = LoggerFactory.getLogger(ChunkTeachingEngine.class);
    
    private List<LearningChunk> learningChunks;
    private int currentChunkIndex = 0;
    private LearningChunk currentChunk;
    
    private final RealTimeMonitor monitor;
    private final ErrorDetectionEngine errorDetector;
    private final ScheduledExecutorService scheduler;
    private final ObjectMapper objectMapper;
    
    // Adaptive pacing variables
    private long chunkStartTime;
    private int consecutiveFastCompletions = 0;
    private int consecutiveSlowCompletions = 0;
    private boolean hintGiven = false;
    
    // Progress tracking
    private LearningProgressManager progressManager;
    private LearningProgressStorage progressStorage;
    private String currentLanguage;
    
    // Callbacks for UI updates
    private BiConsumer<String, String> onInstructionUpdate;
    private Consumer<String> onHintProvided;
    private Consumer<String> onErrorDetected;
    private Consumer<String> onChunkCompleted;
    private Consumer<Double> onProgressUpdate;
    
    public ChunkTeachingEngine() {
        this.monitor = new RealTimeMonitor();
        this.errorDetector = new ErrorDetectionEngine();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.objectMapper = new ObjectMapper();
        
        // Initialize progress tracking
        this.progressStorage = new LearningProgressStorage();
        this.progressManager = progressStorage.loadProgress();
        
        setupMonitorCallbacks();
    }
    
    /**
     * Load learning module from JSON file
     */
    public void loadLearningModule(String moduleFile) throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/learning-modules/" + moduleFile);
        if (inputStream == null) {
            throw new IllegalArgumentException("Learning module not found: " + moduleFile);
        }
        
        TypeReference<List<LearningChunk>> typeRef = new TypeReference<List<LearningChunk>>() {};
        learningChunks = objectMapper.readValue(inputStream, typeRef);
        
        // Extract language from module file name
        this.currentLanguage = extractLanguageFromModuleFile(moduleFile);
        
        // Initialize progress for this language
        progressManager.initializeLanguage(currentLanguage, learningChunks.size());
        progressManager.setCurrentLanguage(currentLanguage);
        
        logger.info("Loaded learning module with {} chunks for language: {}", learningChunks.size(), currentLanguage);
    }
    
    /**
     * Extract language name from module file name
     */
    private String extractLanguageFromModuleFile(String moduleFile) {
        String baseName = moduleFile.replaceFirst("[.][^.]+$", ""); // Remove extension
        if (baseName.contains("-")) {
            return baseName.split("-")[0]; // Take first part before hyphen
        }
        return baseName;
    }
    
    /**
     * Start the teaching session
     */
    public void startTeaching() {
        if (learningChunks == null || learningChunks.isEmpty()) {
            throw new IllegalStateException("No learning module loaded");
        }
        
        // Resume from saved progress
        int savedChunkId = progressManager.getCurrentChunkForLanguage(currentLanguage);
        currentChunkIndex = Math.min(savedChunkId - 1, learningChunks.size() - 1); // Convert to 0-based index
        
        if (progressManager.isLanguageCompleted(currentLanguage)) {
            logger.info("Language {} already completed!", currentLanguage);
            if (onChunkCompleted != null) {
                onChunkCompleted.accept("Congratulations! You've completed " + currentLanguage.toUpperCase() + " course!");
            }
            return;
        }
        
        logger.info("Resuming {} learning from chunk {} ({}%)", 
                   currentLanguage, savedChunkId, 
                   String.format("%.1f", progressManager.getLanguageProgressPercentage(currentLanguage)));
        
        startChunk();
    }
    
    /**
     * Start a specific chunk
     */
    private void startChunk() {
        if (currentChunkIndex >= learningChunks.size()) {
            completeTeachingSession();
            return;
        }
        
        currentChunk = learningChunks.get(currentChunkIndex);
        chunkStartTime = System.currentTimeMillis();
        hintGiven = false;
        
        // Notify UI
        if (onInstructionUpdate != null) {
            String detailedInstruction = currentChunk.getDetailedInstruction();
            if (detailedInstruction == null || detailedInstruction.trim().isEmpty()) {
                detailedInstruction = currentChunk.getInstruction(); // Fallback to regular instruction
            }
            onInstructionUpdate.accept(currentChunk.getInstruction(), detailedInstruction);
        }
        
        if (onProgressUpdate != null) {
            double progress = (double) currentChunkIndex / learningChunks.size();
            onProgressUpdate.accept(progress);
        }
        
        // Start monitoring for this chunk
        monitor.startMonitoring(currentChunk.getExpectedActions());
        
        // Schedule hint if user takes too long
        scheduleHintIfNeeded();
        
        logger.info("Started chunk {}: {}", currentChunk.getChunkId(), currentChunk.getInstruction());
    }
    
    /**
     * Setup callbacks for the monitor
     */
    private void setupMonitorCallbacks() {
        monitor.setOnActionDetected(this::handleActionDetected);
        monitor.setOnErrorDetected(this::handleErrorDetected);
        
        errorDetector.setOnErrorFound((error, suggestion) -> {
            if (onErrorDetected != null) {
                onErrorDetected.accept("Error: " + error + ". Suggestion: " + suggestion);
            }
        });
    }
    
    /**
     * Handle when an expected action is detected
     */
    private void handleActionDetected(LearningChunk.ExpectedAction action) {
        logger.info("Action detected: {} for {}", action.getType(), action.getTarget());
        
        // Check if all expected actions for current chunk are completed
        if (monitor.areAllActionsCompleted()) {
            completeCurrentChunk();
        }
    }
    
    /**
     * Handle error detection
     */
    private void handleErrorDetected(String error) {
        logger.warn("Error detected: {}", error);
        
        String correction = currentChunk.getErrorCorrection();
        if (correction == null) {
            correction = "Please check your work and try again.";
        }
        
        if (onErrorDetected != null) {
            onErrorDetected.accept(correction);
        }
    }
    
    /**
     * Complete current chunk and move to next
     */
    private void completeCurrentChunk() {
        long completionTime = System.currentTimeMillis() - chunkStartTime;
        analyzeCompletionTime(completionTime);
        
        // Save progress for current chunk
        if (progressManager != null && currentLanguage != null) {
            progressManager.completeChunk(currentLanguage, currentChunkIndex);
            progressStorage.saveProgress(progressManager);
            logger.info("Progress saved: {} chunk {} completed", currentLanguage, currentChunkIndex);
        }
        
        if (onChunkCompleted != null) {
            onChunkCompleted.accept("Great job! Moving to next step...");
        }
        
        // Adaptive pacing: merge chunks if user is fast
        if (shouldMergeNextChunk()) {
            mergeNextChunk();
        }
        
        currentChunkIndex++;
        
        // Small delay before next chunk
        scheduler.schedule(() -> Platform.runLater(this::startChunk), 2, TimeUnit.SECONDS);
    }    /**
     * Analyze completion time for adaptive pacing
     */
    private void analyzeCompletionTime(long completionTimeMs) {
        int expectedTimeMs = currentChunk.getTimeoutSeconds() * 1000;
        double ratio = (double) completionTimeMs / expectedTimeMs;
        
        if (ratio < 0.3) { // Completed in less than 30% of expected time
            consecutiveFastCompletions++;
            consecutiveSlowCompletions = 0;
        } else if (ratio > 0.8) { // Took more than 80% of expected time
            consecutiveSlowCompletions++;
            consecutiveFastCompletions = 0;
        } else {
            consecutiveFastCompletions = 0;
            consecutiveSlowCompletions = 0;
        }
        
        logger.debug("Completion time analysis: {}ms (ratio: {:.2f})", completionTimeMs, ratio);
    }
    
    /**
     * Check if next chunk should be merged (for fast learners)
     */
    private boolean shouldMergeNextChunk() {
        return consecutiveFastCompletions >= 2 && 
               currentChunkIndex < learningChunks.size() - 1 &&
               learningChunks.get(currentChunkIndex + 1).getDifficulty() == LearningChunk.ChunkDifficulty.BEGINNER;
    }
    
    /**
     * Merge next chunk with current instruction
     */
    private void mergeNextChunk() {
        if (currentChunkIndex < learningChunks.size() - 1) {
            LearningChunk nextChunk = learningChunks.get(currentChunkIndex + 1);
            String mergedInstruction = currentChunk.getInstruction() + "\n\nBonus: " + nextChunk.getInstruction();
            
            // Create merged chunk
            LearningChunk mergedChunk = new LearningChunk();
            mergedChunk.setChunkId(currentChunk.getChunkId());
            mergedChunk.setInstruction(mergedInstruction);
            
            // Combine expected actions
            List<LearningChunk.ExpectedAction> combinedActions = new ArrayList<>(currentChunk.getExpectedActions());
            combinedActions.addAll(nextChunk.getExpectedActions());
            mergedChunk.setExpectedActions(combinedActions);
            
            learningChunks.set(currentChunkIndex, mergedChunk);
            learningChunks.remove(currentChunkIndex + 1);
            
            logger.info("Merged chunks for fast learner");
        }
    }
    
    /**
     * Schedule hint if user takes too long
     */
    private void scheduleHintIfNeeded() {
        int hintDelaySeconds = Math.max(currentChunk.getTimeoutSeconds() / 3, 30); // 1/3 of timeout or 30 seconds
        
        scheduler.schedule(() -> {
            if (!monitor.areAllActionsCompleted() && !hintGiven) {
                hintGiven = true;
                Platform.runLater(() -> {
                    if (onHintProvided != null) {
                        String hint = currentChunk.getHint();
                        if (hint == null) hint = "Take your time and follow the instructions step by step.";
                        onHintProvided.accept(hint);
                    }
                });
            }
        }, hintDelaySeconds, TimeUnit.SECONDS);
    }
    
    /**
     * Complete the entire teaching session
     */
    private void completeTeachingSession() {
        monitor.stopMonitoring();
        
        if (onProgressUpdate != null) {
            onProgressUpdate.accept(1.0);
        }
        
        if (onChunkCompleted != null) {
            onChunkCompleted.accept("Congratulations! You've completed the entire learning module!");
        }
        
        logger.info("Teaching session completed successfully");
    }
    
    /**
     * Stop the teaching session
     */
    public void stopTeaching() {
        monitor.stopMonitoring();
        scheduler.shutdown();
    }
    
    // Callback setters
    public void setOnInstructionUpdate(BiConsumer<String, String> callback) { this.onInstructionUpdate = callback; }
    public void setOnHintProvided(Consumer<String> callback) { this.onHintProvided = callback; }
    public void setOnErrorDetected(Consumer<String> callback) { this.onErrorDetected = callback; }
    public void setOnChunkCompleted(Consumer<String> callback) { this.onChunkCompleted = callback; }
    public void setOnProgressUpdate(Consumer<Double> callback) { this.onProgressUpdate = callback; }
    
    // Getters
    public int getCurrentChunkIndex() { return currentChunkIndex; }
    public int getTotalChunks() { return learningChunks != null ? learningChunks.size() : 0; }
    public LearningChunk getCurrentChunk() { return currentChunk; }
}
