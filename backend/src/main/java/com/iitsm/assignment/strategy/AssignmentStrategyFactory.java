package com.iitsm.assignment.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory class providing runtime resolution of AssignmentStrategy implementations.
 */
@Component
public class AssignmentStrategyFactory {

    private final Map<String, AssignmentStrategy> strategies;

    public AssignmentStrategyFactory(List<AssignmentStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        strategy -> strategy.getName().toUpperCase(),
                        Function.identity()
                ));
    }

    public Optional<AssignmentStrategy> getStrategy(String strategyName) {
        if (strategyName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(strategies.get(strategyName.toUpperCase()));
    }

    public AssignmentStrategy getDefaultStrategy() {
        return strategies.getOrDefault("ROUND_ROBIN",
                strategies.values().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("No AssignmentStrategy registered")));
    }
}
