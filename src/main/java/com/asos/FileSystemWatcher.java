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
 * File System Watcher for detecting file creation, modification, and deletion.
 *
 * Watches user directories recursively (up to a depth limit), automatically
 * registers newly created subdirectories, and reports absolute file paths.
 */
public class FileSystemWatcher {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemWatcher.class);

    /** How deep below each root directory to register watches. */
    private static final int MAX_DEPTH = 5;

    /** Directory names that would explode the watch count with no learning value. */
    private static final Set<String> SKIPPED_DIR_NAMES = Set.of(
            ".git", ".gradle", ".idea", ".vscode", "node_modules", "build",
            "target", "out", "dist", "__pycache__", ".venv", "venv");

    public enum EventType {
        CREATED, MODIFIED, DELETED
    }

    private WatchService watchService;
    private ExecutorService executorService;
    private volatile boolean isWatching = false;
    private BiConsumer<EventType, String> onFileEvent;

    /** Maps each registered WatchKey to its directory so events can be resolved to full paths. */
    private final Map<WatchKey, Path> watchedDirectories = new ConcurrentHashMap<>();

    public FileSystemWatcher() {
        createWatchService();
    }

    private void createWatchService() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.executorService = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "file-system-watcher");
                t.setDaemon(true);
                return t;
            });
        } catch (IOException e) {
            logger.error("Failed to create FileSystemWatcher", e);
        }
    }

    /**
     * Start watching the file system. Safe to call again after stopWatching():
     * the closed watch service is recreated so monitoring can restart.
     */
    public void startWatching() {
        if (isWatching) return;

        // A previous stopWatching() closed the service - build a fresh one
        if (watchService == null || executorService == null || executorService.isShutdown()) {
            createWatchService();
        }

        isWatching = true;

        // Register common directories (recursively) for monitoring
        registerTree(Paths.get(System.getProperty("user.dir")));
        registerTree(Paths.get(System.getProperty("user.home"), "Desktop"));
        registerTree(Paths.get(System.getProperty("user.home"), "Documents"));
        registerTree(Paths.get(System.getProperty("user.home"), "Downloads"));

        executorService.submit(this::monitoringLoop);

        logger.info("Started file system watching ({} directories registered)",
                watchedDirectories.size());
    }

    /**
     * Stop watching the file system.
     */
    public void stopWatching() {
        isWatching = false;

        try {
            if (watchService != null) {
                watchService.close();
                watchService = null;
            }
            if (executorService != null) {
                executorService.shutdown();
            }
        } catch (IOException e) {
            logger.error("Error stopping file system watcher", e);
        }
        watchedDirectories.clear();

        logger.info("Stopped file system watching");
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
                    return FileVisitResult.CONTINUE; // skip unreadable entries
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
    private void monitoringLoop() {
        while (isWatching) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break; // watcher was stopped
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

                // Newly created subdirectories must be registered to keep coverage
                if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                    registerTree(fullPath);
                }

                EventType eventType = convertEventType(kind);
                if (eventType != null && onFileEvent != null) {
                    onFileEvent.accept(eventType, fullPath.toString());
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                watchedDirectories.remove(key);
            }
        }
    }

    /**
     * Convert WatchEvent.Kind to our EventType.
     */
    private EventType convertEventType(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            return EventType.CREATED;
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            return EventType.MODIFIED;
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            return EventType.DELETED;
        }
        return null;
    }

    /**
     * Set callback for file events. The path passed to the callback is absolute.
     */
    public void setOnFileEvent(BiConsumer<EventType, String> callback) {
        this.onFileEvent = callback;
    }
}
