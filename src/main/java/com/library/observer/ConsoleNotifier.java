package com.library.observer;

/**
 * Console notifier observer (for testing/debugging)
 * Follows Observer Pattern from refactoring.guru
 * @author Library Team
 * @version 1.0
 */
public class ConsoleNotifier implements Observer {

    @Override
    public void update(NotificationEvent event) {
        System.out.println("=== CONSOLE NOTIFICATION ===");
        System.out.println("Event: " + event.getEventType());
        System.out.println("User: " + (event.getUser() != null ? event.getUser().getUserId() : "Unknown"));
        System.out.println("Message: " + event.getMessage());
        System.out.println("Timestamp: " + java.time.LocalDateTime.now());
        System.out.println("===========================");
    }
}