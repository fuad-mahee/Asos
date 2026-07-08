package com.asos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the progress arithmetic shown in the UI. The old code displayed
 * currentChunkId / total, which starts at 1 and means "next chunk to do" -
 * so an untouched course showed 17% and everything was one step ahead.
 */
class LearningProgressTrackingTest {

    @Test
    @DisplayName("A course that was never started shows zero progress")
    void freshCourseHasZeroProgress() {
        LearningProgressManager manager = new LearningProgressManager();
        manager.initializeLanguage("java-complete", 6);

        assertEquals(0, manager.getCompletedChunkCount("java-complete"));
        assertEquals(0.0, manager.getLanguageProgressPercentage("java-complete"));
        assertFalse(manager.isLanguageCompleted("java-complete"));
    }

    @Test
    @DisplayName("Completing chunks advances the completed count and percentage")
    void completingChunksTracksCorrectly() {
        LearningProgressManager manager = new LearningProgressManager();
        manager.initializeLanguage("java-complete", 6);

        manager.completeChunk("java-complete", 1);
        assertEquals(1, manager.getCompletedChunkCount("java-complete"));
        assertEquals(100.0 / 6, manager.getLanguageProgressPercentage("java-complete"), 0.01);

        manager.completeChunk("java-complete", 2);
        assertEquals(2, manager.getCompletedChunkCount("java-complete"));
        // Next chunk to do is 3, but only 2 are COMPLETED
        assertEquals(3, manager.getCurrentChunkForLanguage("java-complete"));
    }

    @Test
    @DisplayName("markLanguageCompleted fills the record to 100%")
    void markCompletedReachesHundredPercent() {
        LearningProgressManager manager = new LearningProgressManager();
        manager.initializeLanguage("python-complete", 7);
        manager.completeChunk("python-complete", 1);

        manager.markLanguageCompleted("python-complete");

        assertTrue(manager.isLanguageCompleted("python-complete"));
        assertEquals(7, manager.getCompletedChunkCount("python-complete"));
        assertEquals(100.0, manager.getLanguageProgressPercentage("python-complete"));
    }

    @Test
    @DisplayName("Progress can never exceed 100% even with stale duplicate records")
    void progressIsClamped() {
        LearningProgressManager manager = new LearningProgressManager();
        manager.initializeLanguage("cpp-complete", 2);

        // Simulate stale data marking more chunks than exist
        manager.completeChunk("cpp-complete", 1);
        manager.completeChunk("cpp-complete", 2);
        manager.getLanguageProgress("cpp-complete").markChunkCompleted(3);

        assertEquals(2, manager.getCompletedChunkCount("cpp-complete"));
        assertEquals(100.0, manager.getLanguageProgressPercentage("cpp-complete"));
    }

    @Test
    @DisplayName("Different modules of the same language have separate records")
    void modulesTrackSeparately() {
        LearningProgressManager manager = new LearningProgressManager();
        manager.initializeLanguage("java-hello-world", 5);
        manager.initializeLanguage("java-complete", 6);

        manager.completeChunk("java-hello-world", 1);

        assertEquals(1, manager.getCompletedChunkCount("java-hello-world"));
        assertEquals(0, manager.getCompletedChunkCount("java-complete"));
        assertEquals(5, manager.getTotalChunks("java-hello-world"));
        assertEquals(6, manager.getTotalChunks("java-complete"));
    }
}
