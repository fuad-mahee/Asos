package com.asos;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * File System Monitor for watching file changes in real-time
 * Used by ASOS study assistant to monitor code changes and provide feedback
 */
public class FileSystemMonitor {
    private WatchService watchService;
    private ExecutorService executorService;
    private BiConsumer<String, String> onFileChange;
    private boolean isRunning = false;
    
    public FileSystemMonitor() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.executorService = Executors.newSingleThreadExecutor();
        } catch (IOException e) {
            System.err.println("Failed to create FileSystemMonitor: " + e.getMessage());
        }
    }
    
    /**
     * Set the callback function to be called when a file changes
     */
    public void setOnFileChange(BiConsumer<String, String> callback) {
        this.onFileChange = callback;
    }
    
    /**
     * Start monitoring the file system
     */
    public void start() {
        if (isRunning) return;
        
        isRunning = true;
        executorService.submit(this::monitorLoop);
        
        // Register common development directories for monitoring
        registerDirectory(System.getProperty("user.home") + "/Downloads");
        registerDirectory(System.getProperty("user.home") + "/Documents");
        registerDirectory(System.getProperty("user.home") + "/Desktop");
    }
    
    /**
     * Stop monitoring the file system
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
            System.err.println("Error stopping FileSystemMonitor: " + e.getMessage());
        }
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
            }
        } catch (IOException e) {
            System.err.println("Failed to register directory: " + directoryPath + " - " + e.getMessage());
        }
    }
    
    /**
     * Main monitoring loop
     */
    private void monitorLoop() {
        while (isRunning) {
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
                    
                    // Only monitor code files
                    if (isCodeFile(filePath)) {
                        String content = readFileContent(filename);
                        if (onFileChange != null && content != null) {
                            onFileChange.accept(filePath, content);
                        }
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
     * Check if the file is a code file we should monitor
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
     * Read the content of a file
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
