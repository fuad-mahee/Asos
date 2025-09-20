package com.asos;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;

/**
 * Main application class for Asos - the offline interactive learning agent
 * Enhanced with advanced analytics and personalization capabilities
 */
public class AsosApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(AsosApplication.class);
    
    // UI Components
    private Stage primaryStage;
    private Stage menuStage;
    private AsosCharacter asosCharacter;
    private LearningPathwayManager pathwayManager;
    private OSMonitorService osMonitor;
    // UI Components
    private VBox cornerBox;
    
    // Notification system components
    private VBox notificationBox;
    private Label notificationLabel;
    private boolean notificationVisible = false;
    
    // Live instruction display components
    private VBox instructionBox;
    private Label instructionLabel;
    private String currentDetailedInstruction = "";
    private Stage instructionDetailStage;
    private Stage instructionBoxStage; // Separate stage for instruction box
    
    // Study monitoring components  
    private FileSystemMonitor fileSystemMonitor;
    private ChunkTeachingEngine teachingEngine;
    private boolean teachingModeActive = false;
    private Timeline notificationTimeline;
    private double currentProgress = 0.0;
    
    // New analytics components
    private UserProfileManager profileManager;
    private LearningAnalytics analytics;
    private PersonalizationEngine personalizationEngine;
    private SessionManager sessionManager;
    
    // AI components
    private ModelManager modelManager;
    private LocalAIEngine aiEngine;
    private IntelligentLearningAssistant aiAssistant;
    private ConversationalInterface conversationalInterface;
    
    public void start(Stage stage) {
        this.primaryStage = stage;
        
        // Initialize core components
        initializeComponents();
        
        // Setup modern corner UI
        setupCornerUI();
        
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
        
        // Initialize AI components
        modelManager = new ModelManager();
        aiEngine = new LocalAIEngine(); // Auto-initializes with downloaded model if available
        aiAssistant = new IntelligentLearningAssistant(aiEngine, profileManager, analytics);
        conversationalInterface = new ConversationalInterface(aiAssistant);
        
        // Initialize Teaching Engine
        teachingEngine = new ChunkTeachingEngine();
        setupTeachingEngineCallbacks();
        
        // Log AI initialization status
        logAIInitializationStatus();
        
        // Initialize the Asos character with analytics
        asosCharacter = new AsosCharacter();
        
        // Initialize learning pathway manager with analytics integration
        pathwayManager = new LearningPathwayManager();
        
        // Initialize OS monitoring service
        osMonitor = new OSMonitorService();
        
        // Set up listeners with analytics integration
        setupAdvancedListeners();
    }
    
    /**
     * Setup callbacks for the teaching engine
     */
    private void setupTeachingEngineCallbacks() {
        teachingEngine.setOnInstructionUpdate((instruction, detailedInstruction) -> {
            // Show instruction in the live display instead of notification
            updateLiveInstruction(instruction, detailedInstruction);
        });
        
        teachingEngine.setOnHintProvided(hint -> {
            Platform.runLater(() -> showNotification("💡 Hint: " + hint));
        });
        
        teachingEngine.setOnErrorDetected(error -> {
            Platform.runLater(() -> showNotification("⚠️ " + error));
        });
        
        teachingEngine.setOnChunkCompleted(message -> {
            Platform.runLater(() -> {
                showNotification("✅ " + message);
                // Hide live instruction when chunk is completed
                hideLiveInstruction();
            });
        });
        
        teachingEngine.setOnProgressUpdate(progress -> {
            Platform.runLater(() -> {
                currentProgress = progress;
                showNotification(String.format("Progress: %.0f%%", progress * 100));
            });
        });
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
    }
    
    private void setupCornerUI() {
        // Create a small corner window
        primaryStage.setTitle("Asos");
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setResizable(false);
        
        // Create the corner box content and assign to class field
        cornerBox = createCornerBox();
        
        // Create separate instruction box
        createSeparateInstructionBox();
        
        Scene scene = new Scene(cornerBox, 220, 110); // Reduced height since instruction box is separate
        scene.setFill(null); // Transparent background
        
        // Load dark theme CSS
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        
        primaryStage.setScene(scene);
        
        // Position in lower right corner after stage is shown
        primaryStage.setOnShown(e -> positionWindowInCorner());
        
        // Handle close request
        primaryStage.setOnCloseRequest(e -> {
            stopApplication();
            Platform.exit();
        });
    }
    
    private VBox createCornerBox() {
        // Create notification box
        notificationBox = new VBox(5);
        notificationBox.setAlignment(Pos.CENTER);
        notificationBox.setPadding(new Insets(8));
        notificationBox.getStyleClass().add("notification-box");
        notificationBox.setVisible(false); // Initially hidden
        
        notificationLabel = new Label();
        notificationLabel.getStyleClass().add("notification-text");
        notificationLabel.setWrapText(true);
        notificationLabel.setMaxWidth(300);
        
        // Close button for notifications
        Button closeNotificationBtn = new Button("×");
        closeNotificationBtn.getStyleClass().add("notification-close");
        closeNotificationBtn.setOnAction(e -> hideNotification());
        
        HBox notificationHeader = new HBox();
        notificationHeader.setAlignment(Pos.CENTER_RIGHT);
        notificationHeader.getChildren().add(closeNotificationBtn);
        
        notificationBox.getChildren().addAll(notificationHeader, notificationLabel);
        
        // Create main corner box
        VBox mainCornerBox = new VBox(5);
        mainCornerBox.setAlignment(Pos.CENTER);
        mainCornerBox.setPadding(new Insets(12));
        mainCornerBox.getStyleClass().add("corner-box");
        
        // Main text
        Label statusLabel = new Label("Asos? is here");
        statusLabel.getStyleClass().add("corner-text");
        
        // Three dots menu button
        Button menuButton = new Button("⋮");
        menuButton.getStyleClass().add("menu-button");
        menuButton.setOnAction(e -> {
            System.out.println("Menu button clicked!"); // Debug output
            showMainMenu();
        });
        
        // Layout with proper spacing
        VBox textSection = new VBox(3);
        textSection.setAlignment(Pos.CENTER);
        textSection.getChildren().add(statusLabel);
        
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(textSection);
        
        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getChildren().add(menuButton);
        
        mainCornerBox.getChildren().addAll(topRow, buttonRow);
        
        // Combine only notification and corner box in a container
        VBox container = new VBox(5);
        container.setAlignment(Pos.BOTTOM_RIGHT);
        container.getChildren().addAll(notificationBox, mainCornerBox);
        
        return container;
    }
    
    /**
     * Create and manage separate instruction box window
     */
    private void createSeparateInstructionBox() {
        // Create instruction box content
        instructionBox = new VBox(5);
        instructionBox.setAlignment(Pos.CENTER);
        instructionBox.setPadding(new Insets(12));
        instructionBox.getStyleClass().add("instruction-box");
        instructionBox.setMinWidth(220); // Match corner box width
        instructionBox.setMaxWidth(220);
        instructionBox.setPrefWidth(220);
        
        // Create language selection buttons
        VBox languageSelectionBox = createLanguageSelectionBox();
        
        instructionLabel = new Label();
        instructionLabel.getStyleClass().add("instruction-text");
        instructionLabel.setWrapText(true);
        instructionLabel.setMaxWidth(200);
        instructionLabel.setMinHeight(Region.USE_PREF_SIZE);
        
        // Make instruction box clickable for detailed view
        instructionBox.setOnMouseClicked(e -> showDetailedInstruction());
        instructionBox.getStyleClass().add("clickable");
        
        instructionBox.getChildren().addAll(languageSelectionBox, instructionLabel);
        
        // Create separate stage for instruction box
        instructionBoxStage = new Stage();
        instructionBoxStage.initStyle(StageStyle.UNDECORATED);
        instructionBoxStage.initOwner(primaryStage);
        instructionBoxStage.setAlwaysOnTop(true);
        
        Scene instructionScene = new Scene(instructionBox);
        instructionScene.setFill(null); // Transparent background
        instructionScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        
        instructionBoxStage.setScene(instructionScene);
        
        // Initially hidden
        instructionBoxStage.hide();
    }
    
    /**
     * Create language selection buttons with progress indicators
     */
    private VBox createLanguageSelectionBox() {
        VBox languageBox = new VBox(3);
        languageBox.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("Select Learning Path");
        titleLabel.getStyleClass().add("language-title");
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        // Java button with progress
        Button javaButton = createLanguageButton("Java", "java");
        
        // Python button with progress  
        Button pythonButton = createLanguageButton("Python", "python");
        
        // C++ button with progress
        Button cppButton = createLanguageButton("C++", "cpp");
        
        languageBox.getChildren().addAll(titleLabel, javaButton, pythonButton, cppButton);
        return languageBox;
    }
    
    /**
     * Create a language selection button with progress indicator
     */
    private Button createLanguageButton(String displayName, String languageKey) {
        Button button = new Button();
        button.setPrefWidth(190);
        button.setPrefHeight(35);
        button.getStyleClass().add("language-button");
        
        // Get progress information
        LearningProgressManager progressManager = new LearningProgressManager();
        LearningProgressStorage storage = new LearningProgressStorage();
        progressManager = storage.loadProgress();
        
        int currentChunk = progressManager.getCurrentChunkForLanguage(languageKey);
        int totalChunks = getTotalChunksForLanguage(languageKey);
        double progressPercent = (double) currentChunk / totalChunks * 100;
        
        String progressText = currentChunk > 0 ? 
            String.format("%s (%.0f%%)", displayName, progressPercent) : 
            displayName;
            
        button.setText(progressText);
        
        button.setOnAction(e -> startLanguageLearning(languageKey));
        
        return button;
    }
    
    /**
     * Get total number of chunks for a language
     */
    private int getTotalChunksForLanguage(String language) {
        switch (language.toLowerCase()) {
            case "java": return 6;
            case "python": return 7; 
            case "cpp": return 8;
            default: return 1;
        }
    }
    
    /**
     * Start learning for selected language
     */
    private void startLanguageLearning(String language) {
        try {
            String moduleFile = String.format("/learning-modules/%s-complete.json", language);
            if (teachingEngine != null) {
                teachingEngine.loadLearningModule(moduleFile);
                instructionLabel.setText("Loading " + language.toUpperCase() + " course...");
            }
        } catch (Exception e) {
            logger.error("Failed to start {} learning", language, e);
            instructionLabel.setText("Error loading " + language + " course");
        }
    }

    /**
     * Position the instruction box above the corner box
     */
    private void positionInstructionBox() {
        if (instructionBoxStage != null && primaryStage != null) {
            Platform.runLater(() -> {
                // Ensure instruction box stage is sized
                instructionBoxStage.sizeToScene();
                
                double cornerX = primaryStage.getX();
                double cornerY = primaryStage.getY();
                
                // Position instruction box above corner box with spacing
                instructionBoxStage.setX(cornerX);
                instructionBoxStage.setY(cornerY - instructionBoxStage.getHeight() - 20); // 20px spacing above corner box
            });
        }
    }
    
    // Notification system methods
    public void showNotification(String message) {
        Platform.runLater(() -> {
            notificationLabel.setText(message);
            notificationBox.setVisible(true);
            notificationVisible = true;
            
            // Auto-hide after 10 seconds for non-critical messages
            if (!message.toLowerCase().contains("error") && !message.toLowerCase().contains("mistake")) {
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> hideNotification()));
                timeline.play();
            }
        });
    }
    
    public void hideNotification() {
        Platform.runLater(() -> {
            notificationBox.setVisible(false);
            notificationVisible = false;
        });
    }
    
    // Live instruction display methods
    public void showLiveInstruction(String instruction) {
        showLiveInstruction(instruction, instruction); // Use same text for short and detailed
    }
    
    public void showLiveInstruction(String shortInstruction, String detailedInstruction) {
        Platform.runLater(() -> {
            instructionLabel.setText("📝 " + shortInstruction);
            currentDetailedInstruction = detailedInstruction;
            
            // Show and position the separate instruction box
            if (instructionBoxStage != null) {
                instructionBoxStage.show();
                positionInstructionBox();
            }
        });
    }
    
    public void hideLiveInstruction() {
        Platform.runLater(() -> {
            if (instructionBoxStage != null) {
                instructionBoxStage.hide();
            }
            instructionLabel.setText("");
            currentDetailedInstruction = "";
        });
    }
    
    public void updateLiveInstruction(String instruction) {
        updateLiveInstruction(instruction, instruction); // Use same text for short and detailed
    }
    
    public void updateLiveInstruction(String shortInstruction, String detailedInstruction) {
        Platform.runLater(() -> {
            instructionLabel.setText("📝 " + shortInstruction);
            currentDetailedInstruction = detailedInstruction;
            
            // Show and position the separate instruction box
            if (instructionBoxStage != null) {
                if (!instructionBoxStage.isShowing()) {
                    instructionBoxStage.show();
                }
                positionInstructionBox();
            }
        });
    }
    
    // Detailed instruction window
    private void showDetailedInstruction() {
        if (currentDetailedInstruction.isEmpty()) return;
        
        Platform.runLater(() -> {
            // Close existing detail window if open
            if (instructionDetailStage != null && instructionDetailStage.isShowing()) {
                instructionDetailStage.close();
            }
            
            // Create detail window
            instructionDetailStage = new Stage();
            instructionDetailStage.initStyle(StageStyle.UNDECORATED); // Remove window controls
            instructionDetailStage.initOwner(primaryStage);
            instructionDetailStage.setAlwaysOnTop(true);
            
            VBox detailBox = createDetailedInstructionBox();
            Scene detailScene = new Scene(detailBox, 400, 300);
            detailScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
            
            instructionDetailStage.setScene(detailScene);
            
            // Center the window on screen
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double centerX = (screenBounds.getWidth() - 400) / 2; // 400 is window width
            double centerY = (screenBounds.getHeight() - 300) / 2; // 300 is window height
            
            instructionDetailStage.setX(centerX);
            instructionDetailStage.setY(centerY);
            
            instructionDetailStage.show();
        });
    }
    
    private VBox createDetailedInstructionBox() {
        VBox detailBox = new VBox(15);
        detailBox.setAlignment(Pos.TOP_LEFT);
        detailBox.setPadding(new Insets(20));
        detailBox.getStyleClass().add("instruction-detail-window");
        
        // Header with back button
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Button backButton = new Button("←");
        backButton.getStyleClass().add("instruction-detail-close");
        backButton.setOnAction(e -> {
            if (instructionDetailStage != null) {
                instructionDetailStage.close();
            }
        });
        
        Label headerLabel = new Label("Step Guide");
        headerLabel.getStyleClass().add("instruction-detail-header");
        
        headerBox.getChildren().addAll(backButton, headerLabel);
        
        // Detailed instruction content
        Label detailLabel = new Label(currentDetailedInstruction);
        detailLabel.getStyleClass().add("instruction-detail-text");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(350);
        
        detailBox.getChildren().addAll(headerBox, detailLabel);
        
        return detailBox;
    }
    
    // Study monitoring methods
    private void initializeStudyMonitoring() {
        // Initialize file system monitoring
        fileSystemMonitor = new FileSystemMonitor();
        fileSystemMonitor.setOnFileChange(this::analyzeFileChange);
        fileSystemMonitor.start();
        
        // Show initial greeting
        showNotification("👋 Asos Study Assistant is now monitoring your coding activities!");
        
        // Test the live instruction display after a short delay
        Timeline testInstruction = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            showLiveInstruction("Click the menu (⋮) and select 'Start Java Tutorial' to begin!");
        }));
        testInstruction.play();
    }
    
    private void analyzeFileChange(String filePath, String content) {
        // Use AI-powered analysis instead of just pattern matching
        if (aiEngine.isReady()) {
            analyzeWithAI(filePath, content);
        } else {
            // Fallback to rule-based analysis
            analyzeWithRules(filePath, content);
        }
    }
    
    /**
     * AI-powered code analysis using Gemma model
     */
    private void analyzeWithAI(String filePath, String content) {
        try {
            String analysisPrompt = buildCodeAnalysisPrompt(filePath, content);
            
            // Create AI context for code analysis
            LocalAIEngine.AIContext context = new LocalAIEngine.AIContext("beginner", "code-analysis");
            context.addMetadata("language", getLanguageFromExtension(filePath));
            context.addMetadata("file", filePath);
            
            aiEngine.generateResponseAsync(analysisPrompt, context).thenAccept(response -> {
                if (response.isSuccess()) {
                    Platform.runLater(() -> {
                        showNotification("🤖 AI Feedback: " + response.getText());
                    });
                }
            });
            
        } catch (Exception e) {
            logger.error("AI analysis failed, falling back to rules", e);
            analyzeWithRules(filePath, content);
        }
    }
    
    /**
     * Build AI prompt for code analysis
     */
    private String buildCodeAnalysisPrompt(String filePath, String content) {
        String language = getLanguageFromExtension(filePath);
        
        return String.format(
            "As a coding tutor, analyze this %s code and provide helpful feedback for a beginner:\n\n" +
            "File: %s\n" +
            "Code:\n%s\n\n" +
            "Please provide:\n" +
            "1. Any syntax errors or typos\n" +
            "2. Best practice suggestions\n" +
            "3. Learning tips\n" +
            "Keep feedback concise and encouraging (max 100 words).",
            language, getFileName(filePath), content
        );
    }
    
    /**
     * Fallback rule-based analysis
     */
    private void analyzeWithRules(String filePath, String content) {
        // Analyze different file types
        if (filePath.endsWith(".java")) {
            analyzeJavaCode(filePath, content);
        } else if (filePath.endsWith(".py")) {
            analyzePythonCode(filePath, content);
        } else if (filePath.endsWith(".js") || filePath.endsWith(".ts")) {
            analyzeJavaScriptCode(filePath, content);
        } else if (filePath.endsWith(".json")) {
            analyzeJsonFile(filePath, content);
        }
    }
    
    private void analyzeJavaCode(String filePath, String content) {
        // Basic Java code analysis
        if (content.contains("System.out.println") && !content.contains("// Debug") && !content.contains("// TODO")) {
            showNotification("💡 Consider using a logger instead of System.out.println for better debugging in: " + getFileName(filePath));
        }
        
        if (content.contains("public static void main") && !content.contains("Scanner")) {
            if (content.contains("nextInt()") || content.contains("next()")) {
                showNotification("⚠️ Remember to close Scanner resources to avoid memory leaks!");
            }
        }
        
        // Check for missing exception handling
        if (content.contains("FileReader") || content.contains("BufferedReader")) {
            if (!content.contains("try") && !content.contains("throws")) {
                showNotification("❗ File operations should include exception handling (try-catch or throws)");
            }
        }
        
        // Check for infinite loops
        if (content.contains("while(true)") && !content.contains("break")) {
            showNotification("⚠️ Potential infinite loop detected! Make sure you have a break condition.");
        }
    }
    
    private void analyzePythonCode(String filePath, String content) {
        // Basic Python code analysis
        if (content.contains("print(") && content.split("print\\(").length > 5) {
            showNotification("💡 Consider using logging module instead of multiple print statements in: " + getFileName(filePath));
        }
        
        if (content.contains("open(") && !content.contains("with ")) {
            showNotification("📝 Consider using 'with' statement for file operations to ensure proper resource management");
        }
        
        // Check for common mistakes
        if (content.contains("== None") || content.contains("!= None")) {
            showNotification("💡 Use 'is None' or 'is not None' instead of '== None' in Python");
        }
    }
    
    private void analyzeJavaScriptCode(String filePath, String content) {
        // Basic JavaScript/TypeScript analysis
        if (content.contains("var ") && (filePath.contains("2020") || filePath.contains("2021") || filePath.contains("2022") || filePath.contains("2023") || filePath.contains("2024") || filePath.contains("2025"))) {
            showNotification("💡 Consider using 'let' or 'const' instead of 'var' for better scoping in: " + getFileName(filePath));
        }
        
        if (content.contains("console.log") && content.split("console\\.log").length > 3) {
            showNotification("🔍 Multiple console.log statements detected. Consider using a proper logging framework");
        }
    }
    
    private void analyzeJsonFile(String filePath, String content) {
        // Basic JSON validation
        try {
            // Simple bracket matching
            int openBrackets = content.length() - content.replace("{", "").length();
            int closeBrackets = content.length() - content.replace("}", "").length();
            
            if (openBrackets != closeBrackets) {
                showNotification("❗ JSON syntax error: Mismatched brackets in " + getFileName(filePath));
            }
        } catch (Exception e) {
            showNotification("❗ Potential JSON formatting issue in " + getFileName(filePath));
        }
    }
    
    private String getFileName(String fullPath) {
        return fullPath.substring(fullPath.lastIndexOf("/") + 1);
    }
    
    private void positionWindowInCorner() {
        // Wait for the stage to be properly initialized
        Platform.runLater(() -> {
            // Get screen dimensions
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            
            // Ensure stage dimensions are set
            primaryStage.sizeToScene();
            
            // Calculate position for lower-right corner with margin
            double windowWidth = primaryStage.getWidth();
            double windowHeight = primaryStage.getHeight();
            
            // Position in lower right corner with 20px margin from edges
            double x = screenBounds.getMaxX() - windowWidth - 20;
            double y = screenBounds.getMaxY() - windowHeight - 60; // Extra margin from bottom for dock/taskbar
            
            primaryStage.setX(x);
            primaryStage.setY(y);
            
            // Also position the instruction box if it exists
            if (instructionBoxStage != null) {
                positionInstructionBox();
            }
            
            System.out.println("Positioning window at: x=" + x + ", y=" + y);
            System.out.println("Screen bounds: " + screenBounds);
            System.out.println("Window size: " + windowWidth + "x" + windowHeight);
        });
    }
    
    private void showMainMenu() {
        System.out.println("showMainMenu() called"); // Debug output
        
        // Hide the corner box when showing menu
        cornerBox.setVisible(false);
        
        // Create main menu window
        Stage menuStage = new Stage();
        menuStage.initStyle(StageStyle.UNDECORATED);
        menuStage.initOwner(primaryStage);
        menuStage.setAlwaysOnTop(true);
        
        VBox menuBox = createMainMenu(menuStage);
        Scene menuScene = new Scene(menuBox, 300, 400); // Original menu size
        menuScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        
        menuStage.setScene(menuScene);
        
        // Position menu so its bottom-right corner aligns with corner box's bottom-right corner
        double cornerBoxRightX = primaryStage.getX() + primaryStage.getWidth();
        double cornerBoxBottomY = primaryStage.getY() + primaryStage.getHeight();
        
        double menuX = cornerBoxRightX - 300; // Menu width is 300
        double menuY = cornerBoxBottomY - 400; // Menu height is 400
        
        menuStage.setX(menuX);
        menuStage.setY(menuY);
        
        System.out.println("Menu positioned at: x=" + menuX + ", y=" + menuY);
        
        // Handle menu close event to restore corner box
        menuStage.setOnHiding(e -> {
            cornerBox.setVisible(true);
        }); // Debug output
        
        menuStage.show();
    }
    
    private VBox createMainMenu(Stage menuStage) {
        VBox menuBox = new VBox(15);
        menuBox.setAlignment(Pos.TOP_CENTER);
        menuBox.setPadding(new Insets(20));
        menuBox.getStyleClass().add("main-menu");
        
        // Header
        Label headerLabel = new Label("Asos Settings");
        headerLabel.getStyleClass().add("menu-header");
        
        // Menu options
        Button chatButton = createMenuButton("💬 Chat", () -> {
            openChatWindow();
            menuStage.close();
        });
        
        Button progressButton = createMenuButton("Current Progress", () -> {
            openProgressWindow();
            menuStage.close();
        });
        
        Button languageButton = createMenuButton("Select Language", () -> {
            openLanguageSelector();
            menuStage.close();
        });
        
        Button exitButton = createMenuButton("❌ Exit", () -> {
            menuStage.close();
            stopApplication();
            Platform.exit();
        });
        
        // Close button
        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> {
            menuStage.close();
            cornerBox.setVisible(true);
        });
        
        // Layout
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER);
        headerRow.getChildren().addAll(headerLabel);
        
        HBox closeRow = new HBox();
        closeRow.setAlignment(Pos.TOP_RIGHT);
        closeRow.getChildren().add(closeButton);
        
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(
            chatButton, progressButton, languageButton, 
            createMenuButton("📚 Start Java Tutorial", this::startJavaTutorial),
            createMenuButton("🎯 Teaching Mode", this::toggleTeachingMode),
            exitButton
        );
        
        menuBox.getChildren().addAll(closeRow, headerRow, content);
        
        return menuBox;
    }
    
    private Button createMenuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-option");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> action.run());
        return button;
    }
    
    /**
     * Start Java tutorial teaching mode
     */
    private void startJavaTutorial() {
        try {
            teachingEngine.loadLearningModule("java-hello-world.json");
            teachingModeActive = true;
            showNotification("🚀 Starting Java Hello World Tutorial!");
            
            // Close menu and start teaching
            if (menuStage != null) {
                menuStage.close();
            }
            cornerBox.setVisible(true);
            
            // Start the teaching session
            teachingEngine.startTeaching();
            
        } catch (Exception e) {
            logger.error("Failed to start Java tutorial", e);
            showNotification("❌ Failed to start tutorial: " + e.getMessage());
        }
    }
    
    /**
     * Toggle teaching mode on/off
     */
    private void toggleTeachingMode() {
        if (teachingModeActive) {
            teachingEngine.stopTeaching();
            teachingModeActive = false;
            showNotification("📚 Teaching mode stopped");
        } else {
            showNotification("🎯 Select a tutorial to start teaching mode");
        }
        
        if (menuStage != null) {
            menuStage.close();
        }
        cornerBox.setVisible(true);
    }
    
    /**
     * Get programming language from file extension
     */
    private String getLanguageFromExtension(String filePath) {
        if (filePath.endsWith(".java")) return "Java";
        if (filePath.endsWith(".py")) return "Python";
        if (filePath.endsWith(".js") || filePath.endsWith(".ts")) return "JavaScript";
        if (filePath.endsWith(".html")) return "HTML";
        if (filePath.endsWith(".css")) return "CSS";
        return "Code";
    }
    
    private void openChatWindow() {
        // Hide the entire primary stage when opening chat
        primaryStage.hide();
        
        Stage chatStage = new Stage();
        chatStage.setTitle("Asos Chat");
        chatStage.initStyle(StageStyle.UNDECORATED); // Remove window decorations
        
        // Create chat interface with back button
        BorderPane chatLayout = new BorderPane();
        chatLayout.getStyleClass().add("chat-window");
        
        // Create back button in upper left
        Button backButton = new Button("<");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> {
            chatStage.close();
            primaryStage.show(); // Show primary stage when closing chat
        });
        
        // Position back button in upper left
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(10));
        topBar.getChildren().add(backButton);
        chatLayout.setTop(topBar);
        
        // Add the conversational interface to the chat window
        chatLayout.setCenter(conversationalInterface.getMainContainer());
        
        Scene chatScene = new Scene(chatLayout, 800, 600);
        chatScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        
        chatStage.setScene(chatScene);
        chatStage.show();
    }
    
    private void openProgressWindow() {
        // Hide the entire primary stage when opening progress window
        primaryStage.hide();
        
        Stage progressStage = new Stage();
        progressStage.setTitle("Learning Progress");
        progressStage.initStyle(StageStyle.UNDECORATED); // Remove window decorations
        
        BorderPane progressLayout = new BorderPane();
        progressLayout.getStyleClass().add("progress-window");
        
        // Create back button in upper left
        Button backButton = new Button("<");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> {
            progressStage.close();
            primaryStage.show(); // Show primary stage when closing progress
        });
        
        // Position back button in upper left
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(10));
        topBar.getChildren().add(backButton);
        progressLayout.setTop(topBar);
        
        // Add progress content
        VBox progressContent = createProgressContent();
        progressLayout.setCenter(progressContent);
        
        Scene progressScene = new Scene(progressLayout, 600, 500);
        progressScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        
        progressStage.setScene(progressScene);
        progressStage.show();
    }
    
    private VBox createProgressContent() {
        VBox progressBox = new VBox(20);
        progressBox.setAlignment(Pos.TOP_CENTER);
        progressBox.setPadding(new Insets(30));
        progressBox.getStyleClass().add("progress-window");
        
        Label titleLabel = new Label("Your Learning Journey");
        titleLabel.getStyleClass().add("progress-title");
        
        // Show analytics if available
        if (analytics != null) {
            LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();
            
            VBox statsBox = new VBox(10);
            statsBox.setAlignment(Pos.CENTER);
            statsBox.getStyleClass().add("stats-box");
            
            Label velocityLabel = new Label(String.format("Learning Velocity: %.1f%%", 
                insights.getLearningVelocity() * 100));
            velocityLabel.getStyleClass().add("stat-label");
            
            Label consistencyLabel = new Label(String.format("Consistency Score: %.1f%%", 
                insights.getConsistencyScore() * 100));
            consistencyLabel.getStyleClass().add("stat-label");
            
            statsBox.getChildren().addAll(velocityLabel, consistencyLabel);
            progressBox.getChildren().addAll(titleLabel, statsBox);
        } else {
            Label noDataLabel = new Label("Start learning to see your progress!");
            noDataLabel.getStyleClass().add("no-data-label");
            progressBox.getChildren().addAll(titleLabel, noDataLabel);
        }
        
        return progressBox;
    }
    
    private void openLanguageSelector() {
        // Hide the entire primary stage when opening language selector
        primaryStage.hide();
        
        Stage languageStage = new Stage();
        languageStage.setTitle("Select Language");
        languageStage.initStyle(StageStyle.UNDECORATED); // Remove window decorations
        
        BorderPane languageLayout = new BorderPane();
        languageLayout.getStyleClass().add("language-window");
        
        // Create back button in upper left
        Button backButton = new Button("<");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> {
            languageStage.close();
            primaryStage.show(); // Show primary stage when closing language selector
        });
        
        // Position back button in upper left
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(10));
        topBar.getChildren().add(backButton);
        languageLayout.setTop(topBar);
        
        // Add language content
        VBox languageContent = createLanguageContent();
        languageLayout.setCenter(languageContent);
        
        Scene languageScene = new Scene(languageLayout, 400, 300);
        languageScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        
        languageStage.setScene(languageScene);
        languageStage.show();
    }
    
    private VBox createLanguageContent() {
        VBox languageBox = new VBox(20);
        languageBox.setAlignment(Pos.TOP_CENTER);
        languageBox.setPadding(new Insets(30));
        languageBox.getStyleClass().add("language-window");
        
        Label titleLabel = new Label("Select Language");
        titleLabel.getStyleClass().add("language-title");
        
        VBox languageOptions = new VBox(10);
        languageOptions.setAlignment(Pos.CENTER);
        
        String[] languages = {"English", "বাংলা (Bengali)", "Español", "Français", "Deutsch", "中文"};
        
        for (String language : languages) {
            Button langButton = new Button(language);
            langButton.getStyleClass().add("language-option");
            langButton.setMaxWidth(Double.MAX_VALUE);
            langButton.setOnAction(e -> {
                // TODO: Implement language switching logic
                System.out.println("Selected language: " + language);
            });
            languageOptions.getChildren().add(langButton);
        }
        
        languageBox.getChildren().addAll(titleLabel, languageOptions);
        
        return languageBox;
    }
    
    private void logAIInitializationStatus() {
        // Log the AI model status for debugging
        Platform.runLater(() -> {
            try {
                boolean aiReady = aiEngine.isReady();
                System.out.println("=== AI ENGINE STATUS ===");
                System.out.println("AI Engine Ready: " + aiReady);
                System.out.println("Model Path: models/gemma-270m.onnx");
                System.out.println("Auto-download: Enabled (runs after gradle build)");
                System.out.println("========================");
            } catch (Exception e) {
                System.out.println("Error checking AI status: " + e.getMessage());
            }
        });
    }
    
    private void startLearningPathway(String pathwayId) {
        asosCharacter.greetForPathway(pathwayId);
        pathwayManager.startPathway(pathwayId);
        
        // Notify AI assistant about the new learning pathway
        if (conversationalInterface != null) {
            conversationalInterface.addContextualInformation("current_pathway", pathwayId);
            conversationalInterface.updateConversationMode(getConversationModeForPathway(pathwayId));
            
            // Add contextual suggestion based on pathway
            String suggestion = getPathwaySuggestion(pathwayId);
            if (suggestion != null) {
                conversationalInterface.addContextualSuggestion(suggestion);
            }
        }
    }
    
    private String getConversationModeForPathway(String pathwayId) {
        switch (pathwayId) {
            case "python-basics":
                return "Programming Help";
            case "file-navigation":
                return "General Learning";
            default:
                return "Study Planning";
        }
    }
    
    private String getPathwaySuggestion(String pathwayId) {
        switch (pathwayId) {
            case "python-basics":
                return "How do I start learning Python programming?";
            case "file-navigation":
                return "What are the essential file management skills I should learn?";
            default:
                return "What's the best way to approach this learning topic?";
        }
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
        
        // Cleanup AI resources
        if (aiEngine != null) {
            try {
                aiEngine.cleanup();
            } catch (Exception e) {
                System.err.println("Error cleaning up AI engine: " + e.getMessage());
            }
        }
        
        if (modelManager != null) {
            try {
                // ModelManager cleanup will be implemented when needed
                System.out.println("Model manager cleanup completed");
            } catch (Exception e) {
                System.err.println("Error cleaning up model manager: " + e.getMessage());
            }
        }
    }
}
