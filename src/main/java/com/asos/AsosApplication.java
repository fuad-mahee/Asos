package com.asos;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Label cornerSubtitleLabel;
    private Label pickerTitleLabel;
    private AsosCharacterView characterView;
    private Button askAsosButton;
    private Stage chatStage;
    private HBox stepperRow;
    private Label stepperLabel;
    private VBox stepperBox;
    private AchievementManager achievementManager;
    
    // Notification system components (separate toast window above the corner widget)
    private Stage notificationStage;
    private VBox notificationCard;
    private Label notificationLabel;
    private boolean notificationVisible = false;
    private long lastNotificationAt = 0;
    
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

        // Greet the learner with a happy bounce
        if (characterView != null) {
            characterView.playHappy();
        }
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

        // Achievements (persisted across sessions)
        achievementManager = new AchievementManager();
        
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

            // Keep the chat aware of the step the learner is on, and offer help
            LearningChunk chunk = teachingEngine.getCurrentChunk();
            if (chunk != null && conversationalInterface != null) {
                conversationalInterface.addContextualInformation("step_context", buildStepContext(chunk));
            }
            Platform.runLater(() -> {
                setAskAsosButtonVisible(true);
                updateProgressStepper();
            });
        });
        
        teachingEngine.setOnHintProvided(hint -> {
            Platform.runLater(() -> showNotification(I18n.t("💡 Hint: ") + hint));
        });
        
        teachingEngine.setOnErrorDetected(error -> {
            Platform.runLater(() -> showNotification("⚠️ " + error));
        });
        
        teachingEngine.setOnChunkCompleted(message -> {
            Platform.runLater(() -> {
                showNotification("✅ " + message);
                // Hide live instruction when chunk is completed
                hideLiveInstruction();
                // Keep the course buttons' progress labels current
                refreshCourseButtons();
                // Check for newly earned achievements
                checkAchievements();
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
    
    // ------------------------------------------------------------------
    // UI helpers (window chrome for undecorated windows)
    // ------------------------------------------------------------------

    /** Drag offsets for moving undecorated windows. */
    private double dragOffsetX;
    private double dragOffsetY;

    /**
     * Make an undecorated window movable by dragging the given handle node.
     */
    private void enableDrag(Node handle, Stage stage, Runnable onMoved) {
        handle.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });
        handle.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
            if (onMoved != null) {
                onMoved.run();
            }
        });
    }

    /**
     * Build a draggable title bar with a back button, window title and close button.
     */
    private HBox createTitleBar(String title, Stage stage, Runnable onBack) {
        HBox titleBar = new HBox(10);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().add("title-bar");

        Button backButton = new Button("←");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> onBack.run());

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-bar-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimizeButton = new Button("—");
        minimizeButton.getStyleClass().add("minimize-button");
        minimizeButton.setOnAction(e -> stage.setIconified(true));

        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> onBack.run());

        titleBar.getChildren().addAll(backButton, titleLabel, spacer, minimizeButton, closeButton);
        enableDrag(titleBar, stage, null);
        return titleBar;
    }

    /**
     * Create a transparent-background scene styled with the dark theme
     * (plus the large-text overlay when that setting is on).
     */
    private Scene createTransparentScene(javafx.scene.Parent root, double width, double height) {
        Scene scene = width > 0 ? new Scene(root, width, height) : new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        applyTextScaleToScene(scene);
        return scene;
    }

    /**
     * Add or remove the large-text stylesheet overlay on one scene.
     */
    private void applyTextScaleToScene(Scene scene) {
        String largeTextCss = getClass().getResource("/large-text.css").toExternalForm();
        scene.getStylesheets().remove(largeTextCss);
        if (AppSettings.isLargeText()) {
            scene.getStylesheets().add(largeTextCss);
        }
    }

    /**
     * Re-apply the text-size setting to every window that stays alive across
     * the toggle (windows opened later pick it up via createTransparentScene).
     */
    private void applyTextScaleEverywhere() {
        for (Stage stage : new Stage[]{primaryStage, instructionBoxStage, notificationStage,
                instructionDetailStage, chatStage, menuStage, languageStage}) {
            if (stage != null && stage.getScene() != null) {
                applyTextScaleToScene(stage.getScene());
            }
        }
    }

    /**
     * Show a stage with a short fade-in for a polished feel.
     */
    private void showWithFade(Stage stage) {
        stage.getScene().getRoot().setOpacity(0);
        stage.show();
        FadeTransition fade = new FadeTransition(Duration.millis(160), stage.getScene().getRoot());
        fade.setToValue(1.0);
        fade.play();
    }

    private void setupCornerUI() {
        // Create a small corner window
        primaryStage.setTitle("Asos");
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setResizable(false);

        // Create the corner box content and assign to class field
        cornerBox = createCornerBox();

        // Create separate instruction box and notification toast
        createSeparateInstructionBox();
        createNotificationStage();

        Scene scene = createTransparentScene(cornerBox, 264, 116);

        primaryStage.setScene(scene);

        // Let the user reposition the widget by dragging it; keep the
        // instruction card and notifications attached while moving
        enableDrag(cornerBox, primaryStage, this::positionAttachedWindows);
        
        // Position in lower right corner after stage is shown
        primaryStage.setOnShown(e -> positionWindowInCorner());
        
        // Handle close request
        primaryStage.setOnCloseRequest(e -> {
            stopApplication();
            Platform.exit();
        });
    }
    
    private VBox createCornerBox() {
        // Create main corner card
        VBox mainCornerBox = new VBox(2);
        mainCornerBox.setAlignment(Pos.CENTER_LEFT);
        mainCornerBox.getStyleClass().add("corner-box");

        // Status dot + title row
        Circle statusDot = new Circle(4);
        statusDot.getStyleClass().add("status-dot");

        Label statusLabel = new Label("Asos?");
        statusLabel.getStyleClass().add("corner-text");

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        // Minimize button (restore from the Windows taskbar)
        Button minimizeButton = new Button("—");
        minimizeButton.getStyleClass().add("menu-button");
        minimizeButton.setOnAction(e -> primaryStage.setIconified(true));

        // Three dots menu button
        Button menuButton = new Button("⋯");
        menuButton.getStyleClass().add("menu-button");
        menuButton.setOnAction(e -> showMainMenu());

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().addAll(statusDot, statusLabel, titleSpacer, minimizeButton, menuButton);

        cornerSubtitleLabel = new Label(I18n.t("Your learning buddy is here"));
        cornerSubtitleLabel.getStyleClass().add("corner-subtitle");

        // Animated mascot on the left, texts and controls on the right
        characterView = new AsosCharacterView();

        VBox textColumn = new VBox(2);
        textColumn.setAlignment(Pos.CENTER_LEFT);
        textColumn.getChildren().addAll(titleRow, cornerSubtitleLabel);
        HBox.setHgrow(textColumn, Priority.ALWAYS);

        HBox cardContent = new HBox(10);
        cardContent.setAlignment(Pos.CENTER_LEFT);
        cardContent.getChildren().addAll(characterView, textColumn);

        mainCornerBox.getChildren().add(cardContent);

        VBox container = new VBox(8);
        container.setAlignment(Pos.BOTTOM_RIGHT);
        container.setPadding(new Insets(6));
        container.getChildren().add(mainCornerBox);

        return container;
    }

    /**
     * Create the separate notification toast window shown above the corner
     * widget. A separate stage means long messages are never clipped by the
     * fixed-size corner scene.
     */
    private void createNotificationStage() {
        notificationCard = new VBox(5);
        notificationCard.setAlignment(Pos.CENTER_LEFT);
        notificationCard.getStyleClass().add("notification-box");

        notificationLabel = new Label();
        notificationLabel.getStyleClass().add("notification-text");
        notificationLabel.setWrapText(true);
        notificationLabel.setMaxWidth(280);

        Button closeNotificationBtn = new Button("✕");
        closeNotificationBtn.getStyleClass().add("notification-close");
        closeNotificationBtn.setOnAction(e -> hideNotification());

        HBox notificationHeader = new HBox();
        notificationHeader.setAlignment(Pos.CENTER_RIGHT);
        notificationHeader.getChildren().add(closeNotificationBtn);

        notificationCard.getChildren().addAll(notificationHeader, notificationLabel);

        notificationStage = new Stage();
        notificationStage.initStyle(StageStyle.TRANSPARENT);
        notificationStage.initOwner(primaryStage);
        notificationStage.setAlwaysOnTop(true);
        notificationStage.setScene(createTransparentScene(notificationCard, -1, -1));
    }

    /**
     * Keep the instruction card and notification toast attached to the corner
     * widget when it is moved or repositioned.
     */
    private void positionAttachedWindows() {
        positionInstructionBox();
        positionNotification();
    }

    /**
     * Place the notification just above the instruction card (or above the
     * corner widget when no instruction is showing), right edges aligned.
     */
    private void positionNotification() {
        if (notificationStage == null || primaryStage == null || !notificationStage.isShowing()) {
            return;
        }
        Platform.runLater(() -> {
            notificationStage.sizeToScene();
            double anchorY = (instructionBoxStage != null && instructionBoxStage.isShowing())
                    ? instructionBoxStage.getY()
                    : primaryStage.getY() + 6; // corner container has 6px padding
            double rightEdge = primaryStage.getX() + primaryStage.getWidth() - 6;
            notificationStage.setX(rightEdge - notificationStage.getWidth());
            notificationStage.setY(anchorY - notificationStage.getHeight() - 8);
        });
    }

    /**
     * Pick a color-coding style class from the message content.
     */
    private String classifyNotification(String message) {
        String lower = message.toLowerCase();
        if (message.contains("⚠️") || message.contains("❌") || lower.contains("error")
                || lower.contains("mistake") || lower.contains("typo") || lower.contains("almost!")) {
            return "notification-error";
        }
        if (message.contains("✅") || message.contains("🚀") || message.contains("🏅")
                || lower.contains("congratulations")) {
            return "notification-success";
        }
        if (message.contains("💡")) {
            return "notification-hint";
        }
        return null;
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

        // Contextual help: opens the chat preloaded with the current step
        askAsosButton = new Button(I18n.t("💬 Ask Asos about this step"));
        askAsosButton.getStyleClass().add("ask-step-button");
        askAsosButton.setMaxWidth(Double.MAX_VALUE);
        askAsosButton.setVisible(false);
        askAsosButton.setManaged(false);
        askAsosButton.setOnAction(e -> askAsosAboutCurrentStep());
        // Stop the click from bubbling to the instruction box (which would
        // also open the detail view)
        askAsosButton.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

        // Progress stepper: one dot per course step, filled as they complete
        stepperRow = new HBox(5);
        stepperRow.setAlignment(Pos.CENTER_LEFT);
        stepperLabel = new Label();
        stepperLabel.getStyleClass().add("stepper-label");
        stepperBox = new VBox(4);
        stepperBox.setAlignment(Pos.CENTER_LEFT);
        stepperBox.getChildren().addAll(stepperRow, stepperLabel);
        stepperBox.setVisible(false);
        stepperBox.setManaged(false);

        instructionBox.getChildren().addAll(languageSelectionBox, stepperBox, instructionLabel, askAsosButton);
        
        // Create separate stage for instruction box
        instructionBoxStage = new Stage();
        instructionBoxStage.initStyle(StageStyle.TRANSPARENT);
        instructionBoxStage.initOwner(primaryStage);
        instructionBoxStage.setAlwaysOnTop(true);

        Scene instructionScene = createTransparentScene(instructionBox, -1, -1);

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
        
        pickerTitleLabel = new Label(I18n.t("SELECT LEARNING PATH"));
        pickerTitleLabel.getStyleClass().add("language-title");
        Label titleLabel = pickerTitleLabel;
        
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
    /** Course-picker buttons, kept for live progress-label refreshes. */
    private final Map<String, Button> courseButtons = new HashMap<>();
    private final Map<String, String> courseDisplayNames = new HashMap<>();

    private Button createLanguageButton(String displayName, String languageKey) {
        Button button = new Button();
        button.setPrefWidth(190);
        button.setPrefHeight(35);
        button.getStyleClass().add("language-button");

        courseButtons.put(languageKey, button);
        courseDisplayNames.put(languageKey, displayName);
        updateCourseButtonLabel(languageKey);

        button.setOnAction(e -> startLanguageLearning(languageKey));
        // Stop the click from bubbling to the instruction box's detail view
        button.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

        return button;
    }

    /**
     * Recompute one course button's progress label from saved progress.
     */
    private void updateCourseButtonLabel(String languageKey) {
        Button button = courseButtons.get(languageKey);
        if (button == null) return;

        // Courses are stored under "<language>-complete"
        LearningProgressManager progressManager = new LearningProgressStorage().loadProgress();
        String progressKey = languageKey + "-complete";

        int completedChunks = progressManager.getCompletedChunkCount(progressKey);
        int totalChunks = progressManager.getTotalChunks(progressKey);
        if (totalChunks <= 0) {
            totalChunks = getTotalChunksForLanguage(languageKey);
        }
        double progressPercent = (double) completedChunks / totalChunks * 100;

        String displayName = courseDisplayNames.getOrDefault(languageKey, languageKey);
        button.setText(completedChunks > 0
                ? String.format("%s (%.0f%%)", displayName, progressPercent)
                : displayName);
    }

    /**
     * Refresh all course buttons' progress labels (after steps complete).
     */
    private void refreshCourseButtons() {
        for (String languageKey : courseButtons.keySet()) {
            updateCourseButtonLabel(languageKey);
        }
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
            // loadLearningModule prepends "/learning-modules/" itself
            String moduleFile = String.format("%s-complete.json", language);
            if (teachingEngine != null) {
                teachingEngine.loadLearningModule(moduleFile);
                teachingModeActive = true;
                showNotification(String.format(I18n.t("🚀 Starting %s course!"), language.toUpperCase()));
                teachingEngine.startTeaching();
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
            if (notificationStage == null) return;

            long now = System.currentTimeMillis();

            // Stack messages that arrive in quick succession (e.g. two code
            // errors at once) instead of overwriting the first one
            String text = message;
            if (notificationVisible && now - lastNotificationAt < 8000) {
                String existing = notificationLabel.getText();
                if (existing != null && !existing.isBlank() && !existing.contains(message)
                        && existing.length() + message.length() < 500) {
                    text = existing + "\n\n" + message;
                }
            }
            lastNotificationAt = now;

            // Color-code by message type (red mistake / green success / amber hint)
            notificationCard.getStyleClass().removeAll(
                    "notification-error", "notification-success", "notification-hint");
            String styleClass = classifyNotification(message);
            if (styleClass != null) {
                notificationCard.getStyleClass().add(styleClass);
            }

            // The mascot and sound cues react to what just happened
            if ("notification-success".equals(styleClass)) {
                if (characterView != null) characterView.playHappy();
                SoundEffects.playSuccess();
            } else if ("notification-error".equals(styleClass)) {
                if (characterView != null) characterView.playConcerned();
                SoundEffects.playError();
            } else if ("notification-hint".equals(styleClass)) {
                if (characterView != null) characterView.playThinking();
                SoundEffects.playHint();
            }

            notificationLabel.setText(text);
            notificationVisible = true;
            if (!notificationStage.isShowing()) {
                showWithFade(notificationStage);
            }
            positionNotification();

            // Auto-hide: errors stay longer so the learner can read the fix
            boolean isError = "notification-error".equals(styleClass);
            if (notificationTimeline != null) {
                notificationTimeline.stop();
            }
            notificationTimeline = new Timeline(new KeyFrame(
                    Duration.seconds(isError ? 25 : 10), e -> hideNotification()));
            notificationTimeline.play();
        });
    }

    public void hideNotification() {
        Platform.runLater(() -> {
            if (notificationTimeline != null) {
                notificationTimeline.stop();
                notificationTimeline = null;
            }
            if (notificationStage != null) {
                notificationStage.hide();
            }
            notificationLabel.setText("");
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
                positionNotification();
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
            positionNotification();
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
                positionNotification();
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
            instructionDetailStage.initStyle(StageStyle.TRANSPARENT);
            instructionDetailStage.initOwner(primaryStage);
            instructionDetailStage.setAlwaysOnTop(true);

            VBox detailBox = createDetailedInstructionBox();
            Scene detailScene = createTransparentScene(detailBox, 440, 360);

            instructionDetailStage.setScene(detailScene);

            // Center the window on screen
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            instructionDetailStage.setX((screenBounds.getWidth() - 440) / 2);
            instructionDetailStage.setY((screenBounds.getHeight() - 360) / 2);

            showWithFade(instructionDetailStage);
        });
    }

    private VBox createDetailedInstructionBox() {
        VBox detailBox = new VBox();
        detailBox.setAlignment(Pos.TOP_LEFT);
        detailBox.getStyleClass().add("instruction-detail-window");

        // Draggable header with back button
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getStyleClass().add("title-bar");

        Button backButton = new Button("←");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> {
            if (instructionDetailStage != null) {
                instructionDetailStage.close();
            }
        });

        Label headerLabel = new Label("Step Guide");
        headerLabel.getStyleClass().add("instruction-detail-header");

        headerBox.getChildren().addAll(backButton, headerLabel);
        enableDrag(headerBox, instructionDetailStage, null);

        // Detailed instruction content (scrollable for long steps)
        Label detailLabel = new Label(currentDetailedInstruction);
        detailLabel.getStyleClass().add("instruction-detail-text");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(380);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(detailLabel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPadding(new Insets(16, 20, 20, 20));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        detailBox.getChildren().addAll(headerBox, scrollPane);

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
            
            // Also position the attached windows (instruction card, notifications)
            positionAttachedWindows();
            
            System.out.println("Positioning window at: x=" + x + ", y=" + y);
            System.out.println("Screen bounds: " + screenBounds);
            System.out.println("Window size: " + windowWidth + "x" + windowHeight);
        });
    }
    
    private void showMainMenu() {
        System.out.println("showMainMenu() called"); // Debug output
        
        // Hide the corner box when showing menu
        cornerBox.setVisible(false);
        
        // Create main menu window (assign the field so other actions can close it)
        menuStage = new Stage();
        menuStage.initStyle(StageStyle.TRANSPARENT);
        menuStage.initOwner(primaryStage);
        menuStage.setAlwaysOnTop(true);

        final double menuWidth = 280;
        final double menuHeight = 510;

        VBox menuBox = createMainMenu(menuStage);
        Scene menuScene = createTransparentScene(menuBox, menuWidth, menuHeight);

        menuStage.setScene(menuScene);

        // Position menu so its bottom-right corner aligns with corner box's bottom-right corner
        double cornerBoxRightX = primaryStage.getX() + primaryStage.getWidth();
        double cornerBoxBottomY = primaryStage.getY() + primaryStage.getHeight();

        menuStage.setX(cornerBoxRightX - menuWidth);
        menuStage.setY(cornerBoxBottomY - menuHeight);

        // Handle menu close event to restore corner box
        menuStage.setOnHiding(e -> cornerBox.setVisible(true));

        showWithFade(menuStage);
    }

    private VBox createMainMenu(Stage menuStage) {
        VBox menuBox = new VBox(4);
        menuBox.setAlignment(Pos.TOP_LEFT);
        menuBox.setPadding(new Insets(14, 14, 16, 14));
        menuBox.getStyleClass().add("main-menu");

        // Header row: title + close, draggable
        Label headerLabel = new Label("Asos?");
        headerLabel.getStyleClass().add("menu-header");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> {
            menuStage.close();
            cornerBox.setVisible(true);
        });

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(0, 0, 10, 4));
        headerRow.getChildren().addAll(headerLabel, headerSpacer, closeButton);
        enableDrag(headerRow, menuStage, null);

        // Menu options grouped in sections
        Button chatButton = createMenuButton(I18n.t("💬  Ask Asos (AI Chat)"), () -> {
            openChatWindow();
            menuStage.close();
        });

        Button progressButton = createMenuButton(I18n.t("📈  My Progress"), () -> {
            openProgressWindow();
            menuStage.close();
        });

        Button tutorialButton = createMenuButton(I18n.t("📚  Start Tutorial"), this::openCoursePicker);
        Button teachingModeButton = createMenuButton(I18n.t("🎯  Teaching Mode"), this::toggleTeachingMode);

        Button languageButton = createMenuButton(I18n.t("🌐  App Language"), () -> {
            openLanguageSelector();
            menuStage.close();
        });

        // Toggle: interface text size (accessibility)
        Button textSizeButton = createMenuButton(textSizeMenuLabel(), () -> { });
        textSizeButton.setOnAction(e -> {
            AppSettings.setLargeText(!AppSettings.isLargeText());
            applyTextScaleEverywhere();
            textSizeButton.setText(textSizeMenuLabel());
            showNotification(I18n.t("🔠 Text size updated"));
        });

        // Toggle: sound cues
        Button soundButton = createMenuButton(soundMenuLabel(), () -> { });
        soundButton.setOnAction(e -> {
            AppSettings.setSoundEnabled(!AppSettings.isSoundEnabled());
            soundButton.setText(soundMenuLabel());
            if (AppSettings.isSoundEnabled()) {
                SoundEffects.playSuccess(); // audible confirmation
            }
        });

        Button exitButton = createMenuButton(I18n.t("⏻  Quit Asos"), () -> {
            menuStage.close();
            stopApplication();
            Platform.exit();
        });
        exitButton.getStyleClass().add("menu-option-danger");

        menuBox.getChildren().addAll(
                headerRow,
                createMenuSectionLabel(I18n.t("LEARN")),
                tutorialButton, teachingModeButton, progressButton,
                createMenuSectionLabel(I18n.t("ASSISTANT")),
                chatButton,
                createMenuSectionLabel(I18n.t("SETTINGS")),
                languageButton, textSizeButton, soundButton, exitButton
        );

        return menuBox;
    }

    private String textSizeMenuLabel() {
        return AppSettings.isLargeText()
                ? I18n.t("🔠  Text Size: Large")
                : I18n.t("🔠  Text Size: Normal");
    }

    private String soundMenuLabel() {
        return AppSettings.isSoundEnabled()
                ? I18n.t("🔊  Sounds: On")
                : I18n.t("🔇  Sounds: Off");
    }

    private Label createMenuSectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("menu-section-label");
        label.setPadding(new Insets(10, 0, 4, 6));
        return label;
    }

    private Button createMenuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-option");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> {
            try {
                action.run();
            } catch (Exception ex) {
                // A failing action must never leave a menu item silently dead
                logger.error("Menu action '{}' failed", text, ex);
                showNotification(I18n.t("❌ Something went wrong opening this view. Please try again."));
            }
        });
        return button;
    }
    
    /**
     * Show or hide the "Ask Asos about this step" button on the instruction card.
     */
    private void setAskAsosButtonVisible(boolean visible) {
        if (askAsosButton != null) {
            askAsosButton.setVisible(visible);
            askAsosButton.setManaged(visible);
        }
    }

    /**
     * Rebuild the stepper dots from the teaching engine's current position:
     * green check-filled for done, glowing indigo for the current step,
     * dim for upcoming ones.
     */
    private void updateProgressStepper() {
        if (stepperRow == null || teachingEngine == null) return;

        int total = teachingEngine.getTotalChunks();
        int currentIndex = teachingEngine.getCurrentChunkIndex();
        if (total <= 0) {
            setStepperVisible(false);
            return;
        }

        stepperRow.getChildren().clear();
        for (int i = 0; i < total; i++) {
            Circle dot = new Circle(i == currentIndex ? 5 : 4);
            if (i < currentIndex) {
                dot.getStyleClass().add("stepper-done");
            } else if (i == currentIndex) {
                dot.getStyleClass().add("stepper-current");
            } else {
                dot.getStyleClass().add("stepper-future");
            }
            stepperRow.getChildren().add(dot);
        }

        stepperLabel.setText(String.format(I18n.t("Step %d of %d"),
                Math.min(currentIndex + 1, total), total));
        setStepperVisible(true);
    }

    private void setStepperVisible(boolean visible) {
        if (stepperBox != null) {
            stepperBox.setVisible(visible);
            stepperBox.setManaged(visible);
        }
    }

    /**
     * Evaluate achievements against saved progress and toast any new ones.
     */
    private void checkAchievements() {
        try {
            LearningProgressManager progress = new LearningProgressStorage().loadProgress();
            for (AchievementManager.Achievement achievement : achievementManager.checkAndUnlock(progress)) {
                showNotification(I18n.t("🏅 Achievement unlocked: ")
                        + achievement.emoji() + " " + I18n.t(achievement.title()));
            }
        } catch (Exception e) {
            logger.warn("Achievement check failed: {}", e.getMessage());
        }
    }

    /**
     * Compact description of the current step, used to ground AI answers.
     */
    private String buildStepContext(LearningChunk chunk) {
        String details = chunk.getDetailedInstruction() != null ? chunk.getDetailedInstruction() : "";
        if (details.length() > 600) {
            details = details.substring(0, 600) + "...";
        }
        return "Course: " + teachingEngine.getCourseDisplayName()
                + "\nStep " + chunk.getChunkId() + ": " + chunk.getInstruction()
                + (details.isBlank() ? "" : "\nInstructions: " + details);
    }

    /**
     * Open the chat preloaded with the current tutorial step and ask for help.
     */
    private void askAsosAboutCurrentStep() {
        LearningChunk chunk = teachingEngine != null ? teachingEngine.getCurrentChunk() : null;

        // Refresh the context right before asking (survives chat rebuilds
        // caused by language switches)
        if (chunk != null && conversationalInterface != null) {
            conversationalInterface.addContextualInformation("step_context", buildStepContext(chunk));
        }

        openChatWindow();

        if (chunk != null && conversationalInterface != null) {
            conversationalInterface.askQuestion(
                    I18n.t("I'm stuck on this step. Can you explain it in a simpler way?"));
        }
    }

    /**
     * Open the course picker: shows the instruction card whose top section
     * lets the learner choose Java, Python, or C++.
     */
    private void openCoursePicker() {
        if (menuStage != null) {
            menuStage.close();
        }
        cornerBox.setVisible(true);
        refreshCourseButtons();
        setAskAsosButtonVisible(false); // no active step while picking a course
        setStepperVisible(false);

        showLiveInstruction(
                I18n.t("Pick a course above to begin!"),
                I18n.t("Choose Java, Python, or C++ using the buttons at the top of this card. " +
                "Each button shows your saved progress for that course.\n\n" +
                "Once you pick a course, Asos will guide you step by step and detect " +
                "your work automatically - create the files it asks for on your " +
                "Desktop, in Documents, in Downloads, or in the Asos project folder."));
    }
    
    /**
     * Toggle teaching mode on/off
     */
    private void toggleTeachingMode() {
        if (teachingModeActive) {
            teachingEngine.stopTeaching();
            teachingModeActive = false;
            showNotification(I18n.t("📚 Teaching mode stopped"));
        } else {
            showNotification(I18n.t("🎯 Select a tutorial to start teaching mode"));
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
        // The chat's UI node can only live in one window - if a chat window is
        // already open, bring it to the front instead of creating a second one
        if (chatStage != null && chatStage.isShowing()) {
            chatStage.toFront();
            return;
        }

        // Hide the entire primary stage when opening chat
        primaryStage.hide();

        chatStage = new Stage();
        chatStage.setTitle("Asos Chat");
        chatStage.initStyle(StageStyle.TRANSPARENT);

        BorderPane chatLayout = new BorderPane();
        chatLayout.getStyleClass().add("chat-window");

        final Stage stage = chatStage;
        chatLayout.setTop(createTitleBar(I18n.t("Asos — AI Learning Assistant"), stage, () -> {
            stage.close();
            primaryStage.show(); // Show primary stage when closing chat
        }));

        // Add the conversational interface to the chat window
        chatLayout.setCenter(conversationalInterface.getMainContainer());

        Scene chatScene = createTransparentScene(chatLayout, 860, 620);
        chatStage.setScene(chatScene);
        showWithFade(chatStage);
    }
    
    private void openProgressWindow() {
        // Hide the entire primary stage when opening progress window
        primaryStage.hide();

        Stage progressStage = new Stage();
        progressStage.setTitle("Learning Progress");
        progressStage.initStyle(StageStyle.TRANSPARENT);

        BorderPane progressLayout = new BorderPane();
        progressLayout.getStyleClass().add("progress-window");

        progressLayout.setTop(createTitleBar(I18n.t("My Learning Progress"), progressStage, () -> {
            progressStage.close();
            primaryStage.show(); // Show primary stage when closing progress
        }));

        // Add progress content
        VBox progressContent = createProgressContent();
        progressLayout.setCenter(progressContent);

        Scene progressScene = createTransparentScene(progressLayout, 560, 520);
        progressStage.setScene(progressScene);
        showWithFade(progressStage);
    }
    
    private VBox createProgressContent() {
        VBox progressBox = new VBox(16);
        progressBox.setAlignment(Pos.TOP_LEFT);
        progressBox.setPadding(new Insets(24, 28, 28, 28));

        Label titleLabel = new Label(I18n.t("Your Learning Journey"));
        titleLabel.getStyleClass().add("progress-title");
        progressBox.getChildren().add(titleLabel);

        // Per-course progress cards (courses are stored under "<language>-complete")
        LearningProgressManager progressManager = new LearningProgressStorage().loadProgress();
        boolean anyProgress = false;

        String[][] courses = {{"Java", "java"}, {"Python", "python"}, {"C++", "cpp"}};
        for (String[] course : courses) {
            String progressKey = course[1] + "-complete";
            int completedChunks = progressManager.getCompletedChunkCount(progressKey);
            int totalChunks = progressManager.getTotalChunks(progressKey);
            if (totalChunks <= 0) {
                totalChunks = getTotalChunksForLanguage(course[1]);
            }
            double fraction = Math.min(1.0, (double) completedChunks / totalChunks);
            if (completedChunks > 0) {
                anyProgress = true;
            }
            progressBox.getChildren().add(
                    createCourseProgressCard(course[0], completedChunks, totalChunks, fraction));
        }

        if (!anyProgress) {
            Label noDataLabel = new Label(I18n.t("Start a course from the menu to see your progress grow!"));
            noDataLabel.getStyleClass().add("no-data-label");
            progressBox.getChildren().add(noDataLabel);
        }

        // Achievements: earned ones highlighted, remaining ones locked
        Label achievementsLabel = new Label(I18n.t("ACHIEVEMENTS"));
        achievementsLabel.getStyleClass().add("menu-section-label");

        javafx.scene.layout.FlowPane achievementsPane = new javafx.scene.layout.FlowPane(8, 8);
        java.util.Set<String> unlockedIds = achievementManager.getUnlockedIds();
        for (AchievementManager.Achievement achievement : AchievementManager.getAll()) {
            boolean unlocked = unlockedIds.contains(achievement.id());
            Label chip = new Label(unlocked
                    ? achievement.emoji() + " " + I18n.t(achievement.title()).split(" - ")[0]
                    : "🔒 " + I18n.t(achievement.title()).split(" - ")[0]);
            chip.getStyleClass().add(unlocked ? "achievement-chip" : "achievement-chip-locked");
            javafx.scene.control.Tooltip.install(chip,
                    new javafx.scene.control.Tooltip(I18n.t(achievement.title())));
            achievementsPane.getChildren().add(chip);
        }
        progressBox.getChildren().addAll(achievementsLabel, achievementsPane);

        // Analytics insights (optional - the progress window must still open
        // even if analytics can't produce insights yet)
        try {
            if (analytics != null) {
                LearningAnalytics.LearningInsights insights = analytics.analyzeLearningProgress();

                VBox statsBox = new VBox(6);
                statsBox.getStyleClass().add("stats-box");

                Label velocityLabel = new Label(String.format(I18n.t("Learning velocity: %.1f%%"),
                    insights.getLearningVelocity() * 100));
                velocityLabel.getStyleClass().add("stat-label");

                Label consistencyLabel = new Label(String.format(I18n.t("Consistency score: %.1f%%"),
                    insights.getConsistencyScore() * 100));
                consistencyLabel.getStyleClass().add("stat-label");

                statsBox.getChildren().addAll(velocityLabel, consistencyLabel);
                progressBox.getChildren().add(statsBox);
            }
        } catch (Exception e) {
            logger.warn("Analytics insights unavailable: {}", e.getMessage());
        }

        return progressBox;
    }

    /**
     * A card showing one course's completion with a progress bar.
     */
    private VBox createCourseProgressCard(String courseName, int completed, int total, double fraction) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label nameLabel = new Label(courseName);
        nameLabel.getStyleClass().add("stat-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(completed > 0
                ? String.format(I18n.t("%d / %d steps · %.0f%%"), completed, total, fraction * 100)
                : I18n.t("Not started"));
        valueLabel.getStyleClass().add(completed > 0 ? "stat-card-value" : "no-data-label");

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getChildren().addAll(nameLabel, spacer, valueLabel);

        ProgressBar progressBar = new ProgressBar(fraction);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(headerRow, progressBar);
        return card;
    }
    
    private Stage languageStage;

    private void openLanguageSelector() {
        // Hide the entire primary stage when opening language selector
        primaryStage.hide();

        languageStage = new Stage();
        languageStage.setTitle("App Language");
        languageStage.initStyle(StageStyle.TRANSPARENT);

        BorderPane languageLayout = new BorderPane();
        languageLayout.getStyleClass().add("language-window");

        languageLayout.setTop(createTitleBar(I18n.t("App Language"), languageStage, () -> {
            languageStage.close();
            primaryStage.show(); // Show primary stage when closing language selector
        }));

        // Add language content
        VBox languageContent = createLanguageContent();
        languageLayout.setCenter(languageContent);

        Scene languageScene = createTransparentScene(languageLayout, 380, 330);
        languageStage.setScene(languageScene);
        showWithFade(languageStage);
    }

    private VBox createLanguageContent() {
        VBox languageBox = new VBox(16);
        languageBox.setAlignment(Pos.TOP_LEFT);
        languageBox.setPadding(new Insets(20, 24, 24, 24));

        Label titleLabel = new Label(I18n.t("Choose your preferred language"));
        titleLabel.getStyleClass().add("stat-label");

        VBox languageOptions = new VBox(8);
        languageOptions.setAlignment(Pos.CENTER);

        String[] languages = {"English", "বাংলা (Bengali)"};
        String currentLanguage = AppSettings.getLanguage();

        for (String language : languages) {
            Button langButton = new Button(language);
            langButton.getStyleClass().add("language-option");
            langButton.setMaxWidth(Double.MAX_VALUE);
            if (language.equals(currentLanguage)) {
                langButton.getStyleClass().add("language-option-selected");
            }
            langButton.setOnAction(e -> applyLanguage(language));
            languageOptions.getChildren().add(langButton);
        }

        Label noteLabel = new Label(I18n.t(
                "Changing the language updates the app's menus and buttons, and the AI chat answers in it too."));
        noteLabel.getStyleClass().add("no-data-label");
        noteLabel.setWrapText(true);

        languageBox.getChildren().addAll(titleLabel, languageOptions, noteLabel);

        return languageBox;
    }

    /**
     * Apply an interface language: persist it, retranslate the static widgets,
     * rebuild the chat UI, and re-render the language window in the new language.
     */
    private void applyLanguage(String language) {
        AppSettings.setLanguage(language);
        logger.info("App language set to {}", language);

        // Retranslate widgets that were built at startup
        if (cornerSubtitleLabel != null) {
            cornerSubtitleLabel.setText(I18n.t("Your learning buddy is here"));
        }
        if (pickerTitleLabel != null) {
            pickerTitleLabel.setText(I18n.t("SELECT LEARNING PATH"));
        }
        if (askAsosButton != null) {
            askAsosButton.setText(I18n.t("💬 Ask Asos about this step"));
        }

        // Rebuild the chat UI so its texts pick up the new language
        conversationalInterface = new ConversationalInterface(aiAssistant);

        showNotification(I18n.t("🌐 Language updated! Menus and chat now use your language."));

        // Re-render this window in the newly selected language
        if (languageStage != null) {
            languageStage.close();
        }
        openLanguageSelector();
    }
    
    private void logAIInitializationStatus() {
        // Log the AI model status for debugging
        Platform.runLater(() -> {
            try {
                boolean aiReady = aiEngine.isReady();
                System.out.println("=== AI ENGINE STATUS ===");
                System.out.println("AI Engine Ready: " + aiReady);
                System.out.println("Ollama URL: " + LocalAIEngine.OLLAMA_BASE_URL);
                System.out.println("Model: " + LocalAIEngine.MODEL_NAME);
                if (!aiReady) {
                    System.out.println("(Install Ollama and run 'ollama pull "
                            + LocalAIEngine.MODEL_NAME + "' to enable AI answers)");
                }
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

        if (teachingEngine != null) {
            try {
                teachingEngine.stopTeaching();
            } catch (Exception e) {
                System.err.println("Error stopping teaching engine: " + e.getMessage());
            }
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
