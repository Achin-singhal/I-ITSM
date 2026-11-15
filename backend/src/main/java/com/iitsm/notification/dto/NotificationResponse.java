package com.iitsm.notification.dto;

import com.iitsm.notification.entity.Notification;
import java.time.Instant;

public record NotificationResponse(Long id, Long incidentId, String title, String message, String channel,
                                   boolean read, Instant createdAt) {
    public static NotificationResponse from(Notification n) { return new NotificationResponse(n.getId(), n.getIncident()==null?null:n.getIncident().getId(), n.getTitle(), n.getMessage(), n.getChannel(), n.isRead(), n.getCreatedAt()); }
}
