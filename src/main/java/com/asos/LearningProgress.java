package com.asos;

/**
 * Represents the current learning progress status
 */
public class LearningProgress {
    
    public enum Status {
        WAITING,
        IN_PROGRESS,
        COMPLETED,
        ERROR
    }
    
    private Status status;
    private int currentStep;
    private int totalSteps;
    private String currentInstruction;
    private String feedback;
    
    public LearningProgress(Status status, int currentStep, int totalSteps) {
        this.status = status;
        this.currentStep = currentStep;
        this.totalSteps = totalSteps;
    }
    
    public LearningProgress(Status status, int currentStep, int totalSteps, String currentInstruction) {
        this(status, currentStep, totalSteps);
        this.currentInstruction = currentInstruction;
    }
    
    // Getters and setters
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public int getCurrentStep() { return currentStep; }
    public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }
    
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    
    public String getCurrentInstruction() { return currentInstruction; }
    public void setCurrentInstruction(String currentInstruction) { this.currentInstruction = currentInstruction; }
    
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    
    @Override
    public String toString() {
        return String.format("LearningProgress{status=%s, step=%d/%d, instruction='%s'}", 
                           status, currentStep, totalSteps, currentInstruction);
    }
}
