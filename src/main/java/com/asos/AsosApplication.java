package com.asos;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.file.Paths;
import java.util.List;

/**
 * Main application class for Asos - the offline interactive learning agent
 * Enhanced with advanced analytics and personalization capabilities
 */
public class AsosApplication {
    
    private Stage primaryStage;
    private AsosCharacter asosCharacter;
    private LearningPathwayManager pathwayManager;
    private OSMonitorService osMonitor;
    
    // New analytics components
    private UserProfileManager profileManager;
    private LearningAnalytics analytics;
    private PersonalizationEngine personalizationEngine;
    private SessionManager sessionManager;
    
    public void start(Stage stage) {
        this.primaryStage = stage;
        
        // Initialize core components
        initializeComponents();
        
        // Setup UI
        setupUI();
        
        // Start monitoring services
        startMonitoringServices();
        
        // Show the application
        primaryStage.show();
    }
    
    private void initializeComponents() {
        // Initialize analytics and profiling system
        profileManager = new UserProfileManager();
        analytics = new LearningAnalytics(profileManager);
        personalizationEngine = new PersonalizationEngine(profileManager, analytics);
        sessionManager = new SessionManager(profileManager, analytics);
        
        // Initialize the Asos character with analytics
        asosCharacter = new AsosCharacter();
        
        // Initialize learning pathway manager with analytics integration
        pathwayManager = new LearningPathwayManager();
        
        // Initialize OS monitoring service
        osMonitor = new OSMonitorService();
        
        // Set up listeners with analytics integration
        setupAdvancedListeners();
    }
    
    private void setupAdvancedListeners() {
        // Listen for OS events and notify the learning pathway manager
        osMonitor.setFileSystemListener(event -> {
            Platform.runLater(() -> {
                pathwayManager.handleFileSystemEvent(event);
                asosCharacter.handleProgress(event);
            });
        });

        osMonitor.setProcessListener(event -> {
            Platform.runLater(() -> {
                pathwayManager.handleProcessEvent(event);
                asosCharacter.handleProgress(event);
            });
        });
        
        // Add validation listener for real-time feedback
        osMonitor.setValidationListener(validation -> {
            Platform.runLater(() -> {
                asosCharacter.handleValidationResult(validation);
            });
        });
        
        // Enhanced progress listener with analytics integration
        pathwayManager.setProgressListener(new LearningPathwayManager.ProgressListener() {
            @Override
            public void onProgressUpdate(LearningProgress progress) {
                Platform.runLater(() -> {
                    asosCharacter.updateProgress(progress);
                    
                    // Record session progress if active
                    if (sessionManager.hasActiveSession()) {
                        // Update real-time analytics
                        updateSessionAnalytics(progress);
                    }
                });
            }
            
            @Override
            public void onStepValidated(EventValidator.ValidationResponse validation) {
                Platform.runLater(() -> {
                    asosCharacter.handleValidationResult(validation);
                    
                    // Record step completion/failure
                    recordStepResult(validation);
                });
            }
            
            @Override
            public void onAdaptiveAction(String action, String reason) {
                Platform.runLater(() -> {
                    // Get personalized response based on user profile
                    String personalizedResponse = getPersonalizedResponse(action, reason);
                    asosCharacter.handleAdaptiveAction(action, personalizedResponse);
                });
            }
        });
    }
    
    /**
     * Updates session analytics based on learning progress
     */
    private void updateSessionAnalytics(LearningProgress progress) {
        // Analytics integration for real-time session tracking
        if (sessionManager.hasActiveSession()) {
            SessionManager.SessionProgress sessionProgress = sessionManager.getSessionProgress();
            if (sessionProgress != null) {
                // Could trigger adaptive recommendations
                List<String> recommendations = sessionManager.getSessionRecommendations();
                if (!recommendations.isEmpty()) {
                    asosCharacter.showRecommendations(recommendations);
                }
            }
        }
    }
    
    /**
     * Records step results for analytics
     */
    private void recordStepResult(EventValidator.ValidationResponse validation) {
        if (validation.getResult() == EventValidator.ValidationResult.SUCCESS) {
            // Record successful step completion
            sessionManager.recordStepCompletion(1, 30000, 0); // Placeholder values
        } else if (validation.getResult() == EventValidator.ValidationResult.ERROR) {
            // Record step failure
            sessionManager.recordStepFailure(1, validation.getMessage(), 30000);
        }
    }
    
