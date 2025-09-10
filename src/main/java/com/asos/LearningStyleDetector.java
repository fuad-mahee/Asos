package com.asos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intelligent learning style detection based on user behavior patterns,
 * interaction preferences, and performance metrics
 */
public class LearningStyleDetector {
    
    private Map<UserProfileManager.LearningStyle, Double> styleScores;
    private List<BehaviorPattern> sessionPatterns;
    private static final int MIN_SESSIONS_FOR_DETECTION = 3;
    
    public LearningStyleDetector() {
        this.styleScores = new ConcurrentHashMap<>();
        this.sessionPatterns = new ArrayList<>();
        initializeScores();
    }
    
    private void initializeScores() {
        // Start with balanced scoring
        for (UserProfileManager.LearningStyle style : UserProfileManager.LearningStyle.values()) {
            styleScores.put(style, 0.25); // Equal distribution
        }
    }
    
    /**
     * Analyzes a learning session to detect behavioral patterns
     */
    public void analyzeSession(UserProfileManager.SessionRecord session) {
        BehaviorPattern pattern = extractBehaviorPattern(session);
        sessionPatterns.add(pattern);
        
        // Keep only recent patterns for analysis
        if (sessionPatterns.size() > 20) {
            sessionPatterns.remove(0);
        }
        
        updateStyleScores(pattern);
    }
    
    private BehaviorPattern extractBehaviorPattern(UserProfileManager.SessionRecord session) {
        BehaviorPattern pattern = new BehaviorPattern();
        
        // Analyze session characteristics
        pattern.setSessionDuration(session.getSessionDuration());
        pattern.setCompletionRate(session.getCompletionRate());
        pattern.setErrorCount(session.getErrorCount());
        pattern.setStepsCompleted(session.getStepsCompleted());
        
        // Calculate derived metrics
        long avgTimePerStep = session.getSessionDuration() / Math.max(1, session.getStepsCompleted());
        pattern.setAverageTimePerStep(avgTimePerStep);
        
        double errorRate = (double) session.getErrorCount() / Math.max(1, session.getStepsCompleted());
        pattern.setErrorRate(errorRate);
        
        // Analyze behavior tendencies
        analyzePacingBehavior(pattern);
        analyzeErrorPatterns(pattern);
        analyzeEngagementLevel(pattern);
        
        return pattern;
    }
    
    private void analyzePacingBehavior(BehaviorPattern pattern) {
        long timePerStep = pattern.getAverageTimePerStep();
        
        if (timePerStep < 30000) { // Less than 30 seconds per step
            pattern.setPacingStyle(PacingStyle.FAST);
        } else if (timePerStep > 120000) { // More than 2 minutes per step
            pattern.setPacingStyle(PacingStyle.SLOW);
        } else {
            pattern.setPacingStyle(PacingStyle.MODERATE);
        }
    }
    
    private void analyzeErrorPatterns(BehaviorPattern pattern) {
        double errorRate = pattern.getErrorRate();
        
        if (errorRate < 0.1) { // Less than 10% error rate
            pattern.setErrorBehavior(ErrorBehavior.CAREFUL);
        } else if (errorRate > 0.3) { // More than 30% error rate
            pattern.setErrorBehavior(ErrorBehavior.EXPERIMENTAL);
        } else {
            pattern.setErrorBehavior(ErrorBehavior.BALANCED);
        }
    }
    
    private void analyzeEngagementLevel(BehaviorPattern pattern) {
        double completionRate = pattern.getCompletionRate();
        long sessionDuration = pattern.getSessionDuration();
        
        // High engagement: Good completion rate and sustained session time
        if (completionRate > 0.8 && sessionDuration > 600000) { // 10+ minutes
            pattern.setEngagementLevel(EngagementLevel.HIGH);
        } else if (completionRate < 0.5 || sessionDuration < 300000) { // <5 minutes
            pattern.setEngagementLevel(EngagementLevel.LOW);
        } else {
            pattern.setEngagementLevel(EngagementLevel.MODERATE);
        }
    }
    
    /**
     * Updates learning style scores based on observed patterns
     */
    private void updateStyleScores(BehaviorPattern pattern) {
        // Visual learners: Prefer structured, step-by-step approaches
        updateVisualScore(pattern);
        
        // Auditory learners: May show different pacing and engagement patterns
        updateAuditoryScore(pattern);
        
        // Kinesthetic learners: Learn by doing, may have more errors initially
        updateKinestheticScore(pattern);
        
        // Balanced learners: Show mixed characteristics
        updateBalancedScore(pattern);
        
        normalizeScores();
    }
    
    private void updateVisualScore(BehaviorPattern pattern) {
        double score = styleScores.get(UserProfileManager.LearningStyle.VISUAL);
        
        // Visual learners tend to be more careful and methodical
        if (pattern.getErrorBehavior() == ErrorBehavior.CAREFUL) {
            score += 0.1;
        }
        
        if (pattern.getPacingStyle() == PacingStyle.MODERATE) {
            score += 0.05;
        }
        
        if (pattern.getCompletionRate() > 0.8) {
            score += 0.05;
        }
        
        styleScores.put(UserProfileManager.LearningStyle.VISUAL, Math.min(1.0, score));
    }
    
