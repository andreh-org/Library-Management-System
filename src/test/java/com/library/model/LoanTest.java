package com.library.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Loan model
 * @author Library Team
 * @version 1.0
 */
class LoanTest {

    @Test
    void testLoanCreation() {
        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plusDays(28);
        Loan loan = new Loan("L0001", "U001", "978-0743273565", "BOOK", borrowDate, dueDate);

        assertEquals("L0001", loan.getLoanId());
        assertEquals("U001", loan.getUserId());
        assertEquals("978-0743273565", loan.getMediaId());
        assertEquals("BOOK", loan.getMediaType());
        assertEquals(borrowDate, loan.getBorrowDate());
        assertEquals(dueDate, loan.getDueDate());
        assertNull(loan.getReturnDate());
        assertFalse(loan.isOverdue());
    }

    @Test
    void testLoanNotOverdueWhenDueToday() {
        LocalDate borrowDate = LocalDate.now().minusDays(28);
        LocalDate dueDate = LocalDate.now();
        Loan loan = new Loan("L0001", "U001", "978-0743273565", "BOOK", borrowDate, dueDate);

        assertFalse(loan.checkOverdue(LocalDate.now()));
        assertFalse(loan.isOverdue());
    }

    @Test
    void testLoanOverdueAfterDueDate() {
        LocalDate borrowDate = LocalDate.now().minusDays(35);
        LocalDate dueDate = borrowDate.plusDays(28);
        Loan loan = new Loan("L0001", "U001", "978-0743273565", "BOOK", borrowDate, dueDate);

        assertTrue(loan.checkOverdue(LocalDate.now()));
        assertTrue(loan.isOverdue());
    }

    @Test
    void testLoanNotOverdueWhenReturned() {
        LocalDate borrowDate = LocalDate.now().minusDays(35);
        LocalDate dueDate = borrowDate.plusDays(28);
        LocalDate returnDate = LocalDate.now().minusDays(30);
        Loan loan = new Loan("L0001", "U001", "978-0743273565", "BOOK", borrowDate, dueDate);
        loan.setReturnDate(returnDate);

        assertFalse(loan.checkOverdue(LocalDate.now()));
        assertFalse(loan.isOverdue());
    }

    @Test
    void testLoanToString() {
        LocalDate borrowDate = LocalDate.of(2025, 10, 1);
        LocalDate dueDate = borrowDate.plusDays(28);
        Loan loan = new Loan("L0001", "U001", "978-0743273565", "BOOK", borrowDate, dueDate);

        String toStringResult = loan.toString();
        assertTrue(toStringResult.contains("L0001"));
        assertTrue(toStringResult.contains("U001"));
        assertTrue(toStringResult.contains("978-0743273565"));
        assertTrue(toStringResult.contains("BOOK"));
        assertTrue(toStringResult.contains("2025-10-29")); // due date
    }

    @Test
    void testCalculateFineForOverdueLoan() {
        LocalDate borrowDate = LocalDate.now().minusDays(35);
        LocalDate dueDate = borrowDate.plusDays(28);
        Loan loan = new Loan("L0001", "U001", "978-0743273565", "BOOK", borrowDate, dueDate);
        loan.setOverdue(true);

        // Loan is overdue - should return flat fine of $10 for books
        double fine = loan.calculateFine(LocalDate.now());
        assertEquals(10.00, fine, 0.001); // $10 flat fine for books
    }

    @Test
    void testCalculateFineForOverdueCD() {
        LocalDate borrowDate = LocalDate.now().minusDays(10);
        LocalDate dueDate = borrowDate.plusDays(7);
        Loan loan = new Loan("L0002", "U001", "CD-001", "CD", borrowDate, dueDate);
        loan.setOverdue(true);

        // CD is overdue - should return flat fine of $20 for CDs
        double fine = loan.calculateFine(LocalDate.now());
        assertEquals(20.00, fine, 0.001); // $20 flat fine for CDs
    }

    @Test
    void testCalculateFineForReturnedLoan() {
        LocalDate borrowDate = LocalDate.now().minusDays(35);
        LocalDate dueDate = borrowDate.plusDays(28);
        LocalDate returnDate = LocalDate.now().minusDays(5);
        Loan loan = new Loan("L0001", "U001", "978-0743273565", "BOOK", borrowDate, dueDate);
        loan.setReturnDate(returnDate);

        // Should be 0 because loan is returned
        double fine = loan.calculateFine(LocalDate.now());
        assertEquals(0.0, fine, 0.001);
    }

    @Test
    void testCalculateFineForNotOverdueLoan() {
        LocalDate borrowDate = LocalDate.now().minusDays(20);
        LocalDate dueDate = borrowDate.plusDays(28);
        Loan loan = new Loan("L0001", "U001", "978-0743273565", "BOOK", borrowDate, dueDate);

        // Loan is not overdue yet
        double fine = loan.calculateFine(LocalDate.now());
        assertEquals(0.0, fine, 0.001);
    }

    @Test
    void testCDLoanCreation() {
        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plusDays(7);
        Loan loan = new Loan("L0002", "U001", "CD-001", "CD", borrowDate, dueDate);

        assertEquals("L0002", loan.getLoanId());
        assertEquals("U001", loan.getUserId());
        assertEquals("CD-001", loan.getMediaId());
        assertEquals("CD", loan.getMediaType());
        assertEquals(borrowDate, loan.getBorrowDate());
        assertEquals(dueDate, loan.getDueDate());

        assertNull(loan.getReturnDate());
        assertFalse(loan.isOverdue());
    }
}