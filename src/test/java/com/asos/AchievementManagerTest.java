package com.asos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Achievement unlock conditions and persistence, tested against a temp file
 * so the user's real achievement data is never touched.
 */
class AchievementManagerTest {

    @TempDir
    Path tempDir;

    private AchievementManager newManager() {
        return new AchievementManager(tempDir.resolve("achievements.properties"));
    }

    @Test
    @DisplayName("No achievements before any step is completed")
    void nothingUnlockedInitially() {
        LearningProgressManager progress = new LearningProgressManager();
        progress.initializeLanguage("java-complete", 6);

        assertTrue(newManager().checkAndUnlock(progress).isEmpty());
    }

    @Test
    @DisplayName("First completed step unlocks First Step, and only once")
    void firstStepUnlocksOnce() {
        LearningProgressManager progress = new LearningProgressManager();
        progress.initializeLanguage("java-complete", 6);
        progress.completeChunk("java-complete", 1);

        AchievementManager manager = newManager();
        List<AchievementManager.Achievement> first = manager.checkAndUnlock(progress);
        assertEquals(1, first.size());
        assertEquals("first_step", first.get(0).id());

        // Re-evaluating must not unlock it again
        assertTrue(manager.checkAndUnlock(progress).isEmpty());
        assertTrue(manager.getUnlockedIds().contains("first_step"));
    }

    @Test
    @DisplayName("Step milestones accumulate across courses")
    void stepMilestonesAccumulateAcrossCourses() {
        LearningProgressManager progress = new LearningProgressManager();
        progress.initializeLanguage("java-complete", 6);
        progress.initializeLanguage("python-complete", 7);
        for (int i = 1; i <= 3; i++) progress.completeChunk("java-complete", i);
        for (int i = 1; i <= 2; i++) progress.completeChunk("python-complete", i);

        // 5 total steps -> first_step + five_steps
        List<AchievementManager.Achievement> unlocked = newManager().checkAndUnlock(progress);
        assertEquals(2, unlocked.size());
        assertTrue(unlocked.stream().anyMatch(a -> a.id().equals("five_steps")));
    }

    @Test
    @DisplayName("Completing courses unlocks champion and polyglot")
    void courseCompletionAchievements() {
        LearningProgressManager progress = new LearningProgressManager();
        for (String course : List.of("java-complete", "python-complete", "cpp-complete")) {
            progress.initializeLanguage(course, 3);
            progress.markLanguageCompleted(course);
        }

        AchievementManager manager = newManager();
        List<AchievementManager.Achievement> unlocked = manager.checkAndUnlock(progress);

        assertTrue(unlocked.stream().anyMatch(a -> a.id().equals("course_complete")));
        assertTrue(unlocked.stream().anyMatch(a -> a.id().equals("all_courses")));
    }

    @Test
    @DisplayName("Unlocks persist across manager instances")
    void unlocksPersist() {
        LearningProgressManager progress = new LearningProgressManager();
        progress.initializeLanguage("java-complete", 6);
        progress.completeChunk("java-complete", 1);

        newManager().checkAndUnlock(progress);

        // A fresh manager reading the same file sees the unlock
        assertTrue(newManager().getUnlockedIds().contains("first_step"));
    }

    @Test
    @DisplayName("Short titles strip the description part")
    void shortTitles() {
        AchievementManager.Achievement a = AchievementManager.getAll().get(0);
        assertEquals("First Step", a.shortTitle());
    }
}
