package com.iitsm.assignment.strategy;

import com.iitsm.incident.entity.Incident;
import com.iitsm.assignment.entity.TechnicianProfile;
import com.iitsm.assignment.service.TechnicianCandidate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete Strategy: Assigns incidents cyclically across active agents.
 * Demonstrates Polymorphic implementation of AssignmentStrategy.
 */
@Component
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String getName() {
        return "ROUND_ROBIN";
    }

    @Override
    public Optional<TechnicianProfile> recommend(Incident incident, List<TechnicianCandidate> candidates) {
        List<TechnicianCandidate> eligible = rank(incident,candidates);
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(eligible.get(0).profile());
    }

    @Override
    public List<TechnicianCandidate> rank(Incident incident, List<TechnicianCandidate> candidates) {
        List<TechnicianCandidate> eligible = candidates == null ? List.of() : candidates.stream().filter(TechnicianCandidate::eligible)
                .sorted(java.util.Comparator.comparing(c -> c.profile().getUser().getId())).toList();
        if (eligible.isEmpty()) return List.of();
        int start = Math.floorMod(counter.getAndIncrement(), eligible.size());
        java.util.ArrayList<TechnicianCandidate> ordered = new java.util.ArrayList<>(eligible.size());
        ordered.addAll(eligible.subList(start, eligible.size()));
        ordered.addAll(eligible.subList(0, start));
        return List.copyOf(ordered);
    }
}
