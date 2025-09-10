package com.asos;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages learning sessions, tracks progress, and handles persistence
 * across app restarts with intelligent session analytics
 */
public class SessionManager {
    
    private UserProfileManager profileManager;
    private LearningAnalytics analytics;
    private PersonalizationEngine personalizationEngine;
    
    private CurrentSession activeSession;
    private Map<String, SessionHistory> sessionHistories;
    private AchievementTracker achievementTracker;
    
    public SessionManager(UserProfileManager profileManager, LearningAnalytics analytics) {
        this.profileManager = profileManager;
        this.analytics = analytics;
        this.personalizationEngine = new PersonalizationEngine(profileManager, analytics);
        this.sessionHistories = new ConcurrentHashMap<>();
        this.achievementTracker = new AchievementTracker();
    }
    
    /**
     * Starts a new learning session
     */
    public void startSession(String pathwayId) {
        if (activeSession != null) {
            endSession(); // End previous session if active
        }
        
        activeSession = new CurrentSession(pathwayId);
        activeSession.setStartTime(LocalDateTime.now());
        
        // Apply personalization
        PersonalizationEngine.SessionPacing pacing = personalizationEngine.calculateOptimalPacing();
        activeSession.setPersonalizedPacing(pacing);
        
        System.out.println("Started learning session for pathway: " + pathwayId);
    }
    
    /**
     * Records step completion in the current session
     */
    public void recordStepCompletion(int stepNumber, long stepDuration, int errorCount) {
        if (activeSession == null) {
            System.err.println("No active session to record step completion");
            return;
        }
        
        StepCompletion completion = new StepCompletion();
        completion.setStepNumber(stepNumber);
        completion.setDuration(stepDuration);
        completion.setErrorCount(errorCount);
        completion.setTimestamp(LocalDateTime.now());
        completion.setSuccess(true);
        
        activeSession.addStepCompletion(completion);
        activeSession.incrementStepsCompleted();
        
        // Check for achievements
        checkStepAchievements(completion);
    }
    
    /**
     * Records step failure or timeout
     */
    public void recordStepFailure(int stepNumber, String reason, long timeSpent) {
        if (activeSession == null) return;
        
        StepCompletion failure = new StepCompletion();
        failure.setStepNumber(stepNumber);
        failure.setDuration(timeSpent);
        failure.setErrorCount(1);
        failure.setTimestamp(LocalDateTime.now());
        failure.setSuccess(false);
        failure.setFailureReason(reason);
        
        activeSession.addStepCompletion(failure);
        activeSession.incrementErrors();
    }
    
    /**
     * Ends the current session and saves progress
     */
    public SessionSummary endSession() {
        if (activeSession == null) {
            return null;
        }
        
        activeSession.setEndTime(LocalDateTime.now());
        long totalDuration = ChronoUnit.MILLIS.between(
            activeSession.getStartTime(), activeSession.getEndTime());
        
        // Calculate session metrics
        SessionSummary summary = calculateSessionSummary(activeSession, totalDuration);
        
        // Save to profile manager
        profileManager.recordSession(
            activeSession.getPathwayId(),
            activeSession.getStepsCompleted(),
            activeSession.getTotalSteps(),
            totalDuration,
            activeSession.getTotalErrors()
        );
        
        // Update session history
        updateSessionHistory(activeSession, summary);
        
        // Check for session achievements
        checkSessionAchievements(summary);
        
        // Clear active session
        CurrentSession completedSession = activeSession;
        activeSession = null;
        
        System.out.println("Session completed: " + summary.getCompletionRate() * 100 + "% completion");
        
        return summary;
    }
    
