package com.iitsm.notification.observer;

/**
 * Observer interface for reacting to incident lifecycle changes.
 * Demonstrates Abstraction and the Observer Design Pattern.
 */
public interface IncidentEventObserver {

    /**
     * Unique identifier for the observer channel (e.g. EMAIL, IN_APP).
     */
    String getChannelName();

    /**
     * Callback triggered when an incident event occurs.
     *
     * @param event The incident event containing context
     */
    void onIncidentEvent(IncidentEvent event);
}
