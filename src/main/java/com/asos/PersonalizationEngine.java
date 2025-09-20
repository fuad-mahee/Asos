package com.asos;

import java.util.*;

/**
 * Intelligent personalization engine that adapts learning content,
 * pacing, and difficulty based on user profile and performance analytics
 */
public class PersonalizationEngine {
    
    private UserProfileManager profileManager;
    private LearningAnalytics analytics;
    private AdaptationRules adaptationRules;
    
    public PersonalizationEngine(UserProfileManager profileManager, LearningAnalytics analytics) {
        this.profileManager = profileManager;
        this.analytics = analytics;
        this.adaptationRules = new AdaptationRules();
    }
    
    /**
     * Personalizes a learning step based on user profile and current performance
     */
    public PersonalizedStep personalizeStep(LearningStep originalStep, String pathwayId) {
        UserProfileManager.UserProfile profile = profileManager.getCurrentProfile();
        LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
        
        PersonalizedStep personalizedStep = new PersonalizedStep(originalStep);
        
        // Adapt based on learning style
        adaptForLearningStyle(personalizedStep, profile.getLearningStyle());
        
        // Adapt based on skill level
        adaptForSkillLevel(personalizedStep, profile.getSkillLevel(), pathwayId);
        
        // Adapt based on performance trends
        adaptForPerformanceTrend(personalizedStep, insights.getOverallTrend());
        
        // Adapt based on user preferences
        adaptForPreferences(personalizedStep, profile.getPreferences());
        
        // Adapt timing and pacing
        adaptTiming(personalizedStep, insights);
        
        return personalizedStep;
    }
    
    /**
     * Generates personalized hints based on user context
     */
    public List<String> generatePersonalizedHints(LearningStep step, int errorCount, long timeSpent) {
        UserProfileManager.UserProfile profile = profileManager.getCurrentProfile();
        List<String> hints = new ArrayList<>();
        
        // Generate adaptive hints based on struggle indicators
        if (errorCount > 2) {
            hints.addAll(generateErrorBasedHints(step, profile.getLearningStyle()));
        }
        
        if (timeSpent > getTimeoutThreshold(profile)) {
            hints.addAll(generateTimeBasedHints(step, profile.getLearningStyle()));
        }
        
        // Skill level appropriate hints
        hints.addAll(generateSkillBasedHints(step, profile.getSkillLevel()));
        
        return hints;
    }
    
    /**
     * Determines optimal difficulty level for user
     */
    public DifficultyAdaptation adaptDifficulty(String pathwayId) {
        UserProfileManager.DifficultyLevel personalizedLevel = 
            profileManager.getPersonalizedDifficulty(pathwayId);
        LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
        
        DifficultyAdaptation adaptation = new DifficultyAdaptation();
        adaptation.setRecommendedLevel(personalizedLevel);
        adaptation.setConfidence(calculateDifficultyConfidence(insights));
        adaptation.setReason(generateDifficultyReason(personalizedLevel, insights));
        
        return adaptation;
    }
    
    /**
     * Calculates optimal session length for the user
     */
    public SessionPacing calculateOptimalPacing() {
        UserProfileManager.UserProfile profile = profileManager.getCurrentProfile();
        LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
        
        SessionPacing pacing = new SessionPacing();
        
        // Base pacing on user preferences
        UserProfileManager.PacingPreference preferred = profile.getPreferences().getPreferredPacing();
        
        switch (preferred) {
            case SLOW:
                pacing.setStepTimeout(180000); // 3 minutes
                pacing.setHintDelay(60000);    // 1 minute
                break;
            case FAST:
                pacing.setStepTimeout(60000);  // 1 minute
                pacing.setHintDelay(20000);    // 20 seconds
                break;
            case NORMAL:
                pacing.setStepTimeout(120000); // 2 minutes
                pacing.setHintDelay(40000);    // 40 seconds
                break;
            case ADAPTIVE:
                // Adapt based on performance
                adaptivePacing(pacing, insights);
                break;
        }
        
        // Adjust for learning velocity
        adjustForVelocity(pacing, insights.getLearningVelocity());
        
        return pacing;
    }
    
