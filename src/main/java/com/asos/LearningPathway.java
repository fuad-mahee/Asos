package com.asos;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete learning pathway with multiple steps
 */
public class LearningPathway {
    
    private String id;
    private String name;
    private List<LearningStep> steps;
    private int currentStepIndex;
    
    public LearningPathway(String id, String name) {
        this.id = id;
        this.name = name;
        this.steps = new ArrayList<>();
        this.currentStepIndex = 0;
    }
    
    public void addStep(LearningStep step) {
        steps.add(step);
    }
    
    public LearningStep getCurrentStep() {
        if (currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex);
        }
        return null; // Pathway completed
    }
    
    public void nextStep() {
        if (currentStepIndex < steps.size()) {
            steps.get(currentStepIndex).complete();
            currentStepIndex++;
        }
    }
    
    public void reset() {
        currentStepIndex = 0;
        for (LearningStep step : steps) {
            step.setCompleted(false);
        }
    }
    
    public boolean isCompleted() {
        return currentStepIndex >= steps.size();
    }
    
    public int getCurrentStepNumber() {
        return currentStepIndex + 1;
    }
    
    public int getTotalSteps() {
        return steps.size();
    }
    
    public double getProgressPercentage() {
        if (steps.isEmpty()) return 0.0;
        return (double) currentStepIndex / steps.size() * 100.0;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<LearningStep> getSteps() { return steps; }
    public void setSteps(List<LearningStep> steps) { this.steps = steps; }
    
    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int currentStepIndex) { this.currentStepIndex = currentStepIndex; }
    
    @Override
    public String toString() {
        return String.format("LearningPathway{id='%s', name='%s', steps=%d, current=%d, progress=%.1f%%}", 
                           id, name, steps.size(), currentStepIndex + 1, getProgressPercentage());
    }
}
