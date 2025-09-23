package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class to check ONNX model compatibility and suggest solutions
 */
public class ModelCompatibilityChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelCompatibilityChecker.class);
    
    /**
     * Check if model is compatible with current ONNX Runtime
     */
    public static boolean isModelCompatible(Path modelPath) {
        try {
            // Read first few bytes to check ONNX signature
            byte[] header = Files.readAllBytes(modelPath);
            
            if (header.length < 8) {
                logger.warn("Model file too small: {} bytes", header.length);
                return false;
            }
            
            // Check for ONNX magic bytes (protobuf format)
            // ONNX files typically start with protobuf message header
            if (header[0] == 0x08 || header[0] == 0x12) {
                logger.info("Model appears to be valid ONNX format");
                return true;
            }
            
            logger.warn("Model does not appear to be valid ONNX format");
            return false;
            
        } catch (IOException e) {
            logger.error("Failed to read model file: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Provide troubleshooting suggestions for model loading issues
     */
    public static void suggestSolutions(String errorMessage) {
        logger.info("=== MODEL COMPATIBILITY TROUBLESHOOTING ===");
        
        if (errorMessage.contains("IR version")) {
            logger.info("Issue: ONNX IR Version incompatibility");
            logger.info("Solutions:");
            logger.info("1. Update to latest ONNX Runtime (current attempt: 1.19.2)");
            logger.info("2. Convert model to compatible IR version using onnx tools");
            logger.info("3. Use different model export settings");
        }
        
        if (errorMessage.contains("Protobuf")) {
            logger.info("Issue: Model file corruption or format issue");
            logger.info("Solutions:");
            logger.info("1. Re-download the model files");
            logger.info("2. Verify model file integrity");
            logger.info("3. Check if external data file is present");
        }
        
        logger.info("Current model integration status: Framework ready, waiting for compatible model");
        logger.info("===============================================");
    }
    
    /**
     * Create a simple text-based AI fallback for testing
     */
    public static String generateFallbackResponse(String input) {
        // Simple rule-based responses for testing
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("hello") || lowerInput.contains("hi")) {
            return "Hello! I'm ASOS, your learning assistant. I'm currently running in fallback mode while we set up the Gemma model. How can I help you learn today?";
        }
        
        if (lowerInput.contains("help") || lowerInput.contains("what can you do")) {
            return "I can help you with:\n• Java programming tutorials\n• Python learning modules\n• C++ fundamentals\n• Programming progress tracking\n\nClick on the language buttons in the instruction box to start learning!";
        }
        
        if (lowerInput.contains("java")) {
            return "Great choice! Java is a powerful programming language. Start with our Java learning module to learn about classes, objects, and basic syntax.";
        }
        
        if (lowerInput.contains("python")) {
            return "Python is excellent for beginners! Our Python module covers variables, functions, and Python's dynamic features.";
        }
        
        if (lowerInput.contains("c++")) {
            return "C++ is a systems programming language. Our C++ module will teach you about compilation, memory management, and more.";
        }
        
        return "I understand you're asking about: '" + input + "'. I'm currently in fallback mode while setting up the Gemma AI model. Try asking about Java, Python, or C++ programming, or use the language selection buttons!";
    }
}