    private void updateAuditoryScore(BehaviorPattern pattern) {
        double score = styleScores.get(UserProfileManager.LearningStyle.AUDITORY);
        
        // Auditory learners may prefer consistent, steady pacing
        if (pattern.getPacingStyle() == PacingStyle.MODERATE) {
            score += 0.05;
        }
        
        if (pattern.getEngagementLevel() == EngagementLevel.HIGH) {
            score += 0.1;
        }
        
        styleScores.put(UserProfileManager.LearningStyle.AUDITORY, Math.min(1.0, score));
    }
    
    private void updateKinestheticScore(BehaviorPattern pattern) {
        double score = styleScores.get(UserProfileManager.LearningStyle.KINESTHETIC);
        
        // Kinesthetic learners learn by doing - may have more initial errors
        if (pattern.getErrorBehavior() == ErrorBehavior.EXPERIMENTAL) {
            score += 0.1;
        }
        
        if (pattern.getPacingStyle() == PacingStyle.FAST) {
            score += 0.05;
        }
        
        // High engagement despite errors
        if (pattern.getEngagementLevel() == EngagementLevel.HIGH && pattern.getErrorRate() > 0.2) {
            score += 0.1;
        }
        
        styleScores.put(UserProfileManager.LearningStyle.KINESTHETIC, Math.min(1.0, score));
    }
    
    private void updateBalancedScore(BehaviorPattern pattern) {
        double score = styleScores.get(UserProfileManager.LearningStyle.BALANCED);
        
        // Balanced learners show moderate characteristics across metrics
        if (pattern.getPacingStyle() == PacingStyle.MODERATE &&
            pattern.getErrorBehavior() == ErrorBehavior.BALANCED &&
            pattern.getEngagementLevel() == EngagementLevel.MODERATE) {
            score += 0.1;
        }
        
        styleScores.put(UserProfileManager.LearningStyle.BALANCED, Math.min(1.0, score));
    }
    
    private void normalizeScores() {
        double total = styleScores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) {
            styleScores.replaceAll((style, score) -> score / total);
        }
    }
    
    /**
     * Gets the currently detected learning style based on accumulated patterns
     */
    public UserProfileManager.LearningStyle getDetectedStyle() {
        if (sessionPatterns.size() < MIN_SESSIONS_FOR_DETECTION) {
            return UserProfileManager.LearningStyle.BALANCED;
        }
        
        return styleScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(UserProfileManager.LearningStyle.BALANCED);
    }
    
    /**
     * Gets confidence level in the detected style (0.0 to 1.0)
     */
    public double getDetectionConfidence() {
        if (sessionPatterns.size() < MIN_SESSIONS_FOR_DETECTION) {
            return 0.0;
        }
        
        double maxScore = styleScores.values().stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.25);
        
        // Confidence is based on how much the top score exceeds the average
        return Math.min(1.0, (maxScore - 0.25) / 0.75);
    }
    
    /**
     * Gets detailed style scores for analytics
     */
    public Map<UserProfileManager.LearningStyle, Double> getStyleScores() {
        return new HashMap<>(styleScores);
    }
    
    // Data Classes
    private static class BehaviorPattern {
        private long sessionDuration;
        private double completionRate;
        private int errorCount;
        private int stepsCompleted;
        private long averageTimePerStep;
        private double errorRate;
        private PacingStyle pacingStyle;
        private ErrorBehavior errorBehavior;
        private EngagementLevel engagementLevel;
        
        // Getters and setters
        public long getSessionDuration() { return sessionDuration; }
        public void setSessionDuration(long sessionDuration) { this.sessionDuration = sessionDuration; }
        
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
        
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        
        public int getStepsCompleted() { return stepsCompleted; }
        public void setStepsCompleted(int stepsCompleted) { this.stepsCompleted = stepsCompleted; }
        
        public long getAverageTimePerStep() { return averageTimePerStep; }
        public void setAverageTimePerStep(long averageTimePerStep) { 
            this.averageTimePerStep = averageTimePerStep; 
        }
        
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
        
        public PacingStyle getPacingStyle() { return pacingStyle; }
        public void setPacingStyle(PacingStyle pacingStyle) { this.pacingStyle = pacingStyle; }
        
        public ErrorBehavior getErrorBehavior() { return errorBehavior; }
        public void setErrorBehavior(ErrorBehavior errorBehavior) { this.errorBehavior = errorBehavior; }
        
        public EngagementLevel getEngagementLevel() { return engagementLevel; }
        public void setEngagementLevel(EngagementLevel engagementLevel) { 
            this.engagementLevel = engagementLevel; 
        }
    }
    
    // Enums for behavior classification
    private enum PacingStyle {
        FAST, MODERATE, SLOW
    }
    
    private enum ErrorBehavior {
        CAREFUL, BALANCED, EXPERIMENTAL
    }
    
    private enum EngagementLevel {
        LOW, MODERATE, HIGH
    }
}
