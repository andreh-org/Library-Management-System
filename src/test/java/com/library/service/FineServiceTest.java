package com.library.service;

import com.library.model.Fine;
import com.library.model.Loan;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FineService
 * @author Library Team
 * @version 1.0
 */
class FineServiceTest {
    private FineService fineService;
    private UserRepository userRepository;
    private BookRepository bookRepository;
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        bookRepository = new BookRepository();

        // Create FineService first (without LoanService dependency)
        fineService = new FineService(userRepository);

        // Create LoanService with the FineService
        loanService = new LoanService(fineService, userRepository, bookRepository);

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
    void testCompleteFineAndReturnFlow() {
        // Step 0: Verify initial state - U002 has an overdue book and unpaid fine
        // Verify U002 has unpaid fines
        List<Fine> unpaidFines = fineService.getUserUnpaidFines("U002");
        assertFalse(unpaidFines.isEmpty());
        assertEquals("F0001", unpaidFines.get(0).getFineId());
        assertEquals(25.0, unpaidFines.get(0).getAmount(), 0.001);

        // Verify U002 has overdue loans
        List<Loan> userLoans = loanService.getUserActiveLoans("U002");
        assertFalse(userLoans.isEmpty());
        boolean hasOverdue = userLoans.stream().anyMatch(Loan::isOverdue);
        assertTrue(hasOverdue);

        // Verify U002 cannot borrow initially
        User userInitial = userRepository.findUserById("U002");
        assertFalse(userInitial.canBorrow());

        // Step 1: Try to pay the fine - should FAIL because of overdue book
        boolean paymentAttempt1 = fineService.payFine("F0001", 25.0);
        assertFalse(paymentAttempt1);

        // Step 2: Try to borrow a new book - should FAIL because of overdue book AND unpaid fine
        // Use a different book ISBN that should be available
        Loan borrowAttempt1 = loanService.borrowBook("U002", "978-0141439518", LocalDate.now());
        assertNull(borrowAttempt1);

        // Step 3: Return the overdue book - should SUCCESS (no fine check for returns)
        boolean returnAttempt1 = loanService.returnBook("L0001", LocalDate.now());
        assertTrue(returnAttempt1);

        // Verify loan is returned
        Loan returnedLoan = loanService.getLoanRepository().findLoanById("L0001");
        assertNotNull(returnedLoan.getReturnDate());
        assertEquals(LocalDate.now(), returnedLoan.getReturnDate());

        // Step 4: Now pay the fine - should SUCCESS because overdue book is returned
        boolean paymentAttempt2 = fineService.payFine("F0001", 25.0);
        assertTrue(paymentAttempt2);

        // Verify fine is paid
        Fine paidFine = fineService.getFineRepository().findFineById("F0001");
        assertTrue(paidFine.isPaid());
        assertEquals(0.0, paidFine.getRemainingBalance(), 0.001);

        // Step 5: IMPORTANT - Update user's canBorrow status after fine is paid
        // The FineService.payFine() method should update this, but let's verify
        User userAfterPayment = userRepository.findUserById("U002");
        assertTrue(userAfterPayment.canBorrow());

        // Step 6: Now try to borrow a new book - should SUCCESS because fine is paid and overdue book is returned
        // Use "978-0141439518" which should be available (Pride and Prejudice)
        Loan borrowAttempt2 = loanService.borrowBook("U002", "978-0141439518", LocalDate.now());
        assertNotNull(borrowAttempt2);

        // Verify the new loan details
        assertEquals("U002", borrowAttempt2.getUserId());
        assertEquals("978-0141439518", borrowAttempt2.getBookIsbn());
        assertEquals(LocalDate.now().plusDays(28), borrowAttempt2.getDueDate());

        // Final verification: User should now be able to borrow
        User userFinal = userRepository.findUserById("U002");
        assertTrue(userFinal.canBorrow());
    }
}