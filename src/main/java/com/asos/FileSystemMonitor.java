package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * File System Monitor for watching code file changes in real-time.
 * Used by the ASOS study assistant to monitor code changes and provide feedback.
 *
 * Watches user directories recursively (up to a depth limit), automatically
 * registers newly created subdirectories, and reports absolute file paths.
 */
public class FileSystemMonitor {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemMonitor.class);

    private static final int MAX_DEPTH = 5;

    private static final Set<String> SKIPPED_DIR_NAMES = Set.of(
            ".git", ".gradle", ".idea", ".vscode", "node_modules", "build",
            "target", "out", "dist", "__pycache__", ".venv", "venv");

    private WatchService watchService;
    private ExecutorService executorService;
    private BiConsumer<String, String> onFileChange;
    private volatile boolean isRunning = false;

    private final Map<WatchKey, Path> watchedDirectories = new ConcurrentHashMap<>();

    public FileSystemMonitor() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.executorService = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "study-file-monitor");
                t.setDaemon(true);
                return t;
            });
        } catch (IOException e) {
            logger.error("Failed to create FileSystemMonitor", e);
        }
    }

    /**
     * Set the callback function to be called when a file changes.
     * Arguments are (absolute file path, file content).
     */
    public void setOnFileChange(BiConsumer<String, String> callback) {
        this.onFileChange = callback;
    }

    /**
     * Start monitoring the file system.
     */
    public void start() {
        if (isRunning) return;

        isRunning = true;

        // Register common development directories (recursively) for monitoring
        registerTree(Paths.get(System.getProperty("user.home"), "Downloads"));
        registerTree(Paths.get(System.getProperty("user.home"), "Documents"));
        registerTree(Paths.get(System.getProperty("user.home"), "Desktop"));

        executorService.submit(this::monitorLoop);

        logger.info("Started study file monitoring ({} directories registered)",
                watchedDirectories.size());
    }

    /**
     * Stop monitoring the file system.
     */
    public void stop() {
        isRunning = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
        } catch (IOException e) {
            logger.error("Error stopping FileSystemMonitor", e);
        }
        watchedDirectories.clear();
    }

    /**
     * Register a directory tree (root and subdirectories up to MAX_DEPTH).
     */
    private void registerTree(Path root) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try {
            Files.walkFileTree(root, Set.of(), MAX_DEPTH, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (SKIPPED_DIR_NAMES.contains(name) || name.startsWith(".")) {
                        return dir.equals(root) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
                    }
                    registerSingleDirectory(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to register directory tree: {} ({})", root, e.getMessage());
        }
    }

    private void registerSingleDirectory(Path dir) {
        try {
            WatchKey key = dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            watchedDirectories.put(key, dir);
        } catch (IOException e) {
            logger.debug("Failed to register directory: {} ({})", dir, e.getMessage());
        }
    }

    /**
     * Main monitoring loop.
     */
    private void monitorLoop() {
        while (isRunning) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break; // monitor was stopped
            }

            Path watchedDir = watchedDirectories.get(key);

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fullPath = watchedDir != null
                        ? watchedDir.resolve(ev.context())
                        : ev.context();

                // Keep coverage as the learner creates new folders
                if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                    registerTree(fullPath);
                    continue;
                }

                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    continue; // nothing to read for deleted files
                }

                String filePath = fullPath.toString();
                if (isCodeFile(filePath)) {
                    String content = readFileContent(fullPath);
                    if (onFileChange != null && content != null) {
                        onFileChange.accept(filePath, content);
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                watchedDirectories.remove(key);
            }
        }
    }

    /**
     * Check if the file is a code file we should monitor.
     */
    private boolean isCodeFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".java") ||
               lower.endsWith(".py") ||
               lower.endsWith(".js") ||
               lower.endsWith(".ts") ||
               lower.endsWith(".jsx") ||
               lower.endsWith(".tsx") ||
               lower.endsWith(".json") ||
               lower.endsWith(".xml") ||
               lower.endsWith(".html") ||
               lower.endsWith(".css") ||
               lower.endsWith(".cpp") ||
               lower.endsWith(".c") ||
               lower.endsWith(".h");
    }

    /**
     * Read the content of a file.
     */
    private String readFileContent(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            return null;
        }
    }

    // Legacy interface support for existing code compatibility
    public interface ProgressListener {
        void onProgress(String message);
    }

    // Legacy constructor for existing code compatibility
    public FileSystemMonitor(Path pathToWatch, ProgressListener listener) {
        this(); // Call default constructor
        if (listener != null) {
            // Convert old interface to new callback format
            this.setOnFileChange((path, content) -> listener.onProgress("File changed: " + path));
        }
    }
}
