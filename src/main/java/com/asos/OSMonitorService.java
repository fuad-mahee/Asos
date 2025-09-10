package com.asos;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Enhanced service that coordinates OS monitoring with intelligent event validation
 */
public class OSMonitorService {
    
    private FileSystemMonitor fileSystemMonitor;
    private ProcessMonitor processMonitor;
    private Thread fileSystemThread;
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
        FileSystemMonitor.ProgressListener fsListener = message -> {
            if (fileSystemListener != null) {
                fileSystemListener.accept(message);
            }
            
            // Validate the event against current step
            if (currentStepConfig != null && validationListener != null) {
                EventValidator.ValidationResponse validation = 
                    EventValidator.validateFileSystemEvent(message, currentStepConfig);
                validationListener.accept(validation);
            }
        };
        
        fileSystemMonitor = new FileSystemMonitor(pathToWatch, fsListener);
        fileSystemThread = new Thread(fileSystemMonitor);
        fileSystemThread.setDaemon(true);
        fileSystemThread.start();
        
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
        if (processMonitor != null) {
            processMonitor.stop();
        }
        
        if (fileSystemThread != null) {
            fileSystemThread.interrupt();
        }
        
        if (processThread != null) {
            processThread.interrupt();
        }
        
        System.out.println("Enhanced OS Monitoring stopped");
    }
    
    public boolean isMonitoring() {
        return fileSystemThread != null && fileSystemThread.isAlive() &&
               processThread != null && processThread.isAlive();
    }
    
    public void pauseMonitoring() {
        // Temporarily stop monitoring without full shutdown
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
