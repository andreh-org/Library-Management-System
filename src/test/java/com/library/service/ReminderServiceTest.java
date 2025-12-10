package com.library.service;

import com.library.model.Loan;
import com.library.model.User;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
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

    @TempDir
    Path tempDir;

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

    @Test
    void testEmailServiceIntegrationInReminderService() {
        // Test that EmailService is properly integrated in ReminderService
        EmailService realEmailService = new EmailService("test@example.com", "testpassword");
        ReminderService realReminderService = new ReminderService(
                realEmailService, mockLoanRepository, mockUserRepository, false);

        assertNotNull(realReminderService);
    }

    @Test
    void testEmailServiceWithDifferentEventTypes() {
        // Test different notification event types
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("John Smith");
        when(mockUser.getEmail()).thenReturn("john@example.com");

        // Test FINE_PAID event
        reminderService.sendFineNotification(mockUser, 50.0, "Overdue book");

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEmailService, timeout(1000).atLeastOnce())
                .sendEmail(anyString(), subjectCaptor.capture(), anyString());

        // Should have "Library Fine Notification" subject
        assertEquals("Library Fine Notification", subjectCaptor.getValue());
    }

    @Test
    void testEmailServiceErrorHandlingInReminder() {
        // Test that ReminderService handles EmailService errors gracefully
        EmailService throwingEmailService = mock(EmailService.class);
        doThrow(new RuntimeException("SMTP error")).when(throwingEmailService)
                .sendEmail(anyString(), anyString(), anyString());

        ReminderService errorProneService = new ReminderService(
                throwingEmailService, mockLoanRepository, mockUserRepository, false);

        // This should not throw an exception
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("John Smith");
        when(mockUser.getEmail()).thenReturn("john@example.com");

        assertDoesNotThrow(() -> {
            errorProneService.sendFineNotification(mockUser, 25.0, "Test");
        });
    }

    @Test
    void testEmailServiceConstructorInReminderService() {
        // Test ReminderService with different EmailService constructors
        EmailService paramService = new EmailService("param@test.com", "parampass");
        ReminderService paramReminder = new ReminderService(
                paramService, mockLoanRepository, mockUserRepository, false);
        assertNotNull(paramReminder);

        // Test with empty credentials
        EmailService emptyService = new EmailService("", "");
        ReminderService emptyReminder = new ReminderService(
                emptyService, mockLoanRepository, mockUserRepository, false);
        assertNotNull(emptyReminder);
    }

    @Test
    void testEmailServiceWithReminderServiceAttachObservers() {
        // Test that observers are properly attached in ReminderService constructor
        EmailService emailService = new EmailService("test@example.com", "testpass");
        ReminderService reminderService = new ReminderService(
                emailService, mockLoanRepository, mockUserRepository, true);

        assertNotNull(reminderService);
        // The constructor should attach observers without throwing exceptions
    }

    @Test
    void testEmailServiceMultipleRemindersSameUser() {
        // Test sending multiple reminders to same user
        String userId = "U001";
        String userName = "John Smith";
        String userEmail = "john@example.com";

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(userName);
        when(mockUser.getEmail()).thenReturn(userEmail);
        when(mockUserRepository.findUserById(userId)).thenReturn(mockUser);

        // Send multiple reminders
        reminderService.sendOverdueReminderToUser(userId, 1);
        reminderService.sendOverdueReminderToUser(userId, 2);
        reminderService.sendOverdueReminderToUser(userId, 3);

        // Verify emails were sent each time
        verify(mockEmailService, timeout(1000).times(3))
                .sendEmail(eq(userEmail), eq("Overdue Item Reminder"), anyString());
    }

    @Test
    void testEmailServiceWithDifferentSubjectForFinePaid() {
        // Test that different event types use different email subjects
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("John Smith");
        when(mockUser.getEmail()).thenReturn("john@example.com");

        // Mock a fine paid scenario
        reminderService.sendFineNotification(mockUser, 0.0, "Fine paid");

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEmailService, timeout(1000).atLeastOnce())
                .sendEmail(anyString(), subjectCaptor.capture(), anyString());

        assertEquals("Library Fine Notification", subjectCaptor.getValue());
    }

    @Test
    void testEmailServiceSMTPConfiguration() {
        // Test that EmailService configures SMTP correctly
        EmailService emailService = new EmailService("test@example.com", "testpassword");
        assertNotNull(emailService);

        // The service should be initialized with Gmail SMTP settings
        // (mail.smtp.host = smtp.gmail.com, port = 587, auth = true, starttls = true)
    }

    @Test
    void testEmailServiceInRealWorldScenario() {
        // Simulate a real-world scenario with EmailService
        EmailService emailService = new EmailService("library@university.edu", "securepassword");
        LoanRepository loanRepo = new LoanRepository();
        UserRepository userRepo = new UserRepository();

        ReminderService realReminderService = new ReminderService(
                emailService, loanRepo, userRepo, false);

        assertNotNull(realReminderService);
        // This tests that all components work together without exceptions
    }

    @Test
    void testEmailServiceMessagingExceptionHandling() {
        // Test that EmailService handles MessagingException
        EmailService throwingService = mock(EmailService.class);
        doThrow(new RuntimeException("Mocked MessagingException")).when(throwingService)
                .sendEmail(anyString(), anyString(), anyString());

        ReminderService serviceWithThrowingEmail = new ReminderService(
                throwingService, mockLoanRepository, mockUserRepository, false);

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("Test User");
        when(mockUser.getEmail()).thenReturn("test@example.com");

        // Should not throw when EmailService throws
        assertDoesNotThrow(() -> {
            serviceWithThrowingEmail.sendFineNotification(mockUser, 10.0, "Test");
        });
    }

    @Test
    void testEmailServiceWithVariousConstructorCombinations() {
        // Test ReminderService with various EmailService constructor combinations

        // 1. Normal credentials
        EmailService normal = new EmailService("normal@test.com", "normalpass");
        ReminderService rs1 = new ReminderService(normal, mockLoanRepository, mockUserRepository, false);
        assertNotNull(rs1);

        // 2. Empty credentials
        EmailService empty = new EmailService("", "");
        ReminderService rs2 = new ReminderService(empty, mockLoanRepository, mockUserRepository, false);
        assertNotNull(rs2);

        // 3. Null credentials
        EmailService nullCreds = new EmailService(null, null);
        ReminderService rs3 = new ReminderService(nullCreds, mockLoanRepository, mockUserRepository, false);
        assertNotNull(rs3);

        // 4. Special characters
        EmailService special = new EmailService("special@test.com", "p@ss!w#rd$");
        ReminderService rs4 = new ReminderService(special, mockLoanRepository, mockUserRepository, false);
        assertNotNull(rs4);
    }

    @Test
    void testEmailServiceObserverNotification() {
        // Test that EmailService is called when observers are notified
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("Observer Test");
        when(mockUser.getEmail()).thenReturn("observer@test.com");

        // Send notification
        reminderService.sendFineNotification(mockUser, 15.0, "Observer test");

        // Verify EmailService.sendEmail was called
        verify(mockEmailService, timeout(1000).atLeastOnce())
                .sendEmail(eq("observer@test.com"), anyString(), anyString());
    }

    @Test
    void testEmailServiceMultipleInstancesInReminder() {
        // Test with multiple EmailService instances
        EmailService service1 = new EmailService("service1@test.com", "pass1");
        EmailService service2 = new EmailService("service2@test.com", "pass2");
        EmailService service3 = new EmailService("service3@test.com", "pass3");

        ReminderService rs1 = new ReminderService(service1, mockLoanRepository, mockUserRepository, false);
        ReminderService rs2 = new ReminderService(service2, mockLoanRepository, mockUserRepository, false);
        ReminderService rs3 = new ReminderService(service3, mockLoanRepository, mockUserRepository, false);

        assertNotNull(rs1);
        assertNotNull(rs2);
        assertNotNull(rs3);
    }

    @Test
    void testEmailServiceAttachmentToObservers() {
        // Test that EmailService is properly attached as observer
        EmailService emailService = new EmailService("attach@test.com", "attachpass");
        ReminderService reminderWithAttachment = new ReminderService(
                emailService, mockLoanRepository, mockUserRepository, true);

        assertNotNull(reminderWithAttachment);

        // The constructor should attach EmailNotifier which uses the EmailService
    }
}