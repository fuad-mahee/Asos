package com.asos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks learning progress across different programming languages
 */
public class LearningProgressManager {
    
    public static class LanguageProgress {
        @JsonProperty
        private String language;
        
        @JsonProperty
        private int currentChunkId;
        
        @JsonProperty
        private int totalChunks;
        
        @JsonProperty
        private boolean completed;
        
        @JsonProperty
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastAccessed;
        
        @JsonProperty
        private Map<Integer, Boolean> completedChunks;
        
        @JsonProperty
        private double progressPercentage;
        
        @JsonProperty
        private long totalTimeSpent; // in seconds
        
        public LanguageProgress() {
            this.completedChunks = new HashMap<>();
            this.lastAccessed = LocalDateTime.now();
        }
        
        public LanguageProgress(String language, int totalChunks) {
            this();
            this.language = language;
            this.totalChunks = totalChunks;
            this.currentChunkId = 1;
            this.completed = false;
            this.progressPercentage = 0.0;
            this.totalTimeSpent = 0;
        }
        
        // Getters and setters
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        
        public int getCurrentChunkId() { return currentChunkId; }
        public void setCurrentChunkId(int currentChunkId) { this.currentChunkId = currentChunkId; }
        
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public void setLastAccessed(LocalDateTime lastAccessed) { this.lastAccessed = lastAccessed; }
        
        public Map<Integer, Boolean> getCompletedChunks() { return completedChunks; }
        public void setCompletedChunks(Map<Integer, Boolean> completedChunks) { this.completedChunks = completedChunks; }
        
        public double getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(double progressPercentage) { this.progressPercentage = progressPercentage; }
        
        public long getTotalTimeSpent() { return totalTimeSpent; }
        public void setTotalTimeSpent(long totalTimeSpent) { this.totalTimeSpent = totalTimeSpent; }
        
        public void markChunkCompleted(int chunkId) {
            completedChunks.put(chunkId, true);
            updateProgress();
            lastAccessed = LocalDateTime.now();
        }
        
        public void moveToNextChunk() {
            if (currentChunkId < totalChunks) {
                currentChunkId++;
            } else {
                completed = true;
            }
            updateProgress();
            lastAccessed = LocalDateTime.now();
        }
        
        private void updateProgress() {
            int completedCount = (int) completedChunks.values().stream().mapToInt(b -> b ? 1 : 0).sum();
            progressPercentage = totalChunks > 0 ? (double) completedCount / totalChunks * 100.0 : 0.0;
        }
        
        public void addTimeSpent(long seconds) {
            totalTimeSpent += seconds;
        }
    }
    
    @JsonProperty
    private Map<String, LanguageProgress> languageProgress;
    
    @JsonProperty
    private String currentLanguage;
    
    @JsonProperty
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOverallAccess;
    
    public LearningProgressManager() {
        this.languageProgress = new HashMap<>();
        this.lastOverallAccess = LocalDateTime.now();
    }
    
    public void initializeLanguage(String language, int totalChunks) {
        if (!languageProgress.containsKey(language)) {
            languageProgress.put(language, new LanguageProgress(language, totalChunks));
        }
    }
    
    public LanguageProgress getLanguageProgress(String language) {
        return languageProgress.get(language);
    }
    
    public void setCurrentLanguage(String language) {
        this.currentLanguage = language;
        this.lastOverallAccess = LocalDateTime.now();
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    public int getCurrentChunkForLanguage(String language) {
        LanguageProgress progress = languageProgress.get(language);
        return progress != null ? progress.getCurrentChunkId() : 1;
    }
    
    public void completeChunk(String language, int chunkId) {
        LanguageProgress progress = languageProgress.get(language);
        if (progress != null) {
            progress.markChunkCompleted(chunkId);
            progress.moveToNextChunk();
        }
    }
    
    public boolean isLanguageCompleted(String language) {
        LanguageProgress progress = languageProgress.get(language);
        return progress != null && progress.isCompleted();
    }
    
    public double getLanguageProgressPercentage(String language) {
        LanguageProgress progress = languageProgress.get(language);
        return progress != null ? progress.getProgressPercentage() : 0.0;
    }
    
    public Map<String, LanguageProgress> getAllProgress() {
        return languageProgress;
    }
    
    public LocalDateTime getLastOverallAccess() {
        return lastOverallAccess;
    }
    
    public void setLastOverallAccess(LocalDateTime lastOverallAccess) {
        this.lastOverallAccess = lastOverallAccess;
    }
    
    /**
     * Get a summary of all language progress for display
     */
    public String getProgressSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Learning Progress Summary:\n");
        summary.append("========================\n");
        
        for (Map.Entry<String, LanguageProgress> entry : languageProgress.entrySet()) {
            LanguageProgress progress = entry.getValue();
            summary.append(String.format("%s: %d/%d chunks (%.1f%%) - %s\n",
                progress.getLanguage().toUpperCase(),
                progress.getCompletedChunks().size(),
                progress.getTotalChunks(),
                progress.getProgressPercentage(),
                progress.isCompleted() ? "COMPLETED!" : "In Progress"
            ));
        }
        
        return summary.toString();
    }
}
