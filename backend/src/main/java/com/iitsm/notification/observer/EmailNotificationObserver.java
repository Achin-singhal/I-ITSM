package com.iitsm.notification.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.iitsm.user.repository.UserRepository;

/**
 * Concrete Observer: Dispatches simulated external email notifications.
 * Demonstrates Polymorphic implementation of IncidentEventObserver.
 */
@Component
public class EmailNotificationObserver implements IncidentEventObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationObserver.class);
    private final UserRepository users;
    private final java.util.Map<String,Boolean> dispatchedEventKeys=new java.util.LinkedHashMap<>(256,0.75f,true){
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String,Boolean> eldest){return size()>10_000;}
    };
    public EmailNotificationObserver(UserRepository users){this.users=users;}

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public void onIncidentEvent(IncidentEvent event) {
        synchronized(dispatchedEventKeys){if(dispatchedEventKeys.putIfAbsent(event.getEventKey(),Boolean.TRUE)!=null)return;}
        var incident=event.getIncident();
        java.util.Set<Long> recipientIds=new java.util.LinkedHashSet<>();
        if(incident.getReporter()!=null)recipientIds.add(incident.getReporter().getId());
        if(event.getEventType()!=IncidentEvent.EventType.CREATED&&event.getEventType()!=IncidentEvent.EventType.INFORMATION_REQUESTED&&incident.getAssignee()!=null)recipientIds.add(incident.getAssignee().getId());
        for(Long id:recipientIds)users.findById(id).ifPresent(recipient->log.info("[SIMULATED EMAIL] To: {} | Incident {} - Event: {}. Details: {}",
                recipient.getEmail(),incident.getTicketNumber(),event.getEventType(),event.getDescription()));
    }
}
