package com.iitsm.assignment.entity;

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
 * Historical record of incident assignments and strategy applied.
 */
@Entity
@Table(name = "assignment_records")
public class AssignmentRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id", nullable = false)
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;

    @Column(name = "strategy_used", nullable = false, length = 64)
    private String strategyUsed;

    @Column(length = 255)
    private String reason;

    public AssignmentRecord() {
    }

    public AssignmentRecord(Incident incident, User assignedTo, User assignedBy, String strategyUsed, String reason) {
        this.incident = incident;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.strategyUsed = strategyUsed;
        this.reason = reason;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String getStrategyUsed() {
        return strategyUsed;
    }

    public void setStrategyUsed(String strategyUsed) {
        this.strategyUsed = strategyUsed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