    /**
     * Gets real-time session progress
     */
    public SessionProgress getSessionProgress() {
        if (activeSession == null) {
            return null;
        }
        
        SessionProgress progress = new SessionProgress();
        progress.setPathwayId(activeSession.getPathwayId());
        progress.setStepsCompleted(activeSession.getStepsCompleted());
        progress.setTotalSteps(activeSession.getTotalSteps());
        progress.setCurrentErrors(activeSession.getTotalErrors());
        
        // Calculate elapsed time
        long elapsed = ChronoUnit.MILLIS.between(activeSession.getStartTime(), LocalDateTime.now());
        progress.setElapsedTime(elapsed);
        
        // Calculate completion rate
        double completionRate = activeSession.getTotalSteps() > 0 ? 
            (double) activeSession.getStepsCompleted() / activeSession.getTotalSteps() : 0.0;
        progress.setCompletionRate(completionRate);
        
        // Estimate remaining time based on current pace
        if (activeSession.getStepsCompleted() > 0) {
            long avgTimePerStep = elapsed / activeSession.getStepsCompleted();
            int remainingSteps = activeSession.getTotalSteps() - activeSession.getStepsCompleted();
            progress.setEstimatedTimeRemaining(avgTimePerStep * remainingSteps);
        }
        
        return progress;
    }
    
    /**
     * Gets personalized recommendations for current session
     */
    public List<String> getSessionRecommendations() {
        if (activeSession == null) {
            return Arrays.asList("Start a learning session to get personalized recommendations");
        }
        
        List<String> recommendations = new ArrayList<>();
        SessionProgress progress = getSessionProgress();
        
        // Performance-based recommendations
        if (progress.getCompletionRate() < 0.3 && progress.getCurrentErrors() > 3) {
            recommendations.add("Consider taking a short break to reset focus");
            recommendations.add("Review the previous step before continuing");
        } else if (progress.getCompletionRate() > 0.8 && progress.getCurrentErrors() <= 1) {
            recommendations.add("Excellent progress! You're doing great");
            recommendations.add("Consider trying a more advanced pathway next");
        }
        
        // Time-based recommendations
        long sessionTime = progress.getElapsedTime();
        long recommendedTime = profileManager.getRecommendedSessionLength();
        
        if (sessionTime > recommendedTime * 1.2) {
            recommendations.add("You've been learning for a while - consider taking a break");
        } else if (sessionTime > recommendedTime * 2) {
            recommendations.add("Great dedication! Consider saving progress and taking a rest");
        }
        
        return recommendations;
    }
    
    /**
     * Gets achievements earned in current or recent sessions
     */
    public List<Achievement> getRecentAchievements() {
        return achievementTracker.getRecentAchievements();
    }
    
    private SessionSummary calculateSessionSummary(CurrentSession session, long totalDuration) {
        SessionSummary summary = new SessionSummary();
        summary.setPathwayId(session.getPathwayId());
        summary.setSessionDuration(totalDuration);
        summary.setStepsCompleted(session.getStepsCompleted());
        summary.setTotalSteps(session.getTotalSteps());
        summary.setTotalErrors(session.getTotalErrors());
        summary.setStartTime(session.getStartTime());
        summary.setEndTime(session.getEndTime());
        
        // Calculate completion rate
        double completionRate = session.getTotalSteps() > 0 ? 
            (double) session.getStepsCompleted() / session.getTotalSteps() : 0.0;
        summary.setCompletionRate(completionRate);
        
        // Calculate average time per step
        long avgTimePerStep = session.getStepsCompleted() > 0 ? 
            totalDuration / session.getStepsCompleted() : 0;
        summary.setAverageTimePerStep(avgTimePerStep);
        
        // Determine performance level
        summary.setPerformanceLevel(determinePerformanceLevel(completionRate, session.getTotalErrors()));
        
        return summary;
    }
    
    private PerformanceLevel determinePerformanceLevel(double completionRate, int errorCount) {
        if (completionRate >= 0.9 && errorCount <= 1) {
            return PerformanceLevel.EXCELLENT;
        } else if (completionRate >= 0.7 && errorCount <= 3) {
            return PerformanceLevel.GOOD;
        } else if (completionRate >= 0.5) {
            return PerformanceLevel.SATISFACTORY;
        } else {
            return PerformanceLevel.NEEDS_IMPROVEMENT;
        }
    }
    
