package com.asos;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * ConversationalInterface provides a chat-like interface for interacting with the AI assistant.
 * It supports features like conversation history, typing indicators, and contextual suggestions.
 */
public class ConversationalInterface {
    private VBox mainContainer;
    private VBox conversationArea;
    private ScrollPane conversationScrollPane;
    private TextField inputField;
    private Button sendButton;
    private ProgressIndicator typingIndicator;
    private VBox suggestionsBox;
    private ComboBox<String> conversationModeSelector;
    
    private IntelligentLearningAssistant aiAssistant;
    private boolean isAiTyping = false;
    private Map<String, String> conversationContext;
    
    // Pre-defined conversation starters and suggestions
    private static final String[] CONVERSATION_STARTERS = {
        "How can I improve my study habits?",
        "Explain the concept of recursion in programming",
        "What's the best way to prepare for exams?",
        "Help me understand machine learning basics",
        "How do I manage my time better?",
        "Explain the difference between HTTP and HTTPS"
    };
    
    private static final String[] CONVERSATION_MODES = {
        "General Learning", "Programming Help", "Study Planning", 
        "Concept Explanation", "Problem Solving", "Career Guidance"
    };
    
    public ConversationalInterface(IntelligentLearningAssistant aiAssistant) {
        this.aiAssistant = aiAssistant;
        this.conversationContext = new HashMap<>();
        initializeInterface();
        setupEventHandlers();
        addWelcomeMessage();
    }
    
    private void initializeInterface() {
        mainContainer = new VBox(10);
        mainContainer.setPadding(new Insets(15));
        mainContainer.getStyleClass().add("conversation-container");
        
        // Header with mode selector
        HBox header = createHeader();
        
        // Conversation area
        conversationArea = new VBox(10);
        conversationArea.setPadding(new Insets(10));
        conversationArea.getStyleClass().add("conversation-area");
        
        conversationScrollPane = new ScrollPane(conversationArea);
        conversationScrollPane.setFitToWidth(true);
        conversationScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        conversationScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        conversationScrollPane.getStyleClass().add("conversation-scroll");
        conversationScrollPane.setPrefHeight(400);
        
        // Suggestions area
        suggestionsBox = createSuggestionsBox();
        
        // Input area
        HBox inputArea = createInputArea();
        
        mainContainer.getChildren().addAll(header, conversationScrollPane, suggestionsBox, inputArea);
        VBox.setVgrow(conversationScrollPane, Priority.ALWAYS);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        header.getStyleClass().add("conversation-header");
        
        Label titleLabel = new Label("AI Learning Assistant");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.getStyleClass().add("conversation-title");
        
        Label modeLabel = new Label("Mode:");
        modeLabel.getStyleClass().add("mode-label");
        
        conversationModeSelector = new ComboBox<>();
        conversationModeSelector.getItems().addAll(CONVERSATION_MODES);
        conversationModeSelector.setValue(CONVERSATION_MODES[0]);
        conversationModeSelector.getStyleClass().add("mode-selector");
        
        Button clearButton = new Button("Clear Chat");
        clearButton.getStyleClass().add("clear-button");
        clearButton.setOnAction(e -> clearConversation());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(titleLabel, spacer, modeLabel, conversationModeSelector, clearButton);
        return header;
    }
    
    private VBox createSuggestionsBox() {
        VBox suggestions = new VBox(5);
        suggestions.setPadding(new Insets(10));
        suggestions.getStyleClass().add("suggestions-box");
        
        Label suggestionsLabel = new Label("Quick Start:");
        suggestionsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        suggestionsLabel.getStyleClass().add("suggestions-label");
        
        FlowPane suggestionsFlow = new FlowPane(5, 5);
        suggestionsFlow.getStyleClass().add("suggestions-flow");
        
        for (String starter : CONVERSATION_STARTERS) {
            Button suggestionBtn = new Button(starter);
            suggestionBtn.getStyleClass().add("suggestion-button");
            suggestionBtn.setWrapText(true);
            suggestionBtn.setMaxWidth(200);
            suggestionBtn.setOnAction(e -> {
                inputField.setText(starter);
                sendMessage();
            });
            suggestionsFlow.getChildren().add(suggestionBtn);
        }
        
        suggestions.getChildren().addAll(suggestionsLabel, suggestionsFlow);
        return suggestions;
    }
    
