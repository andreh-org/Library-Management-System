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
 * @version 1.0
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
        reminderService = new ReminderService(mockEmailService, mockLoanRepository, mockUserRepository);
    }

    @Test
    void testSendOverdueReminderToUser() {
        // Arrange
        String userId = "U001";
        String userName = "John Smith";
        int overdueCount = 2;

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(userName);
        when(mockUserRepository.findUserById(userId)).thenReturn(mockUser);

        // Act
        reminderService.sendOverdueReminderToUser(userId, overdueCount);

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockEmailService).sendEmail(emailCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertEquals("andrehkhouri333@gmail.com", emailCaptor.getValue());
        assertEquals("Overdue Book Reminder", subjectCaptor.getValue());
        assertTrue(bodyCaptor.getValue().contains("Dear " + userName));
        assertTrue(bodyCaptor.getValue().contains("You have " + overdueCount + " overdue book(s)"));
    }

    @Test
    void testSendOverdueReminder() {
        // Arrange
        String email = "test@example.com";
        String userName = "Test User";
        int overdueCount = 3;

        // Act
        reminderService.sendOverdueReminder(email, userName, overdueCount);

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockEmailService).sendEmail(emailCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertEquals(email, emailCaptor.getValue());
        assertEquals("Overdue Book Reminder", subjectCaptor.getValue());
        assertTrue(bodyCaptor.getValue().contains("Dear " + userName));
        assertTrue(bodyCaptor.getValue().contains("You have " + overdueCount + " overdue book(s)"));
    }

    @Test
    void testSendOverdueRemindersToAllUsers() {
        // Arrange
        LocalDate currentDate = LocalDate.now();

        Loan loan1 = mock(Loan.class);
        when(loan1.getUserId()).thenReturn("U001");

        Loan loan2 = mock(Loan.class);
        when(loan2.getUserId()).thenReturn("U001");

        Loan loan3 = mock(Loan.class);
        when(loan3.getUserId()).thenReturn("U002");

        List<Loan> overdueLoans = Arrays.asList(loan1, loan2, loan3);
        when(mockLoanRepository.getOverdueLoans(currentDate)).thenReturn(overdueLoans);

        User user1 = mock(User.class);
        when(user1.getName()).thenReturn("John Smith");
        when(mockUserRepository.findUserById("U001")).thenReturn(user1);

        User user2 = mock(User.class);
        when(user2.getName()).thenReturn("Emma Johnson");
        when(mockUserRepository.findUserById("U002")).thenReturn(user2);

        // Act
        reminderService.sendOverdueRemindersToAllUsers();

        // Assert - Should send 2 emails: one to U001 (2 books) and one to U002 (1 book)
        verify(mockEmailService, times(2)).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendOverdueReminderUserNotFound() {
        // Arrange
        String userId = "NONEXISTENT";
        when(mockUserRepository.findUserById(userId)).thenReturn(null);

        // Act
        reminderService.sendOverdueReminderToUser(userId, 1);

        // Assert
        verify(mockEmailService, never()).sendEmail(anyString(), anyString(), anyString());
    }
}
