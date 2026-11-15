package com.iitsm.assignment.strategy;

import com.iitsm.assignment.entity.TechnicianProfile;
import com.iitsm.assignment.service.TechnicianCandidate;
import com.iitsm.incident.entity.Incident;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class SkillBasedAssignmentStrategy implements AssignmentStrategy {
    @Override public String getName() { return "SKILL_BASED"; }
    @Override public Optional<TechnicianProfile> recommend(Incident incident,List<TechnicianCandidate> candidates) {
        return rank(incident,candidates).stream().map(TechnicianCandidate::profile).findFirst();
    }
    @Override public List<TechnicianCandidate> rank(Incident incident,List<TechnicianCandidate> candidates) {
        if(candidates==null) return List.of();
        String category=incident.getCategory().trim().toUpperCase();
        return candidates.stream().filter(TechnicianCandidate::eligible)
                .filter(c->c.profile().getSkills().contains(category)||c.profile().getSkills().contains("*"))
                .sorted(Comparator.comparing((TechnicianCandidate c)->c.profile().getExperienceYears(),Comparator.reverseOrder())
                        .thenComparingLong(TechnicianCandidate::activeWorkload)
                        .thenComparing(c->c.profile().getUser().getId()))
                .toList();
    }
}