    private HBox createInputArea() {
        HBox inputArea = new HBox(10);
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setPadding(new Insets(10));
        inputArea.getStyleClass().add("input-area");
        
        inputField = new TextField();
        inputField.setPromptText("Type your question or ask for help...");
        inputField.getStyleClass().add("message-input");
        inputField.setPrefHeight(40);
        
        sendButton = new Button("Send");
        sendButton.getStyleClass().add("send-button");
        sendButton.setPrefHeight(40);
        sendButton.setDefaultButton(true);
        
        typingIndicator = new ProgressIndicator();
        typingIndicator.setPrefSize(20, 20);
        typingIndicator.setVisible(false);
        
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputArea.getChildren().addAll(inputField, typingIndicator, sendButton);
        return inputArea;
    }
    
    private void setupEventHandlers() {
        // Send button and Enter key
        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                sendMessage();
            }
        });
        
        // Mode selector change
        conversationModeSelector.setOnAction(e -> {
            String selectedMode = conversationModeSelector.getValue();
            conversationContext.put("mode", selectedMode);
            addSystemMessage("Switched to " + selectedMode + " mode");
        });
        
        // Auto-scroll to bottom when new messages are added
        conversationArea.heightProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> conversationScrollPane.setVvalue(1.0));
        });
    }
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty() || isAiTyping) {
            return;
        }
        
        // Add user message
        addUserMessage(message);
        inputField.clear();
        
        // Hide suggestions after first message
        if (suggestionsBox.isVisible()) {
            suggestionsBox.setVisible(false);
            suggestionsBox.setManaged(false);
        }
        
        // Show typing indicator and get AI response
        showTypingIndicator();
        getAiResponse(message);
    }
    
    private void addUserMessage(String message) {
        VBox messageContainer = new VBox(5);
        messageContainer.getStyleClass().add("user-message-container");
        messageContainer.setAlignment(Pos.CENTER_RIGHT);
        
        HBox messageBox = new HBox();
        messageBox.getStyleClass().add("user-message-box");
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setMaxWidth(400);
        
        TextFlow textFlow = new TextFlow();
        Text messageText = new Text(message);
        messageText.getStyleClass().add("user-message-text");
        textFlow.getChildren().add(messageText);
        
        Label timeLabel = new Label(getCurrentTime());
        timeLabel.getStyleClass().add("message-time");
        
        VBox textContainer = new VBox(2);
        textContainer.getChildren().addAll(textFlow, timeLabel);
        textContainer.setAlignment(Pos.CENTER_RIGHT);
        
        messageBox.getChildren().add(textContainer);
        messageContainer.getChildren().add(messageBox);
        
        conversationArea.getChildren().add(messageContainer);
    }
    
    private void addAiMessage(String message) {
        VBox messageContainer = new VBox(5);
        messageContainer.getStyleClass().add("ai-message-container");
        messageContainer.setAlignment(Pos.CENTER_LEFT);
        
        HBox messageBox = new HBox();
        messageBox.getStyleClass().add("ai-message-box");
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setMaxWidth(400);
        
        TextFlow textFlow = new TextFlow();
        Text messageText = new Text(message);
        messageText.getStyleClass().add("ai-message-text");
        textFlow.getChildren().add(messageText);
        
        Label timeLabel = new Label(getCurrentTime());
        timeLabel.getStyleClass().add("message-time");
        
        VBox textContainer = new VBox(2);
        textContainer.getChildren().addAll(textFlow, timeLabel);
        textContainer.setAlignment(Pos.CENTER_LEFT);
        
        messageBox.getChildren().add(textContainer);
        messageContainer.getChildren().add(messageBox);
        
        conversationArea.getChildren().add(messageContainer);
    }
    
    private void addSystemMessage(String message) {
        HBox messageContainer = new HBox();
        messageContainer.getStyleClass().add("system-message-container");
        messageContainer.setAlignment(Pos.CENTER);
        
        Label systemMessage = new Label(message);
        systemMessage.getStyleClass().add("system-message");
        systemMessage.setWrapText(true);
        
        messageContainer.getChildren().add(systemMessage);
        conversationArea.getChildren().add(messageContainer);
    }
    
    private void addWelcomeMessage() {
        String welcomeText = "Welcome to your AI Learning Assistant! 🎓\n\n" +
                           "I'm here to help you with:\n" +
                           "• Understanding complex concepts\n" +
                           "• Study planning and organization\n" +
                           "• Programming and technical questions\n" +
                           "• Career guidance and advice\n\n" +
                           "Choose a conversation mode above or use the quick start suggestions below. How can I help you today?";
        
        Platform.runLater(() -> addAiMessage(welcomeText));
    }
    
    private void showTypingIndicator() {
        isAiTyping = true;
        typingIndicator.setVisible(true);
        sendButton.setDisable(true);
        inputField.setDisable(true);
    }
    
    private void hideTypingIndicator() {
        isAiTyping = false;
        typingIndicator.setVisible(false);
        sendButton.setDisable(false);
        inputField.setDisable(false);
    }
    
    private void getAiResponse(String userMessage) {
        Task<String> aiResponseTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                // Add current mode to context
                String currentMode = conversationModeSelector.getValue();
                conversationContext.put("mode", currentMode);
                conversationContext.put("timestamp", getCurrentTime());
                
                // Get AI response with context
                return aiAssistant.processQuery(userMessage, conversationContext);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    hideTypingIndicator();
                    String response = getValue();
                    if (response != null && !response.trim().isEmpty()) {
                        addAiMessage(response);
                    } else {
                        addAiMessage("I apologize, but I'm having trouble generating a response right now. Please try rephrasing your question.");
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    hideTypingIndicator();
                    addAiMessage("I apologize, but I encountered an error while processing your request. Please try again.");
                });
            }
        };
        
        // Simulate AI thinking time (1-3 seconds)
        Thread aiThread = new Thread(() -> {
            try {
                Thread.sleep(1000 + (int)(Math.random() * 2000));
                aiResponseTask.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        aiThread.setDaemon(true);
        aiThread.start();
    }
    
    private void clearConversation() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Clear Conversation");
        confirmDialog.setHeaderText("Clear Chat History");
        confirmDialog.setContentText("Are you sure you want to clear the entire conversation history? This action cannot be undone.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                conversationArea.getChildren().clear();
                conversationContext.clear();
                suggestionsBox.setVisible(true);
                suggestionsBox.setManaged(true);
                addWelcomeMessage();
            }
        });
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    // Public methods for integration
    public VBox getMainContainer() {
        return mainContainer;
    }
    
    public void setVisible(boolean visible) {
        mainContainer.setVisible(visible);
        mainContainer.setManaged(visible);
    }
    
    public void focusInput() {
        Platform.runLater(() -> inputField.requestFocus());
    }
    
    public void addContextualSuggestion(String suggestion) {
        Platform.runLater(() -> {
            Button suggestionBtn = new Button(suggestion);
            suggestionBtn.getStyleClass().add("contextual-suggestion");
            suggestionBtn.setOnAction(e -> {
                inputField.setText(suggestion);
                sendMessage();
            });
            // Add to suggestions flow if available
        });
    }
    
    public void updateConversationMode(String mode) {
        if (java.util.Arrays.asList(CONVERSATION_MODES).contains(mode)) {
            conversationModeSelector.setValue(mode);
            conversationContext.put("mode", mode);
        }
    }
    
    public Map<String, String> getConversationContext() {
        return new HashMap<>(conversationContext);
    }
    
    public void addContextualInformation(String key, String value) {
        conversationContext.put(key, value);
    }
    
    public boolean isAiCurrentlyTyping() {
        return isAiTyping;
    }
}
