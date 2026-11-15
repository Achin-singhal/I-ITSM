package com.iitsm.notification.entity;

import com.iitsm.common.BaseEntity;
import com.iitsm.incident.entity.Incident;
import com.iitsm.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Persisted in-app notification entity.
 */
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 32)
    private String channel = "IN_APP";

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name="deduplication_key", nullable=false, unique=true, length=180)
    private String deduplicationKey;

    public Notification() {
    }

    public Notification(User recipient, Incident incident, String title, String message, String channel) {
        this(recipient, incident, title, message, channel, java.util.UUID.randomUUID().toString());
    }

    public Notification(User recipient, Incident incident, String title, String message, String channel, String deduplicationKey) {
        this.recipient = recipient;
        this.incident = incident;
        this.title = title;
        this.message = message;
        this.channel = channel != null ? channel : "IN_APP";
        this.read = false;
        this.deduplicationKey = deduplicationKey;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
    public String getDeduplicationKey() { return deduplicationKey; }
}
