package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Simple persistent app settings stored at ~/.asos/settings.properties.
 */
public final class AppSettings {

    private static final Logger logger = LoggerFactory.getLogger(AppSettings.class);
    private static final Path SETTINGS_FILE =
            Paths.get(System.getProperty("user.home"), ".asos", "settings.properties");

    public static final String DEFAULT_LANGUAGE = "English";

    private AppSettings() {
    }

    /**
     * The user's preferred language for AI answers (display form, e.g. "বাংলা (Bengali)").
     */
    public static String getLanguage() {
        return load().getProperty("app.language", DEFAULT_LANGUAGE);
    }

    public static void setLanguage(String language) {
        Properties props = load();
        props.setProperty("app.language", language);
        save(props);
    }

    /**
     * Accessibility: larger interface text (helpful for the elderly audience).
     */
    public static boolean isLargeText() {
        return "large".equals(load().getProperty("app.textsize", "normal"));
    }

    public static void setLargeText(boolean large) {
        Properties props = load();
        props.setProperty("app.textsize", large ? "large" : "normal");
        save(props);
    }

    /**
     * Sound cues for step completion, mistakes, and hints.
     */
    public static boolean isSoundEnabled() {
        return Boolean.parseBoolean(load().getProperty("app.sounds", "true"));
    }

    public static void setSoundEnabled(boolean enabled) {
        Properties props = load();
        props.setProperty("app.sounds", Boolean.toString(enabled));
        save(props);
    }

    /**
     * English name of the preferred language for use in AI prompts
     * ("বাংলা (Bengali)" -> "Bengali").
     */
    public static String getLanguageForPrompt() {
        String language = getLanguage();
        int open = language.indexOf('(');
        int close = language.indexOf(')');
        if (open >= 0 && close > open) {
            return language.substring(open + 1, close).trim();
        }
        return language.trim();
    }

    private static Properties load() {
        Properties props = new Properties();
        if (Files.exists(SETTINGS_FILE)) {
            try (InputStream in = Files.newInputStream(SETTINGS_FILE)) {
                props.load(in);
            } catch (IOException e) {
                logger.warn("Failed to load settings: {}", e.getMessage());
            }
        }
        return props;
    }

    private static void save(Properties props) {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            try (OutputStream out = Files.newOutputStream(SETTINGS_FILE)) {
                props.store(out, "Asos settings");
            }
        } catch (IOException e) {
            logger.error("Failed to save settings: {}", e.getMessage());
        }
    }
}