    /**
     * Gets personalized response based on user profile and analytics
     */
    private String getPersonalizedResponse(String action, String reason) {
        UserProfileManager.LearningStyle style = profileManager.getLearningStyle();
        
        switch (action) {
            case "fast_learner":
                return style == UserProfileManager.LearningStyle.KINESTHETIC ? 
                    "You're a natural at learning by doing! " + reason :
                    "Excellent progress! " + reason;
            case "needs_more_support":
                return style == UserProfileManager.LearningStyle.VISUAL ?
                    "Let me show you step-by-step. " + reason :
                    "I'm here to help you through this. " + reason;
            default:
                return reason;
        }
    }    private void setupUI() {
        primaryStage.setTitle("Asos? - Your Friendly Learning Companion (Enhanced)");
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setResizable(true);
        
        // Create main layout with enhanced UI components
        BorderPane root = new BorderPane();
        
        // Create enhanced main content area with tabs/sections
        VBox mainContent = createEnhancedMainContent();
        root.setCenter(mainContent);
        
        // Add enhanced Asos character to the right side
        EnhancedAsosCharacter enhancedCharacter = new EnhancedAsosCharacter(profileManager, analytics);
        root.setRight(enhancedCharacter);
        
        // Add learning dashboard to the left side
        LearningDashboard dashboard = new LearningDashboard(profileManager, analytics, sessionManager);
        root.setLeft(dashboard);
        
        // Add real-time feedback system to the bottom
        RealTimeFeedbackSystem feedbackSystem = new RealTimeFeedbackSystem(
            profileManager, analytics, personalizationEngine);
        root.setBottom(feedbackSystem);
        
        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setScene(scene);
        
        // Handle close request
        primaryStage.setOnCloseRequest(e -> {
            stopApplication();
            Platform.exit();
        });
    }
    
    private VBox createEnhancedMainContent() {
        VBox mainContent = new VBox(20);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(30));
        
        // Enhanced welcome section
        VBox welcomeSection = createWelcomeScreen();
        
        // Analytics summary section
        VBox analyticsSection = createAnalyticsOverview();
        
        mainContent.getChildren().addAll(welcomeSection, analyticsSection);
        return mainContent;
    }
    
    private VBox createAnalyticsOverview() {
        VBox analyticsSection = new VBox(15);
        analyticsSection.setAlignment(Pos.CENTER);
        analyticsSection.setPadding(new Insets(20));
        analyticsSection.setStyle("-fx-background-color: rgba(240, 248, 255, 0.8); " +
                                "-fx-background-radius: 10; " +
                                "-fx-border-color: #e6f3ff; " +
                                "-fx-border-radius: 10; " +
                                "-fx-border-width: 1;");
        
        Label analyticsTitle = new Label("📊 Your Learning Journey");
        analyticsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2E8B57;");
        
        // Show quick stats if available
        if (analytics != null) {
            LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
            
            Label velocityLabel = new Label(String.format("Learning Velocity: %.1f%%", 
                insights.getLearningVelocity() * 100));
            velocityLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            
            Label consistencyLabel = new Label(String.format("Consistency Score: %.1f%%", 
                insights.getConsistencyScore() * 100));
            consistencyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            
            analyticsSection.getChildren().addAll(analyticsTitle, velocityLabel, consistencyLabel);
        } else {
            Label noDataLabel = new Label("Start learning to see your progress!");
            noDataLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            analyticsSection.getChildren().addAll(analyticsTitle, noDataLabel);
        }
        
        return analyticsSection;
    }
    
    private VBox createWelcomeScreen() {
        VBox welcomeBox = new VBox(20);
        welcomeBox.setAlignment(Pos.CENTER);
        welcomeBox.setPadding(new Insets(50));
        
        Label titleLabel = new Label("আছস? (Asos?)");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #2E8B57;");
        
        Label subtitleLabel = new Label("Your Friendly Offline Learning Companion");
        subtitleLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #666;");
        
        Label descriptionLabel = new Label(
            "Ready to learn computer skills step by step?\n" +
            "I'll guide you through everything, just like a patient friend!"
        );
        descriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-alignment: center;");
        descriptionLabel.setWrapText(true);
        
        Button startPythonButton = new Button("🐍 Learn Python Basics");
        startPythonButton.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
        startPythonButton.setOnAction(e -> startLearningPathway("python-basics"));
        
        Button startFileNavButton = new Button("📁 Master File Navigation");
        startFileNavButton.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
        startFileNavButton.setOnAction(e -> startLearningPathway("file-navigation"));
        
        welcomeBox.getChildren().addAll(
            titleLabel, 
            subtitleLabel, 
            descriptionLabel, 
            startPythonButton, 
            startFileNavButton
        );
        
        return welcomeBox;
    }
    
    private void startLearningPathway(String pathwayId) {
        asosCharacter.greetForPathway(pathwayId);
        pathwayManager.startPathway(pathwayId);
    }
    
    private void startMonitoringServices() {
        osMonitor.startMonitoring(
            Paths.get(System.getProperty("user.home"), "Desktop")
        );
    }
    
    private void stopApplication() {
        if (osMonitor != null) {
            osMonitor.stopMonitoring();
        }
    }
}
