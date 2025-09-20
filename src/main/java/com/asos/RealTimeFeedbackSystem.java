package com.asos;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Real-time feedback system with visual validation indicators,
 * adaptive hints, and progressive guidance
 */
public class RealTimeFeedbackSystem extends VBox {
    
    private UserProfileManager profileManager;
    private LearningAnalytics analytics;
    private PersonalizationEngine personalizationEngine;
    
    // Feedback components
    private HBox feedbackContainer;
    private VBox validationPanel;
    private HBox progressIndicators;
    private VBox hintSystem;
    private StackPane achievementDisplay;
    
    // Visual indicators
    private Circle statusIndicator;
    private ProgressBar confidenceBar;
    private Label confidenceLabel;
    private Label streakCounter;
    private Group sparkleEffect;
    
    // Feedback state
    private FeedbackType currentFeedback;
    private double confidenceLevel;
    private int currentStreak;
    private List<String> recentFeedback;
    private Timeline feedbackTimer;
    
    // Animation components
    private Timeline pulseAnimation;
    private Timeline sparkleAnimation;
    private RotateTransition celebrationRotation;
    
    public RealTimeFeedbackSystem(UserProfileManager profileManager, 
                                 LearningAnalytics analytics,
                                 PersonalizationEngine personalizationEngine) {
        this.profileManager = profileManager;
        this.analytics = analytics;
        this.personalizationEngine = personalizationEngine;
        this.currentFeedback = FeedbackType.NEUTRAL;
        this.confidenceLevel = 0.5;
        this.currentStreak = 0;
        this.recentFeedback = new ArrayList<>();
        
        initializeFeedbackSystem();
        setupAnimations();
        startRealTimeMonitoring();
    }
    
