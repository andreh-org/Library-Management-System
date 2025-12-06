package com.library.service;

import com.library.model.Loan;
import com.library.model.User;
import com.library.observer.*;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for handling reminder operations with Observer Pattern
 * @author Library Team
 * @version 2.1
 */
public class ReminderService {
    private final EmailService emailService;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final LoanSubject loanSubject; // Observer Pattern subject

    public ReminderService(EmailService emailService, LoanRepository loanRepository, UserRepository userRepository) {
        this.emailService = emailService;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.loanSubject = new LoanSubject(null);

        // Attach observers - use real user emails
        attachObservers(false); // Pass false to use actual user emails
    }

    // NEW: Constructor with configurable email destination
    public ReminderService(EmailService emailService, LoanRepository loanRepository,
                           UserRepository userRepository, boolean useFixedEmail) {
        this.emailService = emailService;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.loanSubject = new LoanSubject(null);

        attachObservers(useFixedEmail);
    }

    private void attachObservers(boolean useFixedEmail) {
        // Attach email notifier with configurable email destination
        loanSubject.attach(new EmailNotifier(emailService, useFixedEmail));

        // Attach console notifier for debugging
        loanSubject.attach(new ConsoleNotifier());

        // Attach file logger
        loanSubject.attach(new FileLoggerNotifier("library_notifications.log"));
    }

    /**
     * Send overdue reminders using Observer Pattern
     */
    public void sendOverdueRemindersToAllUsers() {
        LocalDate currentDate = LocalDate.now();
        List<Loan> overdueLoans = loanRepository.getOverdueLoans(currentDate);

        // Group overdue loans by user
        var loansByUser = overdueLoans.stream()
                .collect(java.util.stream.Collectors.groupingBy(Loan::getUserId));

        for (var entry : loansByUser.entrySet()) {
            String userId = entry.getKey();
            List<Loan> userOverdueLoans = entry.getValue();
            User user = userRepository.findUserById(userId);

            if (user != null) {
                // Create notification event
                NotificationEvent event = new NotificationEvent(
                        user,
                        "OVERDUE_DETECTED",
                        String.format("Dear %s,\n\nYou have %d overdue item(s). Please return them as soon as possible.",
                                user.getName(), userOverdueLoans.size()),
                        userOverdueLoans
                );

                // Update subject and notify observers
                loanSubject.notifyObservers(event);
            }
        }
    }

    /**
     * Send overdue reminder to specific user using Observer Pattern
     */
    public void sendOverdueReminderToUser(String userId, int overdueCount) {
        User user = userRepository.findUserById(userId);
        if (user == null) {
            System.out.println("Error: User not found with ID: " + userId);
            return;
        }

        // Create notification event
        NotificationEvent event = new NotificationEvent(
                user,
                "OVERDUE_DETECTED",
                String.format("Dear %s,\n\nYou have %d overdue item(s). Please return them as soon as possible.",
                        user.getName(), overdueCount)
        );

        // Notify observers
        loanSubject.notifyObservers(event);
    }

    /**
     * Direct method for backward compatibility
     * This method creates a temporary user with the provided email
     */
    public void sendOverdueReminder(String email, String userName, int overdueCount) {
        // Create a temporary user with the provided email
        User tempUser = new User("TEMP", userName, email);

        NotificationEvent event = new NotificationEvent(
                tempUser,
                "OVERDUE_DETECTED",
                String.format("Dear %s,\n\nYou have %d overdue item(s). Please return them as soon as possible.",
                        userName, overdueCount)
        );

        loanSubject.notifyObservers(event);
    }

    /**
     * Send fine notification using Observer Pattern
     */
    public void sendFineNotification(User user, double fineAmount, String reason) {
        NotificationEvent event = new NotificationEvent(
                user,
                "FINE_APPLIED",
                String.format("Dear %s,\n\nA fine of $%.2f has been applied to your account for: %s",
                        user.getName(), fineAmount, reason),
                fineAmount
        );

        loanSubject.notifyObservers(event);
    }
}