package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * File System Watcher for detecting file creation, modification, and deletion
 */
public class FileSystemWatcher {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemWatcher.class);
    
    public enum EventType {
        CREATED, MODIFIED, DELETED
    }
    
    private WatchService watchService;
    private ExecutorService executorService;
    private boolean isWatching = false;
    private BiConsumer<EventType, String> onFileEvent;
    
    public FileSystemWatcher() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.executorService = Executors.newSingleThreadExecutor();
        } catch (IOException e) {
            logger.error("Failed to create FileSystemWatcher", e);
        }
    }
    
    /**
     * Start watching the file system
     */
    public void startWatching() {
        if (isWatching) return;
        
        isWatching = true;
        
        // Register common directories for monitoring
        registerDirectory(System.getProperty("user.dir")); // Current working directory
        registerDirectory(System.getProperty("user.home") + "/Desktop");
        registerDirectory(System.getProperty("user.home") + "/Documents");
        
        // Start monitoring loop
        executorService.submit(this::monitoringLoop);
        
        logger.info("Started file system watching");
    }
    
    /**
     * Stop watching the file system
     */
    public void stopWatching() {
        isWatching = false;
        
        try {
            if (watchService != null) {
                watchService.close();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
        } catch (IOException e) {
            logger.error("Error stopping file system watcher", e);
        }
        
        logger.info("Stopped file system watching");
    }
    
    /**
     * Register a directory for monitoring
     */
    private void registerDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (Files.exists(path) && Files.isDirectory(path)) {
                path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
                logger.debug("Registered directory for watching: {}", directoryPath);
            }
        } catch (IOException e) {
            logger.warn("Failed to register directory for watching: {}", directoryPath);
        }
    }
    
    /**
     * Main monitoring loop
     */
    private void monitoringLoop() {
        while (isWatching) {
            try {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    String filePath = filename.toString();
                    
                    // Convert WatchEvent.Kind to our EventType
                    EventType eventType = convertEventType(kind);
                    if (eventType != null && onFileEvent != null) {
                        onFileEvent.accept(eventType, filePath);
                    }
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Convert WatchEvent.Kind to our EventType
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
     * Set callback for file events
     */
    public void setOnFileEvent(BiConsumer<EventType, String> callback) {
        this.onFileEvent = callback;
    }
}
