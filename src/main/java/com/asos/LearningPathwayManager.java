package com.asos;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced learning pathway manager with intelligent progress tracking and validation
 */
public class LearningPathwayManager {
    
    public interface ProgressListener {
        void onProgressUpdate(LearningProgress progress);
        void onStepValidated(EventValidator.ValidationResponse validation);
        void onAdaptiveAction(String action, String reason);
    }
    
    private Map<String, LearningPathway> pathways;
    private LearningPathway currentPathway;
    private ProgressListener progressListener;
    private KnowledgeBaseManager knowledgeBase;
    private StepProgressTracker progressTracker;
    private int currentStepStartTime;
    
    public LearningPathwayManager() {
        pathways = new HashMap<>();
        knowledgeBase = new KnowledgeBaseManager();
        progressTracker = new StepProgressTracker();
        initializePathways();
        setupProgressTracking();
    }
    
    private void setupProgressTracking() {
        progressTracker.setProgressListener(new StepProgressTracker.ProgressListener() {
            @Override
            public void onStepTimeout(int stepNumber, long elapsedSeconds) {
                handleStepTimeout(stepNumber, elapsedSeconds);
            }
            
            @Override
            public void onStepCompleted(int stepNumber, long elapsedSeconds, int errorCount) {
                handleStepCompletion(stepNumber, elapsedSeconds, errorCount);
            }
            
            @Override
            public void onErrorDetected(int stepNumber, String errorMessage) {
                handleStepError(stepNumber, errorMessage);
            }
            
            @Override
            public void onProgressUpdate(int stepNumber, StepProgressTracker.StepProgress progress) {
                handleProgressUpdate(stepNumber, progress);
            }
        });
    }
    
