package com.iitsm.audit.entity;

import com.iitsm.common.BaseEntity;
import com.iitsm.incident.entity.Incident;
import com.iitsm.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Immutable audit log entity recording state modifications.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "target_entity", length = 64)
    private String targetEntity;

    @Column(name = "target_id")
    private Long targetId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "previous_state", length = 255)
    private String previousState;

    @Column(name = "new_state", length = 255)
    private String newState;

    @Column(nullable=false, columnDefinition="TEXT")
    private String description;

    public AuditLog() {
    }

    public AuditLog(Incident incident, User actor, String action, String previousState, String newState) {
        this(incident, actor, action, previousState, newState, action);
    }

    public AuditLog(Incident incident, User actor, String action, String previousState, String newState, String description) {
        this(incident, actor, action, previousState, newState, description,
                incident == null ? null : "INCIDENT", incident == null ? null : incident.getId());
    }

    public AuditLog(Incident incident, User actor, String action, String previousState, String newState,
                    String description, String targetEntity, Long targetId) {
        this.incident = incident;
        this.actor = actor;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
        this.description = description;
        this.targetEntity = targetEntity;
        this.targetId = targetId;
    }

    public Incident getIncident() {
        return incident;
    }

    public User getActor() {
        return actor;
    }

    public String getTargetEntity() { return targetEntity; }
    public Long getTargetId() { return targetId; }

    public String getAction() {
        return action;
    }

    public String getPreviousState() {
        return previousState;
    }

    public String getNewState() {
        return newState;
    }

    public String getDescription() { return description; }
}
