package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple, persistent achievement system. Achievements are evaluated against
 * the saved learning progress after every completed step and stored at
 * ~/.asos/achievements.properties (id = unlock timestamp).
 */
public class AchievementManager {

    private static final Logger logger = LoggerFactory.getLogger(AchievementManager.class);

    /** One achievement: id, emoji, and title (English key for I18n). */
    public record Achievement(String id, String emoji, String title) {
        /** Short display name: the part before the " - " description. */
        public String shortTitle() {
            int dash = title.indexOf(" - ");
            return dash > 0 ? title.substring(0, dash) : title;
        }
    }

    private static final List<String> COURSE_KEYS =
            List.of("java-complete", "python-complete", "cpp-complete");

    private static final List<Achievement> ALL = List.of(
            new Achievement("first_step", "🥇", "First Step - you completed your first tutorial step!"),
            new Achievement("five_steps", "✋", "High Five - 5 tutorial steps completed!"),
            new Achievement("ten_steps", "🔟", "Perfect Ten - 10 tutorial steps completed!"),
            new Achievement("course_complete", "🏆", "Course Champion - you finished a whole course!"),
            new Achievement("all_courses", "🌟", "Polyglot - you finished all three courses!"));

    private final Path storageFile;

    public AchievementManager() {
        this(Paths.get(System.getProperty("user.home"), ".asos", "achievements.properties"));
    }

    /** Visible for testing: use a custom storage location. */
    AchievementManager(Path storageFile) {
        this.storageFile = storageFile;
    }

    public static List<Achievement> getAll() {
        return ALL;
    }

    /**
     * Evaluate all achievement conditions against the given progress and
     * persist any newly unlocked ones.
     *
     * @return the achievements unlocked by this evaluation (empty if none)
     */
    public List<Achievement> checkAndUnlock(LearningProgressManager progress) {
        Properties unlocked = load();
        List<Achievement> newlyUnlocked = new ArrayList<>();

        int totalSteps = progress.getAllProgress().keySet().stream()
                .mapToInt(progress::getCompletedChunkCount)
                .sum();
        long completedCourses = COURSE_KEYS.stream()
                .filter(progress::isLanguageCompleted)
                .count();

        for (Achievement achievement : ALL) {
            if (unlocked.containsKey(achievement.id())) {
                continue;
            }
            boolean earned = switch (achievement.id()) {
                case "first_step" -> totalSteps >= 1;
                case "five_steps" -> totalSteps >= 5;
                case "ten_steps" -> totalSteps >= 10;
                case "course_complete" -> completedCourses >= 1;
                case "all_courses" -> completedCourses >= COURSE_KEYS.size();
                default -> false;
            };
            if (earned) {
                unlocked.setProperty(achievement.id(), String.valueOf(System.currentTimeMillis()));
                newlyUnlocked.add(achievement);
            }
        }

        if (!newlyUnlocked.isEmpty()) {
            save(unlocked);
            logger.info("Achievements unlocked: {}",
                    newlyUnlocked.stream().map(Achievement::id).collect(Collectors.joining(", ")));
        }
        return newlyUnlocked;
    }

    /** IDs of all achievements unlocked so far. */
    public Set<String> getUnlockedIds() {
        return load().stringPropertyNames();
    }

    private Properties load() {
        Properties props = new Properties();
        if (Files.exists(storageFile)) {
            try (InputStream in = Files.newInputStream(storageFile)) {
                props.load(in);
            } catch (IOException e) {
                logger.warn("Failed to load achievements: {}", e.getMessage());
            }
        }
        return props;
    }

    private void save(Properties props) {
        try {
            Files.createDirectories(storageFile.getParent());
            try (OutputStream out = Files.newOutputStream(storageFile)) {
                props.store(out, "Asos achievements");
            }
        } catch (IOException e) {
            logger.error("Failed to save achievements: {}", e.getMessage());
        }
    }
}
