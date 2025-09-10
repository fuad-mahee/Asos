package com.asos;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
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

import java.util.List;
import java.util.Random;

/**
 * Enhanced interactive character with sophisticated animations,
 * emotional expressions, and context-aware interactions
 */
public class EnhancedAsosCharacter extends StackPane {
    
    private UserProfileManager profileManager;
    private LearningAnalytics analytics;
    
    // Character components
    private Circle characterBody;
    private Circle characterFace;
    private Circle leftEye;
    private Circle rightEye;
    private Arc smile;
    private VBox speechBubble;
    private Label speechText;
    private Region speechTail;
    
    // Animation components
    private Timeline idleAnimation;
    private Timeline breathingAnimation;
    private Timeline blinkAnimation;
    private RotateTransition bodyRotation;
    private ScaleTransition pulseAnimation;
    
    // Character state
    private CharacterMood currentMood;
    private EmotionalState emotionalState;
    private boolean isInteracting;
    private Random random;
    
    // Expression components
    private Group expressionGroup;
    private Path expressionPath;
    
    public EnhancedAsosCharacter(UserProfileManager profileManager, LearningAnalytics analytics) {
        this.profileManager = profileManager;
        this.analytics = analytics;
        this.currentMood = CharacterMood.NEUTRAL;
        this.emotionalState = new EmotionalState();
        this.random = new Random();
        
        initializeCharacter();
        setupAnimations();
        startIdleBehavior();
    }
    
    private void initializeCharacter() {
        this.setPrefSize(200, 250);
        this.setMaxSize(200, 250);
        
        // Character body (larger background circle)
        characterBody = new Circle(80);
        characterBody.setFill(createGradientFill(Color.LIGHTSTEELBLUE, Color.ALICEBLUE));
        characterBody.setStroke(Color.STEELBLUE);
        characterBody.setStrokeWidth(3);
        characterBody.setEffect(createDropShadow());
        
        // Character face
        characterFace = new Circle(60);
        characterFace.setFill(createGradientFill(Color.LIGHTYELLOW, Color.LEMONCHIFFON));
        characterFace.setStroke(Color.GOLD);
        characterFace.setStrokeWidth(2);
        
        // Eyes
        leftEye = new Circle(8);
        leftEye.setFill(Color.DARKBLUE);
        leftEye.setTranslateX(-20);
        leftEye.setTranslateY(-15);
        
        rightEye = new Circle(8);
        rightEye.setFill(Color.DARKBLUE);
        rightEye.setTranslateX(20);
        rightEye.setTranslateY(-15);
        
        // Smile
        smile = new Arc();
        smile.setCenterX(0);
        smile.setCenterY(5);
        smile.setRadiusX(25);
        smile.setRadiusY(15);
        smile.setStartAngle(0);
        smile.setLength(180);
        smile.setType(ArcType.OPEN);
        smile.setFill(null);
        smile.setStroke(Color.DARKRED);
        smile.setStrokeWidth(3);
        smile.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Expression group for additional facial features
        expressionGroup = new Group();
        expressionPath = new Path();
        expressionGroup.getChildren().add(expressionPath);
        
        // Speech bubble
        createSpeechBubble();
        
        // Assemble character
        Group characterGroup = new Group();
        characterGroup.getChildren().addAll(
            characterBody, characterFace, leftEye, rightEye, smile, expressionGroup
        );
        
        VBox characterContainer = new VBox(10);
        characterContainer.setAlignment(Pos.CENTER);
        characterContainer.getChildren().addAll(characterGroup, speechBubble);
        
        this.getChildren().add(characterContainer);
    }
    
    private void createSpeechBubble() {
        speechBubble = new VBox();
        speechBubble.setAlignment(Pos.CENTER);
        speechBubble.setPadding(new Insets(15));
        speechBubble.setMaxWidth(250);
        speechBubble.setStyle("-fx-background-color: white; " +
                            "-fx-background-radius: 15; " +
                            "-fx-border-color: #ddd; " +
                            "-fx-border-radius: 15; " +
                            "-fx-border-width: 2;");
        speechBubble.setEffect(createDropShadow());
        
        speechText = new Label("Hello! I'm Asos, your learning companion!");
        speechText.setWrapText(true);
        speechText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        speechText.setTextFill(Color.DARKBLUE);
        speechText.setAlignment(Pos.CENTER);
        
        speechBubble.getChildren().add(speechText);
        speechBubble.setVisible(false);
    }
    
    private LinearGradient createGradientFill(Color start, Color end) {
        return new LinearGradient(0, 0, 0, 1, true, null,
            new Stop(0, start), new Stop(1, end));
    }
    
