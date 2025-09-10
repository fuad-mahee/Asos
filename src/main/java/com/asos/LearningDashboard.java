package com.asos;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.List;

/**
 * Interactive dashboard that displays learning progress, analytics,
 * and personalized insights in a visually engaging interface
 */
public class LearningDashboard extends VBox {
    
    private UserProfileManager profileManager;
    private LearningAnalytics analytics;
    private SessionManager sessionManager;
    
    // UI Components
    private Label welcomeLabel;
    private ProgressIndicator overallProgress;
    private Arc progressArc;
    private Label progressText;
    private LineChart<String, Number> performanceChart;
    private VBox achievementsList;
    private VBox recommendationsList;
    private Label learningStyleLabel;
    private Label skillLevelLabel;
    private Button startSessionButton;
    private Button viewAnalyticsButton;
    
    // Real-time update components
    private Timeline updateTimeline;
    private SessionProgressPanel sessionPanel;
    
    public LearningDashboard(UserProfileManager profileManager, 
                           LearningAnalytics analytics, 
                           SessionManager sessionManager) {
        this.profileManager = profileManager;
        this.analytics = analytics;
        this.sessionManager = sessionManager;
        
        initializeComponents();
        setupLayout();
        setupAnimations();
        updateDashboard();
    }
    
    private void initializeComponents() {
        // Welcome section
        welcomeLabel = new Label();
        welcomeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        welcomeLabel.setTextFill(Color.DARKBLUE);
        
        // Progress visualization
        overallProgress = new ProgressIndicator(0.0);
        overallProgress.setPrefSize(120, 120);
        
        // Custom circular progress arc
        progressArc = new Arc();
        progressArc.setCenterX(60);
        progressArc.setCenterY(60);
        progressArc.setRadiusX(50);
        progressArc.setRadiusY(50);
        progressArc.setStartAngle(90);
        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(null);
        progressArc.setStroke(Color.LIGHTBLUE);
        progressArc.setStrokeWidth(8);
        
        progressText = new Label("0%");
        progressText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        progressText.setTextFill(Color.DARKBLUE);
        
        // Performance chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Recent Sessions");
        yAxis.setLabel("Completion Rate");
        performanceChart = new LineChart<>(xAxis, yAxis);
        performanceChart.setTitle("Learning Progress");
        performanceChart.setPrefHeight(200);
        performanceChart.setLegendVisible(false);
        
        // Learning profile info
        learningStyleLabel = new Label();
        learningStyleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        
        skillLevelLabel = new Label();
        skillLevelLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        
        // Achievements list
        achievementsList = new VBox(5);
        achievementsList.setPadding(new Insets(10));
        achievementsList.setStyle("-fx-background-color: #f0f8ff; -fx-background-radius: 10;");
        
        // Recommendations list
        recommendationsList = new VBox(5);
        recommendationsList.setPadding(new Insets(10));
        recommendationsList.setStyle("-fx-background-color: #fff8dc; -fx-background-radius: 10;");
        
        // Action buttons
        startSessionButton = new Button("Start Learning Session");
        startSessionButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        startSessionButton.setOnAction(e -> startNewSession());
        
        viewAnalyticsButton = new Button("View Detailed Analytics");
        viewAnalyticsButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        viewAnalyticsButton.setOnAction(e -> showDetailedAnalytics());
        
        // Session progress panel
        sessionPanel = new SessionProgressPanel();
        sessionPanel.setVisible(false);
    }
    