    private void adaptForLearningStyle(PersonalizedStep step, UserProfileManager.LearningStyle style) {
        switch (style) {
            case VISUAL:
                step.setEmphasisMode(EmphasisMode.VISUAL);
                step.addAdaptation("Use clear, step-by-step visual cues");
                step.setDetailLevel(DetailLevel.HIGH);
                break;
            case AUDITORY:
                step.setEmphasisMode(EmphasisMode.VERBAL);
                step.addAdaptation("Focus on clear verbal instructions");
                step.setDetailLevel(DetailLevel.MEDIUM);
                break;
            case KINESTHETIC:
                step.setEmphasisMode(EmphasisMode.INTERACTIVE);
                step.addAdaptation("Encourage hands-on practice");
                step.setDetailLevel(DetailLevel.PRACTICAL);
                break;
            case BALANCED:
                step.setEmphasisMode(EmphasisMode.MIXED);
                step.setDetailLevel(DetailLevel.MEDIUM);
                break;
        }
    }
    
    private void adaptForSkillLevel(PersonalizedStep step, UserProfileManager.SkillLevel skillLevel, String pathwayId) {
        switch (skillLevel) {
            case BEGINNER:
                step.addAdaptation("Extra guidance and detailed explanations");
                step.setComplexityLevel(ComplexityLevel.SIMPLE);
                step.setGuidanceLevel(GuidanceLevel.HIGH);
                break;
            case INTERMEDIATE:
                step.setComplexityLevel(ComplexityLevel.MODERATE);
                step.setGuidanceLevel(GuidanceLevel.MEDIUM);
                break;
            case ADVANCED:
                step.addAdaptation("Concise instructions with optional details");
                step.setComplexityLevel(ComplexityLevel.ADVANCED);
                step.setGuidanceLevel(GuidanceLevel.LOW);
                break;
            case EXPERT:
                step.addAdaptation("Minimal guidance, focus on efficiency");
                step.setComplexityLevel(ComplexityLevel.EXPERT);
                step.setGuidanceLevel(GuidanceLevel.MINIMAL);
                break;
        }
    }
    
    private void adaptForPerformanceTrend(PersonalizedStep step, LearningAnalytics.PerformanceTrend trend) {
        switch (trend) {
            case IMPROVING:
                step.addAdaptation("Great progress! Ready for the next challenge");
                step.setMotivationLevel(MotivationLevel.CONFIDENT);
                break;
            case STABLE:
                step.setMotivationLevel(MotivationLevel.STEADY);
                break;
            case DECLINING:
                step.addAdaptation("Let's take this step by step");
                step.setMotivationLevel(MotivationLevel.SUPPORTIVE);
                step.setGuidanceLevel(GuidanceLevel.HIGH);
                break;
        }
    }
    
    private void adaptForPreferences(PersonalizedStep step, UserProfileManager.UserPreferences preferences) {
        // Adapt hint frequency
        switch (preferences.getHintFrequency()) {
            case MINIMAL:
                step.setHintThreshold(180000); // 3 minutes
                break;
            case NORMAL:
                step.setHintThreshold(60000);  // 1 minute
                break;
            case FREQUENT:
                step.setHintThreshold(30000);  // 30 seconds
                break;
        }
        
        // Adapt feedback verbosity
        switch (preferences.getFeedbackVerbosity()) {
            case MINIMAL:
                step.setFeedbackLevel(FeedbackLevel.BRIEF);
                break;
            case NORMAL:
                step.setFeedbackLevel(FeedbackLevel.STANDARD);
                break;
            case VERBOSE:
                step.setFeedbackLevel(FeedbackLevel.DETAILED);
                break;
        }
    }
    
    private void adaptTiming(PersonalizedStep step, LearningAnalytics.LearningInsights insights) {
        // Adjust timeouts based on learning velocity
        double velocity = insights.getLearningVelocity();
        
        if (velocity < 0.3) {
            step.setTimeoutAdjustment(1.5); // 50% more time
        } else if (velocity > 0.8) {
            step.setTimeoutAdjustment(0.7); // 30% less time
        } else {
            step.setTimeoutAdjustment(1.0); // Normal time
        }
    }
    
