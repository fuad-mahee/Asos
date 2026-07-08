package com.asos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the interface language switch: Bangla translations resolve, and
 * untranslated strings fall back to English instead of breaking the UI.
 */
class I18nTest {

    @Test
    @DisplayName("Bangla mode translates known strings and falls back for unknown ones")
    void banglaTranslationsResolve() {
        String original = AppSettings.getLanguage();
        try {
            AppSettings.setLanguage("বাংলা (Bengali)");
            assertTrue(I18n.isBangla());
            assertEquals("📚  টিউটোরিয়াল শুরু করো", I18n.t("📚  Start Tutorial"));
            assertEquals("📈  আমার অগ্রগতি", I18n.t("📈  My Progress"));
            assertEquals("🎯  টিচিং মোড", I18n.t("🎯  Teaching Mode"));
            // Unknown strings fall back to English rather than showing blanks
            assertEquals("some untranslated text", I18n.t("some untranslated text"));

            AppSettings.setLanguage("English");
            assertFalse(I18n.isBangla());
            assertEquals("📚  Start Tutorial", I18n.t("📚  Start Tutorial"));
        } finally {
            AppSettings.setLanguage(original);
        }
    }

    @Test
    @DisplayName("Format strings keep their placeholders in both languages")
    void formatStringsKeepPlaceholders() {
        String original = AppSettings.getLanguage();
        try {
            AppSettings.setLanguage("বাংলা (Bengali)");
            String formatted = String.format(I18n.t("%d / %d steps · %.0f%%"), 2, 7, 28.6);
            assertTrue(formatted.contains("2 / 7"));
        } finally {
            AppSettings.setLanguage(original);
        }
    }
}
