package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Real-time monitoring system for detecting learner actions.
 * Monitors file system changes and verifies compilation/run results.
 *
 * Since the app cannot observe the learner's external terminal, COMPILE_SUCCESS
 * is detected by watching for the compiled artifact (.class file) to appear,
 * and RUN_SUCCESS is verified by running the learner's compiled program in the
 * background and checking its output against the expected pattern.
 */
public class RealTimeMonitor {
    private static final Logger logger = LoggerFactory.getLogger(RealTimeMonitor.class);

    private static final long ARTIFACT_POLL_INTERVAL_SECONDS = 3;

    private final FileSystemWatcher fileWatcher;
    private final TerminalMonitor terminalMonitor;
    private final CodeAnalyzer codeAnalyzer;
    private final ScheduledExecutorService scheduler;

    private List<LearningChunk.ExpectedAction> expectedActions = new ArrayList<>();
    private final Set<LearningChunk.ExpectedAction> completedActions;
    private final Map<String, String> fileContents;

    /** Last known absolute location of each file name seen in file events. */
    private final Map<String, Path> fileLocations = new ConcurrentHashMap<>();

    private ScheduledFuture<?> artifactPollTask;
    private final AtomicBoolean verificationRunInProgress = new AtomicBoolean(false);

    /** Verification runs per action, so a broken program isn't re-run forever. */
    private final Map<LearningChunk.ExpectedAction, Integer> verificationAttempts = new ConcurrentHashMap<>();
    private static final int MAX_VERIFICATION_ATTEMPTS = 10;

    // Callbacks
    private Consumer<LearningChunk.ExpectedAction> onActionDetected;
    private Consumer<String> onErrorDetected;

