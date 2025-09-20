package com.asos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Terminal Monitor for capturing command execution and output
 */
public class TerminalMonitor {
    private static final Logger logger = LoggerFactory.getLogger(TerminalMonitor.class);
    
    private final ExecutorService executorService;
    private boolean isMonitoring = false;
    private BiConsumer<String, String> onTerminalOutput;
    
    // Store last executed commands and their outputs
    private String lastCommand = "";
    private String lastOutput = "";
    
    public TerminalMonitor() {
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Start monitoring terminal activity
     */
    public void startMonitoring() {
        isMonitoring = true;
        logger.info("Started terminal monitoring");
    }
    
    /**
     * Stop monitoring terminal activity
     */
    public void stopMonitoring() {
        isMonitoring = false;
        executorService.shutdown();
        logger.info("Stopped terminal monitoring");
    }
    
    /**
     * Execute a command and monitor its output
     */
    public CompletableFuture<String> executeAndMonitor(String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeCommand(command);
            } catch (Exception e) {
                logger.error("Failed to execute command: {}", command, e);
                return "Error: " + e.getMessage();
            }
        }, executorService);
    }
    
    /**
     * Execute a command and capture output
     */
    private String executeCommand(String command) throws IOException, InterruptedException {
        logger.debug("Executing command: {}", command);
        
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        // Handle different operating systems
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("bash", "-c", command);
        }
        
        processBuilder.directory(new File(System.getProperty("user.dir")));
        Process process = processBuilder.start();
        
        // Capture output
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        // Read stdout
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // Read stderr
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        
        String fullOutput = output.toString();
        if (errorOutput.length() > 0) {
            fullOutput += "\nErrors:\n" + errorOutput.toString();
        }
        
        // Store last command and output
        lastCommand = command;
        lastOutput = fullOutput;
        
        // Notify listeners
        if (isMonitoring && onTerminalOutput != null) {
            onTerminalOutput.accept(command, fullOutput);
        }
        
        logger.debug("Command '{}' completed with exit code: {}", command, exitCode);
        return fullOutput;
    }
    
    /**
     * Simulate monitoring common Java compilation commands
     */
    public void monitorJavaCompilation(String javaFile) {
        if (!isMonitoring) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                // Monitor javac compilation
                String compileCommand = "javac " + javaFile;
                String compileOutput = executeCommand(compileCommand);
                
                // Check if compilation was successful (no output usually means success)
                if (compileOutput.trim().isEmpty()) {
                    // Look for .class file
                    String classFile = javaFile.replace(".java", ".class");
                    if (new File(classFile).exists()) {
                        if (onTerminalOutput != null) {
                            onTerminalOutput.accept(compileCommand, "Compilation successful");
                        }
                    }
                }
                
            } catch (Exception e) {
                logger.error("Error monitoring Java compilation", e);
            }
        }, executorService);
    }
    
    /**
     * Monitor Java program execution
     */
    public void monitorJavaExecution(String className) {
        if (!isMonitoring) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                String runCommand = "java " + className;
                String runOutput = executeCommand(runCommand);
                
                if (onTerminalOutput != null) {
                    onTerminalOutput.accept(runCommand, runOutput);
                }
                
            } catch (Exception e) {
                logger.error("Error monitoring Java execution", e);
            }
        }, executorService);
    }
    
    /**
     * Check if a command is currently being executed
     */
    public boolean isCommandRunning(String command) {
        return lastCommand.contains(command);
    }
    
    /**
     * Get the last command output
     */
    public String getLastOutput() {
        return lastOutput;
    }
    
    /**
     * Get the last executed command
     */
    public String getLastCommand() {
        return lastCommand;
    }
    
    /**
     * Set callback for terminal output
     */
    public void setOnTerminalOutput(BiConsumer<String, String> callback) {
        this.onTerminalOutput = callback;
    }
    
    /**
     * Check if monitoring is active
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }
}
