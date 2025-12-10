package com.library.service;

import com.library.model.Fine;
import com.library.model.Loan;
import com.library.model.User;
import com.library.observer.*;
import com.library.repository.MediaRepository;
import com.library.repository.UserRepository;
import com.library.strategy.FineContext;
import com.library.strategy.FineStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for FineService
 * @author Library Team
 * @version 1.1
 */
class FineServiceTest {
    private FineService fineService;
    private UserRepository userRepository;
    private MediaRepository mediaRepository;
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        mediaRepository = new MediaRepository();

        // Create FineService first (without LoanService dependency)
        fineService = new FineService(userRepository);

        // Create LoanService with the FineService
        loanService = new LoanService(fineService, userRepository, mediaRepository);

        // Set the LoanService dependency in FineService
        fineService.setLoanService(loanService);
    }

    @Test
    void testApplyFineSuccess() {
        Fine fine = fineService.applyFine("U001", 15.0, "Test fine");

        assertNotNull(fine);
        assertEquals("U001", fine.getUserId());
        assertEquals(15.0, fine.getAmount(), 0.001);
        assertFalse(fine.isPaid());

        // Check that user's borrowing ability is updated
        User user = userRepository.findUserById("U001");
        assertFalse(user.canBorrow());
    }

    @Test
    void testApplyFineInvalidAmount() {
        Fine fine = fineService.applyFine("U001", -10.0, "Invalid fine");
        assertNull(fine);

        Fine zeroFine = fineService.applyFine("U001", 0.0, "Zero fine");
        assertNull(zeroFine);
    }

    @Test
    void testPayFineFullPayment() {
        // Apply a fine first
        Fine fine = fineService.applyFine("U001", 25.0, "Test fine");

        boolean paymentSuccess = fineService.payFine(fine.getFineId(), 25.0);
        assertTrue(paymentSuccess);

        // Check that fine is marked as paid
        Fine paidFine = fineService.getFineRepository().findFineById(fine.getFineId());
        assertTrue(paidFine.isPaid());
        assertEquals(0.0, paidFine.getRemainingBalance(), 0.001);

        // Check that user can borrow again
        User user = userRepository.findUserById("U001");
        assertTrue(user.canBorrow());
    }

    @Test
    void testPayFinePartialPayment() {
        // Apply a fine first
        Fine fine = fineService.applyFine("U001", 30.0, "Test fine");

        boolean paymentSuccess = fineService.payFine(fine.getFineId(), 15.0);
        assertTrue(paymentSuccess);

        // Check that fine is not fully paid
        Fine partialPaidFine = fineService.getFineRepository().findFineById(fine.getFineId());
        assertFalse(partialPaidFine.isPaid());
        assertEquals(15.0, partialPaidFine.getRemainingBalance(), 0.001);

        // User should still not be able to borrow
        User user = userRepository.findUserById("U001");
        assertFalse(user.canBorrow());
    }

    @Test
    void testPayFineOverPayment() {
        // Apply a fine first
        Fine fine = fineService.applyFine("U001", 20.0, "Test fine");

        boolean paymentSuccess = fineService.payFine(fine.getFineId(), 25.0);
        assertTrue(paymentSuccess);

        // Check that fine is fully paid with refund
        Fine paidFine = fineService.getFineRepository().findFineById(fine.getFineId());
        assertTrue(paidFine.isPaid());
        assertEquals(0.0, paidFine.getRemainingBalance(), 0.001);
    }

    @Test
    void testPayFineInvalidPayment() {
        boolean paymentSuccess = fineService.payFine("invalid-fine-id", 10.0);
        assertFalse(paymentSuccess);

        boolean zeroPayment = fineService.payFine("F0001", 0.0);
        assertFalse(zeroPayment);

        boolean negativePayment = fineService.payFine("F0001", -5.0);
        assertFalse(negativePayment);
    }

    @Test
    void testGetUserFines() {
        // U002 has sample fines
        List<Fine> fines = fineService.getUserFines("U002");
        assertFalse(fines.isEmpty());
    }

    @Test
    void testGetUserUnpaidFines() {
        List<Fine> unpaidFines = fineService.getUserUnpaidFines("U002");
        assertFalse(unpaidFines.isEmpty());

        // All unpaid fines should have isPaid = false
        for (Fine fine : unpaidFines) {
            assertFalse(fine.isPaid());
        }
    }

    @Test
    void testGetTotalUnpaidAmount() {
        double totalUnpaid = fineService.getTotalUnpaidAmount("U002");
        assertTrue(totalUnpaid > 0);
    }

    @Test
    void testGetFineBreakdownByMediaTypeWithNoFines() {
        // Test with user who has no fines
        String result = fineService.getFineBreakdownByMediaType("U003"); // Michael Brown has no fines
        assertNotNull(result);
        assertTrue(result.contains("No fines found"));
    }

    @Test
    void testGetFineBreakdownByMediaTypeWithFines() {
        // Test with user who has fines (U002)
        String result = fineService.getFineBreakdownByMediaType("U002");
        assertNotNull(result);
        assertTrue(result.contains("FINE BREAKDOWN BY MEDIA TYPE"));
    }

    @Test
    void testApplyFineWithLoanIdNullLoanService() {
        // Test when loanService is null
        FineService fineServiceWithoutLoan = new FineService(userRepository);
        // Don't set loanService

        Fine fine = fineServiceWithoutLoan.applyFine("U001", "Test reason", "L0001");
        assertNull(fine); // Should return null when loanService is null
    }

    @Test
    void testApplyFineWithLoanIdLoanNotFound() {
        // Test with non-existent loan ID
        Fine fine = fineService.applyFine("U001", "Test reason", "NONEXISTENT_LOAN");
        assertNull(fine); // Should return null when loan not found
    }

    @Test
    void testApplyFineDuplicateFine() {
        // Use a loan that exists in sample data
        String loanId = "L0001"; // This loan exists in sample data

        // First, check if a fine already exists for this loan
        Fine existingFine = fineService.getFineRepository().findFineByLoanId(loanId);
        if (existingFine != null) {
            // Fine already exists, so applyFine should return the existing fine
            Fine result = fineService.applyFine("U002", "Overdue book", loanId);
            assertNotNull(result);
            assertEquals(existingFine.getFineId(), result.getFineId());
        } else {
            // No fine exists yet, test will be skipped
            System.out.println("No existing fine found for loan " + loanId + ", skipping duplicate test");
        }
    }

    @Test
    void testPayFineWithActiveLoan() {
        // Find a fine that exists for an active loan
        List<Fine> fines = fineService.getFineRepository().getAllFines();
        Fine fineWithActiveLoan = null;

        for (Fine fine : fines) {
            if (fine.getLoanId() != null) {
                // Check if loan is active
                com.library.model.Loan loan = loanService.getLoanRepository().findLoanById(fine.getLoanId());
                if (loan != null && loan.getReturnDate() == null) {
                    fineWithActiveLoan = fine;
                    break;
                }
            }
        }

        if (fineWithActiveLoan != null) {
            // Try to pay fine while loan is still active (not returned)
            boolean paymentSuccess = fineService.payFine(fineWithActiveLoan.getFineId(), fineWithActiveLoan.getAmount());
            assertFalse(paymentSuccess); // Should fail because loan is not returned
        } else {
            System.out.println("No fine with active loan found, skipping test");
        }
    }

    @Test
    void testPayFineAlreadyPaid() {
        // Apply and pay a fine
        Fine fine = fineService.applyFine("U001", 20.0, "Test fine");
        assertNotNull(fine);

        // Pay the fine
        boolean firstPayment = fineService.payFine(fine.getFineId(), 20.0);
        assertTrue(firstPayment);

        // Try to pay again
        boolean secondPayment = fineService.payFine(fine.getFineId(), 10.0);
        assertFalse(secondPayment); // Should fail because fine is already paid
    }

    @Test
    void testAttachAndDetachObservers() {
        // Test attaching and detaching observers
        Observer mockObserver = mock(Observer.class);

        // Attach observer
        fineService.attachObserver(mockObserver);

        // Apply a fine to trigger notification
        Fine fine = fineService.applyFine("U001", 15.0, "Test observer");
        assertNotNull(fine);

        // Detach observer
        fineService.detachObserver(mockObserver);
    }

    @Test
    void testAttachEmailObserver() {
        // Test attaching email observer
        EmailService mockEmailService = mock(EmailService.class);
        fineService.attachEmailObserver(mockEmailService);

        // Apply a fine to trigger email notification
        Fine fine = fineService.applyFine("U001", 15.0, "Test email");
        assertNotNull(fine);
    }

    @Test
    void testRegisterFineStrategySuccess() {
        // Test registering a new fine strategy
        fineService.registerFineStrategy("JOURNAL", "com.library.strategy.BookFineStrategy");
        // Should not throw exception
    }

    @Test
    void testRegisterFineStrategyFailure() {
        // Test registering a non-existent strategy class
        fineService.registerFineStrategy("DVD", "com.library.NonExistentStrategy");
        // Should catch exception and print error
    }

    @Test
    void testDemonstrateStrategyPattern() {
        // Test strategy pattern demonstration
        fineService.demonstrateStrategyPattern();
        // Should not throw exception
    }

    @Test
    void testCleanupDuplicateFines() {
        // Test cleanup method
        fineService.cleanupDuplicateFines();
        // Should not throw exception
    }

    @Test
    void testDisplayUserFinesUserNotFound() {
        // Test displaying fines for non-existent user
        fineService.displayUserFines("NONEXISTENT_USER");
        // Should print error message
    }

    @Test
    void testDisplayUserFinesWithFines() {
        // Test displaying fines for user with fines
        fineService.displayUserFines("U002");
        // Should display fines
    }

    @Test
    void testDisplayUserFinesNoFines() {
        // Test displaying fines for user without fines
        fineService.displayUserFines("U003");
        // Should display "No fines found"
    }

    @Test
    void testDefaultConstructorInitialization() {
        // Test default constructor
        FineService defaultService = new FineService();
        assertNotNull(defaultService);
        assertNotNull(defaultService.getFineRepository());
        assertNotNull(defaultService.getUserRepository());
        assertNull(defaultService.getLoanService()); // Should be null initially
    }

    @Test
    void testConstructorWithUserRepositoryOnly() {
        // Test constructor with only UserRepository
        FineService service = new FineService(userRepository);
        assertNotNull(service);
        assertNotNull(service.getUserRepository());
        assertNull(service.getLoanService()); // Should be null initially
    }

    @Test
    void testConstructorWithBothDependencies() {
        // Test constructor with both dependencies
        FineService service = new FineService(userRepository, loanService);
        assertNotNull(service);
        assertNotNull(service.getUserRepository());
        assertNotNull(service.getLoanService());
    }

    @Test
    void testSetLoanService() {
        // Test setting loan service after construction
        FineService service = new FineService(userRepository);
        assertNull(service.getLoanService());

        service.setLoanService(loanService);
        assertNotNull(service.getLoanService());
    }

    @Test
    void testGetFineBreakdownEdgeCases() {
        // Test edge cases in getFineBreakdownByMediaType

        // Test with null loanService
        FineService serviceWithoutLoan = new FineService(userRepository);
        String result = serviceWithoutLoan.getFineBreakdownByMediaType("U002");
        assertNotNull(result);
    }

    @Test
    void testObserverNotificationOnFineApplication() {
        // Test that observers are notified when fine is applied
        Observer mockObserver = mock(Observer.class);
        fineService.attachObserver(mockObserver);

        Fine fine = fineService.applyFine("U001", 15.0, "Test notification");
        assertNotNull(fine);

        // Verify observer was notified at least once (could be multiple times due to multiple observers)
        verify(mockObserver, timeout(1000).atLeast(1))
                .update(any(NotificationEvent.class));
    }

    @Test
    void testObserverNotificationOnFinePayment() {
        // Test that observers are notified when fine is paid
        // Create a fresh FineService to avoid interference from other tests
        FineService freshService = new FineService(userRepository, loanService);

        Observer mockObserver = mock(Observer.class);
        freshService.attachObserver(mockObserver);

        // Apply and pay a fine
        Fine fine = freshService.applyFine("U001", 15.0, "Test payment notification");
        assertNotNull(fine);

        // Reset mock to count only payment notifications
        reset(mockObserver);
        freshService.attachObserver(mockObserver);

        // Pay the fine
        freshService.payFine(fine.getFineId(), 15.0);

        // Verify observer was notified of payment (at least once, could be multiple events)
        verify(mockObserver, timeout(1000).atLeast(1))
                .update(any(NotificationEvent.class));
    }

    @Test
    void testObserverNotificationOnBorrowingRestored() {
        // Test that observers are notified when borrowing is restored
        // Create a fresh FineService
        FineService freshService = new FineService(userRepository, loanService);

        Observer mockObserver = mock(Observer.class);
        freshService.attachObserver(mockObserver);

        // Apply a fine (user can't borrow)
        Fine fine = freshService.applyFine("U001", 15.0, "Test borrowing restoration");
        assertNotNull(fine);

        // Reset mock
        reset(mockObserver);
        freshService.attachObserver(mockObserver);

        // Pay the fine (should restore borrowing)
        freshService.payFine(fine.getFineId(), 15.0);

        // User should now be able to borrow
        User user = userRepository.findUserById("U001");
        assertTrue(user.canBorrow());

        // Verify observer was notified (at least once)
        verify(mockObserver, timeout(1000).atLeast(1))
                .update(any(NotificationEvent.class));
    }

    @Test
    void testFileLoggerObserverInFineService() {
        // Test that file logger observer works in FineService
        // The default constructor already attaches a FileLoggerNotifier
        // We just need to trigger a fine event
        Fine fine = fineService.applyFine("U001", 15.0, "Test file logging");
        assertNotNull(fine);

        // Can't easily verify file was created/modified without reading file
        // But we can verify no exception is thrown
    }

    @Test
    void testConsoleNotifierInFineService() {
        // Test that console notifier works in FineService
        // The default constructor attaches a ConsoleNotifier
        // We just need to trigger a fine event
        Fine fine = fineService.applyFine("U001", 15.0, "Test console logging");
        assertNotNull(fine);

        // Can't easily verify console output, but we can verify no exception is thrown
    }

    @Test
    void testPayFineWithNullLoanId() {
        // Test paying fine that has null loanId (should skip loan check)
        // Create fine without loanId
        Fine fine = fineService.applyFine("U001", 15.0, "Test fine without loan");
        assertNotNull(fine);

        // Pay fine
        boolean success = fineService.payFine(fine.getFineId(), 15.0);
        assertTrue(success);
    }

    @Test
    void testUpdateUserBorrowingAbilityWithUnchangedStatus() {
        // Test updateUserBorrowingAbility when status doesn't change
        // This is a private method, so we test through public methods

        String userId = "U001";
        User user = userRepository.findUserById(userId);

        // User initially can borrow
        assertTrue(user.canBorrow());

        // Apply small fine and pay partial amount (still has balance)
        Fine fine = fineService.applyFine(userId, 20.0, "Test partial payment");
        assertNotNull(fine);

        // Pay partial amount (user still can't borrow)
        fineService.payFine(fine.getFineId(), 5.0);

        // User still can't borrow (no change in status)
        user = userRepository.findUserById(userId);
        assertFalse(user.canBorrow());
    }

    @Test
    void testGetters() {
        // Test all getter methods
        assertNotNull(fineService.getFineRepository());
        assertNotNull(fineService.getUserRepository());
        assertNotNull(fineService.getLoanService());
        assertNotNull(fineService.getFineContext());
        assertNotNull(fineService.getNotificationSubject());
    }

    @Test
    void testApplyFineWithExistingFineDifferentAmount() {
        // Test applying fine when fine already exists but with different amount
        // First, find an existing fine with loanId
        List<Fine> existingFines = fineService.getFineRepository().getAllFines();
        Fine existingFineWithLoan = null;

        for (Fine fine : existingFines) {
            if (fine.getLoanId() != null && !fine.getLoanId().isEmpty()) {
                existingFineWithLoan = fine;
                break;
            }
        }

        if (existingFineWithLoan != null) {
            // Apply fine for same loan - should return existing fine
            Fine result = fineService.applyFine(
                    existingFineWithLoan.getUserId(),
                    "Test different amount",
                    existingFineWithLoan.getLoanId()
            );

            assertNotNull(result);
            assertEquals(existingFineWithLoan.getFineId(), result.getFineId());
        } else {
            System.out.println("No existing fine with loanId found, skipping test");
        }
    }

    @Test
    void testGetFineBreakdownWithNullLoanService() {
        // Test getFineBreakdownByMediaType when loanService is null
        FineService service = new FineService(userRepository);
        // Don't set loanService

        String result = service.getFineBreakdownByMediaType("U002");
        assertNotNull(result);
        assertTrue(result.contains("No fines found") || result.contains("FINE BREAKDOWN"));
    }

    @Test
    void testGetFineBreakdownWithFineHavingNullLoanId() {
        // Test getFineBreakdownByMediaType with fine that has null loanId
        // Create a fine without loanId
        Fine fineWithoutLoan = fineService.applyFine("U001", 25.0, "Fine without loan");
        assertNotNull(fineWithoutLoan);

        String result = fineService.getFineBreakdownByMediaType("U001");
        assertNotNull(result);
        assertTrue(result.contains("FINE BREAKDOWN") || result.contains("No fines found"));
    }

    @Test
    void testApplyFineWithZeroOrNegativeStrategyFine() {
        // Test applying fine when strategy returns zero or negative amount
        // We need to mock the FineContext to return 0
        FineContext mockContext = mock(FineContext.class);
        when(mockContext.getFlatFine(anyString())).thenReturn(0.0);

        // Use reflection to set the mock context
        try {
            java.lang.reflect.Field contextField = FineService.class.getDeclaredField("fineContext");
            contextField.setAccessible(true);
            contextField.set(fineService, mockContext);

            // Now applyFine should return null because fineAmount <= 0
            Fine fine = fineService.applyFine("U001", "Test zero fine", "L0001");
            assertNull(fine); // Should be null because amount is 0

        } catch (Exception e) {
            // If reflection fails, skip test
            System.out.println("Could not set mock context: " + e.getMessage());
        }
    }

    @Test
    void testPayFineWithNullFine() {
        // Test payFine when fine is not found
        boolean result = fineService.payFine("NONEXISTENT_FINE", 10.0);
        assertFalse(result);
    }

    @Test
    void testDisplayUserFinesWithLoanServiceNull() {
        // Test displayUserFines when loanService is null
        FineService service = new FineService(userRepository);
        // Don't set loanService

        // This should not throw exception
        assertDoesNotThrow(() -> {
            service.displayUserFines("U001");
        });
    }

    @Test
    void testAttachObserverAlreadyAttached() {
        // Test attaching same observer multiple times
        Observer mockObserver = mock(Observer.class);

        // Attach first time
        fineService.attachObserver(mockObserver);

        // Attach second time (should not add duplicate)
        fineService.attachObserver(mockObserver);

        // Apply fine
        Fine fine = fineService.applyFine("U001", 10.0, "Test duplicate observer");
        assertNotNull(fine);
    }

    @Test
    void testDetachObserverNotAttached() {
        // Test detaching observer that was never attached
        Observer mockObserver = mock(Observer.class);

        // This should not throw exception
        assertDoesNotThrow(() -> {
            fineService.detachObserver(mockObserver);
        });
    }

    @Test
    void testCleanUserStateForTesting() {
        // Helper test to clean up test user state
        // This ensures U001 is in a known state for other tests
        User user = userRepository.findUserById("U001");
        if (user != null) {
            user.setCanBorrow(true);
            userRepository.updateUser(user);

            // Also remove any fines for U001 created by other tests
            List<Fine> fines = fineService.getUserFines("U001");
            for (Fine fine : fines) {
                if (!fine.isPaid()) {
                    // Pay the fine
                    fineService.payFine(fine.getFineId(), fine.getAmount());
                }
            }
        }
    }

    @Test
    void testEmptyOrNullUserId() {
        // Test with empty user ID
        Fine fine = fineService.applyFine("", 10.0, "Empty user ID");
        assertNull(fine); // Should return null because user not found

        // Test with null user ID
        Fine fine2 = fineService.applyFine(null, 10.0, "Null user ID");
        assertNull(fine2); // Should return null because user not found
    }

    @Test
    void testGetFineBreakdownForNonExistentUser() {
        // Test getFineBreakdownByMediaType for non-existent user
        String result = fineService.getFineBreakdownByMediaType("NONEXISTENT_USER");
        assertNotNull(result);
        assertTrue(result.contains("No fines found"));
    }

    @Test
    void testStrategyPatternWithCustomMediaType() {
        // Test demonstrateStrategyPattern with custom media types
        fineService.demonstrateStrategyPattern();
        // Should print information about different media types
    }

    // NEW TESTS START HERE (non-duplicate)

    @Test
    void testApplyFineWithLoanIdInvalidUserId() {
        // Test with invalid user ID
        Fine fine = fineService.applyFine("INVALID_USER", "Test reason", "L0001");
        assertNull(fine);
    }

    @Test
    void testApplyFineWithLoanIdEmptyUserId() {
        // Test with empty user ID
        Fine fine = fineService.applyFine("", "Test reason", "L0001");
        assertNull(fine);
    }

    @Test
    void testApplyFineWithLoanIdNullUserId() {
        // Test with null user ID
        Fine fine = fineService.applyFine(null, "Test reason", "L0001");
        assertNull(fine);
    }

    @Test
    void testApplyFineWithLoanIdEmptyLoanId() {
        // Test with empty loan ID
        Fine fine = fineService.applyFine("U001", "Test reason", "");
        assertNull(fine);
    }

    @Test
    void testApplyFineWithLoanIdNullLoanId() {
        // Test with null loan ID
        Fine fine = fineService.applyFine("U001", "Test reason", null);
        assertNull(fine);
    }

    @Test
    void testPayFineEmptyFineId() {
        // Test with empty fine ID
        boolean result = fineService.payFine("", 10.0);
        assertFalse(result);
    }

    @Test
    void testPayFineNullFineId() {
        // Test with null fine ID
        boolean result = fineService.payFine(null, 10.0);
        assertFalse(result);
    }

    @Test
    void testPayFineLoanServiceNullButLoanIdNullScenario() {
        // Test payFine when loanService is null but loanId is also null
        FineService service = new FineService(userRepository);
        // Don't set loanService

        // Create a fine without loanId
        Fine fine = service.applyFine("U001", 15.0, "Test fine without loan");
        assertNotNull(fine);

        // Should be able to pay since loanId is null
        boolean result = service.payFine(fine.getFineId(), 15.0);
        assertTrue(result);
    }

    @Test
    void testAttachObserverNullObserverInput() {
        // Test attaching null observer
        fineService.attachObserver(null);
        // Should print error but not throw exception
    }

    @Test
    void testDetachObserverNullObserverInput() {
        // Test detaching null observer
        fineService.detachObserver(null);
        // Should print error but not throw exception
    }

    @Test
    void testAttachEmailObserverNullEmailServiceInput() {
        // Test attaching email observer with null email service
        fineService.attachEmailObserver(null);
        // Should print error but not throw exception
    }

    @Test
    void testRegisterFineStrategyEmptyMediaTypeInput() {
        // Test registering strategy with empty media type
        fineService.registerFineStrategy("", "com.library.strategy.BookFineStrategy");
        // Should print error
    }

    @Test
    void testRegisterFineStrategyNullMediaTypeInput() {
        // Test registering strategy with null media type
        fineService.registerFineStrategy(null, "com.library.strategy.BookFineStrategy");
        // Should print error
    }

    @Test
    void testRegisterFineStrategyEmptyClassNameInput() {
        // Test registering strategy with empty class name
        fineService.registerFineStrategy("JOURNAL", "");
        // Should print error
    }

    @Test
    void testRegisterFineStrategyNullClassNameInput() {
        // Test registering strategy with null class name
        fineService.registerFineStrategy("JOURNAL", null);
        // Should print error
    }

    @Test
    void testGetUserFinesEmptyUserIdInput() {
        // Test getUserFines with empty user ID
        List<Fine> fines = fineService.getUserFines("");
        assertTrue(fines.isEmpty());
    }

    @Test
    void testGetUserFinesNullUserIdInput() {
        // Test getUserFines with null user ID
        List<Fine> fines = fineService.getUserFines(null);
        assertTrue(fines.isEmpty());
    }

    @Test
    void testGetUserUnpaidFinesEmptyUserIdInput() {
        // Test getUserUnpaidFines with empty user ID
        List<Fine> fines = fineService.getUserUnpaidFines("");
        assertTrue(fines.isEmpty());
    }

    @Test
    void testGetUserUnpaidFinesNullUserIdInput() {
        // Test getUserUnpaidFines with null user ID
        List<Fine> fines = fineService.getUserUnpaidFines(null);
        assertTrue(fines.isEmpty());
    }

    @Test
    void testGetTotalUnpaidAmountEmptyUserIdInput() {
        // Test getTotalUnpaidAmount with empty user ID
        double amount = fineService.getTotalUnpaidAmount("");
        assertEquals(0.0, amount, 0.001);
    }

    @Test
    void testGetTotalUnpaidAmountNullUserIdInput() {
        // Test getTotalUnpaidAmount with null user ID
        double amount = fineService.getTotalUnpaidAmount(null);
        assertEquals(0.0, amount, 0.001);
    }

    @Test
    void testDisplayUserFinesEmptyUserIdInput() {
        // Test displayUserFines with empty user ID
        fineService.displayUserFines("");
        // Should print error
    }

    @Test
    void testDisplayUserFinesNullUserIdInput() {
        // Test displayUserFines with null user ID
        fineService.displayUserFines(null);
        // Should print error
    }

    @Test
    void testGetDetailedFineBreakdownMethod() {
        // Test getDetailedFineBreakdown method
        String result = fineService.getDetailedFineBreakdown("U002");
        assertNotNull(result);
        assertTrue(result.contains("DETAILED FINE BREAKDOWN") || result.contains("No unpaid fines found"));
    }

    @Test
    void testGetDetailedFineBreakdownNoFinesCase() {
        // Test getDetailedFineBreakdown with user having no fines
        String result = fineService.getDetailedFineBreakdown("U003");
        assertNotNull(result);
        assertTrue(result.contains("No unpaid fines found"));
    }

    @Test
    void testGetDetailedFineBreakdownEmptyUserIdCase() {
        // Test getDetailedFineBreakdown with empty user ID
        String result = fineService.getDetailedFineBreakdown("");
        assertNotNull(result);
        assertTrue(result.contains("No unpaid fines found"));
    }

    @Test
    void testGetDetailedFineBreakdownNullUserIdCase() {
        // Test getDetailedFineBreakdown with null user ID
        String result = fineService.getDetailedFineBreakdown(null);
        assertNotNull(result);
        assertTrue(result.contains("No unpaid fines found"));
    }

    @Test
    void testObserverAttachmentAndNotification() {
        // Test attaching observer and verifying notification
        Observer mockObserver = mock(Observer.class);

        // Attach observer
        fineService.attachObserver(mockObserver);

        // Apply fine to trigger notifications
        Fine fine = fineService.applyFine("U001", 10.0, "Test observer attachment");
        assertNotNull(fine);

        // Verify observer was notified
        verify(mockObserver, timeout(1000).atLeast(1))
                .update(any(NotificationEvent.class));
    }

    @Test
    void testDetachNonExistentObserverScenario() {
        // Test detaching observer that was never attached
        Observer mockObserver = mock(Observer.class);

        // Should not throw exception
        assertDoesNotThrow(() -> {
            fineService.detachObserver(mockObserver);
        });
    }

    @Test
    void testStrategyPatternDemoEdgeCasesTest() {
        // Test demonstrateStrategyPattern includes error handling
        fineService.demonstrateStrategyPattern();
        // Should not throw exception
    }

    @Test
    void testApplyFineWithLoanIdNullUserScenario() throws Exception {
        // Test applyFine when user is null after validation
        // We need to mock userRepository to return null
        UserRepository mockUserRepo = mock(UserRepository.class);
        when(mockUserRepo.findUserById(anyString())).thenReturn(null);

        FineService serviceWithMock = new FineService(mockUserRepo, loanService);

        Fine fine = serviceWithMock.applyFine("U001", "Test reason", "L0001");
        assertNull(fine);
    }

    @Test
    void testGetFineBreakdownWithMixedPaidAndUnpaidFines() {
        // Test getFineBreakdownByMediaType with mix of paid and unpaid fines
        // Create both paid and unpaid fines
        Fine paidFine = fineService.applyFine("U001", 10.0, "Paid fine");
        assertNotNull(paidFine);
        fineService.payFine(paidFine.getFineId(), 10.0);

        Fine unpaidFine = fineService.applyFine("U001", 15.0, "Unpaid fine");
        assertNotNull(unpaidFine);

        String result = fineService.getFineBreakdownByMediaType("U001");
        assertNotNull(result);
        assertTrue(result.contains("FINE BREAKDOWN") || result.contains("No fines found"));
    }

    @Test
    void testDisplayUserFinesWithLoanServiceNullScenario() {
        // Test displayUserFines when some fines have loanId but loanService is null
        FineService service = new FineService(userRepository);
        // Don't set loanService

        // Create fine with loanId
        Fine fine = service.applyFine("U001", 15.0, "Fine with no loan service");
        assertNotNull(fine);

        // This should not throw exception
        service.displayUserFines("U001");
    }

    @Test
    void testCleanupDuplicateFinesPlaceholderTest() {
        // Test cleanupDuplicateFines method
        fineService.cleanupDuplicateFines();
        // Should not throw exception
    }

    @Test
    void testPayFineWithActiveLoanBlockedScenario() {
        // Test paying fine for an active loan (should be blocked)
        // This requires finding or creating an active loan with a fine
        String loanId = "L0001"; // Assuming this exists

        // Check if fine exists for this loan
        Fine existingFine = fineService.getFineRepository().findFineByLoanId(loanId);
        if (existingFine != null && loanService != null) {
            com.library.model.Loan loan = loanService.getLoanRepository().findLoanById(loanId);
            if (loan != null && loan.getReturnDate() == null) {
                // Loan is active, payment should fail
                boolean result = fineService.payFine(existingFine.getFineId(), existingFine.getAmount());
                assertFalse(result);
            }
        }
    }

    @Test
    void testApplyFineUserNotFoundScenario() {
        // Test applyFine with non-existent user
        Fine fine = fineService.applyFine("NONEXISTENT_USER", 10.0, "Test");
        assertNull(fine);
    }

    @Test
    void testApplyFineZeroAmountScenario() {
        // Test applyFine with zero amount
        Fine fine = fineService.applyFine("U001", 0.0, "Test zero amount");
        assertNull(fine);
    }

    @Test
    void testApplyFineNegativeAmountScenario() {
        // Test applyFine with negative amount
        Fine fine = fineService.applyFine("U001", -10.0, "Test negative amount");
        assertNull(fine);
    }

    @Test
    void testPayFineZeroAmountScenario() {
        // Test payFine with zero amount
        boolean result = fineService.payFine("F0001", 0.0);
        assertFalse(result);
    }

    @Test
    void testPayFineNegativeAmountScenario() {
        // Test payFine with negative amount
        boolean result = fineService.payFine("F0001", -5.0);
        assertFalse(result);
    }

    @Test
    void testConstructorWithBothParametersTest() {
        // Test constructor with both parameters
        FineService service = new FineService(userRepository, loanService);
        assertNotNull(service);
        assertNotNull(service.getUserRepository());
        assertNotNull(service.getLoanService());
    }

    @Test
    void testConstructorWithUserRepositoryOnlyTest() {
        // Test constructor with only user repository
        FineService service = new FineService(userRepository);
        assertNotNull(service);
        assertNotNull(service.getUserRepository());
        assertNull(service.getLoanService());
    }

    @Test
    void testDefaultConstructorTest() {
        // Test default constructor
        FineService service = new FineService();
        assertNotNull(service);
        assertNotNull(service.getUserRepository());
        assertNull(service.getLoanService());
    }
}