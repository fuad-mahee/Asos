package com.asos;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced analytics engine that analyzes learning patterns,
 * predicts performance, and provides intelligent insights
 */
public class LearningAnalytics {
    
    private UserProfileManager profileManager;
    private Map<String, PathwayAnalytics> pathwayAnalytics;
    private PerformancePredictor predictor;
    
    public LearningAnalytics(UserProfileManager profileManager) {
        this.profileManager = profileManager;
        this.pathwayAnalytics = new HashMap<>();
        this.predictor = new PerformancePredictor();
    }
    
    /**
     * Analyzes overall learning progress and performance trends
     */
    public LearningInsights analyzeLearningProgress() {
        UserProfileManager.UserProfile profile = profileManager.getCurrentProfile();
        List<UserProfileManager.SessionRecord> history = profile.getPerformanceHistory();
        
        if (history.isEmpty()) {
            return createDefaultInsights();
        }
        
        LearningInsights insights = new LearningInsights();
        
        // Calculate performance trends
        insights.setOverallTrend(calculatePerformanceTrend(history));
        insights.setLearningVelocity(calculateLearningVelocity(history));
        insights.setConsistencyScore(calculateConsistencyScore(history));
        insights.setRetentionRate(calculateRetentionRate(history));
        
        // Identify strengths and weaknesses
        insights.setStrongAreas(identifyStrongAreas(history));
        insights.setImprovementAreas(identifyImprovementAreas(history));
        
        // Generate recommendations
        insights.setRecommendations(generateRecommendations(insights, profile));
        
        // Predict next session performance
        insights.setPredictedPerformance(predictor.predictNextSession(history));
        
        return insights;
    }
    
    /**
     * Analyzes performance for a specific pathway
     */
    public PathwayAnalytics analyzePathwayPerformance(String pathwayId) {
        if (!pathwayAnalytics.containsKey(pathwayId)) {
            pathwayAnalytics.put(pathwayId, new PathwayAnalytics(pathwayId));
        }
        
        PathwayAnalytics analytics = pathwayAnalytics.get(pathwayId);
        List<UserProfileManager.SessionRecord> pathwaySessions = getSessionsForPathway(pathwayId);
        
        if (pathwaySessions.isEmpty()) {
            return analytics;
        }
        
        // Update analytics
        analytics.updateAnalytics(pathwaySessions);
        
        return analytics;
    }
    
    private List<UserProfileManager.SessionRecord> getSessionsForPathway(String pathwayId) {
        return profileManager.getCurrentProfile().getPerformanceHistory().stream()
            .filter(session -> pathwayId.equals(session.getPathwayId()))
            .collect(Collectors.toList());
    }
    
    private PerformanceTrend calculatePerformanceTrend(List<UserProfileManager.SessionRecord> history) {
        if (history.size() < 3) {
            return PerformanceTrend.STABLE;
        }
        
        // Take recent sessions vs earlier sessions
        int splitPoint = Math.max(1, history.size() / 2);
        List<UserProfileManager.SessionRecord> earlier = history.subList(0, splitPoint);
        List<UserProfileManager.SessionRecord> recent = history.subList(splitPoint, history.size());
        
        double earlierAvg = earlier.stream()
            .mapToDouble(UserProfileManager.SessionRecord::getCompletionRate)
            .average()
            .orElse(0.0);
        
        double recentAvg = recent.stream()
            .mapToDouble(UserProfileManager.SessionRecord::getCompletionRate)
            .average()
            .orElse(0.0);
        
        double improvement = recentAvg - earlierAvg;
        
        if (improvement > 0.1) {
            return PerformanceTrend.IMPROVING;
        } else if (improvement < -0.1) {
            return PerformanceTrend.DECLINING;
        } else {
            return PerformanceTrend.STABLE;
        }
    }
    
    private double calculateLearningVelocity(List<UserProfileManager.SessionRecord> history) {
        if (history.size() < 2) {
            return 0.5; // Default moderate velocity
        }
        
        // Calculate steps completed per hour
        long totalTime = history.stream()
            .mapToLong(UserProfileManager.SessionRecord::getSessionDuration)
            .sum();
        
        int totalSteps = history.stream()
            .mapToInt(UserProfileManager.SessionRecord::getStepsCompleted)
            .sum();
        
        if (totalTime == 0) return 0.0;
        
        // Steps per hour
        double stepsPerHour = (double) totalSteps / (totalTime / 3600000.0);
        
        // Normalize to 0-1 scale (assuming 20 steps/hour is excellent)
        return Math.min(1.0, stepsPerHour / 20.0);
    }
    
