package com.asos;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Enhanced service that coordinates OS monitoring with intelligent event validation
 */
public class OSMonitorService {
    
    private FileSystemMonitor fileSystemMonitor;
    private ProcessMonitor processMonitor;
    private Thread processThread;
    
    private Consumer<String> fileSystemListener;
    private Consumer<String> processListener;
    private Consumer<EventValidator.ValidationResponse> validationListener;
    
    private PathwayConfig.StepConfig currentStepConfig;
    
    public void setFileSystemListener(Consumer<String> listener) {
        this.fileSystemListener = listener;
    }
    
    public void setProcessListener(Consumer<String> listener) {
        this.processListener = listener;
    }
    
    public void setValidationListener(Consumer<EventValidator.ValidationResponse> listener) {
        this.validationListener = listener;
    }
    
    public void setCurrentStep(PathwayConfig.StepConfig stepConfig) {
        this.currentStepConfig = stepConfig;
        System.out.println("OS Monitor now watching for: " + 
                         (stepConfig != null ? stepConfig.getInstruction() : "any activity"));
    }
    
    public void startMonitoring(Path pathToWatch) {
        // Start file system monitoring with enhanced validation
        fileSystemMonitor = new FileSystemMonitor();
        
        // Set up callback for file changes
        fileSystemMonitor.setOnFileChange((filePath, content) -> {
            String message = "File changed: " + filePath;
            if (fileSystemListener != null) {
                fileSystemListener.accept(message);
            }
            
            // Validate the event against current step
            if (currentStepConfig != null && validationListener != null) {
                EventValidator.ValidationResponse validation = 
                    EventValidator.validateFileSystemEvent(message, currentStepConfig);
                validationListener.accept(validation);
            }
        });
        
        // Start the file system monitor
        fileSystemMonitor.start();
        
        // Start process monitoring with enhanced validation
        ProcessMonitor.ProgressListener procListener = message -> {
            if (processListener != null) {
                processListener.accept(message);
            }
            
            // Validate the event against current step
            if (currentStepConfig != null && validationListener != null) {
                EventValidator.ValidationResponse validation = 
                    EventValidator.validateProcessEvent(message, currentStepConfig);
                validationListener.accept(validation);
            }
        };
        
        processMonitor = new ProcessMonitor(procListener, 2000);
        processThread = new Thread(processMonitor);
        processThread.setDaemon(true);
        processThread.start();
        
        System.out.println("Enhanced OS Monitoring started - watching: " + pathToWatch);
    }
    
    public void stopMonitoring() {
        if (fileSystemMonitor != null) {
            fileSystemMonitor.stop();
        }
        
        if (processMonitor != null) {
            processMonitor.stop();
        }
        
        if (processThread != null) {
            processThread.interrupt();
        }
        
        System.out.println("Enhanced OS Monitoring stopped");
    }
    
    public boolean isMonitoring() {
        return fileSystemMonitor != null && processMonitor != null &&
               processThread != null && processThread.isAlive();
    }
    
    public void pauseMonitoring() {
        // Temporarily stop monitoring without full shutdown
        if (fileSystemMonitor != null) {
            fileSystemMonitor.stop();
        }
        if (processMonitor != null) {
            processMonitor.stop();
        }
        System.out.println("OS Monitoring paused");
    }
    
    public void resumeMonitoring(Path pathToWatch) {
        if (!isMonitoring()) {
            startMonitoring(pathToWatch);
            System.out.println("OS Monitoring resumed");
        }
    }
}
