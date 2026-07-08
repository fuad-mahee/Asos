package com.asos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the rule-based fallback responder used when the local
 * Ollama model is unavailable.
 */
class FallbackResponseGeneratorTest {

    @Test
    @DisplayName("Greeting inputs get a greeting response")
    void greetingGetsResponse() {
        String response = FallbackResponseGenerator.generateFallbackResponse("hello there");
        assertNotNull(response);
        assertTrue(response.toLowerCase().contains("hello"));
    }

    @Test
    @DisplayName("Java questions get Java-specific guidance")
    void javaQuestionGetsJavaResponse() {
        String response = FallbackResponseGenerator.generateFallbackResponse("how do I learn java?");
        assertNotNull(response);
        assertTrue(response.contains("Java"));
    }

    @Test
    @DisplayName("Python questions get Python-specific guidance")
    void pythonQuestionGetsPythonResponse() {
        String response = FallbackResponseGenerator.generateFallbackResponse("teach me python");
        assertNotNull(response);
        assertTrue(response.contains("Python"));
    }

    @Test
    @DisplayName("Help requests list available capabilities")
    void helpRequestListsCapabilities() {
        String response = FallbackResponseGenerator.generateFallbackResponse("help");
        assertNotNull(response);
        assertTrue(response.contains("learn"));
    }

    @Test
    @DisplayName("Unmatched input returns null so callers can supply their own default")
    void unmatchedInputReturnsNull() {
        assertNull(FallbackResponseGenerator.generateFallbackResponse("xyzzy quantum flux capacitor"));
    }

    @Test
    @DisplayName("Matching is case-insensitive")
    void matchingIsCaseInsensitive() {
        assertNotNull(FallbackResponseGenerator.generateFallbackResponse("HELLO"));
        assertNotNull(FallbackResponseGenerator.generateFallbackResponse("JAVA basics"));
    }
}
