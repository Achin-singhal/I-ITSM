package com.iitsm.notification.service;

import com.iitsm.notification.observer.IncidentEvent;
import com.iitsm.notification.observer.IncidentEventObserver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject / Publisher in the Observer Pattern.
 * Manages subscriber observers and multicasts incident lifecycle events.
 */
@Service
public class NotificationPublisher {

    private final List<IncidentEventObserver> observers = new CopyOnWriteArrayList<>();

    // Spring automatically injects all beans implementing IncidentEventObserver
    public NotificationPublisher(List<IncidentEventObserver> initialObservers) {
        if (initialObservers != null) {
            this.observers.addAll(initialObservers);
        }
    }

    public void registerObserver(IncidentEventObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregisterObserver(IncidentEventObserver observer) {
        observers.remove(observer);
    }

    public void publishEvent(IncidentEvent event) {
        for (IncidentEventObserver observer : observers) {
            observer.onIncidentEvent(event);
        }
    }
}
