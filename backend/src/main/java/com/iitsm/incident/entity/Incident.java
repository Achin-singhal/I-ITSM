package com.iitsm.incident.entity;

import com.iitsm.common.BaseEntity;
import com.iitsm.user.entity.User;
import com.iitsm.incident.category.entity.IncidentCategory;
import com.iitsm.servicecatalog.entity.ItService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;

/**
 * Core incident entity representing IT service tickets.
 * Demonstrates Inheritance (BaseEntity) and domain Encapsulation.
 */
@Entity
@Table(name = "incidents")
public class Incident extends BaseEntity {

    @Column(name = "ticket_number", nullable = false, unique = true, length = 32)
    private String ticketNumber;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentStatus status = IncidentStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentPriority priority = IncidentPriority.MEDIUM;

    @Column(nullable = false, length = 64)
    private String category = "SOFTWARE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private IncidentCategory categoryDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ItService serviceDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Enumerated(EnumType.STRING) @Column(name="impact", nullable=false, length=32)
    private ImpactLevel impact = ImpactLevel.MODERATE;
    @Enumerated(EnumType.STRING) @Column(name="urgency", nullable=false, length=32)
    private UrgencyLevel urgency = UrgencyLevel.NORMAL;
    @Column(name="business_criticality", nullable=false) private int businessCriticality = 3;
    @Column(name="affected_user_count", nullable=false) private int affectedUserCount;
    @Column(name="affected_service", nullable=false, length=120) private String affectedService = "General IT Service";
    @Column(name="service_criticality", nullable=false) private int serviceCriticality = 3;
    @Column(name="priority_score", nullable=false, precision=7, scale=2) private BigDecimal priorityScore = BigDecimal.ZERO;
    @Column(name="priority_explanation", nullable=false, length=1000) private String priorityExplanation = "Priority not calculated";
    @Column(name="resolution_details",columnDefinition="TEXT") private String resolutionDetails;
    @Column(name="acknowledged_at") private java.time.Instant acknowledgedAt;
    @Column(name="resolved_at") private java.time.Instant resolvedAt;
    @Column(name="verified_at") private java.time.Instant verifiedAt;
    @Column(name="closed_at") private java.time.Instant closedAt;

    public Incident() {
        this.ticketNumber = generateTicketNumber();
    }

    public Incident(String title, String description, IncidentPriority priority, String category, User reporter) {
        this.ticketNumber = generateTicketNumber();
        this.title = title;
        this.description = description;
        this.priority = priority != null ? priority : IncidentPriority.MEDIUM;
        this.category = category != null ? category : "SOFTWARE";
        this.reporter = reporter;
        this.status = IncidentStatus.NEW;
        this.priorityExplanation = "Legacy incident priority supplied as " + this.priority;
    }

    public Incident(String title, String description, String category, User reporter, ImpactLevel impact,
                    UrgencyLevel urgency, int businessCriticality, int affectedUserCount,
                    String affectedService, int serviceCriticality) {
        this(title, description, IncidentPriority.MEDIUM, category, reporter);
        this.impact=impact; this.urgency=urgency; this.businessCriticality=businessCriticality;
        this.affectedUserCount=affectedUserCount; this.affectedService=affectedService; this.serviceCriticality=serviceCriticality;
    }