    private void updateSessionHistory(CurrentSession session, SessionSummary summary) {
        String pathwayId = session.getPathwayId();
        SessionHistory history = sessionHistories.computeIfAbsent(pathwayId, 
            k -> new SessionHistory(pathwayId));
        
        history.addSession(summary);
    }
    
    private void checkStepAchievements(StepCompletion completion) {
        if (completion.isSuccess() && completion.getErrorCount() == 0) {
            achievementTracker.checkAchievement("perfect_step", 
                "Completed step without errors");
        }
        
        if (completion.getDuration() < 30000) { // Less than 30 seconds
            achievementTracker.checkAchievement("speed_demon", 
                "Completed step in under 30 seconds");
        }
    }
    
    private void checkSessionAchievements(SessionSummary summary) {
        if (summary.getCompletionRate() >= 1.0) {
            achievementTracker.checkAchievement("pathway_master", 
                "Completed entire pathway");
        }
        
        if (summary.getPerformanceLevel() == PerformanceLevel.EXCELLENT) {
            achievementTracker.checkAchievement("excellence", 
                "Achieved excellent performance");
        }
        
        if (summary.getSessionDuration() > 30 * 60 * 1000) { // 30 minutes
            achievementTracker.checkAchievement("dedication", 
                "Learned for 30+ minutes");
        }
    }
    
    // Getters
    public boolean hasActiveSession() { return activeSession != null; }
    public CurrentSession getActiveSession() { return activeSession; }
    public Map<String, SessionHistory> getSessionHistories() { return sessionHistories; }
    
    // Data Classes
    public static class CurrentSession {
        private String pathwayId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int stepsCompleted;
        private int totalSteps;
        private int totalErrors;
        private List<StepCompletion> stepCompletions;
        private PersonalizationEngine.SessionPacing personalizedPacing;
        
        public CurrentSession(String pathwayId) {
            this.pathwayId = pathwayId;
            this.stepCompletions = new ArrayList<>();
            this.totalSteps = 10; // Default, should be set based on pathway
        }
        
        public void addStepCompletion(StepCompletion completion) {
            stepCompletions.add(completion);
        }
        
        public void incrementStepsCompleted() { stepsCompleted++; }
        public void incrementErrors() { totalErrors++; }
        
        // Getters and setters
        public String getPathwayId() { return pathwayId; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public int getStepsCompleted() { return stepsCompleted; }
        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        
        public int getTotalErrors() { return totalErrors; }
        
        public List<StepCompletion> getStepCompletions() { return new ArrayList<>(stepCompletions); }
        
        public PersonalizationEngine.SessionPacing getPersonalizedPacing() { return personalizedPacing; }
        public void setPersonalizedPacing(PersonalizationEngine.SessionPacing pacing) { 
            this.personalizedPacing = pacing; 
        }
    }
    
    public static class StepCompletion {
        private int stepNumber;
        private long duration;
        private int errorCount;
        private LocalDateTime timestamp;
        private boolean success;
        private String failureReason;
        
        // Getters and setters
        public int getStepNumber() { return stepNumber; }
        public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    }
    
    public static class SessionSummary {
        private String pathwayId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long sessionDuration;
        private int stepsCompleted;
        private int totalSteps;
        private int totalErrors;
        private double completionRate;
        private long averageTimePerStep;
        private PerformanceLevel performanceLevel;
        