    private List<String> generateErrorBasedHints(LearningStep step, UserProfileManager.LearningStyle style) {
        List<String> hints = new ArrayList<>();
        
        switch (style) {
            case VISUAL:
                hints.add("Take a look at the visual example again");
                hints.add("Check each step carefully in sequence");
                break;
            case KINESTHETIC:
                hints.add("Try practicing the action once more");
                hints.add("Break down the task into smaller parts");
                break;
            case AUDITORY:
                hints.add("Read the instruction aloud to yourself");
                hints.add("Think through each step verbally");
                break;
            case BALANCED:
                hints.add("Review the instruction and try a different approach");
                break;
        }
        
        return hints;
    }
    
    private List<String> generateTimeBasedHints(LearningStep step, UserProfileManager.LearningStyle style) {
        List<String> hints = new ArrayList<>();
        
        hints.add("No rush! Take your time to understand");
        
        switch (style) {
            case VISUAL:
                hints.add("Focus on the visual indicators");
                break;
            case KINESTHETIC:
                hints.add("Try the hands-on approach");
                break;
            case AUDITORY:
                hints.add("Talk through the steps out loud");
                break;
            case BALANCED:
                hints.add("Use any approach that feels comfortable");
                break;
        }
        
        return hints;
    }
    
    private List<String> generateSkillBasedHints(LearningStep step, UserProfileManager.SkillLevel skillLevel) {
        List<String> hints = new ArrayList<>();
        
        switch (skillLevel) {
            case BEGINNER:
                hints.add("Remember: practice makes perfect!");
                hints.add("Don't worry about making mistakes - that's how we learn");
                break;
            case INTERMEDIATE:
                hints.add("You can build on what you already know");
                break;
            case ADVANCED:
            case EXPERT:
                hints.add("Consider alternative approaches");
                break;
        }
        
        return hints;
    }
    
    private long getTimeoutThreshold(UserProfileManager.UserProfile profile) {
        UserProfileManager.PacingPreference pacing = profile.getPreferences().getPreferredPacing();
        
        switch (pacing) {
            case SLOW: return 300000;    // 5 minutes
            case NORMAL: return 180000;  // 3 minutes
            case FAST: return 120000;    // 2 minutes
            case ADAPTIVE: return 150000; // 2.5 minutes
            default: return 180000;
        }
    }
    
    private double calculateDifficultyConfidence(LearningAnalytics.LearningInsights insights) {
        // Higher confidence with more consistent performance
        return Math.min(1.0, insights.getConsistencyScore() + 0.2);
    }
    
    private String generateDifficultyReason(UserProfileManager.DifficultyLevel level, 
                                          LearningAnalytics.LearningInsights insights) {
        switch (level) {
            case EASY:
                return "Based on your learning pace, let's focus on building confidence";
            case MEDIUM:
                return "This difficulty level matches your current skill development";
            case HARD:
                return "You're ready for more challenging content!";
            default:
                return "Personalized difficulty based on your progress";
        }
    }
    
    private void adaptivePacing(SessionPacing pacing, LearningAnalytics.LearningInsights insights) {
        double velocity = insights.getLearningVelocity();
        double consistency = insights.getConsistencyScore();
        
        // Base timeouts
        long baseTimeout = 120000; // 2 minutes
        long baseHintDelay = 40000; // 40 seconds
        
        // Adjust for velocity
        double velocityMultiplier = 1.0;
        if (velocity < 0.3) {
            velocityMultiplier = 1.5; // Slower learners get more time
        } else if (velocity > 0.7) {
            velocityMultiplier = 0.8; // Faster learners get less time
        }
        
        // Adjust for consistency
        double consistencyMultiplier = 1.0;
        if (consistency < 0.5) {
            consistencyMultiplier = 1.2; // Inconsistent learners get more support
        }
        
        pacing.setStepTimeout((long) (baseTimeout * velocityMultiplier * consistencyMultiplier));
        pacing.setHintDelay((long) (baseHintDelay * velocityMultiplier));
    }
    
    private void adjustForVelocity(SessionPacing pacing, double velocity) {
        double adjustment = velocity > 0.5 ? 0.9 : 1.1;
        pacing.setStepTimeout((long) (pacing.getStepTimeout() * adjustment));
        pacing.setHintDelay((long) (pacing.getHintDelay() * adjustment));
    }
    
