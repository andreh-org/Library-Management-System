package com.library.observer;

import com.library.model.User;

/**
 * Notification event class
 * Contains all information needed for notification
 * @author Library Team
 * @version 1.0
 */
public class NotificationEvent {
    private User user;
    private String eventType;
    private String message;
    private Object data;

    public NotificationEvent(User user, String eventType, String message) {
        this.user = user;
        this.eventType = eventType;
        this.message = message;
    }

    public NotificationEvent(User user, String eventType, String message, Object data) {
        this.user = user;
        this.eventType = eventType;
        this.message = message;
        this.data = data;
    }

    // Getters
    public User getUser() { return user; }
    public String getEventType() { return eventType; }
    public String getMessage() { return message; }
    public Object getData() { return data; }

    @Override
    public String toString() {
        return String.format("NotificationEvent[type=%s, user=%s, message=%s]",
                eventType, user != null ? user.getUserId() : "null", message);
    }
}