    private double calculateConsistencyScore(List<UserProfileManager.SessionRecord> history) {
        if (history.size() < 3) {
            return 0.5;
        }
        
        List<Double> completionRates = history.stream()
            .map(UserProfileManager.SessionRecord::getCompletionRate)
            .collect(Collectors.toList());
        
        // Calculate standard deviation
        double mean = completionRates.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double variance = completionRates.stream()
            .mapToDouble(rate -> Math.pow(rate - mean, 2))
            .average()
            .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        
        // Convert to consistency score (lower deviation = higher consistency)
        return Math.max(0.0, 1.0 - (stdDev * 2));
    }
    
    private double calculateRetentionRate(List<UserProfileManager.SessionRecord> history) {
        // Simplified retention calculation based on completion rates over time
        if (history.size() < 5) {
            return 0.8; // Default assumption
        }
        
        // Check if performance is maintained over time intervals
        List<UserProfileManager.SessionRecord> recent = history.subList(
            Math.max(0, history.size() - 5), history.size());
        
        double avgRecent = recent.stream()
            .mapToDouble(UserProfileManager.SessionRecord::getCompletionRate)
            .average()
            .orElse(0.0);
        
        // If recent performance is good, assume good retention
        return Math.min(1.0, avgRecent + 0.2);
    }
    
