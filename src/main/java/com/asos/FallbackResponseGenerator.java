package com.asos;

/**
 * Rule-based fallback responses used when the local AI model (Ollama) is
 * unavailable - e.g. Ollama not installed, not running, or the model not pulled.
 * Keeps the assistant usable in a degraded, keyword-matched mode.
 */
public class FallbackResponseGenerator {

    private FallbackResponseGenerator() {
        // utility class
    }

    /**
     * Generate a canned response for common learning questions.
     *
     * @return a response string, or null if no rule matched (callers should
     *         then provide their own generic default)
     */
    public static String generateFallbackResponse(String input) {
        String lowerInput = input.toLowerCase().trim();

        if (lowerInput.contains("hello") || lowerInput.contains("hi") || lowerInput.contains("hey")) {
            return "Hello! I'm ASOS, your learning assistant. How can I help you learn today?";
        }

        if (lowerInput.contains("help") || lowerInput.contains("what can you do")) {
            return "I can help you with:\n• Java programming tutorials\n• Python learning modules\n• C++ fundamentals\n• Programming progress tracking\n• Explaining concepts step by step\n• Debugging code issues\n\nWhat would you like to learn about?";
        }

        if (lowerInput.contains("java")) {
            return "Great choice! Java is a powerful programming language. I can help you learn about:\n• Classes and Objects\n• Basic syntax and variables\n• Control structures (if/else, loops)\n• Methods and functions\n• Object-oriented programming concepts\n\nWhat Java topic interests you most?";
        }

        if (lowerInput.contains("python")) {
            return "Python is excellent for beginners! I can teach you about:\n• Variables and data types\n• Functions and modules\n• Lists, dictionaries, and tuples\n• Control flow (if statements, loops)\n• File handling and libraries\n\nWhat Python concept would you like to explore?";
        }

        if (lowerInput.contains("c++")) {
            return "C++ is a powerful systems programming language! I can help with:\n• Basic syntax and compilation\n• Memory management (pointers, references)\n• Classes and inheritance\n• Standard Template Library (STL)\n• Performance optimization\n\nWhat C++ topic would you like to learn?";
        }

        if (lowerInput.contains("javascript") || lowerInput.contains("js")) {
            return "JavaScript is great for web development! I can help you learn:\n• Variables, functions, and objects\n• DOM manipulation\n• Event handling\n• Asynchronous programming (promises, async/await)\n• Modern ES6+ features\n\nWhat JavaScript concept interests you?";
        }

        if (lowerInput.contains("html") || lowerInput.contains("css")) {
            return "Web development fundamentals! I can teach you:\n• HTML structure and semantic elements\n• CSS styling and layouts\n• Responsive design\n• Flexbox and Grid\n• Web accessibility\n\nWhat web development topic would you like to explore?";
        }

        if (lowerInput.contains("algorithm") || lowerInput.contains("data structure")) {
            return "Algorithms and data structures are fundamental to programming! I can explain:\n• Array and string manipulation\n• Sorting and searching algorithms\n• Trees, graphs, and hash tables\n• Time and space complexity\n• Problem-solving techniques\n\nWhat algorithm topic interests you?";
        }

        if (lowerInput.contains("debug") || lowerInput.contains("error") || lowerInput.contains("fix")) {
            return "Debugging is an essential skill! I can help you:\n• Understand common error messages\n• Use debugging techniques and tools\n• Write better code to prevent bugs\n• Test your code effectively\n\nWhat kind of issue are you trying to solve?";
        }

        // No specific match found - let the caller handle it
        return null;
    }
}
