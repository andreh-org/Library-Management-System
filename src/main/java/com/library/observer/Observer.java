package com.library.observer;

/**
 * Observer interface for Observer Pattern
 * Follows Observer Pattern from refactoring.guru
 * @author Library Team
 * @version 1.0
 */
public interface Observer {
    /**
     * Update method called by the subject
     * @param event the notification event
     */
    void update(NotificationEvent event);
}