    // Data Classes
    public static class PersonalizedStep {
        private LearningStep originalStep;
        private List<String> adaptations;
        private EmphasisMode emphasisMode;
        private DetailLevel detailLevel;
        private ComplexityLevel complexityLevel;
        private GuidanceLevel guidanceLevel;
        private MotivationLevel motivationLevel;
        private FeedbackLevel feedbackLevel;
        private long hintThreshold;
        private double timeoutAdjustment;
        
        public PersonalizedStep(LearningStep originalStep) {
            this.originalStep = originalStep;
            this.adaptations = new ArrayList<>();
            this.timeoutAdjustment = 1.0;
        }
        
        public void addAdaptation(String adaptation) {
            adaptations.add(adaptation);
        }
        
        // Getters and setters
        public LearningStep getOriginalStep() { return originalStep; }
        public List<String> getAdaptations() { return new ArrayList<>(adaptations); }
        
        public EmphasisMode getEmphasisMode() { return emphasisMode; }
        public void setEmphasisMode(EmphasisMode emphasisMode) { this.emphasisMode = emphasisMode; }
        
        public DetailLevel getDetailLevel() { return detailLevel; }
        public void setDetailLevel(DetailLevel detailLevel) { this.detailLevel = detailLevel; }
        
        public ComplexityLevel getComplexityLevel() { return complexityLevel; }
        public void setComplexityLevel(ComplexityLevel complexityLevel) { this.complexityLevel = complexityLevel; }
        
        public GuidanceLevel getGuidanceLevel() { return guidanceLevel; }
        public void setGuidanceLevel(GuidanceLevel guidanceLevel) { this.guidanceLevel = guidanceLevel; }
        
        public MotivationLevel getMotivationLevel() { return motivationLevel; }
        public void setMotivationLevel(MotivationLevel motivationLevel) { this.motivationLevel = motivationLevel; }
        
        public FeedbackLevel getFeedbackLevel() { return feedbackLevel; }
        public void setFeedbackLevel(FeedbackLevel feedbackLevel) { this.feedbackLevel = feedbackLevel; }
        
        public long getHintThreshold() { return hintThreshold; }
        public void setHintThreshold(long hintThreshold) { this.hintThreshold = hintThreshold; }
        
        public double getTimeoutAdjustment() { return timeoutAdjustment; }
        public void setTimeoutAdjustment(double timeoutAdjustment) { this.timeoutAdjustment = timeoutAdjustment; }
    }
    
    public static class DifficultyAdaptation {
        private UserProfileManager.DifficultyLevel recommendedLevel;
        private double confidence;
        private String reason;
        
        // Getters and setters
        public UserProfileManager.DifficultyLevel getRecommendedLevel() { return recommendedLevel; }
        public void setRecommendedLevel(UserProfileManager.DifficultyLevel recommendedLevel) { 
            this.recommendedLevel = recommendedLevel; 
        }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    public static class SessionPacing {
        private long stepTimeout;
        private long hintDelay;
        
        // Getters and setters
        public long getStepTimeout() { return stepTimeout; }
        public void setStepTimeout(long stepTimeout) { this.stepTimeout = stepTimeout; }
        
        public long getHintDelay() { return hintDelay; }
        public void setHintDelay(long hintDelay) { this.hintDelay = hintDelay; }
    }
    
    // Enums
    public enum EmphasisMode {
        VISUAL, VERBAL, INTERACTIVE, MIXED
    }
    
    public enum DetailLevel {
        MINIMAL, MEDIUM, HIGH, PRACTICAL
    }
    
    public enum ComplexityLevel {
        SIMPLE, MODERATE, ADVANCED, EXPERT
    }
    
    public enum GuidanceLevel {
        MINIMAL, LOW, MEDIUM, HIGH
    }
    
    public enum MotivationLevel {
        SUPPORTIVE, STEADY, CONFIDENT
    }
    
    public enum FeedbackLevel {
        BRIEF, STANDARD, DETAILED
    }
    
    // Placeholder for adaptation rules
    private static class AdaptationRules {
        // Future: More sophisticated rule-based adaptations
    }
}