    private void initializeFeedbackSystem() {
        this.setSpacing(15);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.CENTER);
        this.setMaxWidth(400);
        this.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); " +
                     "-fx-background-radius: 15; " +
                     "-fx-border-color: #e0e0e0; " +
                     "-fx-border-radius: 15; " +
                     "-fx-border-width: 2;");
        this.setEffect(createDropShadow());
        
        // Status indicator and confidence display
        createStatusIndicator();
        
        // Progress indicators
        createProgressIndicators();
        
        // Validation panel
        createValidationPanel();
        
        // Hint system
        createHintSystem();
        
        // Achievement display
        createAchievementDisplay();
        
        // Assemble components
        this.getChildren().addAll(
            feedbackContainer,
            progressIndicators,
            validationPanel,
            hintSystem,
            achievementDisplay
        );
    }
    
    private void createStatusIndicator() {
        feedbackContainer = new HBox(15);
        feedbackContainer.setAlignment(Pos.CENTER);
        
        // Status circle
        statusIndicator = new Circle(25);
        statusIndicator.setFill(createGradientFill(Color.LIGHTGRAY, Color.GRAY));
        statusIndicator.setStroke(Color.DARKGRAY);
        statusIndicator.setStrokeWidth(3);
        statusIndicator.setEffect(createGlow(Color.TRANSPARENT));
        
        // Confidence display
        VBox confidenceDisplay = new VBox(5);
        confidenceDisplay.setAlignment(Pos.CENTER);
        
        confidenceLabel = new Label("Confidence: 50%");
        confidenceLabel.setFont(Font.font("Helvetica", FontWeight.NORMAL, 14));
        confidenceLabel.setTextFill(Color.DARKBLUE);
        
        confidenceBar = new ProgressBar(0.5);
        confidenceBar.setPrefWidth(150);
        confidenceBar.setStyle("-fx-accent: #4CAF50;");
        
        confidenceDisplay.getChildren().addAll(confidenceLabel, confidenceBar);
        
        // Streak counter
        streakCounter = new Label("0");
        streakCounter.setFont(Font.font("Helvetica", FontWeight.NORMAL, 16));
        streakCounter.setTextFill(Color.ORANGE);
        
        feedbackContainer.getChildren().addAll(statusIndicator, confidenceDisplay, streakCounter);
    }
    
    private void createProgressIndicators() {
        progressIndicators = new HBox(10);
        progressIndicators.setAlignment(Pos.CENTER);
        
        // Create progress dots
        for (int i = 0; i < 5; i++) {
            Circle progressDot = new Circle(8);
            progressDot.setFill(Color.LIGHTGRAY);
            progressDot.setStroke(Color.GRAY);
            progressDot.setStrokeWidth(2);
            progressIndicators.getChildren().add(progressDot);
        }
        
        Label progressLabel = new Label("Learning Progress");
        progressLabel.setFont(Font.font("Helvetica", FontWeight.NORMAL, 12));
        progressLabel.setTextFill(Color.GRAY);
        
        VBox progressContainer = new VBox(5);
        progressContainer.setAlignment(Pos.CENTER);
        progressContainer.getChildren().addAll(progressLabel, progressIndicators);
    }
    
    private void createValidationPanel() {
        validationPanel = new VBox(10);
        validationPanel.setAlignment(Pos.CENTER);
        validationPanel.setPadding(new Insets(15));
        validationPanel.setStyle("-fx-background-color: rgba(240, 248, 255, 0.8); " +
                               "-fx-background-radius: 10; " +
                               "-fx-border-color: #e6f3ff; " +
                               "-fx-border-radius: 10; " +
                               "-fx-border-width: 1;");
        
        Label validationTitle = new Label("Real-time Analysis");
        validationTitle.setFont(Font.font("Helvetica", FontWeight.NORMAL, 14));
        validationTitle.setTextFill(Color.WHITE);
        
        // Validation indicators will be added dynamically
        validationPanel.getChildren().add(validationTitle);
        validationPanel.setVisible(false);
    }
    
    private void createHintSystem() {
        hintSystem = new VBox(8);
        hintSystem.setAlignment(Pos.CENTER);
        hintSystem.setPadding(new Insets(12));
        hintSystem.setStyle("-fx-background-color: rgba(255, 252, 230, 0.9); " +
                          "-fx-background-radius: 10; " +
                          "-fx-border-color: #fff8dc; " +
                          "-fx-border-radius: 10; " +
                          "-fx-border-width: 1;");
        
        // Removed icon for clean UI
        
        hintSystem.setVisible(false);
    }
    
    private void createAchievementDisplay() {
        achievementDisplay = new StackPane();
        achievementDisplay.setPrefSize(80, 80);
        achievementDisplay.setVisible(false);
        
        // Sparkle effect
        sparkleEffect = new Group();
        createSparkleParticles();
        
        achievementDisplay.getChildren().add(sparkleEffect);
    }
    
    private void createSparkleParticles() {
        sparkleEffect.getChildren().clear();
        Random random = new Random();
        
        for (int i = 0; i < 12; i++) {
            Polygon star = createStar();
            star.setFill(Color.GOLD);
            star.setStroke(Color.ORANGE);
            star.setStrokeWidth(1);
            
            // Random position in circle
            double angle = (360.0 / 12) * i;
            double radius = 30 + random.nextDouble() * 20;
            double x = Math.cos(Math.toRadians(angle)) * radius;
            double y = Math.sin(Math.toRadians(angle)) * radius;
            
            star.setTranslateX(x);
            star.setTranslateY(y);
            star.setScaleX(0.3 + random.nextDouble() * 0.4);
            star.setScaleY(0.3 + random.nextDouble() * 0.4);
            
            sparkleEffect.getChildren().add(star);
        }
    }
    
    private Polygon createStar() {
        Polygon star = new Polygon();
        double[] points = {
            0, -8,
            2, -2,
            8, -2,
            3, 2,
            5, 8,
            0, 4,
            -5, 8,
            -3, 2,
            -8, -2,
            -2, -2
        };
        star.getPoints().addAll(
            java.util.Arrays.stream(points).boxed().toArray(Double[]::new)
        );
        return star;
    }
    
    private LinearGradient createGradientFill(Color start, Color end) {
        return new LinearGradient(0, 0, 0, 1, true, null,
            new Stop(0, start), new Stop(1, end));
    }
    
    private DropShadow createDropShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(8);
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        return shadow;
    }
    
    private Glow createGlow(Color color) {
        Glow glow = new Glow(0.5);
        return glow;
    }
    
    private void setupAnimations() {
        // Pulse animation for status indicator
        pulseAnimation = new Timeline();
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        pulseAnimation.setAutoReverse(true);
        KeyFrame pulse = new KeyFrame(Duration.seconds(1),
            new KeyValue(statusIndicator.scaleXProperty(), 1.1),
            new KeyValue(statusIndicator.scaleYProperty(), 1.1));
        pulseAnimation.getKeyFrames().add(pulse);
        
        // Sparkle animation
        sparkleAnimation = new Timeline();
        sparkleAnimation.setCycleCount(Timeline.INDEFINITE);
        for (int i = 0; i < sparkleEffect.getChildren().size(); i++) {
            final int index = i;
            KeyFrame twinkle = new KeyFrame(Duration.millis(500 + i * 100),
                e -> animateSparkle(index));
            sparkleAnimation.getKeyFrames().add(twinkle);
        }
        
        // Celebration rotation
        celebrationRotation = new RotateTransition(Duration.seconds(2), achievementDisplay);
        celebrationRotation.setFromAngle(0);
        celebrationRotation.setToAngle(360);
        celebrationRotation.setCycleCount(3);
    }
    
    private void animateSparkle(int index) {
        if (index < sparkleEffect.getChildren().size()) {
            javafx.scene.Node sparkle = sparkleEffect.getChildren().get(index);
            
            Timeline twinkle = new Timeline();
            KeyFrame fade = new KeyFrame(Duration.millis(300),
                new KeyValue(sparkle.opacityProperty(), 0.3));
            KeyFrame bright = new KeyFrame(Duration.millis(600),
                new KeyValue(sparkle.opacityProperty(), 1.0));
            twinkle.getKeyFrames().addAll(fade, bright);
            twinkle.play();
        }
    }
    
    private void startRealTimeMonitoring() {
        feedbackTimer = new Timeline();
        feedbackTimer.setCycleCount(Timeline.INDEFINITE);
        KeyFrame monitorFrame = new KeyFrame(Duration.seconds(2), e -> updateFeedback());
        feedbackTimer.getKeyFrames().add(monitorFrame);
        feedbackTimer.play();
    }
    
    /**
     * Provide real-time feedback based on current interaction
     */
    public void provideFeedback(String userInput, boolean isCorrect, double responseTime) {
        FeedbackType newFeedback = determineFeedbackType(isCorrect, responseTime);
        updateFeedbackDisplay(newFeedback, isCorrect);
        updateConfidence(isCorrect, responseTime);
        updateStreak(isCorrect);
        
        // Show personalized hints if needed
        if (!isCorrect) {
            showPersonalizedHint(userInput);
        }
        
        // Store feedback for analytics
        storeFeedbackData(userInput, isCorrect, responseTime, newFeedback);
    }
    
    private FeedbackType determineFeedbackType(boolean isCorrect, double responseTime) {
        if (isCorrect) {
            if (responseTime < 2.0) {
                return FeedbackType.EXCELLENT;
            } else if (responseTime < 5.0) {
                return FeedbackType.GOOD;
            } else {
                return FeedbackType.CORRECT_SLOW;
            }
        } else {
            if (currentStreak > 3) {
                return FeedbackType.ENCOURAGING;
            } else {
                return FeedbackType.INCORRECT;
            }
        }
    }
    
    private void updateFeedbackDisplay(FeedbackType feedback, boolean isCorrect) {
        this.currentFeedback = feedback;
        
        Color indicatorColor;
        String feedbackText;
        
        switch (feedback) {
            case EXCELLENT:
                indicatorColor = Color.GOLD;
                feedbackText = "Excellent!";
                showAchievement();
                break;
            case GOOD:
                indicatorColor = Color.LIGHTGREEN;
                feedbackText = "✅ Well done!";
                break;
            case CORRECT_SLOW:
                indicatorColor = Color.LIGHTBLUE;
                feedbackText = "👍 Correct, but take your time to think";
                break;
            case ENCOURAGING:
                indicatorColor = Color.ORANGE;
                feedbackText = "💪 Don't give up! You're learning!";
                break;
            case INCORRECT:
                indicatorColor = Color.LIGHTCORAL;
                feedbackText = "🤔 Not quite right, let's try again";
                break;
            default:
                indicatorColor = Color.LIGHTGRAY;
                feedbackText = "Ready when you are!";
        }
        
        // Animate status indicator color change
        Timeline colorTransition = new Timeline();
        KeyFrame colorChange = new KeyFrame(Duration.millis(500),
            new KeyValue(statusIndicator.fillProperty(), indicatorColor));
        colorTransition.getKeyFrames().add(colorChange);
        colorTransition.play();
        
        // Update glow effect
        statusIndicator.setEffect(createGlow(indicatorColor));
        
        // Show feedback animation
        if (isCorrect) {
            pulseAnimation.play();
            Timeline stopPulse = new Timeline();
            KeyFrame stop = new KeyFrame(Duration.seconds(2), e -> pulseAnimation.stop());
            stopPulse.getKeyFrames().add(stop);
            stopPulse.play();
        }
        
        // Update validation panel
        updateValidationPanel(feedbackText, feedback);
    }
    
    private void updateValidationPanel(String feedbackText, FeedbackType feedback) {
        validationPanel.getChildren().clear();
        
        Label validationTitle = new Label("Real-time Analysis");
        validationTitle.setFont(Font.font("Helvetica", FontWeight.NORMAL, 14));
        validationTitle.setTextFill(Color.WHITE);
        
        Label feedbackLabel = new Label(feedbackText);
        feedbackLabel.setFont(Font.font("Helvetica", FontWeight.NORMAL, 13));
        feedbackLabel.setTextFill(Color.WHITE);
        
        // Add analytical insights
        if (analytics != null) {
            LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
            Label velocityLabel = new Label(String.format("Learning Velocity: %.1f%%", 
                insights.getLearningVelocity() * 100));
            velocityLabel.setFont(Font.font("Helvetica", FontWeight.NORMAL, 11));
            velocityLabel.setTextFill(Color.WHITE);
            
            validationPanel.getChildren().addAll(validationTitle, feedbackLabel, velocityLabel);
        } else {
            validationPanel.getChildren().addAll(validationTitle, feedbackLabel);
        }
        
        validationPanel.setVisible(true);
        
        // Auto-hide after delay
        Timeline hidePanel = new Timeline();
        KeyFrame hide = new KeyFrame(Duration.seconds(4), e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(500), validationPanel);
            fade.setToValue(0);
            fade.setOnFinished(event -> validationPanel.setVisible(false));
            fade.play();
        });
        hidePanel.getKeyFrames().add(hide);
        hidePanel.play();
    }
    
    private Color getFeedbackColor(FeedbackType feedback) {
        switch (feedback) {
            case EXCELLENT:
                return Color.GOLD;
            case GOOD:
                return Color.GREEN;
            case CORRECT_SLOW:
                return Color.BLUE;
            case ENCOURAGING:
                return Color.ORANGE;
            case INCORRECT:
                return Color.CRIMSON;
            default:
                return Color.GRAY;
        }
    }
    
    private void updateConfidence(boolean isCorrect, double responseTime) {
        if (isCorrect) {
            // Boost confidence based on speed and current level
            double boost = 0.1;
            if (responseTime < 3.0) boost = 0.15;
            confidenceLevel = Math.min(1.0, confidenceLevel + boost);
        } else {
            // Decrease confidence, but not too harshly
            confidenceLevel = Math.max(0.1, confidenceLevel - 0.05);
        }
        
        // Animate confidence bar
        Timeline confidenceAnimation = new Timeline();
        KeyFrame updateBar = new KeyFrame(Duration.millis(800),
            new KeyValue(confidenceBar.progressProperty(), confidenceLevel));
        confidenceAnimation.getKeyFrames().add(updateBar);
        confidenceAnimation.play();
        
        // Update label
        confidenceLabel.setText(String.format("Confidence: %.0f%%", confidenceLevel * 100));
        
        // Update bar color based on confidence level
        if (confidenceLevel > 0.8) {
            confidenceBar.setStyle("-fx-accent: #4CAF50;"); // Green
        } else if (confidenceLevel > 0.5) {
            confidenceBar.setStyle("-fx-accent: #FF9800;"); // Orange
        } else {
            confidenceBar.setStyle("-fx-accent: #F44336;"); // Red
        }
    }
    
    private void updateStreak(boolean isCorrect) {
        if (isCorrect) {
            currentStreak++;
            streakCounter.setText("" + currentStreak);
            
            // Animate streak counter for milestones
            if (currentStreak % 5 == 0) {
                ScaleTransition streakPulse = new ScaleTransition(Duration.millis(300), streakCounter);
                streakPulse.setFromX(1.0);
                streakPulse.setFromY(1.0);
                streakPulse.setToX(1.3);
                streakPulse.setToY(1.3);
                streakPulse.setAutoReverse(true);
                streakPulse.setCycleCount(2);
                streakPulse.play();
            }
        } else {
            currentStreak = 0;
            streakCounter.setText("0");
        }
    }
    
    private void showPersonalizedHint(String userInput) {
        if (personalizationEngine == null) return;
        
        // Create a simple learning step for hint generation
        LearningStep step = new LearningStep(
            1, "Solve the current problem", "correct_answer", "Well done!"
        );
        
        List<String> hints = personalizationEngine.generatePersonalizedHints(step, 1, 5000);
        String hint = hints.isEmpty() ? "Take your time and think step by step!" : hints.get(0);
        
        hintSystem.getChildren().clear();
        
        // Removed icon for clean UI
        
        Label hintText = new Label(hint);
        hintText.setWrapText(true);
        hintText.setFont(Font.font("Helvetica", FontWeight.NORMAL, 12));
        hintText.setTextFill(Color.WHITE);
        hintText.setMaxWidth(300);
        
        hintSystem.getChildren().addAll(hintText);
        hintSystem.setVisible(true);
        
        // Animate hint appearance
        hintSystem.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), hintSystem);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        
        // Auto-hide hint after reading time
        Timeline hideHint = new Timeline();
        KeyFrame hide = new KeyFrame(Duration.seconds(8), e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), hintSystem);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(event -> hintSystem.setVisible(false));
            fadeOut.play();
        });
        hideHint.getKeyFrames().add(hide);
        hideHint.play();
    }
    
    private void showAchievement() {
        achievementDisplay.setVisible(true);
        sparkleAnimation.play();
        celebrationRotation.play();
        
        // Auto-hide achievement display
        Timeline hideAchievement = new Timeline();
        KeyFrame hide = new KeyFrame(Duration.seconds(3), e -> {
            achievementDisplay.setVisible(false);
            sparkleAnimation.stop();
        });
        hideAchievement.getKeyFrames().add(hide);
        hideAchievement.play();
    }
    
    private void updateFeedback() {
        // Periodic updates based on learning analytics
        if (analytics != null) {
            LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
            
            // Update progress indicators based on recent performance
            updateProgressDots(insights.getOverallTrend());
            
            // Adjust confidence based on analytics
            if (insights.getConsistencyScore() > 0.8) {
                confidenceLevel = Math.min(1.0, confidenceLevel + 0.02);
            }
        }
    }
    
    private void updateProgressDots(LearningAnalytics.PerformanceTrend trend) {
        Color dotColor;
        switch (trend) {
            case IMPROVING:
                dotColor = Color.LIGHTGREEN;
                break;
            case DECLINING:
                dotColor = Color.LIGHTCORAL;
                break;
            case STABLE:
            default:
                dotColor = Color.LIGHTBLUE;
                break;
        }
        
        // Animate progress dots
        for (int i = 0; i < progressIndicators.getChildren().size(); i++) {
            Circle dot = (Circle) progressIndicators.getChildren().get(i);
            Timeline dotAnimation = new Timeline();
            KeyFrame colorChange = new KeyFrame(Duration.millis(200 + i * 100),
                new KeyValue(dot.fillProperty(), dotColor));
            dotAnimation.getKeyFrames().add(colorChange);
            dotAnimation.play();
        }
    }
    
    private void storeFeedbackData(String userInput, boolean isCorrect, 
                                 double responseTime, FeedbackType feedback) {
        recentFeedback.add(String.format("Input: %s, Correct: %b, Time: %.1fs, Type: %s",
            userInput, isCorrect, responseTime, feedback));
        
        // Keep only recent feedback (last 10 entries)
        if (recentFeedback.size() > 10) {
            recentFeedback.remove(0);
        }
    }
    
    /**
     * Get current confidence level
     */
    public double getConfidenceLevel() {
        return confidenceLevel;
    }
    
    /**
     * Get current streak
     */
    public int getCurrentStreak() {
        return currentStreak;
    }
    
    /**
     * Get recent feedback for analytics
     */
    public List<String> getRecentFeedback() {
        return new ArrayList<>(recentFeedback);
    }
    
    // Enums
    public enum FeedbackType {
        EXCELLENT, GOOD, CORRECT_SLOW, ENCOURAGING, INCORRECT, NEUTRAL
    }
}
