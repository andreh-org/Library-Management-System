package com.library.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for User model
 * @author Library Team
 * @version 1.1
 */
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("U001", "John Doe", "john.doe@email.com");
    }

    @Test
    void testUserCreation() {
        assertEquals("U001", user.getUserId());
        assertEquals("John Doe", user.getName());
        assertEquals("john.doe@email.com", user.getEmail());
        assertTrue(user.isActive());
        assertEquals(0.0, user.getUnpaidFines());
        assertTrue(user.canBorrow());
    }

    @Test
    void testUserSetters() {
        user.setUserId("U999");
        user.setName("Updated Name");
        user.setEmail("updated@email.com");
        user.setActive(false);
        user.setUnpaidFines(50.0);

        assertEquals("U999", user.getUserId());
        assertEquals("Updated Name", user.getName());
        assertEquals("updated@email.com", user.getEmail());
        assertFalse(user.isActive());
        assertEquals(50.0, user.getUnpaidFines());
    }

    @Test
    void testCanBorrowWhenInactive() {
        user.setActive(false);
        assertFalse(user.canBorrow());
    }

    @Test
    void testCanBorrowWithUnpaidFines() {
        user.addFine(10.0);
        assertFalse(user.canBorrow());
    }

    @Test
    void testCanBorrowWhenSetExplicitlyFalse() {
        user.setCanBorrow(false);
        assertFalse(user.canBorrow());
    }

    @Test
    void testAddFinePositiveAmount() {
        user.addFine(15.5);
        assertEquals(15.5, user.getUnpaidFines());
        assertFalse(user.canBorrow()); // Should automatically disable borrowing
    }

    @Test
    void testAddFineZeroOrNegative() {
        user.addFine(0);
        assertEquals(0, user.getUnpaidFines());

        user.addFine(-10);
        assertEquals(0, user.getUnpaidFines());
    }

    @Test
    void testPayFineFullPayment() {
        user.addFine(25.0);
        double remaining = user.payFine(25.0);

        assertEquals(0, remaining);
        assertEquals(0, user.getUnpaidFines());
        assertTrue(user.canBorrow()); // Should restore borrowing permission
    }

    @Test
    void testPayFinePartialPayment() {
        user.addFine(30.0);
        double remaining = user.payFine(15.0);

        assertEquals(15.0, remaining);
        assertEquals(15.0, user.getUnpaidFines());
        assertFalse(user.canBorrow()); // Still has unpaid fines
    }

    @Test
    void testPayFineZeroOrNegative() {
        user.addFine(20.0);
        double remaining = user.payFine(0);

        assertEquals(20.0, remaining);
        assertEquals(20.0, user.getUnpaidFines());
    }

    @Test
    void testPayFineOverpayment() {
        user.addFine(10.0);
        double remaining = user.payFine(15.0);

        assertEquals(0, remaining);
        assertEquals(0, user.getUnpaidFines());
        assertTrue(user.canBorrow());
    }

    @Test
    void testAddAndRemoveLoan() {
        String loanId = "L001";
        assertFalse(user.hasCurrentLoans());

        user.addLoan(loanId);
        assertTrue(user.hasCurrentLoans());
        assertEquals(1, user.getCurrentLoans().size());

        user.removeLoan(loanId);
        assertFalse(user.hasCurrentLoans());
    }

    @Test
    void testAddDuplicateLoan() {
        String loanId = "L001";
        user.addLoan(loanId);
        user.addLoan(loanId); // Should not add duplicate

        assertEquals(1, user.getCurrentLoans().size());
    }

    @Test
    void testCanBeUnregistered() {
        // User with no loans and no fines can be unregistered
        assertTrue(user.canBeUnregistered());

        // User with active loans cannot be unregistered
        user.addLoan("L001");
        assertFalse(user.canBeUnregistered());

        // Remove loan but add fine
        user.removeLoan("L001");
        user.addFine(10.0);
        assertFalse(user.canBeUnregistered());

        // User with neither loans nor fines can be unregistered
        user.payFine(10.0);
        assertTrue(user.canBeUnregistered());
    }

    @ParameterizedTest
    @ValueSource(strings = {"U001", "John Doe", "john.doe@email.com"})
    void testToStringContainsInfo(String expectedContent) {
        String result = user.toString();
        assertTrue(result.contains(expectedContent));
    }

    @Test
    void testToStringFormat() {
        String result = user.toString();
        assertTrue(result.contains("Active: Yes"));
        assertTrue(result.contains("Can Borrow: Yes"));
        assertTrue(result.contains("Unpaid Fines: 0.00"));
    }

    @Test
    void testUserWithEmptyFields() {
        User emptyUser = new User("", "", "");
        assertEquals("", emptyUser.getUserId());
        assertEquals("", emptyUser.getName());
        assertEquals("", emptyUser.getEmail());
    }

    @Test
    void testHasUnpaidFines() {
        assertFalse(user.hasUnpaidFines());

        user.addFine(0.01);
        assertTrue(user.hasUnpaidFines());

        user.payFine(0.01);
        assertFalse(user.hasUnpaidFines());
    }
}