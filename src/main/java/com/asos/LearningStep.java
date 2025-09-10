package com.asos;

/**
 * Represents a single learning step within a pathway
 */
public class LearningStep {
    
    private int stepNumber;
    private String instruction;
    private String expectedAction;
    private String successMessage;
    private boolean completed;
    
    public LearningStep(int stepNumber, String instruction, String expectedAction, String successMessage) {
        this.stepNumber = stepNumber;
        this.instruction = instruction;
        this.expectedAction = expectedAction;
        this.successMessage = successMessage;
        this.completed = false;
    }
    
    // Getters and setters
    public int getStepNumber() { return stepNumber; }
    public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }
    
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    
    public String getExpectedAction() { return expectedAction; }
    public void setExpectedAction(String expectedAction) { this.expectedAction = expectedAction; }
    
    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public void complete() {
        this.completed = true;
    }
    
    @Override
    public String toString() {
        return String.format("Step %d: %s (Expected: %s, Completed: %s)", 
                           stepNumber, instruction, expectedAction, completed);
    }
}
