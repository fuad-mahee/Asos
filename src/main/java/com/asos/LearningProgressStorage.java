package com.asos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages saving and loading learning progress to/from disk
 */
public class LearningProgressStorage {
    private static final Logger logger = LoggerFactory.getLogger(LearningProgressStorage.class);
    private static final String PROGRESS_FILE = "learning_progress.json";
    private final ObjectMapper objectMapper;
    private final Path progressFilePath;
    
    public LearningProgressStorage() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // Create progress directory if it doesn't exist
        Path progressDir = Paths.get(System.getProperty("user.home"), ".asos");
        try {
            Files.createDirectories(progressDir);
            this.progressFilePath = progressDir.resolve(PROGRESS_FILE);
            logger.info("Progress file path: {}", progressFilePath.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create progress directory", e);
        }
    }
    
    /**
     * Save learning progress to disk
     */
    public void saveProgress(LearningProgressManager progressManager) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                      .writeValue(progressFilePath.toFile(), progressManager);
            logger.info("Learning progress saved successfully to {}", progressFilePath);
        } catch (IOException e) {
            logger.error("Failed to save learning progress", e);
        }
    }
    
    /**
     * Load learning progress from disk
     */
    public LearningProgressManager loadProgress() {
        File progressFile = progressFilePath.toFile();
        
        if (!progressFile.exists()) {
            logger.info("No existing progress file found, creating new progress manager");
            return new LearningProgressManager();
        }
        
        try {
            LearningProgressManager progressManager = objectMapper.readValue(progressFile, LearningProgressManager.class);
            logger.info("Learning progress loaded successfully from {}", progressFilePath);
            return progressManager;
        } catch (IOException e) {
            logger.error("Failed to load learning progress, creating new one", e);
            return new LearningProgressManager();
        }
    }
    
    /**
     * Check if progress file exists
     */
    public boolean progressFileExists() {
        return progressFilePath.toFile().exists();
    }
    
    /**
     * Delete progress file (for reset functionality)
     */
    public boolean deleteProgress() {
        try {
            Files.deleteIfExists(progressFilePath);
            logger.info("Learning progress file deleted");
            return true;
        } catch (IOException e) {
            logger.error("Failed to delete progress file", e);
            return false;
        }
    }
    
    /**
     * Get the size of the progress file
     */
    public long getProgressFileSize() {
        try {
            return Files.size(progressFilePath);
        } catch (IOException e) {
            return 0;
        }
    }
}
