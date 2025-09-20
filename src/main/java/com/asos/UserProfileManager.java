package com.asos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced user profiling system that tracks learning patterns,
 * preferences, and performance metrics for personalized experiences
 */
public class UserProfileManager {
    
    private static final String PROFILE_FILE = "user_profile.json";
    private UserProfile currentProfile;
    private ObjectMapper objectMapper;
    private Map<String, Double> skillLevels;
    private LearningStyleDetector styleDetector;
    
    public UserProfileManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.skillLevels = new ConcurrentHashMap<>();
        this.styleDetector = new LearningStyleDetector();
        loadOrCreateProfile();
    }
    
    private void loadOrCreateProfile() {
        File profileFile = new File(PROFILE_FILE);
        if (profileFile.exists()) {
            try {
                currentProfile = objectMapper.readValue(profileFile, UserProfile.class);
                System.out.println("Loaded existing user profile");
            } catch (IOException e) {
                System.err.println("Error loading profile, creating new: " + e.getMessage());
                createNewProfile();
            }
        } else {
            createNewProfile();
        }
    }
    
    private void createNewProfile() {
        currentProfile = new UserProfile();
        currentProfile.setUserId("user_" + System.currentTimeMillis());
        currentProfile.setCreatedDate(LocalDateTime.now());
        currentProfile.setLearningStyle(LearningStyle.BALANCED);
        currentProfile.setSkillLevel(SkillLevel.BEGINNER);
        currentProfile.setPreferences(new UserPreferences());
        currentProfile.setPerformanceHistory(new ArrayList<>());
        currentProfile.setSessionStats(new SessionStatistics());
        saveProfile();
    }
    
    public void saveProfile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(PROFILE_FILE), currentProfile);
        } catch (IOException e) {
            System.err.println("Error saving profile: " + e.getMessage());
        }
    }
    
    /**
     * Records a learning session and updates user analytics
     */
    public void recordSession(String pathwayId, int stepsCompleted, int totalSteps, 
                             long sessionDuration, int errorCount) {
        
        SessionRecord session = new SessionRecord();
        session.setPathwayId(pathwayId);
        session.setStepsCompleted(stepsCompleted);
        session.setTotalSteps(totalSteps);
        session.setSessionDuration(sessionDuration);
        session.setErrorCount(errorCount);
        session.setTimestamp(LocalDateTime.now());
        session.setCompletionRate((double) stepsCompleted / totalSteps);
        
        currentProfile.getPerformanceHistory().add(session);
        updateSkillLevel(pathwayId, session.getCompletionRate(), errorCount);
        updateLearningStyle(session);
        updateSessionStatistics(session);
        
        saveProfile();
    }
    
    /**
     * Updates skill level based on performance patterns
     */
    private void updateSkillLevel(String pathwayId, double completionRate, int errorCount) {
        double currentSkill = skillLevels.getOrDefault(pathwayId, 0.5);
        
        // Calculate skill adjustment based on performance
        double adjustment = 0.0;
        if (completionRate > 0.9 && errorCount <= 1) {
            adjustment = 0.1; // Excellent performance
        } else if (completionRate > 0.7 && errorCount <= 3) {
            adjustment = 0.05; // Good performance
        } else if (completionRate < 0.5 || errorCount > 5) {
            adjustment = -0.05; // Needs improvement
        }
        
        currentSkill = Math.max(0.0, Math.min(1.0, currentSkill + adjustment));
        skillLevels.put(pathwayId, currentSkill);
        
        // Update overall skill level
        updateOverallSkillLevel();
    }
    
    private void updateOverallSkillLevel() {
        if (skillLevels.isEmpty()) return;
        
        double averageSkill = skillLevels.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.5);
        
        if (averageSkill < 0.3) {
            currentProfile.setSkillLevel(SkillLevel.BEGINNER);
        } else if (averageSkill < 0.6) {
            currentProfile.setSkillLevel(SkillLevel.INTERMEDIATE);
        } else if (averageSkill < 0.8) {
            currentProfile.setSkillLevel(SkillLevel.ADVANCED);
        } else {
            currentProfile.setSkillLevel(SkillLevel.EXPERT);
        }
    }
    
    /**
     * Detects and updates learning style based on behavior patterns
     */
    private void updateLearningStyle(SessionRecord session) {
        styleDetector.analyzeSession(session);
        LearningStyle detectedStyle = styleDetector.getDetectedStyle();
        if (detectedStyle != currentProfile.getLearningStyle()) {
            currentProfile.setLearningStyle(detectedStyle);
            System.out.println("Learning style updated to: " + detectedStyle);
        }
    }
    
    private void updateSessionStatistics(SessionRecord session) {
        SessionStatistics stats = currentProfile.getSessionStats();
        stats.setTotalSessions(stats.getTotalSessions() + 1);
        stats.setTotalLearningTime(stats.getTotalLearningTime() + session.getSessionDuration());
        stats.setTotalStepsCompleted(stats.getTotalStepsCompleted() + session.getStepsCompleted());
        stats.setTotalErrors(stats.getTotalErrors() + session.getErrorCount());
        
        // Calculate averages
        stats.setAverageSessionTime(stats.getTotalLearningTime() / stats.getTotalSessions());
        stats.setAverageCompletionRate(
            currentProfile.getPerformanceHistory().stream()
                .mapToDouble(SessionRecord::getCompletionRate)
                .average()
                .orElse(0.0)
        );
    }
    
    /**
     * Gets personalized difficulty level for a pathway
     */
    public DifficultyLevel getPersonalizedDifficulty(String pathwayId) {
        double skillLevel = skillLevels.getOrDefault(pathwayId, 0.5);
        
        if (skillLevel < 0.3) {
            return DifficultyLevel.EASY;
        } else if (skillLevel < 0.7) {
            return DifficultyLevel.MEDIUM;
        } else {
            return DifficultyLevel.HARD;
        }
    }
    
    /**
     * Gets recommended session length based on user patterns
     */
    public long getRecommendedSessionLength() {
        SessionStatistics stats = currentProfile.getSessionStats();
        if (stats.getTotalSessions() < 3) {
            return 15 * 60 * 1000; // 15 minutes for new users
        }
        
        long avgSession = stats.getAverageSessionTime();
        // Recommend 10% longer than average, capped at 45 minutes
        return Math.min(avgSession + (avgSession / 10), 45 * 60 * 1000);
    }
    
    /**
     * Checks if user needs encouragement based on recent performance
     */
    public boolean needsEncouragement() {
        List<SessionRecord> recent = getRecentSessions(5);
        if (recent.size() < 3) return false;
        
        double avgCompletion = recent.stream()
            .mapToDouble(SessionRecord::getCompletionRate)
            .average()
            .orElse(1.0);
        
        return avgCompletion < 0.6; // Below 60% completion rate
    }
    
    private List<SessionRecord> getRecentSessions(int count) {
        List<SessionRecord> history = currentProfile.getPerformanceHistory();
        int size = history.size();
        if (size <= count) return history;
        return history.subList(size - count, size);
    }
    
    // Getters
    public UserProfile getCurrentProfile() { return currentProfile; }
    public LearningStyle getLearningStyle() { return currentProfile.getLearningStyle(); }
    public SkillLevel getSkillLevel() { return currentProfile.getSkillLevel(); }
    public Map<String, Double> getSkillLevels() { return new HashMap<>(skillLevels); }
    
    // Data Classes
    public static class UserProfile {
        @JsonProperty("userId")
        private String userId;
        
        @JsonProperty("createdDate")
        private LocalDateTime createdDate;
        
        @JsonProperty("learningStyle")
        private LearningStyle learningStyle;
        
        @JsonProperty("skillLevel")
        private SkillLevel skillLevel;
        
        @JsonProperty("preferences")
        private UserPreferences preferences;
        
        @JsonProperty("performanceHistory")
        private List<SessionRecord> performanceHistory;
        
        @JsonProperty("sessionStats")
        private SessionStatistics sessionStats;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
        
        public LearningStyle getLearningStyle() { return learningStyle; }
        public void setLearningStyle(LearningStyle learningStyle) { this.learningStyle = learningStyle; }
        
        public SkillLevel getSkillLevel() { return skillLevel; }
        public void setSkillLevel(SkillLevel skillLevel) { this.skillLevel = skillLevel; }
        
        public UserPreferences getPreferences() { return preferences; }
        public void setPreferences(UserPreferences preferences) { this.preferences = preferences; }
        
        public List<SessionRecord> getPerformanceHistory() { return performanceHistory; }
        public void setPerformanceHistory(List<SessionRecord> performanceHistory) { 
            this.performanceHistory = performanceHistory; 
        }
        
        public SessionStatistics getSessionStats() { return sessionStats; }
        public void setSessionStats(SessionStatistics sessionStats) { this.sessionStats = sessionStats; }
    }
    
    public static class SessionRecord {
        @JsonProperty("pathwayId")
        private String pathwayId;
        
        @JsonProperty("timestamp")
        private LocalDateTime timestamp;
        
        @JsonProperty("stepsCompleted")
        private int stepsCompleted;
        
        @JsonProperty("totalSteps")
        private int totalSteps;
        
        @JsonProperty("sessionDuration")
        private long sessionDuration;
        
        @JsonProperty("errorCount")
        private int errorCount;
        
        @JsonProperty("completionRate")
        private double completionRate;
        
        // Getters and setters
        public String getPathwayId() { return pathwayId; }
        public void setPathwayId(String pathwayId) { this.pathwayId = pathwayId; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public int getStepsCompleted() { return stepsCompleted; }
        public void setStepsCompleted(int stepsCompleted) { this.stepsCompleted = stepsCompleted; }
        
        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        
        public long getSessionDuration() { return sessionDuration; }
        public void setSessionDuration(long sessionDuration) { this.sessionDuration = sessionDuration; }
        
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
    }
    
    public static class UserPreferences {
        @JsonProperty("animationSpeed")
        private double animationSpeed = 1.0;
        
        @JsonProperty("hintFrequency")
        private HintFrequency hintFrequency = HintFrequency.NORMAL;
        
        @JsonProperty("feedbackVerbosity")
        private FeedbackVerbosity feedbackVerbosity = FeedbackVerbosity.NORMAL;
        
        @JsonProperty("preferredPacing")
        private PacingPreference preferredPacing = PacingPreference.ADAPTIVE;
        
        // Getters and setters
        public double getAnimationSpeed() { return animationSpeed; }
        public void setAnimationSpeed(double animationSpeed) { this.animationSpeed = animationSpeed; }
        
        public HintFrequency getHintFrequency() { return hintFrequency; }
        public void setHintFrequency(HintFrequency hintFrequency) { this.hintFrequency = hintFrequency; }
        
        public FeedbackVerbosity getFeedbackVerbosity() { return feedbackVerbosity; }
        public void setFeedbackVerbosity(FeedbackVerbosity feedbackVerbosity) { 
            this.feedbackVerbosity = feedbackVerbosity; 
        }
        
        public PacingPreference getPreferredPacing() { return preferredPacing; }
        public void setPreferredPacing(PacingPreference preferredPacing) { 
            this.preferredPacing = preferredPacing; 
        }
    }
    
    public static class SessionStatistics {
        @JsonProperty("totalSessions")
        private int totalSessions = 0;
        
        @JsonProperty("totalLearningTime")
        private long totalLearningTime = 0;
        
        @JsonProperty("totalStepsCompleted")
        private int totalStepsCompleted = 0;
        
        @JsonProperty("totalErrors")
        private int totalErrors = 0;
        
        @JsonProperty("averageSessionTime")
        private long averageSessionTime = 0;
        
        @JsonProperty("averageCompletionRate")
        private double averageCompletionRate = 0.0;
        
        // Getters and setters
        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
        
        public long getTotalLearningTime() { return totalLearningTime; }
        public void setTotalLearningTime(long totalLearningTime) { this.totalLearningTime = totalLearningTime; }
        
        public int getTotalStepsCompleted() { return totalStepsCompleted; }
        public void setTotalStepsCompleted(int totalStepsCompleted) { 
            this.totalStepsCompleted = totalStepsCompleted; 
        }
        
        public int getTotalErrors() { return totalErrors; }
        public void setTotalErrors(int totalErrors) { this.totalErrors = totalErrors; }
        
        public long getAverageSessionTime() { return averageSessionTime; }
        public void setAverageSessionTime(long averageSessionTime) { 
            this.averageSessionTime = averageSessionTime; 
        }
        
        public double getAverageCompletionRate() { return averageCompletionRate; }
        public void setAverageCompletionRate(double averageCompletionRate) { 
            this.averageCompletionRate = averageCompletionRate; 
        }
    }
    
    // Enums
    public enum LearningStyle {
        VISUAL, AUDITORY, KINESTHETIC, BALANCED
    }
    
    public enum SkillLevel {
        BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    }
    
    public enum DifficultyLevel {
        EASY, MEDIUM, HARD
    }
    
    public enum HintFrequency {
        MINIMAL, NORMAL, FREQUENT
    }
    
    public enum FeedbackVerbosity {
        MINIMAL, NORMAL, VERBOSE
    }
    
    public enum PacingPreference {
        SLOW, NORMAL, FAST, ADAPTIVE
    }
}
