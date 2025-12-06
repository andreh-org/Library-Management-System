package com.library.observer;

import java.util.List;

/**
 * Subject interface for Observer Pattern
 * Follows Observer Pattern from refactoring.guru
 * @author Library Team
 * @version 1.0
 */
public interface Subject {
    /**
     * Attach an observer to the subject
     * @param observer the observer to attach
     */
    void attach(Observer observer);

    /**
     * Detach an observer from the subject
     * @param observer the observer to detach
     */
    void detach(Observer observer);

    /**
     * Notify all observers about an event
     * @param event the event that occurred
     */
    void notifyObservers(NotificationEvent event);
}