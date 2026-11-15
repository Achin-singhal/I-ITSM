package com.iitsm.notification.observer;

import org.springframework.stereotype.Component;
import com.iitsm.notification.entity.Notification;
import com.iitsm.notification.repository.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;
import com.iitsm.user.entity.User;
import com.iitsm.incident.repository.IncidentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Concrete Observer: Persists in-app alert records for users.
 * Demonstrates Polymorphic implementation of IncidentEventObserver.
 */
@Component
public class InAppNotificationObserver implements IncidentEventObserver {

    private final NotificationRepository repository;
    private final IncidentRepository incidents;
    public InAppNotificationObserver(NotificationRepository repository, IncidentRepository incidents) {
        this.repository=repository;
        this.incidents=incidents;
    }

    @Override
    public String getChannelName() {
        return "IN_APP";
    }

    @Override
    @Transactional
    public void onIncidentEvent(IncidentEvent event) {
        var incident=event.getIncident();
        if (incident.getId()!=null) {
            incident=incidents.findByIdForUpdate(incident.getId())
                    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found while recording notification"));
        }
        java.util.List<User> recipients = new java.util.ArrayList<>();
        if (incident.getReporter()!=null) recipients.add(incident.getReporter());
        User assignedUser = incident.getAssignee();
        boolean assigneeAlreadyIncluded = assignedUser != null && recipients.stream()
                .anyMatch(user -> user.getId().equals(assignedUser.getId()));
        if (event.getEventType()!=IncidentEvent.EventType.CREATED && event.getEventType()!=IncidentEvent.EventType.INFORMATION_REQUESTED
                && incident.getAssignee()!=null && !assigneeAlreadyIncluded) recipients.add(incident.getAssignee());
        String title = switch(event.getEventType()) {
            case CREATED -> "Incident created"; case ASSIGNED -> "Incident assigned";
            case PRIORITY_CHANGED -> "Incident priority changed"; case SLA_WARNING -> "SLA deadline approaching";
            case SLA_VIOLATED -> "SLA target violated"; case INFORMATION_REQUESTED -> "Additional information requested";
            case SLA_ESCALATED -> "SLA incident escalated";
            case RESOLVED -> "Incident resolved"; case CLOSED -> "Incident closed"; default -> "Incident updated";
        };
        for (User recipient : recipients) {
            String key="IN_APP:"+incident.getId()+":"+event.getEventKey()+":"+recipient.getId();
            if (repository.existsByDeduplicationKey(key)) continue;
            repository.saveAndFlush(new Notification(recipient, incident, title, event.getDescription(), "IN_APP", key));
        }
    }
}
