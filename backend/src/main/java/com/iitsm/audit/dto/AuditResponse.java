package com.iitsm.audit.dto;

import com.iitsm.audit.entity.AuditLog;
import java.time.Instant;

public record AuditResponse(Long id, Long incidentId, Long actorId, String actorEmail, String targetEntity, Long targetId, String action,
                            String previousValue, String newValue, String description, Instant timestamp) {
    public static AuditResponse from(AuditLog a) { return new AuditResponse(a.getId(),a.getIncident()==null?null:a.getIncident().getId(),a.getActor()==null?null:a.getActor().getId(),a.getActor()==null?null:a.getActor().getEmail(),a.getTargetEntity(),a.getTargetId(),a.getAction(),a.getPreviousState(),a.getNewState(),a.getDescription(),a.getCreatedAt()); }
}
