package com.iitsm.intelligence.priority;

public interface PriorityCalculationStrategy {
    String getName();
    PriorityDecision calculate(PriorityInput input);
}
