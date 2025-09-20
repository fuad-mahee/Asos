package com.asos;

import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tracks user progress through learning steps with timing and error monitoring
 */
public class StepProgressTracker {
    
    private Map<Integer, StepProgress> stepProgressMap;
    private ScheduledExecutorService scheduler;
    private ProgressListener progressListener;
    
    public interface ProgressListener {
        void onStepTimeout(int stepNumber, long elapsedSeconds);
        void onStepCompleted(int stepNumber, long elapsedSeconds, int errorCount);
        void onErrorDetected(int stepNumber, String errorMessage);
        void onProgressUpdate(int stepNumber, StepProgress progress);
    }
    
    public static class StepProgress {
        private int stepNumber;
        private Instant startTime;
        private Instant endTime;
        private int errorCount;
        private int hintCount;
        private boolean completed;
        private String lastError;
        private long timeoutSeconds;
        
        public StepProgress(int stepNumber, long timeoutSeconds) {
            this.stepNumber = stepNumber;
            this.startTime = Instant.now();
            this.timeoutSeconds = timeoutSeconds;
            this.errorCount = 0;
            this.hintCount = 0;
            this.completed = false;
        }
        
        public void addError(String errorMessage) {
            this.errorCount++;
            this.lastError = errorMessage;
        }
        
        public void addHint() {
            this.hintCount++;
        }
        
        public void complete() {
            this.completed = true;
            this.endTime = Instant.now();
        }
        
        public long getElapsedSeconds() {
            Instant endPoint = completed ? endTime : Instant.now();
            return Duration.between(startTime, endPoint).getSeconds();
        }
        
        public boolean isTimeout() {
            return getElapsedSeconds() > timeoutSeconds;
        }
        
        public double getProgressScore() {
            // Calculate a progress score based on time and errors
            long elapsed = getElapsedSeconds();
            double timeRatio = Math.min(1.0, (double) elapsed / timeoutSeconds);
            double errorPenalty = Math.min(0.5, errorCount * 0.1);
            return Math.max(0.0, 1.0 - timeRatio - errorPenalty);
        }
        
        public String getPerformanceCategory() {
            double score = getProgressScore();
            if (score > 0.8) return "excellent";
            if (score > 0.6) return "good";
            if (score > 0.4) return "average";
            return "needs_help";
        }
        
        // Getters
        public int getStepNumber() { return stepNumber; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public int getErrorCount() { return errorCount; }
        public int getHintCount() { return hintCount; }
        public boolean isCompleted() { return completed; }
        public String getLastError() { return lastError; }
        public long getTimeoutSeconds() { return timeoutSeconds; }
    }
    
    public StepProgressTracker() {
        this.stepProgressMap = new HashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }
    
    public void startStep(int stepNumber, PathwayConfig.StepConfig stepConfig) {
        long timeoutSeconds = stepConfig.getValidation() != null ? 
            stepConfig.getValidation().getTimeout() : 60;
        
        StepProgress progress = new StepProgress(stepNumber, timeoutSeconds);
        stepProgressMap.put(stepNumber, progress);
        
        // Schedule timeout check
        scheduler.schedule(() -> {
            if (!progress.isCompleted() && progress.isTimeout()) {
                if (progressListener != null) {
                    progressListener.onStepTimeout(stepNumber, progress.getElapsedSeconds());
                }
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
        
        // Schedule periodic progress updates
        scheduler.scheduleAtFixedRate(() -> {
            if (!progress.isCompleted() && progressListener != null) {
                progressListener.onProgressUpdate(stepNumber, progress);
            }
        }, 10, 10, TimeUnit.SECONDS);
        
        System.out.println("Started tracking step " + stepNumber + " with " + timeoutSeconds + "s timeout");
    }
    
    public void completeStep(int stepNumber) {
        StepProgress progress = stepProgressMap.get(stepNumber);
        if (progress != null && !progress.isCompleted()) {
            progress.complete();
            
            if (progressListener != null) {
                progressListener.onStepCompleted(stepNumber, progress.getElapsedSeconds(), progress.getErrorCount());
            }
            
            System.out.println("Step " + stepNumber + " completed in " + 
                             progress.getElapsedSeconds() + "s with " + 
                             progress.getErrorCount() + " errors");
        }
    }
    
    public void recordError(int stepNumber, String errorMessage) {
        StepProgress progress = stepProgressMap.get(stepNumber);
        if (progress != null) {
            progress.addError(errorMessage);
            
            if (progressListener != null) {
                progressListener.onErrorDetected(stepNumber, errorMessage);
            }
            
            System.out.println("Error recorded for step " + stepNumber + ": " + errorMessage);
        }
    }
    
    public void recordHint(int stepNumber) {
        StepProgress progress = stepProgressMap.get(stepNumber);
        if (progress != null) {
            progress.addHint();
            System.out.println("Hint provided for step " + stepNumber);
        }
    }
    
    public StepProgress getStepProgress(int stepNumber) {
        return stepProgressMap.get(stepNumber);
    }
    
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        int totalSteps = stepProgressMap.size();
        int completedSteps = (int) stepProgressMap.values().stream().filter(StepProgress::isCompleted).count();
        long totalTime = stepProgressMap.values().stream().mapToLong(StepProgress::getElapsedSeconds).sum();
        int totalErrors = stepProgressMap.values().stream().mapToInt(StepProgress::getErrorCount).sum();
        int totalHints = stepProgressMap.values().stream().mapToInt(StepProgress::getHintCount).sum();
        
        analytics.put("totalSteps", totalSteps);
        analytics.put("completedSteps", completedSteps);
        analytics.put("completionRate", totalSteps > 0 ? (double) completedSteps / totalSteps : 0.0);
        analytics.put("totalTimeSeconds", totalTime);
        analytics.put("averageTimePerStep", completedSteps > 0 ? (double) totalTime / completedSteps : 0.0);
        analytics.put("totalErrors", totalErrors);
        analytics.put("totalHints", totalHints);
        analytics.put("errorRate", totalSteps > 0 ? (double) totalErrors / totalSteps : 0.0);
        
        return analytics;
    }
    
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
