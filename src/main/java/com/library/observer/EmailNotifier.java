package com.library.observer;

import com.library.service.EmailService;
import com.library.model.User; // ADD THIS IMPORT

/**
 * Email notifier observer
 * Follows Observer Pattern from refactoring.guru
 * @author Library Team
 * @version 1.1
 */
public class EmailNotifier implements Observer {
    private EmailService emailService;
    private boolean useFixedEmail;

    public EmailNotifier(EmailService emailService) {
        this.emailService = emailService;
        this.useFixedEmail = true;
    }

    // NEW: Constructor with configurable email destination
    public EmailNotifier(EmailService emailService, boolean useFixedEmail) {
        this.emailService = emailService;
        this.useFixedEmail = useFixedEmail;
    }

    @Override
    public void update(NotificationEvent event) {
        if (event.getUser() != null) {
            String toEmail = getDestinationEmail(event.getUser());
            String subject = getSubjectForEvent(event);
            String body = getBodyForEvent(event);

            try {
                emailService.sendEmail(toEmail, subject, body);
                System.out.println("Email notification sent to " + toEmail);
            } catch (Exception e) {
                System.err.println("Failed to send email notification: " + e.getMessage());
            }
        }
    }

    private String getDestinationEmail(User user) {
        if (useFixedEmail) {
            return "andrehkhouri333@gmail.com"; // Fixed email for testing
        } else {
            return user.getEmail() != null ? user.getEmail() : "andrehkhouri333@gmail.com";
        }
    }

    private String getSubjectForEvent(NotificationEvent event) {
        switch (event.getEventType()) {
            case "OVERDUE_DETECTED":
                return "Overdue Item Reminder";
            case "FINE_APPLIED":
                return "Library Fine Notification";
            case "FINE_PAID":
                return "Fine Payment Confirmation";
            case "BORROWING_RESTORED":
                return "Borrowing Privileges Restored";
            default:
                return "Library Notification";
        }
    }

    private String getBodyForEvent(NotificationEvent event) {
        return event.getMessage() + "\n\nBest regards,\nAn Najah Library System";
    }
}