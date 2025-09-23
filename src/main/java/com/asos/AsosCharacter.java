package com.asos;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * The Asos character - animated companion that guides users through learning
 * Now uses JSON-based knowledge base for dynamic responses
 */
public class AsosCharacter {
    
    private VBox characterPane;
    private Label speechBubble;
    private StackPane characterAvatar;
    private Circle face;
    private KnowledgeBaseManager knowledgeBase;
    
    public AsosCharacter() {
        this.knowledgeBase = new KnowledgeBaseManager();
        initializeCharacter();
    }
    
    private void initializeCharacter() {
        characterPane = new VBox(15);
        characterPane.setAlignment(Pos.CENTER);
        characterPane.setPadding(new Insets(20));
        characterPane.setMaxWidth(300);
        
        // Create character avatar (simple circular face)
        createCharacterAvatar();
        
        // Create speech bubble
        createSpeechBubble();
        
        characterPane.getChildren().addAll(speechBubble, characterAvatar);
        
        // Initial greeting
        updateSpeechBubble(knowledgeBase.getRandomResponse("greetings"));
    }
    
    private void createCharacterAvatar() {
        characterAvatar = new StackPane();
        
        // Main face circle
        face = new Circle(50);
        face.setFill(Color.LIGHTBLUE);
        face.setStroke(Color.DARKBLUE);
        face.setStrokeWidth(3);
        
        // Eyes
        Circle leftEye = new Circle(5);
        leftEye.setFill(Color.BLACK);
        leftEye.setTranslateX(-15);
        leftEye.setTranslateY(-10);
        
        Circle rightEye = new Circle(5);
        rightEye.setFill(Color.BLACK);
        rightEye.setTranslateX(15);
        rightEye.setTranslateY(-10);
        
        // Smile (simple arc using polygon)
        Polygon smile = new Polygon();
        smile.getPoints().addAll(new Double[]{
            -15.0, 10.0,
            0.0, 25.0,
            15.0, 10.0
        });
        smile.setFill(null);
        smile.setStroke(Color.BLACK);
        smile.setStrokeWidth(2);
        
        characterAvatar.getChildren().addAll(face, leftEye, rightEye, smile);
    }
    
