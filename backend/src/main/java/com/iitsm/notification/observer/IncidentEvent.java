package com.iitsm.notification.observer;

import com.iitsm.incident.entity.Incident;

import java.time.Instant;

/**
 * Event object passed to IncidentEventObservers.
 */
public class IncidentEvent {

    public enum EventType {
        CREATED,
        ASSIGNED,
        PRIORITY_CHANGED,
        SLA_WARNING,
        SLA_VIOLATED,
        SLA_ESCALATED,
        INFORMATION_REQUESTED,
        STATUS_CHANGED,
        RESOLVED,
        CLOSED
    }

    private final Incident incident;
    private final EventType eventType;
    private final String description;
    private final Instant timestamp;
    private final String eventKey;

    public IncidentEvent(Incident incident, EventType eventType, String description) {
        this.incident = incident;
        this.eventType = eventType;
        this.description = description;
        this.timestamp = Instant.now();
        this.eventKey = java.util.UUID.randomUUID().toString();
    }

    public Incident getIncident() {
        return incident;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
    public String getEventKey() { return eventKey; }
}