        // Getters and setters
        public String getPathwayId() { return pathwayId; }
        public void setPathwayId(String pathwayId) { this.pathwayId = pathwayId; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public long getSessionDuration() { return sessionDuration; }
        public void setSessionDuration(long sessionDuration) { this.sessionDuration = sessionDuration; }
        
        public int getStepsCompleted() { return stepsCompleted; }
        public void setStepsCompleted(int stepsCompleted) { this.stepsCompleted = stepsCompleted; }
        
        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        
        public int getTotalErrors() { return totalErrors; }
        public void setTotalErrors(int totalErrors) { this.totalErrors = totalErrors; }
        
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
        
        public long getAverageTimePerStep() { return averageTimePerStep; }
        public void setAverageTimePerStep(long averageTimePerStep) { 
            this.averageTimePerStep = averageTimePerStep; 
        }
        
        public PerformanceLevel getPerformanceLevel() { return performanceLevel; }
        public void setPerformanceLevel(PerformanceLevel performanceLevel) { 
            this.performanceLevel = performanceLevel; 
        }
    }
    
    public static class SessionProgress {
        private String pathwayId;
        private int stepsCompleted;
        private int totalSteps;
        private int currentErrors;
        private long elapsedTime;
        private long estimatedTimeRemaining;
        private double completionRate;
        
        // Getters and setters
        public String getPathwayId() { return pathwayId; }
        public void setPathwayId(String pathwayId) { this.pathwayId = pathwayId; }
        
        public int getStepsCompleted() { return stepsCompleted; }
        public void setStepsCompleted(int stepsCompleted) { this.stepsCompleted = stepsCompleted; }
        
        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        
        public int getCurrentErrors() { return currentErrors; }
        public void setCurrentErrors(int currentErrors) { this.currentErrors = currentErrors; }
        
        public long getElapsedTime() { return elapsedTime; }
        public void setElapsedTime(long elapsedTime) { this.elapsedTime = elapsedTime; }
        
        public long getEstimatedTimeRemaining() { return estimatedTimeRemaining; }
        public void setEstimatedTimeRemaining(long estimatedTimeRemaining) { 
            this.estimatedTimeRemaining = estimatedTimeRemaining; 
        }
        
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
    }
    
    public static class SessionHistory {
        private String pathwayId;
        private List<SessionSummary> sessions;
        
        public SessionHistory(String pathwayId) {
            this.pathwayId = pathwayId;
            this.sessions = new ArrayList<>();
        }
        
        public void addSession(SessionSummary summary) {
            sessions.add(summary);
        }
        
        public String getPathwayId() { return pathwayId; }
        public List<SessionSummary> getSessions() { return new ArrayList<>(sessions); }
    }
    
    public static class Achievement {
        private String id;
        private String title;
        private String description;
        private LocalDateTime earnedDate;
        
        public Achievement(String id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.earnedDate = LocalDateTime.now();
        }
        
        // Getters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public LocalDateTime getEarnedDate() { return earnedDate; }
    }
    
    // Enums
    public enum PerformanceLevel {
        EXCELLENT, GOOD, SATISFACTORY, NEEDS_IMPROVEMENT
    }
    
    // Inner class for achievement tracking
    private static class AchievementTracker {
        private Set<String> earnedAchievements;
        private List<Achievement> recentAchievements;
        
        public AchievementTracker() {
            this.earnedAchievements = new HashSet<>();
            this.recentAchievements = new ArrayList<>();
        }
        
        public void checkAchievement(String achievementId, String description) {
            if (!earnedAchievements.contains(achievementId)) {
                earnedAchievements.add(achievementId);
                Achievement achievement = new Achievement(achievementId, 
                    formatAchievementTitle(achievementId), description);
                recentAchievements.add(achievement);
                
                // Keep only recent achievements
                if (recentAchievements.size() > 10) {
                    recentAchievements.remove(0);
                }
            }
        }
        
        private String formatAchievementTitle(String achievementId) {
            return achievementId.replace("_", " ").toUpperCase();
        }
        
        public List<Achievement> getRecentAchievements() {
            return new ArrayList<>(recentAchievements);
        }
    }
}