    public RealTimeMonitor() {
        this.fileWatcher = new FileSystemWatcher();
        this.terminalMonitor = new TerminalMonitor();
        this.codeAnalyzer = new CodeAnalyzer();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "realtime-monitor");
            t.setDaemon(true);
            return t;
        });
        this.completedActions = ConcurrentHashMap.newKeySet();
        this.fileContents = new ConcurrentHashMap<>();

        setupWatchers();
    }

    /**
     * Start monitoring for specific expected actions.
     */
    public void startMonitoring(List<LearningChunk.ExpectedAction> expectedActions) {
        this.expectedActions = new ArrayList<>(expectedActions);
        this.completedActions.clear();
        this.verificationAttempts.clear();

        // Start all watchers
        fileWatcher.startWatching();
        terminalMonitor.startMonitoring();

        // Poll for compile/run artifacts if this chunk expects them
        scheduleArtifactPollingIfNeeded();

        logger.info("Started monitoring for {} expected actions", expectedActions.size());
    }

    /**
     * Stop all monitoring.
     */
    public void stopMonitoring() {
        cancelArtifactPolling();
        fileWatcher.stopWatching();
        terminalMonitor.stopMonitoring();
        completedActions.clear();

        logger.info("Stopped monitoring");
    }

    /**
     * Check if all expected actions are completed.
     */
    public boolean areAllActionsCompleted() {
        return !expectedActions.isEmpty() && completedActions.size() == expectedActions.size();
    }

    /**
     * Setup watchers with callbacks.
     */
    private void setupWatchers() {
        // File system watcher
        fileWatcher.setOnFileEvent(this::handleFileEvent);

        // Terminal output watcher (fires for verification runs executed by the app)
        terminalMonitor.setOnTerminalOutput(this::handleTerminalOutput);

        // Code analyzer for syntax/error detection (show just the file name -
        // full paths are too long for the notification card)
        codeAnalyzer.setOnErrorDetected((filePath, error) -> {
            if (onErrorDetected != null) {
                Path p = Paths.get(filePath);
                String name = p.getFileName() != null ? p.getFileName().toString() : filePath;
                onErrorDetected.accept(name + ": " + error);
            }
        });
    }

    /**
     * Handle file system events. The path is absolute.
     */
    private void handleFileEvent(FileSystemWatcher.EventType eventType, String filePath) {
        logger.debug("File event: {} - {}", eventType, filePath);

        // Remember where files live so compile/run verification can find them later
        Path path = Paths.get(filePath);
        if (path.getFileName() != null && eventType != FileSystemWatcher.EventType.DELETED) {
            fileLocations.put(path.getFileName().toString(), path);
        }

        // Run error analysis on any modified file the current chunk cares about,
        // so typos get caught even in chunks that only expect CODE_CONTAINS
        if (eventType == FileSystemWatcher.EventType.MODIFIED && isRelevantToCurrentChunk(filePath)) {
            updateFileContents(filePath);
        }

        // "Almost right" detection: file created with the right name but wrong
        // extension or casing (e.g. Hello.txt or hello.java instead of Hello.java)
        if (eventType == FileSystemWatcher.EventType.CREATED) {
            checkNearMissCreation(filePath);
        }

        for (LearningChunk.ExpectedAction action : expectedActions) {
            if (completedActions.contains(action)) continue;

            boolean actionMatched = false;

            switch (action.getType()) {
                case FILE_CREATED:
                    if (eventType == FileSystemWatcher.EventType.CREATED &&
                        matchesPattern(filePath, action.getPattern())) {
                        actionMatched = true;
                    }
                    break;

                case FILE_MODIFIED:
                    if (eventType == FileSystemWatcher.EventType.MODIFIED &&
                        matchesPattern(filePath, action.getPattern())) {
                        actionMatched = true;
                    }
                    break;

                case CODE_CONTAINS:
                    if (eventType == FileSystemWatcher.EventType.MODIFIED &&
                        filePath.contains(action.getTarget())) {
                        // Check if code contains expected pattern
                        String content = readFileContent(filePath);
                        if (content != null && matchesPattern(content, action.getPattern())) {
                            actionMatched = true;
                        }
                    }
                    break;

                case COMPILE_SUCCESS:
                    // The compiled artifact (.class / .exe) appearing is proof of compilation
                    if ((eventType == FileSystemWatcher.EventType.CREATED ||
                         eventType == FileSystemWatcher.EventType.MODIFIED)
                            && path.getFileName() != null) {
                        String eventFileName = path.getFileName().toString();
                        if (compiledArtifactNamesFor(action.getTarget()).contains(eventFileName)) {
                            actionMatched = true;
                        }
                    }
                    break;

                default:
                    break;
            }

            if (actionMatched) {
                markActionCompleted(action);
            }
        }
    }

    /**
     * Handle terminal output from verification runs.
     */
    private void handleTerminalOutput(String command, String output) {
        logger.debug("Terminal output: {} -> {}", command, output);

        for (LearningChunk.ExpectedAction action : expectedActions) {
            if (completedActions.contains(action)) continue;

            boolean actionMatched = false;

            switch (action.getType()) {
                case TERMINAL_OUTPUT:
                    if (command.contains(action.getTarget()) &&
                        matchesPattern(output, action.getPattern())) {
                        actionMatched = true;
                    }
                    break;

                case COMPILE_SUCCESS:
                    if (findCompiledArtifact(action.getTarget()) != null) {
                        actionMatched = true;
                    }
                    break;

                case RUN_SUCCESS:
                    // The verification command always contains the program's base
                    // name (java Hello / python ...hello.py / ...hello.exe)
                    if (command.contains(baseName(action.getTarget())) &&
                        matchesPattern(output, action.getPattern())) {
                        actionMatched = true;
                    }
                    break;

                default:
                    break;
            }

            if (actionMatched) {
                markActionCompleted(action);
            }
        }
    }

    // ------------------------------------------------------------------
    // Compile/run artifact verification
    // ------------------------------------------------------------------

    /**
     * Poll for conditions that file events alone can't cover:
     * - COMPILE_SUCCESS / RUN_SUCCESS artifacts and verification runs
     * - FILE_CREATED / CODE_CONTAINS satisfied by files that already existed
     *   before this chunk started (e.g. resuming a course)
     */
    private void scheduleArtifactPollingIfNeeded() {
        cancelArtifactPolling();

        boolean needed = expectedActions.stream().anyMatch(a ->
                a.getType() == LearningChunk.ExpectedAction.ActionType.COMPILE_SUCCESS ||
                a.getType() == LearningChunk.ExpectedAction.ActionType.RUN_SUCCESS ||
                a.getType() == LearningChunk.ExpectedAction.ActionType.FILE_CREATED ||
                a.getType() == LearningChunk.ExpectedAction.ActionType.CODE_CONTAINS);
        if (!needed) {
            return;
        }

        artifactPollTask = scheduler.scheduleWithFixedDelay(this::pollArtifacts,
                1, ARTIFACT_POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void cancelArtifactPolling() {
        if (artifactPollTask != null) {
            artifactPollTask.cancel(false);
            artifactPollTask = null;
        }
    }

    private void pollArtifacts() {
        try {
            for (LearningChunk.ExpectedAction action : expectedActions) {
                if (completedActions.contains(action)) continue;

                switch (action.getType()) {
                    case FILE_CREATED: {
                        // File may already exist from a previous session
                        Path existing = findArtifact(action.getTarget());
                        if (existing != null && matchesPattern(existing.toString(), action.getPattern())) {
                            markActionCompleted(action);
                        }
                        break;
                    }
                    case CODE_CONTAINS: {
                        Path file = findArtifact(action.getTarget());
                        if (file != null) {
                            String content = readFileContent(file.toString());
                            if (content != null && matchesPattern(content, action.getPattern())) {
                                markActionCompleted(action);
                            }
                        }
                        break;
                    }
                    case COMPILE_SUCCESS: {
                        if (findCompiledArtifact(action.getTarget()) != null) {
                            markActionCompleted(action);
                        }
                        break;
                    }
                    case RUN_SUCCESS: {
                        startVerificationRun(action);
                        break;
                    }
                    default:
                        break;
                }
            }

            if (areAllActionsCompleted()) {
                cancelArtifactPolling();
            }
        } catch (Exception e) {
            logger.warn("Artifact polling failed: {}", e.getMessage());
        }
    }

    /**
     * Run the learner's program in the background and let the terminal monitor's
     * output callback match it against the RUN_SUCCESS pattern. Supports Java
     * classes, Python scripts, and compiled C/C++ executables.
     */
    private void startVerificationRun(LearningChunk.ExpectedAction action) {
        int attempts = verificationAttempts.getOrDefault(action, 0);
        if (attempts >= MAX_VERIFICATION_ATTEMPTS) {
            return; // program keeps failing to produce the expected output
        }

        String command = buildRunCommand(action.getTarget());
        if (command == null) {
            return; // program/artifact not found yet - keep polling
        }

        if (verificationRunInProgress.compareAndSet(false, true)) {
            verificationAttempts.put(action, attempts + 1);
            logger.info("Verifying learner program (attempt {}): {}", attempts + 1, command);
            terminalMonitor.executeAndMonitor(command)
                    .whenComplete((output, error) -> verificationRunInProgress.set(false));
        }
    }

    /**
     * Build the command that runs the learner's program, based on the target's language.
     */
    private String buildRunCommand(String target) {
        if (target.endsWith(".py")) {
            Path script = findArtifact(target);
            return script != null ? String.format("python \"%s\"", script) : null;
        }

        if (target.endsWith(".cpp") || target.endsWith(".cc") || target.endsWith(".c")) {
            Path executable = findCompiledArtifact(target);
            return executable != null ? "\"" + executable + "\"" : null;
        }

        // Java: target is a class name ("Hello") or source file ("Hello.java")
        String clazz = baseName(target);
        Path classFile = findCompiledArtifact(clazz + ".java");
        return classFile != null
                ? String.format("java -cp \"%s\" %s", classFile.getParent(), clazz)
                : null;
    }

    /**
     * Locate a file by name: first from observed file events, then by checking
     * common working directories.
     */
    private Path findArtifact(String fileName) {
        Path known = fileLocations.get(fileName);
        if (known != null && Files.isRegularFile(known)) {
            return known;
        }

        List<Path> candidates = List.of(
                Paths.get(System.getProperty("user.dir"), fileName),
                Paths.get(System.getProperty("user.home"), "Desktop", fileName),
                Paths.get(System.getProperty("user.home"), "Documents", fileName),
                Paths.get(System.getProperty("user.home"), "Downloads", fileName));

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                fileLocations.put(fileName, candidate);
                return candidate;
            }
        }

        return null;
    }

    /**
     * Locate the compiled artifact for a source target, also checking next to
     * the source file itself (javac/g++ put outputs beside the source).
     */
    private Path findCompiledArtifact(String sourceTarget) {
        Path sourceDir = null;
        Path source = fileLocations.get(Paths.get(sourceTarget).getFileName().toString());
        if (source != null && source.getParent() != null) {
            sourceDir = source.getParent();
        }

        for (String artifactName : compiledArtifactNamesFor(sourceTarget)) {
            Path found = findArtifact(artifactName);
            if (found != null) {
                return found;
            }
            if (sourceDir != null) {
                Path sibling = sourceDir.resolve(artifactName);
                if (Files.isRegularFile(sibling)) {
                    fileLocations.put(artifactName, sibling);
                    return sibling;
                }
            }
        }
        return null;
    }

    /**
     * Names the compiler's output can have for a given source target.
     */
    private List<String> compiledArtifactNamesFor(String target) {
        String base = baseName(target);
        if (target.endsWith(".cpp") || target.endsWith(".cc") || target.endsWith(".c")) {
            return List.of(base + ".exe", base + ".out", base, "a.exe", "a.out");
        }
        // Java source or bare class name
        return List.of(base + ".class");
    }

    /** "Hello.java" / "hello.py" / "Hello" -> base name without extension */
    private String baseName(String target) {
        int lastDot = target.lastIndexOf('.');
        return lastDot > 0 ? target.substring(0, lastDot) : target;
    }

    /**
     * True when the file is a target of any expected action of the current chunk.
     */
    private boolean isRelevantToCurrentChunk(String filePath) {
        for (LearningChunk.ExpectedAction action : expectedActions) {
            String target = action.getTarget();
            if (target != null && !target.isBlank() && filePath.endsWith(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detect "almost right" file creations: same base name as the expected file
     * but wrong extension or wrong casing - and give specific corrective feedback.
     */
    private void checkNearMissCreation(String filePath) {
        Path path = Paths.get(filePath);
        if (path.getFileName() == null) return;
        String fileName = path.getFileName().toString();
        String createdBase = stripExtension(fileName);

        for (LearningChunk.ExpectedAction action : expectedActions) {
            if (completedActions.contains(action)) continue;
            if (action.getType() != LearningChunk.ExpectedAction.ActionType.FILE_CREATED) continue;

            String targetName = action.getTarget();
            if (targetName == null || fileName.equals(targetName)) continue;

            boolean sameBaseName = createdBase.equalsIgnoreCase(stripExtension(targetName));
            if (sameBaseName && !matchesPattern(filePath, action.getPattern()) && onErrorDetected != null) {
                onErrorDetected.accept(String.format(
                        "Almost! You created '%s' but this step needs '%s'. " +
                        "Check the exact file name - the extension and capital letters matter. Please rename it and try again.",
                        fileName, targetName));
            }
        }
    }

    private String stripExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    /**
     * Mark an action as completed.
     */
    private void markActionCompleted(LearningChunk.ExpectedAction action) {
        completedActions.add(action);
        logger.info("Action completed: {} for {}", action.getType(), action.getTarget());

        if (onActionDetected != null) {
            onActionDetected.accept(action);
        }
    }

    /**
     * Check if text matches a pattern (regex substring match, falling back to
     * a plain case-insensitive contains check for invalid regex).
     */
    private boolean matchesPattern(String text, String pattern) {
        if (text == null || pattern == null) return false;

        try {
            return Pattern.compile(pattern).matcher(text).find();
        } catch (Exception e) {
            // Fallback to simple contains check
            return text.toLowerCase().contains(pattern.toLowerCase());
        }
    }

    /**
     * Read file content safely.
     */
    private String readFileContent(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            logger.warn("Failed to read file: {}", filePath);
            return null;
        }
    }

    /**
     * Update cached file contents and trigger analysis.
     */
    private void updateFileContents(String filePath) {
        String content = readFileContent(filePath);
        if (content != null) {
            fileContents.put(filePath, content);

            // Trigger code analysis for common programming languages
            if (filePath.endsWith(".java") || filePath.endsWith(".py") ||
                filePath.endsWith(".js") || filePath.endsWith(".ts")) {
                codeAnalyzer.analyzeCode(filePath, content);
            }
        }
    }

    // Setters for callbacks
    public void setOnActionDetected(Consumer<LearningChunk.ExpectedAction> callback) {
        this.onActionDetected = callback;
    }

    public void setOnErrorDetected(Consumer<String> callback) {
        this.onErrorDetected = callback;
    }

    // Getters
    public Set<LearningChunk.ExpectedAction> getCompletedActions() {
        return new HashSet<>(completedActions);
    }

    public int getCompletedActionCount() {
        return completedActions.size();
    }

    public int getTotalActionCount() {
        return expectedActions != null ? expectedActions.size() : 0;
    }
}
