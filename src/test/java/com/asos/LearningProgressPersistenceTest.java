package com.asos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the bug where progress NEVER persisted: the ObjectMapper
 * was missing the JavaTimeModule, so serializing the LocalDateTime fields threw
 * mid-write and left a truncated file (always cut off at "lastAccessed").
 *
 * This test does a real save -> load round-trip on a real file, which fails
 * immediately if date/time serialization ever breaks again.
 */
class LearningProgressPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Progress survives a full save -> load round-trip on disk")
    void progressSurvivesSaveLoadRoundTrip() {
        Path file = tempDir.resolve("learning_progress.json");
        LearningProgressStorage storage = new LearningProgressStorage(file);

        LearningProgressManager manager = new LearningProgressManager();
        manager.initializeLanguage("java-complete", 6);
        manager.setCurrentLanguage("java-complete");
        manager.completeChunk("java-complete", 1);
        manager.completeChunk("java-complete", 2);

        storage.saveProgress(manager);

        // The save must actually produce the final file (not a stranded .tmp)
        assertTrue(Files.exists(file), "Progress file must exist after saving");
        assertFalse(Files.exists(tempDir.resolve("learning_progress.json.tmp")),
                "Temp file must be renamed away after a successful save");

        // And a fresh load must return the same progress
        LearningProgressManager loaded = new LearningProgressStorage(file).loadProgress();
        assertEquals(2, loaded.getCompletedChunkCount("java-complete"),
                "Completed steps must survive the round-trip");
        assertEquals(6, loaded.getTotalChunks("java-complete"));
        assertEquals(3, loaded.getCurrentChunkForLanguage("java-complete"));
        assertEquals("java-complete", loaded.getCurrentLanguage());
    }

    @Test
    @DisplayName("Saving repeatedly keeps the file valid (no truncation)")
    void repeatedSavesStayValid() throws Exception {
        Path file = tempDir.resolve("learning_progress.json");
        LearningProgressStorage storage = new LearningProgressStorage(file);

        LearningProgressManager manager = new LearningProgressManager();
        manager.initializeLanguage("python-complete", 7);
        for (int chunk = 1; chunk <= 7; chunk++) {
            manager.completeChunk("python-complete", chunk);
            storage.saveProgress(manager);
        }
        manager.markLanguageCompleted("python-complete");
        storage.saveProgress(manager);

        String json = Files.readString(file);
        assertTrue(json.trim().endsWith("}"), "Saved JSON must be complete, not truncated");

        LearningProgressManager loaded = new LearningProgressStorage(file).loadProgress();
        assertTrue(loaded.isLanguageCompleted("python-complete"));
        assertEquals(100.0, loaded.getLanguageProgressPercentage("python-complete"));
    }
}