    private void setupLayout() {
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: #fafafa;");
        
        // Header section
        VBox headerSection = new VBox(10);
        headerSection.setAlignment(Pos.CENTER);
        headerSection.getChildren().addAll(welcomeLabel);
        
        // Progress section
        HBox progressSection = new HBox(20);
        progressSection.setAlignment(Pos.CENTER);
        
        StackPane progressStack = new StackPane();
        progressStack.getChildren().addAll(progressArc, progressText);
        
        VBox progressInfo = new VBox(10);
        progressInfo.setAlignment(Pos.CENTER_LEFT);
        progressInfo.getChildren().addAll(
            learningStyleLabel,
            skillLevelLabel,
            new Label("Overall Progress")
        );
        
        progressSection.getChildren().addAll(progressStack, progressInfo);
        
        // Main content area
        HBox mainContent = new HBox(20);
        mainContent.setAlignment(Pos.TOP_CENTER);
        
        // Left panel - Analytics
        VBox leftPanel = new VBox(15);
        leftPanel.setPrefWidth(300);
        
        Label analyticsTitle = new Label("Performance Analytics");
        analyticsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        leftPanel.getChildren().addAll(
            analyticsTitle,
            performanceChart
        );
        
        // Right panel - Achievements and Recommendations
        VBox rightPanel = new VBox(15);
        rightPanel.setPrefWidth(300);
        
        Label achievementsTitle = new Label("🏆 Recent Achievements");
        achievementsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        Label recommendationsTitle = new Label("💡 Personalized Recommendations");
        recommendationsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        rightPanel.getChildren().addAll(
            achievementsTitle,
            achievementsList,
            recommendationsTitle,
            recommendationsList
        );
        
        mainContent.getChildren().addAll(leftPanel, rightPanel);
        
        // Action buttons
        HBox buttonSection = new HBox(15);
        buttonSection.setAlignment(Pos.CENTER);
        buttonSection.getChildren().addAll(startSessionButton, viewAnalyticsButton);
        
        // Add all sections
        this.getChildren().addAll(
            headerSection,
            progressSection,
            sessionPanel,
            mainContent,
            buttonSection
        );
    }
    
