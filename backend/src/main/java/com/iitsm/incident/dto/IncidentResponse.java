package com.iitsm.incident.dto;

import com.iitsm.incident.entity.Incident;
import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.incident.entity.IncidentStatus;
import java.time.Instant;

public record IncidentResponse(Long id, String ticketNumber, String title, String description,
        IncidentStatus status, IncidentPriority priority, String priorityCode, String category, Long reporterId,
        String reporterName, Long assigneeId, String assigneeName, Instant createdAt, Instant updatedAt,
        com.iitsm.incident.entity.ImpactLevel impact, com.iitsm.incident.entity.UrgencyLevel urgency,
        int businessCriticality, int affectedUserCount, String affectedService, int serviceCriticality,
        java.math.BigDecimal priorityScore, String priorityExplanation, String resolutionDetails,
        Instant acknowledgedAt,Instant resolvedAt,Instant verifiedAt,Instant closedAt,
        Long serviceId, String serviceName) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(incident.getId(), incident.getTicketNumber(), incident.getTitle(), incident.getDescription(),
                incident.getStatus(), incident.getPriority(), switch(incident.getPriority()){case CRITICAL->"P1";case HIGH->"P2";case MEDIUM->"P3";case LOW->"P4";}, incident.getCategory(), incident.getReporter().getId(),
                incident.getReporter().getFullName(), incident.getAssignee() == null ? null : incident.getAssignee().getId(),
                incident.getAssignee() == null ? null : incident.getAssignee().getFullName(), incident.getCreatedAt(), incident.getUpdatedAt(),
                incident.getImpact(), incident.getUrgency(), incident.getBusinessCriticality(), incident.getAffectedUserCount(),
                incident.getAffectedService(), incident.getServiceCriticality(), incident.getPriorityScore(), incident.getPriorityExplanation(),
                incident.getResolutionDetails(),incident.getAcknowledgedAt(),incident.getResolvedAt(),incident.getVerifiedAt(),incident.getClosedAt(),
                incident.getServiceDefinition() == null ? null : incident.getServiceDefinition().getId(),
                incident.getServiceDefinition() == null ? null : incident.getServiceDefinition().getName());
    }
}