    private List<String> identifyStrongAreas(List<UserProfileManager.SessionRecord> history) {
        Map<String, List<Double>> pathwayPerformance = history.stream()
            .collect(Collectors.groupingBy(
                UserProfileManager.SessionRecord::getPathwayId,
                Collectors.mapping(UserProfileManager.SessionRecord::getCompletionRate, 
                    Collectors.toList())
            ));
        
        return pathwayPerformance.entrySet().stream()
            .filter(entry -> {
                double avg = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                return avg > 0.8;
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private List<String> identifyImprovementAreas(List<UserProfileManager.SessionRecord> history) {
        Map<String, List<Double>> pathwayPerformance = history.stream()
            .collect(Collectors.groupingBy(
                UserProfileManager.SessionRecord::getPathwayId,
                Collectors.mapping(UserProfileManager.SessionRecord::getCompletionRate, 
                    Collectors.toList())
            ));
        
        return pathwayPerformance.entrySet().stream()
            .filter(entry -> {
                double avg = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                return avg < 0.6;
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private List<String> generateRecommendations(LearningInsights insights, 
                                                UserProfileManager.UserProfile profile) {
        List<String> recommendations = new ArrayList<>();
        
        // Trend-based recommendations
        if (insights.getOverallTrend() == PerformanceTrend.DECLINING) {
            recommendations.add("Consider taking shorter, more frequent learning sessions");
            recommendations.add("Review fundamentals before advancing to new topics");
        } else if (insights.getOverallTrend() == PerformanceTrend.IMPROVING) {
            recommendations.add("Great progress! Consider tackling more challenging content");
        }
        
        // Velocity-based recommendations
        if (insights.getLearningVelocity() < 0.3) {
            recommendations.add("Take time to understand each step thoroughly");
            recommendations.add("Use hints more frequently to maintain momentum");
        } else if (insights.getLearningVelocity() > 0.8) {
            recommendations.add("Consider advanced pathways to match your learning pace");
        }
        
        // Consistency-based recommendations
        if (insights.getConsistencyScore() < 0.5) {
            recommendations.add("Try to maintain regular learning sessions");
            recommendations.add("Set achievable daily learning goals");
        }
        
        // Learning style recommendations
        UserProfileManager.LearningStyle style = profile.getLearningStyle();
        switch (style) {
            case VISUAL:
                recommendations.add("Focus on step-by-step visual instructions");
                break;
            case KINESTHETIC:
                recommendations.add("Practice hands-on exercises more frequently");
                break;
            case AUDITORY:
                recommendations.add("Take advantage of audio explanations when available");
                break;
            case BALANCED:
                recommendations.add("Continue with your well-rounded learning approach");
                break;
        }
        
        return recommendations;
    }
    
    private LearningInsights createDefaultInsights() {
        LearningInsights insights = new LearningInsights();
        insights.setOverallTrend(PerformanceTrend.STABLE);
        insights.setLearningVelocity(0.5);
        insights.setConsistencyScore(0.5);
        insights.setRetentionRate(0.8);
        insights.setStrongAreas(new ArrayList<>());
        insights.setImprovementAreas(new ArrayList<>());
        insights.setRecommendations(Arrays.asList(
            "Start with beginner-friendly pathways",
            "Take your time to understand each step",
            "Practice regularly for best results"
        ));
        insights.setPredictedPerformance(new PerformancePrediction(0.7, 0.5));
        return insights;
    }
    
    // Data Classes
    public static class LearningInsights {
        private PerformanceTrend overallTrend;
        private double learningVelocity;
        private double consistencyScore;
        private double retentionRate;
        private List<String> strongAreas;
        private List<String> improvementAreas;
        private List<String> recommendations;
        private PerformancePrediction predictedPerformance;
        
        // Getters and setters
        public PerformanceTrend getOverallTrend() { return overallTrend; }
        public void setOverallTrend(PerformanceTrend overallTrend) { this.overallTrend = overallTrend; }
        
        public double getLearningVelocity() { return learningVelocity; }
        public void setLearningVelocity(double learningVelocity) { this.learningVelocity = learningVelocity; }
        
        public double getConsistencyScore() { return consistencyScore; }
        public void setConsistencyScore(double consistencyScore) { this.consistencyScore = consistencyScore; }
        
        public double getRetentionRate() { return retentionRate; }
        public void setRetentionRate(double retentionRate) { this.retentionRate = retentionRate; }
        
        public List<String> getStrongAreas() { return strongAreas; }
        public void setStrongAreas(List<String> strongAreas) { this.strongAreas = strongAreas; }
        
        public List<String> getImprovementAreas() { return improvementAreas; }
        public void setImprovementAreas(List<String> improvementAreas) { 
            this.improvementAreas = improvementAreas; 
        }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public PerformancePrediction getPredictedPerformance() { return predictedPerformance; }
        public void setPredictedPerformance(PerformancePrediction predictedPerformance) { 
            this.predictedPerformance = predictedPerformance; 
        }
    }
    
    public static class PathwayAnalytics {
        private String pathwayId;
        private int attemptCount;
        private double averageCompletionRate;
        private double averageSessionTime;
        private List<Double> progressionCurve;
        private Map<Integer, Integer> stepDifficultyMap;
        
        public PathwayAnalytics(String pathwayId) {
            this.pathwayId = pathwayId;
            this.progressionCurve = new ArrayList<>();
            this.stepDifficultyMap = new HashMap<>();
        }
        
        public void updateAnalytics(List<UserProfileManager.SessionRecord> sessions) {
            this.attemptCount = sessions.size();
            this.averageCompletionRate = sessions.stream()
                .mapToDouble(UserProfileManager.SessionRecord::getCompletionRate)
                .average()
                .orElse(0.0);
            this.averageSessionTime = sessions.stream()
                .mapToLong(UserProfileManager.SessionRecord::getSessionDuration)
                .average()
                .orElse(0.0);
            
            // Update progression curve
            this.progressionCurve = sessions.stream()
                .map(UserProfileManager.SessionRecord::getCompletionRate)
                .collect(Collectors.toList());
        }
        
        // Getters
        public String getPathwayId() { return pathwayId; }
        public int getAttemptCount() { return attemptCount; }
        public double getAverageCompletionRate() { return averageCompletionRate; }
        public double getAverageSessionTime() { return averageSessionTime; }
        public List<Double> getProgressionCurve() { return new ArrayList<>(progressionCurve); }
    }
    
    public static class PerformancePrediction {
        private double expectedCompletionRate;
        private double confidence;
        
        public PerformancePrediction(double expectedCompletionRate, double confidence) {
            this.expectedCompletionRate = expectedCompletionRate;
            this.confidence = confidence;
        }
        
        public double getExpectedCompletionRate() { return expectedCompletionRate; }
        public double getConfidence() { return confidence; }
    }
    
    // Enums
    public enum PerformanceTrend {
        IMPROVING, STABLE, DECLINING
    }
    
    // Inner class for performance prediction
    private static class PerformancePredictor {
        
        public PerformancePrediction predictNextSession(List<UserProfileManager.SessionRecord> history) {
            if (history.size() < 3) {
                return new PerformancePrediction(0.7, 0.3); // Low confidence for new users
            }
            
            // Use weighted average of recent sessions
            List<UserProfileManager.SessionRecord> recent = history.subList(
                Math.max(0, history.size() - 5), history.size());
            
            double weightedSum = 0.0;
            double weightSum = 0.0;
            
            for (int i = 0; i < recent.size(); i++) {
                double weight = (i + 1) * 0.2; // More weight to recent sessions
                weightedSum += recent.get(i).getCompletionRate() * weight;
                weightSum += weight;
            }
            
            double predicted = weightSum > 0 ? weightedSum / weightSum : 0.7;
            
            // Calculate confidence based on consistency
            double variance = recent.stream()
                .mapToDouble(s -> Math.pow(s.getCompletionRate() - predicted, 2))
                .average()
                .orElse(0.0);
            
            double confidence = Math.max(0.1, 1.0 - Math.sqrt(variance));
            
            return new PerformancePrediction(predicted, confidence);
        }
    }
}
