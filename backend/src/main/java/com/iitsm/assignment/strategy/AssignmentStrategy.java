package com.iitsm.assignment.strategy;

import com.iitsm.incident.entity.Incident;
import com.iitsm.assignment.entity.TechnicianProfile;
import com.iitsm.assignment.service.TechnicianCandidate;

import java.util.List;
import java.util.Optional;

/**
 * Strategy pattern interface for dynamic incident assignment routing.
 * Demonstrates Abstraction and enables Polymorphism.
 */
public interface AssignmentStrategy {

    /**
     * Unique identifier for the strategy (e.g. ROUND_ROBIN, WORKLOAD_BALANCED).
     */
    String getName();

    /**
     * Selects an appropriate agent for the given incident from candidate agents.
     *
     * @param incident The incident requiring assignment
     * @param candidateAgents Available agents
     * @return Selected agent, or Optional.empty() if no agent could be assigned
     */
    Optional<TechnicianProfile> recommend(Incident incident, List<TechnicianCandidate> candidates);

    /** Ordered eligible candidates, from strongest fit to weakest fit. */
    default List<TechnicianCandidate> rank(Incident incident, List<TechnicianCandidate> candidates) {
        if (candidates == null) return List.of();
        return recommend(incident, candidates).map(profile -> candidates.stream()
                .filter(TechnicianCandidate::eligible)
                .sorted(java.util.Comparator.comparing((TechnicianCandidate c) -> !c.profile().equals(profile))
                        .thenComparing(c -> c.profile().getUser().getId()))
                .toList()).orElseGet(List::of);
    }
}
