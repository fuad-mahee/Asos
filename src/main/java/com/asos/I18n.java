package com.asos;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal interface translation for the app UI (English / Bangla).
 *
 * Usage: wrap any user-facing string in I18n.t("...") - the English text is
 * the key. If the current language is Bangla and a translation exists, the
 * Bangla text is returned; otherwise the English string is used as-is.
 */
public final class I18n {

    private static final Map<String, String> BN = new HashMap<>();

    static {
        // Corner widget
        BN.put("Your learning buddy is here", "তোমার শেখার বন্ধু এখানে আছে");

        // Main menu
        BN.put("LEARN", "শেখা");
        BN.put("ASSISTANT", "সহকারী");
        BN.put("SETTINGS", "সেটিংস");
        BN.put("📚  Start Tutorial", "📚  টিউটোরিয়াল শুরু করো");
        BN.put("🎯  Teaching Mode", "🎯  টিচিং মোড");
        BN.put("📈  My Progress", "📈  আমার অগ্রগতি");
        BN.put("💬  Ask Asos (AI Chat)", "💬  আছস-কে জিজ্ঞেস করো (এআই চ্যাট)");
        BN.put("🌐  App Language", "🌐  অ্যাপের ভাষা");
        BN.put("⏻  Quit Asos", "⏻  আছস বন্ধ করো");

        // Window titles
        BN.put("Asos — AI Learning Assistant", "আছস — এআই শেখার সহকারী");
        BN.put("My Learning Progress", "আমার শেখার অগ্রগতি");
        BN.put("App Language", "অ্যাপের ভাষা");

        // Course picker
        BN.put("SELECT LEARNING PATH", "শেখার পথ বেছে নাও");
        BN.put("Pick a course above to begin!", "শুরু করতে উপরের একটি কোর্স বেছে নাও!");
        BN.put("Choose Java, Python, or C++ using the buttons at the top of this card. " +
               "Each button shows your saved progress for that course.\n\n" +
               "Once you pick a course, Asos will guide you step by step and detect " +
               "your work automatically - create the files it asks for on your " +
               "Desktop, in Documents, in Downloads, or in the Asos project folder.",
               "উপরের বাটন থেকে Java, Python বা C++ বেছে নাও। প্রতিটি বাটনে সেই কোর্সে " +
               "তোমার সেভ করা অগ্রগতি দেখানো হয়।\n\n" +
               "কোর্স বেছে নিলে আছস তোমাকে ধাপে ধাপে গাইড করবে আর তোমার কাজ নিজে থেকেই " +
               "শনাক্ত করবে - Desktop, Documents, Downloads বা Asos প্রজেক্ট ফোল্ডারে " +
               "ফাইলগুলো তৈরি করলেই হবে।");
        BN.put("🎓 Choose a course to start learning!", "🎓 শেখা শুরু করতে একটি কোর্স বেছে নাও!");
        BN.put("🚀 Starting %s course!", "🚀 %s কোর্স শুরু হচ্ছে!");

        // Progress window
        BN.put("Your Learning Journey", "তোমার শেখার যাত্রা");
        BN.put("Not started", "শুরু হয়নি");
        BN.put("%d / %d steps · %.0f%%", "%d / %d ধাপ · %.0f%%");
        BN.put("Start a course from the menu to see your progress grow!",
               "মেনু থেকে একটি কোর্স শুরু করো, তোমার অগ্রগতি এখানে দেখা যাবে!");
        BN.put("Learning velocity: %.1f%%", "শেখার গতি: %.1f%%");
        BN.put("Consistency score: %.1f%%", "নিয়মিততার স্কোর: %.1f%%");

        // Language window
        BN.put("Choose your preferred language", "তোমার পছন্দের ভাষা বেছে নাও");
        BN.put("Changing the language updates the app's menus and buttons, and the AI chat answers in it too.",
               "ভাষা বদলালে অ্যাপের মেনু ও বাটন সেই ভাষায় দেখা যাবে, আর এআই চ্যাটও সেই ভাষায় উত্তর দেবে।");
        BN.put("🌐 Language updated! Menus and chat now use your language.",
               "🌐 ভাষা বদলে গেছে! মেনু ও চ্যাট এখন বাংলায়।");

        // Teaching engine messages
        BN.put("Great job! Moving to next step...", "দারুণ কাজ! পরের ধাপে যাচ্ছি...");
        BN.put("Congratulations! You've completed the entire learning module!",
               "অভিনন্দন! তুমি পুরো কোর্সটি শেষ করেছ!");
        BN.put("Congratulations! You've completed the %s course!",
               "অভিনন্দন! তুমি %s কোর্সটি শেষ করেছ!");
        BN.put("Take your time and follow the instructions step by step.",
               "সময় নাও, ধাপে ধাপে নির্দেশনা অনুসরণ করো।");
        BN.put("💡 Hint: ", "💡 ইঙ্গিত: ");
        BN.put("Please check your work and try again.", "তোমার কাজটা আরেকবার দেখে আবার চেষ্টা করো।");

        // Notifications / errors
        BN.put("❌ Something went wrong opening this view. Please try again.",
               "❌ কিছু একটা সমস্যা হয়েছে। আবার চেষ্টা করো।");
        BN.put("📚 Teaching mode stopped", "📚 টিচিং মোড বন্ধ হয়েছে");
        BN.put("🎯 Select a tutorial to start teaching mode", "🎯 টিচিং মোড শুরু করতে একটি টিউটোরিয়াল বেছে নাও");

        // Progress stepper
        BN.put("Step %d of %d", "ধাপ %d / %d");

        // Settings toggles
        BN.put("🔠  Text Size: Normal", "🔠  লেখার আকার: সাধারণ");
        BN.put("🔠  Text Size: Large", "🔠  লেখার আকার: বড়");
        BN.put("🔊  Sounds: On", "🔊  শব্দ: চালু");
        BN.put("🔇  Sounds: Off", "🔇  শব্দ: বন্ধ");
        BN.put("🔠 Text size updated", "🔠 লেখার আকার বদলানো হয়েছে");

        // Achievements
        BN.put("🏅 Achievement unlocked: ", "🏅 অর্জন আনলক হয়েছে: ");
        BN.put("ACHIEVEMENTS", "অর্জনসমূহ");
        BN.put("First Step - you completed your first tutorial step!",
               "প্রথম ধাপ - তুমি তোমার প্রথম টিউটোরিয়াল ধাপ শেষ করেছ!");
        BN.put("High Five - 5 tutorial steps completed!", "হাই ফাইভ - ৫টি ধাপ শেষ!");
        BN.put("Perfect Ten - 10 tutorial steps completed!", "দুর্দান্ত দশ - ১০টি ধাপ শেষ!");
        BN.put("Course Champion - you finished a whole course!",
               "কোর্স চ্যাম্পিয়ন - তুমি একটি পুরো কোর্স শেষ করেছ!");
        BN.put("Polyglot - you finished all three courses!", "পলিগ্লট - তিনটি কোর্সই শেষ!");

        // Contextual step help
        BN.put("💬 Ask Asos about this step", "💬 এই ধাপ নিয়ে আছস-কে জিজ্ঞেস করো");
        BN.put("I'm stuck on this step. Can you explain it in a simpler way?",
               "আমি এই ধাপে আটকে গেছি। একটু সহজভাবে বুঝিয়ে দেবে?");

        // Chat window
        BN.put("AI Learning Assistant", "এআই শেখার সহকারী");
        BN.put("Mode:", "মোড:");
        BN.put("Clear Chat", "চ্যাট মুছে ফেলো");
        BN.put("QUICK START", "দ্রুত শুরু");
        BN.put("Send", "পাঠাও");
        BN.put("Type your question or ask for help...", "তোমার প্রশ্ন লেখো বা সাহায্য চাও...");
        BN.put("Welcome to your AI Learning Assistant! 🎓\n\n" +
               "I'm here to help you with:\n" +
               "• Understanding complex concepts\n" +
               "• Study planning and organization\n" +
               "• Programming and technical questions\n" +
               "• Career guidance and advice\n\n" +
               "Choose a conversation mode above or use the quick start suggestions below. How can I help you today?",
               "তোমার এআই শেখার সহকারীতে স্বাগতম! 🎓\n\n" +
               "আমি তোমাকে সাহায্য করতে পারি:\n" +
               "• কঠিন বিষয় সহজভাবে বুঝতে\n" +
               "• পড়াশোনার পরিকল্পনা করতে\n" +
               "• প্রোগ্রামিং ও প্রযুক্তির প্রশ্নে\n" +
               "• ক্যারিয়ার নিয়ে পরামর্শে\n\n" +
               "নিচের সাজেশন থেকে শুরু করতে পারো। আজ কীভাবে সাহায্য করতে পারি?");
    }

    private I18n() {
    }

    /**
     * True when the interface language is Bangla.
     */
    public static boolean isBangla() {
        return AppSettings.getLanguage().contains("বাংলা");
    }

    /**
     * Translate a UI string. The English text is the key; falls back to the
     * English text itself when no translation exists.
     */
    public static String t(String english) {
        return isBangla() ? BN.getOrDefault(english, english) : english;
    }
}