    private void createSpeechBubble() {
        speechBubble = new Label();
        speechBubble.setWrapText(true);
        speechBubble.setTextAlignment(TextAlignment.CENTER);
        speechBubble.setAlignment(Pos.CENTER);
        speechBubble.setMaxWidth(280);
        speechBubble.setFont(Font.font("Helvetica", 14));
        speechBubble.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #ccc; " +
            "-fx-border-radius: 15; " +
            "-fx-background-radius: 15; " +
            "-fx-padding: 15; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2);"
        );
    }
    
    public VBox getCharacterPane() {
        return characterPane;
    }
    
    public void greetUser() {
        updateSpeechBubble(knowledgeBase.getRandomResponse("greetings"));
        animateCharacter();
    }
    
    public void greetForPathway(String pathwayId) {
        String welcomeMessage = knowledgeBase.getWelcomeMessage(pathwayId);
        updateSpeechBubble(welcomeMessage);
        animateCharacter();
    }
    
    public void updateSpeechBubble(String message) {
        speechBubble.setText(message);
        animateSpeechBubble();
    }
    
    public void handleProgress(String event) {
        // React to user progress with varied responses
        if (event.contains("ENTRY_CREATE")) {
            updateSpeechBubble(knowledgeBase.getRandomResponse("encouragement"));
        } else if (event.contains("Started")) {
            updateSpeechBubble(knowledgeBase.getRandomResponse("success"));
        }
        animateCharacter();
    }
    
    public void updateProgress(LearningProgress progress) {
        String message = switch (progress.getStatus()) {
            case WAITING -> knowledgeBase.getRandomResponse("waiting");
            case IN_PROGRESS -> knowledgeBase.getRandomResponse("encouragement");
            case COMPLETED -> knowledgeBase.getRandomResponse("success");
            case ERROR -> knowledgeBase.getRandomResponse("errors");
        };
        
        updateSpeechBubble(message + "\n\nStep " + progress.getCurrentStep() + " of " + progress.getTotalSteps());
    }
    
    public void showInstruction(String instruction) {
        updateSpeechBubble("📝 " + instruction);
        animateCharacter();
    }
    
    public void showError(String errorMessage) {
        String errorResponse = knowledgeBase.getRandomResponse("errors");
        updateSpeechBubble("Warning: " + errorResponse + "\n" + errorMessage);
        // Change face color to indicate error
        face.setFill(Color.LIGHTCORAL);
        animateCharacter();
        
        // Reset color after animation
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(2000);
                face.setFill(Color.LIGHTBLUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    public void showSuccess(String successMessage) {
        String successResponse = knowledgeBase.getRandomResponse("success");
        updateSpeechBubble("✅ " + successResponse + "\n" + successMessage);
        // Change face color to indicate success
        face.setFill(Color.LIGHTGREEN);
        animateCharacter();
        
        // Reset color after animation
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(2000);
                face.setFill(Color.LIGHTBLUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    public void showCompletion(String pathwayId) {
        String completionMessage = knowledgeBase.getCompletionMessage(pathwayId);
        updateSpeechBubble(completionMessage);
        face.setFill(Color.GOLD);
        animateCharacter();
    }
    
    private void animateCharacter() {
        // Simple bounce animation
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(300), characterAvatar);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.1);
        scaleTransition.setToY(1.1);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);
        scaleTransition.play();
    }
    
    private void animateSpeechBubble() {
        // Simple fade-in animation for speech bubble
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), speechBubble);
        fadeTransition.setFromValue(0.7);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }
    
    public void handleValidationResult(EventValidator.ValidationResponse validation) {
        if (validation.getResult() == EventValidator.ValidationResult.SUCCESS) {
            // Show positive feedback
            face.setFill(Color.LIGHTGREEN);
            updateSpeechBubble("Great job! " + validation.getMessage());
            animateCharacter();
        } else if (validation.getResult() == EventValidator.ValidationResult.ERROR) {
            // Show gentle correction
            face.setFill(Color.LIGHTYELLOW);
            String message = "Let's try again. " + validation.getMessage();
            if (validation.getSuggestion() != null) {
                message += " " + validation.getSuggestion();
            }
            updateSpeechBubble(message);
        } else if (validation.getConfidence() > 0.5) {
            // Partial match - encouraging feedback
            face.setFill(Color.LIGHTBLUE);
            updateSpeechBubble("You're on the right track! " + validation.getMessage());
        }
    }
    
    public void handleAdaptiveAction(String action, String reason) {
        switch (action) {
            case "fast_learner":
                face.setFill(Color.GOLD);
                updateSpeechBubble("Wow! You're a fast learner! Let's move on.");
                break;
            case "needs_more_support":
                face.setFill(Color.LIGHTCORAL);
                updateSpeechBubble("Let me help you with this step. " + reason);
                break;
            case "provide_encouragement":
                face.setFill(Color.LIGHTBLUE);
                updateSpeechBubble("Don't worry, you're doing great! Keep trying.");
                break;
            case "timeout_help":
                face.setFill(Color.ORANGE);
                updateSpeechBubble("Need a hint? " + reason);
                break;
            case "provide_help":
                face.setFill(Color.LIGHTCYAN);
                updateSpeechBubble("Let me guide you through this. " + reason);
                break;
            case "offer_assistance":
                face.setFill(Color.PLUM);
                updateSpeechBubble("I'm here to help! " + reason);
                break;
            default:
                updateSpeechBubble(reason);
        }
        animateCharacter();
    }
    
    public void showRecommendations(java.util.List<String> recommendations) {
        if (recommendations.isEmpty()) return;
        
        face.setFill(Color.LIGHTSTEELBLUE);
        String message = "Recommendation: " + recommendations.get(0);
        updateSpeechBubble(message);
        animateCharacter();
    }
}
