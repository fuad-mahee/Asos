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
        this(Paths.get(System.getProperty("user.home"), ".asos").resolve(PROGRESS_FILE));
    }

    /** Visible for testing: use a custom storage location. */
    LearningProgressStorage(Path progressFilePath) {
        this.objectMapper = new ObjectMapper();
        // REQUIRED for the LocalDateTime fields in LearningProgressManager -
        // without this module every save throws mid-write, silently truncating
        // the file (this was why progress never persisted)
        this.objectMapper.registerModule(
                new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // Never let an unknown/extra field in an old progress file wipe all
        // progress - ignore what we don't recognize instead of failing
        this.objectMapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            Files.createDirectories(progressFilePath.getParent());
            this.progressFilePath = progressFilePath;
            logger.info("Progress file path: {}", progressFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create progress directory", e);
        }
    }
    
    /**
     * Save learning progress to disk atomically: write to a temp file first,
     * then swap it into place. If the app is killed mid-save, the previous
     * progress file stays intact instead of being left truncated/corrupt.
     */
    public void saveProgress(LearningProgressManager progressManager) {
        try {
            Path tempFile = progressFilePath.resolveSibling(PROGRESS_FILE + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter()
                      .writeValue(tempFile.toFile(), progressManager);
            try {
                Files.move(tempFile, progressFilePath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                // Some filesystems refuse atomic replaces - fall back to a
                // plain replace rather than losing the save
                Files.move(tempFile, progressFilePath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
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
