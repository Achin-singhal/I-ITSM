package com.iitsm.intelligence.priority;

import com.iitsm.incident.entity.ImpactLevel;
import com.iitsm.incident.entity.UrgencyLevel;

public record PriorityInput(ImpactLevel impact, UrgencyLevel urgency, int businessCriticality,
                            int affectedUsers, String affectedService, int serviceCriticality) {
    public PriorityInput {
        if (impact == null || urgency == null) throw new IllegalArgumentException("Impact and urgency are required");
        if (businessCriticality < 1 || businessCriticality > 5) throw new IllegalArgumentException("Business criticality must be between 1 and 5");
        if (serviceCriticality < 1 || serviceCriticality > 5) throw new IllegalArgumentException("Service criticality must be between 1 and 5");
        if (affectedUsers < 0) throw new IllegalArgumentException("Affected user count cannot be negative");
        if (affectedService == null || affectedService.isBlank()) throw new IllegalArgumentException("Affected service is required");
    }
}