    private void initializePathways() {
        // Create pathways from JSON configuration
        for (String pathwayId : knowledgeBase.getAllPathways().keySet()) {
            LearningPathway pathway = knowledgeBase.createLearningPathway(pathwayId);
            if (pathway != null) {
                pathways.put(pathwayId, pathway);
                System.out.println("Initialized pathway: " + pathway.getName());
            }
        }
    }
    
    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }
    
    public void startPathway(String pathwayId) {
        currentPathway = pathways.get(pathwayId);
        if (currentPathway != null) {
            currentPathway.reset();
            startCurrentStep();
            notifyProgress();
            System.out.println("Started pathway: " + currentPathway.getName());
        }
    }
    
    private void startCurrentStep() {
        if (currentPathway != null) {
            LearningStep currentStep = currentPathway.getCurrentStep();
            if (currentStep != null) {
                PathwayConfig.StepConfig stepConfig = knowledgeBase.getStepConfig(
                    currentPathway.getId(), currentStep.getStepNumber());
                if (stepConfig != null) {
                    progressTracker.startStep(currentStep.getStepNumber(), stepConfig);
                }
            }
        }
    }
    
    private void handleStepTimeout(int stepNumber, long elapsedSeconds) {
        System.out.println("Step " + stepNumber + " timed out after " + elapsedSeconds + " seconds");
        
        if (progressListener != null) {
            progressListener.onAdaptiveAction("timeout_help", 
                "User is taking longer than expected on step " + stepNumber);
        }
        
        // Provide additional hints or break down the step
        PathwayConfig.StepConfig stepConfig = knowledgeBase.getStepConfig(
            currentPathway.getId(), stepNumber);
        if (stepConfig != null && stepConfig.getHints() != null && !stepConfig.getHints().isEmpty()) {
            progressTracker.recordHint(stepNumber);
        }
    }
    
    private void handleStepCompletion(int stepNumber, long elapsedSeconds, int errorCount) {
        System.out.println("Step " + stepNumber + " completed successfully in " + 
                         elapsedSeconds + "s with " + errorCount + " errors");
        
        if (progressListener != null) {
            String adaptiveAction = determineAdaptiveAction(elapsedSeconds, errorCount);
            if (adaptiveAction != null) {
                progressListener.onAdaptiveAction(adaptiveAction, 
                    "Based on performance: " + elapsedSeconds + "s, " + errorCount + " errors");
            }
        }
    }
    
    private void handleStepError(int stepNumber, String errorMessage) {
        System.out.println("Error detected in step " + stepNumber + ": " + errorMessage);
        
        if (progressListener != null) {
            progressListener.onAdaptiveAction("provide_help", 
                "User encountered error: " + errorMessage);
        }
    }
    
    private void handleProgressUpdate(int stepNumber, StepProgressTracker.StepProgress progress) {
        if (progressListener != null) {
            String category = progress.getPerformanceCategory();
            if ("needs_help".equals(category)) {
                progressListener.onAdaptiveAction("offer_assistance", 
                    "User appears to be struggling with step " + stepNumber);
            }
        }
    }
    
    private String determineAdaptiveAction(long elapsedSeconds, int errorCount) {
        if (elapsedSeconds < 30 && errorCount == 0) {
            return "fast_learner";
        } else if (elapsedSeconds > 180 || errorCount > 2) {
            return "needs_more_support";
        } else if (errorCount > 0) {
            return "provide_encouragement";
        }
        return null;
    }
    
    public void handleFileSystemEvent(String event) {
        if (currentPathway == null) return;
        
        LearningStep currentStep = currentPathway.getCurrentStep();
        if (currentStep != null) {
            PathwayConfig.StepConfig stepConfig = knowledgeBase.getStepConfig(
                currentPathway.getId(), currentStep.getStepNumber());
            
            // Validate the event
            EventValidator.ValidationResponse validation = 
                EventValidator.validateFileSystemEvent(event, stepConfig);
            
            if (progressListener != null) {
                progressListener.onStepValidated(validation);
            }
            
            // Handle successful validation
            if (validation.getResult() == EventValidator.ValidationResult.SUCCESS) {
                advanceStep();
            } else if (validation.getResult() == EventValidator.ValidationResult.ERROR) {
                progressTracker.recordError(currentStep.getStepNumber(), validation.getMessage());
            }
        }
    }
    
    public void handleProcessEvent(String event) {
        if (currentPathway == null) return;
        
        LearningStep currentStep = currentPathway.getCurrentStep();
        if (currentStep != null) {
            PathwayConfig.StepConfig stepConfig = knowledgeBase.getStepConfig(
                currentPathway.getId(), currentStep.getStepNumber());
            
            // Validate the event
            EventValidator.ValidationResponse validation = 
                EventValidator.validateProcessEvent(event, stepConfig);
            
            if (progressListener != null) {
                progressListener.onStepValidated(validation);
            }
            
            // Handle successful validation
            if (validation.getResult() == EventValidator.ValidationResult.SUCCESS) {
                advanceStep();
            } else if (validation.getResult() == EventValidator.ValidationResult.ERROR) {
                progressTracker.recordError(currentStep.getStepNumber(), validation.getMessage());
            }
        }
    }
    
    private void advanceStep() {
        if (currentPathway != null) {
            currentPathway.nextStep();
            notifyProgress();
        }
    }
    
    private void notifyProgress() {
        if (progressListener != null && currentPathway != null) {
            LearningStep currentStep = currentPathway.getCurrentStep();
            LearningProgress.Status status = currentStep != null ? 
                LearningProgress.Status.IN_PROGRESS : LearningProgress.Status.COMPLETED;
            
            LearningProgress progress = new LearningProgress(
                status,
                currentPathway.getCurrentStepNumber(),
                currentPathway.getTotalSteps(),
                currentStep != null ? currentStep.getInstruction() : "Pathway completed!"
            );
            
            progressListener.onProgressUpdate(progress);
        }
    }
    
    public LearningPathway getCurrentPathway() {
        return currentPathway;
    }
    
    public KnowledgeBaseManager getKnowledgeBase() {
        return knowledgeBase;
    }
    
    public String getDetailedInstruction(int stepNumber) {
        if (currentPathway != null) {
            PathwayConfig.StepConfig stepConfig = knowledgeBase.getStepConfig(
                currentPathway.getId(), stepNumber);
            if (stepConfig != null) {
                return stepConfig.getDetailedInstruction();
            }
        }
        return null;
    }
    
    public java.util.List<String> getHints(int stepNumber) {
        if (currentPathway != null) {
            PathwayConfig.StepConfig stepConfig = knowledgeBase.getStepConfig(
                currentPathway.getId(), stepNumber);
            if (stepConfig != null) {
                return stepConfig.getHints();
            }
        }
        return java.util.List.of();
    }
    
    private LearningStep findStepByNumber(int stepNumber) {
        if (currentPathway != null) {
            return currentPathway.getSteps().stream()
                .filter(step -> step.getStepNumber() == stepNumber)
                .findFirst()
                .orElse(null);
        }
        return null;
    }
}
