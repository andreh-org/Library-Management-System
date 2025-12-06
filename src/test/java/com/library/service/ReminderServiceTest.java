package com.library.service;

import com.library.model.Loan;
import com.library.model.User;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ReminderService
 * @author Library Team
 * @version 2.1
 */
class ReminderServiceTest {
    private EmailService mockEmailService;
    private LoanRepository mockLoanRepository;
    private UserRepository mockUserRepository;
    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        mockEmailService = mock(EmailService.class);
        mockLoanRepository = mock(LoanRepository.class);
        mockUserRepository = mock(UserRepository.class);
        // Use the constructor that sends to actual user emails for testing
        reminderService = new ReminderService(mockEmailService, mockLoanRepository, mockUserRepository, false);
    }

    @Test
    void testSendOverdueReminderToUserWithFixedEmail() {
        // Test with fixed email (for backward compatibility)
        ReminderService fixedEmailService = new ReminderService(
                mockEmailService, mockLoanRepository, mockUserRepository, true);

        String userId = "U001";
        String userName = "John Smith";
        String userEmail = "john.smith@email.com";
        int overdueCount = 2;

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(userName);
        when(mockUser.getEmail()).thenReturn(userEmail);
        when(mockUserRepository.findUserById(userId)).thenReturn(mockUser);

        // Act
        fixedEmailService.sendOverdueReminderToUser(userId, overdueCount);

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        // Verify email was sent
        verify(mockEmailService, timeout(1000).atLeastOnce())
                .sendEmail(emailCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertEquals("andrehkhouri333@gmail.com", emailCaptor.getValue());
        assertEquals("Overdue Item Reminder", subjectCaptor.getValue());

        // Check email body
        String body = bodyCaptor.getValue();
        assertTrue(body.contains("Dear " + userName));
        assertTrue(body.contains("" + overdueCount + " overdue item(s)"));
    }

    @Test
    void testSendOverdueReminderToUserWithActualEmail() {
        // Test with actual user email
        String userId = "U001";
        String userName = "John Smith";
        String userEmail = "john.smith@email.com";
        int overdueCount = 2;

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(userName);
        when(mockUser.getEmail()).thenReturn(userEmail);
        when(mockUserRepository.findUserById(userId)).thenReturn(mockUser);

        // Act
        reminderService.sendOverdueReminderToUser(userId, overdueCount);

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        // Verify email was sent to user's actual email
        verify(mockEmailService, timeout(1000).atLeastOnce())
                .sendEmail(emailCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertEquals(userEmail, emailCaptor.getValue()); // User's actual email
        assertEquals("Overdue Item Reminder", subjectCaptor.getValue());

        // Check email body
        String body = bodyCaptor.getValue();
        assertTrue(body.contains("Dear " + userName));
        assertTrue(body.contains("" + overdueCount + " overdue item(s)"));
    }

    @Test
    void testSendOverdueReminder() {
        // This method should send to the email provided in the parameter
        String email = "test@example.com";
        String userName = "Test User";
        int overdueCount = 3;

        // Act
        reminderService.sendOverdueReminder(email, userName, overdueCount);

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        // Verify email was sent
        verify(mockEmailService, timeout(1000).atLeastOnce())
                .sendEmail(emailCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertEquals(email, emailCaptor.getValue()); // Should use the provided email
        assertEquals("Overdue Item Reminder", subjectCaptor.getValue());

        // Check email body
        String body = bodyCaptor.getValue();
        assertTrue(body.contains("Dear " + userName));
        assertTrue(body.contains("" + overdueCount + " overdue item(s)"));
        assertTrue(body.contains("Best regards"));
    }

    @Test
    void testSendOverdueRemindersToAllUsers() {
        // Arrange
        LocalDate currentDate = LocalDate.now();

        Loan loan1 = mock(Loan.class);
        when(loan1.getUserId()).thenReturn("U001");
        when(loan1.isOverdue()).thenReturn(true);

        Loan loan2 = mock(Loan.class);
        when(loan2.getUserId()).thenReturn("U001");
        when(loan2.isOverdue()).thenReturn(true);

        Loan loan3 = mock(Loan.class);
        when(loan3.getUserId()).thenReturn("U002");
        when(loan3.isOverdue()).thenReturn(true);

        List<Loan> overdueLoans = Arrays.asList(loan1, loan2, loan3);
        when(mockLoanRepository.getOverdueLoans(currentDate)).thenReturn(overdueLoans);

        User user1 = mock(User.class);
        when(user1.getName()).thenReturn("John Smith");
        when(user1.getEmail()).thenReturn("john@email.com");
        when(mockUserRepository.findUserById("U001")).thenReturn(user1);

        User user2 = mock(User.class);
        when(user2.getName()).thenReturn("Emma Johnson");
        when(user2.getEmail()).thenReturn("emma@email.com");
        when(mockUserRepository.findUserById("U002")).thenReturn(user2);

        // Act
        reminderService.sendOverdueRemindersToAllUsers();

        // Assert - Should send emails to actual user emails
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify emails were sent to actual user emails
        verify(mockEmailService, timeout(1000)).sendEmail(eq("john@email.com"),
                eq("Overdue Item Reminder"), anyString());
        verify(mockEmailService, timeout(1000)).sendEmail(eq("emma@email.com"),
                eq("Overdue Item Reminder"), anyString());
    }

    @Test
    void testSendOverdueReminderUserNotFound() {
        // Arrange
        String userId = "NONEXISTENT";
        when(mockUserRepository.findUserById(userId)).thenReturn(null);

        // Act
        reminderService.sendOverdueReminderToUser(userId, 1);

        // Assert - Should not send email
        verify(mockEmailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendFineNotification() {
        // Arrange
        String userId = "U001";
        String userName = "John Smith";
        String userEmail = "john.smith@email.com";
        double fineAmount = 25.0;
        String reason = "Late return";

        User mockUser = mock(User.class);
        when(mockUser.getUserId()).thenReturn(userId);
        when(mockUser.getName()).thenReturn(userName);
        when(mockUser.getEmail()).thenReturn(userEmail);
        when(mockUserRepository.findUserById(userId)).thenReturn(mockUser);

        // Act
        reminderService.sendFineNotification(mockUser, fineAmount, reason);

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        // Verify email was sent to user's actual email
        verify(mockEmailService, timeout(1000).atLeastOnce())
                .sendEmail(emailCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertEquals(userEmail, emailCaptor.getValue());
        assertEquals("Library Fine Notification", subjectCaptor.getValue());

        String body = bodyCaptor.getValue();
        assertTrue(body.contains("$25.00"));
        assertTrue(body.contains("Late return"));
    }

    @Test
    void testSendOverdueReminderToUserWithNoEmail() {
        // Test user with no email address
        String userId = "U003";
        String userName = "Michael Brown";
        int overdueCount = 1;

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(userName);
        when(mockUser.getEmail()).thenReturn(null); // No email
        when(mockUserRepository.findUserById(userId)).thenReturn(mockUser);

        // Act
        reminderService.sendOverdueReminderToUser(userId, overdueCount);

        // Assert - Should fall back to fixed email
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        // Verify email was sent to fixed email
        verify(mockEmailService, timeout(1000).atLeastOnce())
                .sendEmail(emailCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertEquals("andrehkhouri333@gmail.com", emailCaptor.getValue()); // Fallback email
        assertEquals("Overdue Item Reminder", subjectCaptor.getValue());
    }
}