    private String generateTicketNumber() {
        return "INC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Domain methods encapsulating state change rules
    public void assignTo(User agent) {
        if (agent == null) {
            throw new IllegalArgumentException("Assignee cannot be null");
        }
        if (this.status == IncidentStatus.NEW) {
            transitionTo(IncidentStatus.CLASSIFIED);
        }
        if (this.status == IncidentStatus.CLASSIFIED || this.status == IncidentStatus.OPEN) {
            transitionTo(IncidentStatus.ASSIGNED);
        } else if (this.status != IncidentStatus.ASSIGNED) {
            throw new IllegalStateException("Incidents can only be assigned before acknowledgement");
        }
        this.assignee = agent;
    }

    public void classify(com.iitsm.intelligence.priority.PriorityDecision decision) {
        this.priorityScore=BigDecimal.valueOf(decision.score()); this.priority=decision.priority(); this.priorityExplanation=decision.explanation();
        if (status == IncidentStatus.NEW || status == IncidentStatus.OPEN) transitionTo(IncidentStatus.CLASSIFIED);
    }

    public void acknowledge(User actor) {
        if (assignee == null || actor == null || !(assignee == actor || (assignee.getId() != null && assignee.getId().equals(actor.getId())))) throw new IllegalStateException("Only the assigned technician may acknowledge this incident");
        transitionTo(IncidentStatus.ACKNOWLEDGED);
    }

    public void resolveWith(String details) {
        if(details==null||details.isBlank()) throw new IllegalArgumentException("Resolution details are required");
        this.resolutionDetails=details.trim(); transitionTo(IncidentStatus.RESOLVED);
    }

    public void resolve() {
        transitionTo(IncidentStatus.RESOLVED);
    }

    public void close() {
        transitionTo(IncidentStatus.CLOSED);
    }

    public void transitionTo(IncidentStatus target) {
        Map<IncidentStatus, Set<IncidentStatus>> allowed = Map.of(
                IncidentStatus.NEW, Set.of(IncidentStatus.CLASSIFIED),
                IncidentStatus.CLASSIFIED, Set.of(IncidentStatus.ASSIGNED),
                IncidentStatus.ASSIGNED, Set.of(IncidentStatus.ACKNOWLEDGED),
                IncidentStatus.ACKNOWLEDGED, Set.of(IncidentStatus.IN_PROGRESS),
                IncidentStatus.IN_PROGRESS, Set.of(IncidentStatus.RESOLVED),
                IncidentStatus.RESOLVED, Set.of(IncidentStatus.VERIFIED),
                IncidentStatus.VERIFIED, Set.of(IncidentStatus.CLOSED),
                // OPEN is retained for incidents created before the classified lifecycle.
                IncidentStatus.OPEN, Set.of(IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS),
                IncidentStatus.CLOSED, Set.of());
        if (target == null || !allowed.getOrDefault(this.status, Set.of()).contains(target)) {
            throw new IllegalStateException("Invalid incident transition: " + this.status + " -> " + target);
        }
        this.status = target;
        java.time.Instant now=java.time.Instant.now();
        if(target==IncidentStatus.ACKNOWLEDGED) acknowledgedAt=now;
        if(target==IncidentStatus.RESOLVED) resolvedAt=now;
        if(target==IncidentStatus.VERIFIED) verifiedAt=now;
        if(target==IncidentStatus.CLOSED) closedAt=now;
    }

    // Getters and Setters
    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        if (this.status == null) this.status = status;
        else transitionTo(status);
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public IncidentCategory getCategoryDefinition() { return categoryDefinition; }

    public void setCategoryDefinition(IncidentCategory categoryDefinition) {
        this.categoryDefinition = categoryDefinition;
    }

    public ItService getServiceDefinition() { return serviceDefinition; }
    public void setServiceDefinition(ItService serviceDefinition) { this.serviceDefinition = serviceDefinition; }

    public void setCategory(String category) {
        this.category = category;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public ImpactLevel getImpact() { return impact; }
    public UrgencyLevel getUrgency() { return urgency; }
    public int getBusinessCriticality() { return businessCriticality; }
    public int getAffectedUserCount() { return affectedUserCount; }
    public String getAffectedService() { return affectedService; }
    public int getServiceCriticality() { return serviceCriticality; }
    public BigDecimal getPriorityScore() { return priorityScore; }
    public String getPriorityExplanation() { return priorityExplanation; }
    public String getResolutionDetails(){return resolutionDetails;}
    public java.time.Instant getAcknowledgedAt(){return acknowledgedAt;}
    public java.time.Instant getResolvedAt(){return resolvedAt;}
    public java.time.Instant getVerifiedAt(){return verifiedAt;}
    public java.time.Instant getClosedAt(){return closedAt;}
    public void updateDetails(String title, String description, String category, ImpactLevel impact, UrgencyLevel urgency,
                              int businessCriticality, int affectedUserCount, String affectedService, int serviceCriticality) {
        this.title=title; this.description=description; this.category=category; this.impact=impact; this.urgency=urgency;
        this.businessCriticality=businessCriticality; this.affectedUserCount=affectedUserCount;
        this.affectedService=affectedService; this.serviceCriticality=serviceCriticality;
    }
}
