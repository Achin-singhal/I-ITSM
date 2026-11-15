package com.iitsm.audit.service;

import com.iitsm.audit.entity.AuditLog;
import com.iitsm.incident.entity.Incident;
import com.iitsm.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.iitsm.audit.repository.AuditLogRepository;

/**
 * Service managing audit log emission and persistence.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    public AuditService(AuditLogRepository repository) { this.repository = repository; }

    public AuditLog recordAction(Incident incident, User actor, String action, String previousState, String newState) {
        return recordAction(incident, actor, action, previousState, newState, action);
    }

    public AuditLog recordAction(Incident incident, User actor, String action, String previousState, String newState, String description) {
        AuditLog auditLog = repository.save(new AuditLog(incident, actor, action, previousState, newState, description,
                incident == null ? null : "INCIDENT", incident == null ? null : incident.getId()));
        logAction(incident, actor, action, previousState, newState);
        return auditLog;
    }

    public AuditLog recordEntityAction(String targetEntity, Long targetId, User actor, String action,
                                       String previousState, String newState, String description) {
        return recordEntityAction(null, targetEntity, targetId, actor, action, previousState, newState, description);
    }

    public AuditLog recordEntityAction(Incident incident, String targetEntity, Long targetId, User actor,
                                       String action, String previousState, String newState, String description) {
        AuditLog auditLog = repository.save(new AuditLog(incident, actor, action, previousState, newState,
                description, targetEntity, targetId));
        logAction(incident, actor, action, previousState, newState);
        return auditLog;
    }

    private void logAction(Incident incident, User actor, String action, String previousState, String newState) {
        log.info("[AUDIT] Action: {} on ticket: {} by user: {} ({} -> {})",
                action,
                incident != null ? incident.getTicketNumber() : "N/A",
                actor != null ? actor.getEmail() : "SYSTEM",
                previousState,
                newState);
    }
}
