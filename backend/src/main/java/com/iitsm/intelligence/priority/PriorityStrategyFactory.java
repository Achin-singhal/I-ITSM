package com.iitsm.intelligence.priority;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PriorityStrategyFactory {
    private final Map<String, PriorityCalculationStrategy> strategies;
    public PriorityStrategyFactory(List<PriorityCalculationStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toUnmodifiableMap(s -> s.getName().toUpperCase(), Function.identity()));
    }
    public PriorityCalculationStrategy get(String name) {
        PriorityCalculationStrategy strategy = strategies.get(name == null ? "WEIGHTED" : name.toUpperCase());
        if (strategy == null) throw new IllegalArgumentException("Unknown priority strategy: " + name);
        return strategy;
    }
}