    private DropShadow createDropShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(5);
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        return shadow;
    }
    
    private void setupAnimations() {
        // Idle breathing animation
        breathingAnimation = new Timeline();
        breathingAnimation.setCycleCount(Timeline.INDEFINITE);
        breathingAnimation.setAutoReverse(true);
        KeyFrame breatheIn = new KeyFrame(Duration.seconds(2),
            new KeyValue(characterBody.scaleXProperty(), 1.05),
            new KeyValue(characterBody.scaleYProperty(), 1.05),
            new KeyValue(characterFace.scaleXProperty(), 1.03),
            new KeyValue(characterFace.scaleYProperty(), 1.03));
        breathingAnimation.getKeyFrames().add(breatheIn);
        
        // Blinking animation
        blinkAnimation = new Timeline();
        blinkAnimation.setCycleCount(Timeline.INDEFINITE);
        KeyFrame blink = new KeyFrame(Duration.millis(100),
            new KeyValue(leftEye.scaleYProperty(), 0.1),
            new KeyValue(rightEye.scaleYProperty(), 0.1));
        KeyFrame openEyes = new KeyFrame(Duration.millis(200),
            new KeyValue(leftEye.scaleYProperty(), 1.0),
            new KeyValue(rightEye.scaleYProperty(), 1.0));
        KeyFrame pause = new KeyFrame(Duration.seconds(3));
        blinkAnimation.getKeyFrames().addAll(blink, openEyes, pause);
        
        // Gentle body rotation for liveliness
        bodyRotation = new RotateTransition(Duration.seconds(8), characterBody);
        bodyRotation.setFromAngle(-2);
        bodyRotation.setToAngle(2);
        bodyRotation.setAutoReverse(true);
        bodyRotation.setCycleCount(Timeline.INDEFINITE);
        
        // Pulse animation for excitement
        pulseAnimation = new ScaleTransition(Duration.millis(500), characterFace);
        pulseAnimation.setFromX(1.0);
        pulseAnimation.setFromY(1.0);
        pulseAnimation.setToX(1.15);
        pulseAnimation.setToY(1.15);
        pulseAnimation.setAutoReverse(true);
        pulseAnimation.setCycleCount(2);
    }
    
    private void startIdleBehavior() {
        breathingAnimation.play();
        blinkAnimation.play();
        bodyRotation.play();
        
        // Random idle movements
        idleAnimation = new Timeline();
        idleAnimation.setCycleCount(Timeline.INDEFINITE);
        KeyFrame idleFrame = new KeyFrame(Duration.seconds(5), e -> performRandomIdleAction());
        idleAnimation.getKeyFrames().add(idleFrame);
        idleAnimation.play();
    }
    
    private void performRandomIdleAction() {
        if (isInteracting) return;
        
        int action = random.nextInt(4);
        switch (action) {
            case 0:
                lookAround();
                break;
            case 1:
                subtleNod();
                break;
            case 2:
                adjustExpression();
                break;
            case 3:
                showThinkingExpression();
                break;
        }
    }
    
    private void lookAround() {
        Timeline lookTimeline = new Timeline();
        KeyFrame look1 = new KeyFrame(Duration.millis(500),
            new KeyValue(leftEye.translateXProperty(), -25),
            new KeyValue(rightEye.translateXProperty(), 15));
        KeyFrame look2 = new KeyFrame(Duration.millis(1000),
            new KeyValue(leftEye.translateXProperty(), -15),
            new KeyValue(rightEye.translateXProperty(), 25));
        KeyFrame center = new KeyFrame(Duration.millis(1500),
            new KeyValue(leftEye.translateXProperty(), -20),
            new KeyValue(rightEye.translateXProperty(), 20));
        lookTimeline.getKeyFrames().addAll(look1, look2, center);
        lookTimeline.play();
    }
    
    private void subtleNod() {
        Timeline nodTimeline = new Timeline();
        KeyFrame nod1 = new KeyFrame(Duration.millis(300),
            new KeyValue(characterFace.translateYProperty(), 5));
        KeyFrame nod2 = new KeyFrame(Duration.millis(600),
            new KeyValue(characterFace.translateYProperty(), 0));
        nodTimeline.getKeyFrames().addAll(nod1, nod2);
        nodTimeline.play();
    }
    
    private void adjustExpression() {
        // Subtle smile adjustment based on current analytics
        if (analytics != null) {
            LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
            double positivity = insights.getLearningVelocity() * insights.getConsistencyScore();
            
            Timeline expressionTimeline = new Timeline();
            KeyFrame adjust = new KeyFrame(Duration.millis(800),
                new KeyValue(smile.radiusXProperty(), 25 + (positivity * 10)),
                new KeyValue(smile.radiusYProperty(), 15 + (positivity * 5)));
            expressionTimeline.getKeyFrames().add(adjust);
            expressionTimeline.play();
        }
    }
    
    private void showThinkingExpression() {
        // Add thinking bubble or expression
        expressionPath.getElements().clear();
        
        // Create thought bubble dots
        Circle dot1 = new Circle(2, Color.LIGHTGRAY);
        Circle dot2 = new Circle(3, Color.LIGHTGRAY);
        Circle dot3 = new Circle(4, Color.LIGHTGRAY);
        
        dot1.setTranslateX(40);
        dot1.setTranslateY(-40);
        dot2.setTranslateX(50);
        dot2.setTranslateY(-50);
        dot3.setTranslateX(60);
        dot3.setTranslateY(-60);
        
        expressionGroup.getChildren().addAll(dot1, dot2, dot3);
        
        // Animate appearance and disappearance
        Timeline thinkTimeline = new Timeline();
        KeyFrame appear = new KeyFrame(Duration.millis(1000),
            new KeyValue(dot1.opacityProperty(), 1),
            new KeyValue(dot2.opacityProperty(), 1),
            new KeyValue(dot3.opacityProperty(), 1));
        KeyFrame disappear = new KeyFrame(Duration.millis(3000),
            new KeyValue(dot1.opacityProperty(), 0),
            new KeyValue(dot2.opacityProperty(), 0),
            new KeyValue(dot3.opacityProperty(), 0));
        
        thinkTimeline.getKeyFrames().addAll(appear, disappear);
        thinkTimeline.setOnFinished(e -> expressionGroup.getChildren().removeAll(dot1, dot2, dot3));
        thinkTimeline.play();
    }
    
    /**
     * Enhanced speech with emotional context and personalization
     */
    public void speak(String message, CharacterMood mood) {
        isInteracting = true;
        this.currentMood = mood;
        
        // Update visual appearance based on mood
        updateMoodVisuals(mood);
        
        // Personalize message based on user profile
        String personalizedMessage = personalizeMessage(message);
        
        speechText.setText(personalizedMessage);
        
        // Animate speech bubble appearance
        speechBubble.setVisible(true);
        speechBubble.setScaleX(0);
        speechBubble.setScaleY(0);
        speechBubble.setOpacity(0);
        
        Timeline bubbleAnimation = new Timeline();
        KeyFrame appear = new KeyFrame(Duration.millis(300),
            new KeyValue(speechBubble.scaleXProperty(), 1.1),
            new KeyValue(speechBubble.scaleYProperty(), 1.1),
            new KeyValue(speechBubble.opacityProperty(), 1));
        KeyFrame settle = new KeyFrame(Duration.millis(500),
            new KeyValue(speechBubble.scaleXProperty(), 1.0),
            new KeyValue(speechBubble.scaleYProperty(), 1.0));
        
        bubbleAnimation.getKeyFrames().addAll(appear, settle);
        bubbleAnimation.play();
        
        // Character expression animation
        animateCharacterExpression(mood);
        
        // Auto-hide speech bubble after delay
        Timeline hideTimeline = new Timeline();
        KeyFrame hide = new KeyFrame(Duration.seconds(5), e -> hideSpeechBubble());
        hideTimeline.getKeyFrames().add(hide);
        hideTimeline.play();
    }
    
    private String personalizeMessage(String message) {
        if (profileManager == null) return message;
        
        UserProfileManager.UserProfile profile = profileManager.getCurrentProfile();
        UserProfileManager.LearningStyle style = profile.getLearningStyle();
        
        // Add learning style-specific encouragement
        String styleEncouragement = "";
        switch (style) {
            case VISUAL:
                styleEncouragement = " 🎨 ";
                break;
            case AUDITORY:
                styleEncouragement = " 🎵 ";
                break;
            case KINESTHETIC:
                styleEncouragement = " ✋ ";
                break;
            case BALANCED:
                styleEncouragement = " ⭐ ";
                break;
        }
        
        return styleEncouragement + message;
    }
    
    private void updateMoodVisuals(CharacterMood mood) {
        Color faceColor;
        Color bodyGlow = Color.TRANSPARENT;
        
        switch (mood) {
            case HAPPY:
                faceColor = Color.LIGHTGREEN;
                bodyGlow = Color.LIGHTGREEN;
                smile.setRadiusX(30);
                smile.setRadiusY(20);
                break;
            case EXCITED:
                faceColor = Color.GOLD;
                bodyGlow = Color.GOLD;
                pulseAnimation.play();
                break;
            case ENCOURAGING:
                faceColor = Color.LIGHTBLUE;
                bodyGlow = Color.LIGHTBLUE;
                break;
            case CONCERNED:
                faceColor = Color.LIGHTYELLOW;
                smile.setRadiusX(20);
                smile.setRadiusY(10);
                break;
            case THINKING:
                faceColor = Color.LAVENDER;
                showThinkingExpression();
                break;
            case NEUTRAL:
            default:
                faceColor = Color.LIGHTYELLOW;
                break;
        }
        
        // Animate color change
        Timeline colorTimeline = new Timeline();
        KeyFrame colorChange = new KeyFrame(Duration.millis(500),
            new KeyValue(characterFace.fillProperty(), faceColor));
        colorTimeline.getKeyFrames().add(colorChange);
        colorTimeline.play();
        
        // Add glow effect for positive emotions
        if (!bodyGlow.equals(Color.TRANSPARENT)) {
            Glow glowEffect = new Glow(0.5);
            characterBody.setEffect(glowEffect);
            
            Timeline glowTimeline = new Timeline();
            KeyFrame removeGlow = new KeyFrame(Duration.seconds(2), 
                e -> characterBody.setEffect(createDropShadow()));
            glowTimeline.getKeyFrames().add(removeGlow);
            glowTimeline.play();
        }
    }
    
    private void animateCharacterExpression(CharacterMood mood) {
        Timeline expressionAnimation = new Timeline();
        
        switch (mood) {
            case HAPPY:
            case EXCITED:
                // Bounce animation
                KeyFrame bounce1 = new KeyFrame(Duration.millis(200),
                    new KeyValue(characterFace.translateYProperty(), -10));
                KeyFrame bounce2 = new KeyFrame(Duration.millis(400),
                    new KeyValue(characterFace.translateYProperty(), 0));
                expressionAnimation.getKeyFrames().addAll(bounce1, bounce2);
                break;
                
            case ENCOURAGING:
                // Gentle nod
                subtleNod();
                break;
                
            case CONCERNED:
                // Slight head tilt
                KeyFrame tilt = new KeyFrame(Duration.millis(500),
                    new KeyValue(characterFace.rotateProperty(), 10));
                KeyFrame untilt = new KeyFrame(Duration.millis(1000),
                    new KeyValue(characterFace.rotateProperty(), 0));
                expressionAnimation.getKeyFrames().addAll(tilt, untilt);
                break;
                
            case THINKING:
                // Already handled in updateMoodVisuals
                break;
                
            case NEUTRAL:
            default:
                // No special animation for neutral mood
                break;
        }
        
        if (!expressionAnimation.getKeyFrames().isEmpty()) {
            expressionAnimation.play();
        }
    }
    
    private void hideSpeechBubble() {
        Timeline hideAnimation = new Timeline();
        KeyFrame fade = new KeyFrame(Duration.millis(300),
            new KeyValue(speechBubble.opacityProperty(), 0),
            new KeyValue(speechBubble.scaleXProperty(), 0.8),
            new KeyValue(speechBubble.scaleYProperty(), 0.8));
        hideAnimation.getKeyFrames().add(fade);
        hideAnimation.setOnFinished(e -> {
            speechBubble.setVisible(false);
            isInteracting = false;
        });
        hideAnimation.play();
    }
    
    /**
     * React to learning analytics with appropriate emotional response
     */
    public void reactToAnalytics(LearningAnalytics.LearningInsights insights) {
        CharacterMood reactionMood;
        String reactionMessage;
        
        if (insights.getOverallTrend() == LearningAnalytics.PerformanceTrend.IMPROVING) {
            reactionMood = CharacterMood.EXCITED;
            reactionMessage = "Fantastic progress! You're really getting the hang of this!";
        } else if (insights.getLearningVelocity() > 0.8) {
            reactionMood = CharacterMood.HAPPY;
            reactionMessage = "Wow! You're learning so quickly!";
        } else if (insights.getConsistencyScore() < 0.4) {
            reactionMood = CharacterMood.ENCOURAGING;
            reactionMessage = "Remember, consistent practice leads to mastery. You've got this!";
        } else {
            reactionMood = CharacterMood.HAPPY;
            reactionMessage = "You're doing great! Keep up the excellent work!";
        }
        
        speak(reactionMessage, reactionMood);
    }
    
    /**
     * Show multiple recommendations with enhanced visual presentation
     */
    public void showRecommendations(List<String> recommendations) {
        if (recommendations.isEmpty()) return;
        
        String message = "💡 " + recommendations.get(0);
        if (recommendations.size() > 1) {
            message += "\n\n(Tap me for more tips!)";
        }
        
        speak(message, CharacterMood.THINKING);
    }
    
    // Enums and data classes
    public enum CharacterMood {
        NEUTRAL, HAPPY, EXCITED, ENCOURAGING, CONCERNED, THINKING
    }
    
    private static class EmotionalState {
        private double happiness = 0.5;
        private double excitement = 0.5;
        private double confidence = 0.5;
        
        // Getters and setters for emotional state tracking
        public double getHappiness() { return happiness; }
        public void setHappiness(double happiness) { this.happiness = happiness; }
        
        public double getExcitement() { return excitement; }
        public void setExcitement(double excitement) { this.excitement = excitement; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
