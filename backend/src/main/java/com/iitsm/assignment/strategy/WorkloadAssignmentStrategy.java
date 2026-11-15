package com.iitsm.assignment.strategy;

import com.iitsm.incident.entity.Incident;
import org.springframework.stereotype.Component;

import com.iitsm.assignment.entity.TechnicianProfile;
import com.iitsm.assignment.service.TechnicianCandidate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Concrete Strategy: Assigns incidents based on agent capacity/workload heuristics.
 * Demonstrates Polymorphic implementation of AssignmentStrategy.
 */
@Component
public class WorkloadAssignmentStrategy implements AssignmentStrategy {

    @Override
    public String getName() {
        return "WORKLOAD_BALANCED";
    }

    @Override
    public Optional<TechnicianProfile> recommend(Incident incident, List<TechnicianCandidate> candidates) {
        return rank(incident,candidates).stream().map(TechnicianCandidate::profile).findFirst();
    }
    @Override
    public List<TechnicianCandidate> rank(Incident incident, List<TechnicianCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        String category = incident.getCategory().trim().toUpperCase();
        return candidates.stream().filter(TechnicianCandidate::eligible)
                .sorted(Comparator.comparing((TechnicianCandidate c) -> !(c.profile().getSkills().contains(category) || c.profile().getSkills().contains("*")))
                        .thenComparingLong(TechnicianCandidate::activeWorkload)
                        .thenComparing((TechnicianCandidate c) -> c.profile().getExperienceYears(), Comparator.reverseOrder())
                        .thenComparing(c -> c.profile().getUser().getId())).toList();
    }
}