    private void setupAnimations() {
        // Setup periodic updates
        updateTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> updateRealTimeData()));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
        
        // Progress arc animation
        Timeline arcAnimation = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(2), 
            new KeyValue(progressArc.lengthProperty(), 0));
        arcAnimation.getKeyFrames().add(keyFrame);
    }
    
    /**
     * Updates the dashboard with current user data
     */
    public void updateDashboard() {
        UserProfileManager.UserProfile profile = profileManager.getCurrentProfile();
        LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
        
        // Update welcome message
        String timeOfDay = getTimeOfDayGreeting();
        welcomeLabel.setText(timeOfDay + "! Ready to learn?");
        
        // Update learning profile
        learningStyleLabel.setText("Learning Style: " + 
            formatLearningStyle(profile.getLearningStyle()));
        skillLevelLabel.setText("Skill Level: " + 
            formatSkillLevel(profile.getSkillLevel()));
        
        // Update progress visualization
        updateProgressVisualization(insights);
        
        // Update performance chart
        updatePerformanceChart(profile);
        
        // Update achievements
        updateAchievements();
        
        // Update recommendations
        updateRecommendations(insights);
    }
    
    private void updateProgressVisualization(LearningAnalytics.LearningInsights insights) {
        double completionRate = insights.getLearningVelocity();
        
        // Animate progress arc
        Timeline progressAnimation = new Timeline();
        KeyValue progressValue = new KeyValue(progressArc.lengthProperty(), 
            -360 * completionRate);
        KeyFrame progressFrame = new KeyFrame(Duration.seconds(1.5), progressValue);
        progressAnimation.getKeyFrames().add(progressFrame);
        progressAnimation.play();
        
        // Update progress text with animation
        Timeline textAnimation = new Timeline();
        int targetPercent = (int) (completionRate * 100);
        for (int i = 0; i <= targetPercent; i++) {
            final int percent = i;
            KeyFrame frame = new KeyFrame(Duration.millis(i * 30), 
                e -> progressText.setText(percent + "%"));
            textAnimation.getKeyFrames().add(frame);
        }
        textAnimation.play();
        
        // Color coding based on performance
        Color progressColor = getProgressColor(completionRate);
        progressArc.setStroke(progressColor);
        progressText.setTextFill(progressColor);
    }
    
    private void updatePerformanceChart(UserProfileManager.UserProfile profile) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        List<UserProfileManager.SessionRecord> recent = profile.getPerformanceHistory();
        int maxSessions = Math.min(10, recent.size());
        
        for (int i = Math.max(0, recent.size() - maxSessions); i < recent.size(); i++) {
            UserProfileManager.SessionRecord session = recent.get(i);
            series.getData().add(new XYChart.Data<>("Session " + (i + 1), 
                session.getCompletionRate() * 100));
        }
        
        performanceChart.getData().clear();
        performanceChart.getData().add(series);
        
        // Animate chart appearance
        for (XYChart.Data<String, Number> data : series.getData()) {
            data.getNode().setScaleY(0);
            Timeline scaleAnimation = new Timeline();
            KeyValue scaleValue = new KeyValue(data.getNode().scaleYProperty(), 1);
            KeyFrame scaleFrame = new KeyFrame(Duration.seconds(0.8), scaleValue);
            scaleAnimation.getKeyFrames().add(scaleFrame);
            scaleAnimation.play();
        }
    }
    
    private void updateAchievements() {
        achievementsList.getChildren().clear();
        
        List<SessionManager.Achievement> achievements = sessionManager.getRecentAchievements();
        
        if (achievements.isEmpty()) {
            Label noAchievements = new Label("Complete your first session to earn achievements!");
            noAchievements.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            achievementsList.getChildren().add(noAchievements);
        } else {
            for (SessionManager.Achievement achievement : achievements) {
                HBox achievementItem = createAchievementItem(achievement);
                achievementsList.getChildren().add(achievementItem);
            }
        }
    }
    
    private HBox createAchievementItem(SessionManager.Achievement achievement) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(5));
        item.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");
        
        Label icon = new Label("🏆");
        icon.setFont(Font.font(16));
        
        VBox textContent = new VBox(2);
        Label title = new Label(achievement.getTitle());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        
        Label description = new Label(achievement.getDescription());
        description.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        description.setStyle("-fx-text-fill: #666;");
        
        textContent.getChildren().addAll(title, description);
        item.getChildren().addAll(icon, textContent);
        
        // Add entrance animation
        item.setTranslateX(100);
        item.setOpacity(0);
        Timeline animation = new Timeline();
        animation.getKeyFrames().addAll(
            new KeyFrame(Duration.seconds(0.5), 
                new KeyValue(item.translateXProperty(), 0),
                new KeyValue(item.opacityProperty(), 1))
        );
        animation.play();
        
        return item;
    }
    
    private void updateRecommendations(LearningAnalytics.LearningInsights insights) {
        recommendationsList.getChildren().clear();
        
        List<String> recommendations = insights.getRecommendations();
        
        for (String recommendation : recommendations) {
            Label recommendationLabel = new Label("• " + recommendation);
            recommendationLabel.setWrapText(true);
            recommendationLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            recommendationLabel.setStyle("-fx-text-fill: #333; -fx-padding: 5;");
            recommendationsList.getChildren().add(recommendationLabel);
        }
    }
    
    private void updateRealTimeData() {
        if (sessionManager.hasActiveSession()) {
            sessionPanel.setVisible(true);
            sessionPanel.updateProgress(sessionManager.getSessionProgress());
        } else {
            sessionPanel.setVisible(false);
        }
    }
    
    private void startNewSession() {
        // This would integrate with the pathway selection system
        sessionManager.startSession("basic_computer_skills");
        startSessionButton.setText("Session Active");
        startSessionButton.setDisable(true);
        
        // Re-enable after session ends (simplified)
        Timeline enableTimer = new Timeline(new KeyFrame(Duration.seconds(5), 
            e -> {
                startSessionButton.setText("Start Learning Session");
                startSessionButton.setDisable(false);
            }));
        enableTimer.play();
    }
    
    private void showDetailedAnalytics() {
        // This would open a detailed analytics view
        Alert analyticsAlert = new Alert(Alert.AlertType.INFORMATION);
        analyticsAlert.setTitle("Detailed Analytics");
        analyticsAlert.setHeaderText("Learning Analytics Summary");
        
        LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
        String content = String.format(
            "Learning Velocity: %.1f\n" +
            "Consistency Score: %.1f\n" +
            "Retention Rate: %.1f\n" +
            "Performance Trend: %s",
            insights.getLearningVelocity() * 100,
            insights.getConsistencyScore() * 100,
            insights.getRetentionRate() * 100,
            insights.getOverallTrend()
        );
        
        analyticsAlert.setContentText(content);
        analyticsAlert.showAndWait();
    }
    
    // Utility methods
    private String getTimeOfDayGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Good Morning";
        else if (hour < 17) return "Good Afternoon";
        else return "Good Evening";
    }
    
    private String formatLearningStyle(UserProfileManager.LearningStyle style) {
        switch (style) {
            case VISUAL: return "🎨 Visual Learner";
            case AUDITORY: return "🎵 Auditory Learner";
            case KINESTHETIC: return "✋ Hands-On Learner";
            case BALANCED: return "⚖️ Balanced Approach";
            default: return "Adaptive";
        }
    }
    
    private String formatSkillLevel(UserProfileManager.SkillLevel level) {
        switch (level) {
            case BEGINNER: return "🌱 Beginner";
            case INTERMEDIATE: return "🌿 Intermediate";
            case ADVANCED: return "🌳 Advanced";
            case EXPERT: return "🏆 Expert";
            default: return "Learning";
        }
    }
    
    private Color getProgressColor(double completionRate) {
        if (completionRate < 0.3) return Color.CORAL;
        else if (completionRate < 0.7) return Color.GOLD;
        else return Color.LIGHTGREEN;
    }
    
    /**
     * Inner class for real-time session progress display
     */
    private class SessionProgressPanel extends VBox {
        private ProgressBar sessionProgressBar;
        private Label sessionStatusLabel;
        private Label timeElapsedLabel;
        private Label stepsCompletedLabel;
        
        public SessionProgressPanel() {
            this.setSpacing(10);
            this.setPadding(new Insets(15));
            this.setStyle("-fx-background-color: #e8f5e8; -fx-background-radius: 10; -fx-border-color: #4CAF50; -fx-border-radius: 10;");
            
            Label title = new Label("🎯 Current Session Progress");
            title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            title.setTextFill(Color.DARKGREEN);
            
            sessionProgressBar = new ProgressBar(0.0);
            sessionProgressBar.setPrefWidth(400);
            sessionProgressBar.setStyle("-fx-accent: #4CAF50;");
            
            sessionStatusLabel = new Label("Ready to start...");
            timeElapsedLabel = new Label("Time: 0:00");
            stepsCompletedLabel = new Label("Steps: 0/0");
            
            HBox infoBox = new HBox(20);
            infoBox.setAlignment(Pos.CENTER);
            infoBox.getChildren().addAll(timeElapsedLabel, stepsCompletedLabel, sessionStatusLabel);
            
            this.getChildren().addAll(title, sessionProgressBar, infoBox);
        }
        
        public void updateProgress(SessionManager.SessionProgress progress) {
            if (progress != null) {
                sessionProgressBar.setProgress(progress.getCompletionRate());
                
                long minutes = progress.getElapsedTime() / 60000;
                long seconds = (progress.getElapsedTime() % 60000) / 1000;
                timeElapsedLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
                
                stepsCompletedLabel.setText(String.format("Steps: %d/%d", 
                    progress.getStepsCompleted(), progress.getTotalSteps()));
                
                sessionStatusLabel.setText(String.format("%.0f%% Complete", 
                    progress.getCompletionRate() * 100));
            }
        }
    }
}
