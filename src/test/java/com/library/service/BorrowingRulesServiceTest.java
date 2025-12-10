package com.library.service;

import com.library.model.BorrowingRules;
import com.library.model.Fine;
import com.library.model.Loan;
import com.library.model.User;
import com.library.repository.FineRepository;
import com.library.repository.LoanRepository;
import com.library.repository.MediaRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for BorrowingRulesService
 * @author Library Team
 * @version 1.0
 */
class BorrowingRulesServiceTest {
    private BorrowingRulesService borrowingRulesService;
    private LoanRepository mockLoanRepository;
    private FineRepository mockFineRepository;
    private UserRepository mockUserRepository;
    private AuthService mockAuthService;

    @BeforeEach
    void setUp() {
        mockLoanRepository = Mockito.mock(LoanRepository.class);
        mockFineRepository = Mockito.mock(FineRepository.class);
        mockUserRepository = Mockito.mock(UserRepository.class);
        mockAuthService = Mockito.mock(AuthService.class);

        borrowingRulesService = new BorrowingRulesService(
                mockLoanRepository, mockFineRepository, mockUserRepository
        );
    }

    @Test
    void testDefaultConstructor() {
        BorrowingRulesService defaultService = new BorrowingRulesService();
        assertNotNull(defaultService);
        assertNotNull(defaultService.getBorrowingRules());
    }

    @Test
    void testCanUserBorrowUserNotFound() {
        when(mockUserRepository.findUserById("NONEXISTENT")).thenReturn(null);

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("NONEXISTENT");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("User not found"));
    }

    @Test
    void testCanUserBorrowUserNotActive() {
        User inactiveUser = new User("U001", "Inactive User", "inactive@email.com");
        inactiveUser.setActive(false);
        when(mockUserRepository.findUserById("U001")).thenReturn(inactiveUser);

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("User account is not active"));
    }

    @Test
    void testCanUserBorrowMaxBooksReached() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // Create 5 active loans (default max is 5)
        List<Loan> activeLoans = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Loan loan = new Loan("L" + i, "U001", "BOOK-" + i, "BOOK",
                    LocalDate.now(), LocalDate.now().plusDays(28));
            activeLoans.add(loan);
        }

        when(mockLoanRepository.getActiveLoans()).thenReturn(activeLoans);

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("maximum limit"));
        assertTrue(result.getMessage().contains("5 books")); // Default max
    }

    @Test
    void testCanUserBorrowWithOverdueBooks() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // Create active loans, one of which is overdue
        List<Loan> activeLoans = new ArrayList<>();
        Loan overdueLoan = new Loan("L001", "U001", "BOOK-001", "BOOK",
                LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(2));
        overdueLoan.checkOverdue(LocalDate.now());
        activeLoans.add(overdueLoan);

        when(mockLoanRepository.getActiveLoans()).thenReturn(activeLoans);

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("overdue books"));
    }

    @Test
    void testCanUserBorrowWithUnpaidFines() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // No active loans
        when(mockLoanRepository.getActiveLoans()).thenReturn(new ArrayList<>());

        // Has unpaid fines
        List<Fine> unpaidFines = new ArrayList<>();
        Fine fine = new Fine("F001", "U001", 25.0);
        unpaidFines.add(fine);
        when(mockFineRepository.getUnpaidFinesByUser("U001")).thenReturn(unpaidFines);

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("unpaid fines"));
        assertTrue(result.getMessage().contains("$25.00"));
    }

    @Test
    void testCanUserBorrowUserCannotBorrow() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(false); // User cannot borrow
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // No active loans
        when(mockLoanRepository.getActiveLoans()).thenReturn(new ArrayList<>());

        // No unpaid fines
        when(mockFineRepository.getUnpaidFinesByUser("U001")).thenReturn(new ArrayList<>());

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("borrowing restrictions"));
    }

    @Test
    void testCanUserBorrowSuccess() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // Has some active loans but under limit
        List<Loan> activeLoans = new ArrayList<>();
        activeLoans.add(new Loan("L001", "U001", "BOOK-001", "BOOK",
                LocalDate.now(), LocalDate.now().plusDays(28)));
        when(mockLoanRepository.getActiveLoans()).thenReturn(activeLoans);

        // No overdue books
        // No unpaid fines
        when(mockFineRepository.getUnpaidFinesByUser("U001")).thenReturn(new ArrayList<>());

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertTrue(result.isValid());
        assertTrue(result.getMessage().contains("User can borrow books"));
    }

    @Test
    void testCanUserBorrowWithOverdueRestrictionDisabled() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // Disable overdue restriction
        BorrowingRules rules = borrowingRulesService.getBorrowingRules();
        rules.setRestrictBorrowingForOverdue(false);

        // Create overdue loan
        List<Loan> activeLoans = new ArrayList<>();
        Loan overdueLoan = new Loan("L001", "U001", "BOOK-001", "BOOK",
                LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(2));
        overdueLoan.checkOverdue(LocalDate.now());
        activeLoans.add(overdueLoan);
        when(mockLoanRepository.getActiveLoans()).thenReturn(activeLoans);

        // No unpaid fines
        when(mockFineRepository.getUnpaidFinesByUser("U001")).thenReturn(new ArrayList<>());

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        // Should succeed even with overdue book because restriction is disabled
        assertTrue(result.isValid());
    }

    @Test
    void testCanUserBorrowWithFineRestrictionDisabled() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // Disable fine restriction
        BorrowingRules rules = borrowingRulesService.getBorrowingRules();
        rules.setRestrictBorrowingForUnpaidFines(false);

        // No active loans
        when(mockLoanRepository.getActiveLoans()).thenReturn(new ArrayList<>());

        // Has unpaid fines but restriction is disabled
        List<Fine> unpaidFines = new ArrayList<>();
        unpaidFines.add(new Fine("F001", "U001", 50.0));
        when(mockFineRepository.getUnpaidFinesByUser("U001")).thenReturn(unpaidFines);

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        // Should succeed even with unpaid fines because restriction is disabled
        assertTrue(result.isValid());
    }

    @Test
    void testGetBorrowingRules() {
        BorrowingRules rules = borrowingRulesService.getBorrowingRules();
        assertNotNull(rules);

        // Check default values
        assertEquals(5, rules.getMaxBooksPerUser());
        assertEquals(28, rules.getLoanPeriodDays());
        assertTrue(rules.isRestrictBorrowingForOverdue());
        assertTrue(rules.isRestrictBorrowingForUnpaidFines());
    }

    @Test
    void testUpdateBorrowingRulesWithoutAdmin() {
        when(mockAuthService.isLoggedIn()).thenReturn(false);

        BorrowingRules newRules = new BorrowingRules();
        newRules.setMaxBooksPerUser(10);

        boolean result = borrowingRulesService.updateBorrowingRules(newRules, mockAuthService);

        assertFalse(result);
        verify(mockAuthService, times(1)).isLoggedIn();
    }

    @Test
    void testUpdateBorrowingRulesWithAdmin() {
        when(mockAuthService.isLoggedIn()).thenReturn(true);

        BorrowingRules newRules = new BorrowingRules();
        newRules.setMaxBooksPerUser(10);
        newRules.setLoanPeriodDays(14);
        newRules.setRestrictBorrowingForOverdue(false);
        newRules.setRestrictBorrowingForUnpaidFines(false);

        boolean result = borrowingRulesService.updateBorrowingRules(newRules, mockAuthService);

        assertTrue(result);
        verify(mockAuthService, times(1)).isLoggedIn();

        // Verify rules were updated
        BorrowingRules updatedRules = borrowingRulesService.getBorrowingRules();
        assertEquals(10, updatedRules.getMaxBooksPerUser());
        assertEquals(14, updatedRules.getLoanPeriodDays());
        assertFalse(updatedRules.isRestrictBorrowingForOverdue());
        assertFalse(updatedRules.isRestrictBorrowingForUnpaidFines());
    }

    @Test
    void testDisplayBorrowingRules() {
        // This test just ensures the method doesn't throw exceptions
        assertDoesNotThrow(() -> borrowingRulesService.displayBorrowingRules());

        // We can't easily test System.out.println, but we can verify the method runs
        // For coverage purposes, we just need to call it
        borrowingRulesService.displayBorrowingRules();
    }

    @Test
    void testValidationResultCreation() {
        BorrowingRulesService.ValidationResult result =
                new BorrowingRulesService.ValidationResult();

        assertFalse(result.isValid());
        assertEquals("", result.getMessage());

        result.setValid(true);
        result.setMessage("Test message");

        assertTrue(result.isValid());
        assertEquals("Test message", result.getMessage());
    }

    @Test
    void testValidationResultConstructorWithParams() {
        BorrowingRulesService.ValidationResult result =
                new BorrowingRulesService.ValidationResult(true, "Success");

        assertTrue(result.isValid());
        assertEquals("Success", result.getMessage());

        BorrowingRulesService.ValidationResult result2 =
                new BorrowingRulesService.ValidationResult(false, "Failure");

        assertFalse(result2.isValid());
        assertEquals("Failure", result2.getMessage());
    }

    @Test
    void testValidationResultToString() {
        BorrowingRulesService.ValidationResult result =
                new BorrowingRulesService.ValidationResult(true, "Test");

        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("Valid: Yes"));
        assertTrue(str.contains("Test"));

        result.setValid(false);
        str = result.toString();
        assertTrue(str.contains("Valid: No"));
    }

    @Test
    void testCanUserBorrowWithEmptyActiveLoans() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // Empty active loans list
        when(mockLoanRepository.getActiveLoans()).thenReturn(new ArrayList<>());

        // No unpaid fines
        when(mockFineRepository.getUnpaidFinesByUser("U001")).thenReturn(new ArrayList<>());

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertTrue(result.isValid());
    }

    @Test
    void testCanUserBorrowWithZeroUnpaidFines() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // Some active loans
        List<Loan> activeLoans = new ArrayList<>();
        activeLoans.add(new Loan("L001", "U001", "BOOK-001", "BOOK",
                LocalDate.now(), LocalDate.now().plusDays(28)));
        when(mockLoanRepository.getActiveLoans()).thenReturn(activeLoans);

        // Empty unpaid fines list
        when(mockFineRepository.getUnpaidFinesByUser("U001")).thenReturn(new ArrayList<>());

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertTrue(result.isValid());
    }

    @Test
    void testCanUserBorrowWithMultipleUnpaidFines() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // No active loans
        when(mockLoanRepository.getActiveLoans()).thenReturn(new ArrayList<>());

        // Multiple unpaid fines
        List<Fine> unpaidFines = new ArrayList<>();
        unpaidFines.add(new Fine("F001", "U001", 15.0));
        unpaidFines.add(new Fine("F002", "U001", 25.0));
        unpaidFines.add(new Fine("F003", "U001", 10.0));
        when(mockFineRepository.getUnpaidFinesByUser("U001")).thenReturn(unpaidFines);

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("$50.00")); // 15 + 25 + 10 = 50
    }

    @Test
    void testCanUserBorrowWithPartiallyPaidFines() {
        User user = new User("U001", "Test User", "test@email.com");
        user.setActive(true);
        user.setCanBorrow(true);
        when(mockUserRepository.findUserById("U001")).thenReturn(user);

        // No active loans
        when(mockLoanRepository.getActiveLoans()).thenReturn(new ArrayList<>());

        // Fine with partial payment
        List<Fine> unpaidFines = new ArrayList<>();
        Fine fine = new Fine("F001", "U001", 30.0);
        fine.makePayment(10.0); // Pay $10, still owes $20
        unpaidFines.add(fine);
        when(mockFineRepository.getUnpaidFinesByUser("U001")).thenReturn(unpaidFines);

        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow("U001");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("$20.00")); // Remaining balance
    }

    @Test
    void testCanUserBorrowEdgeCases() {
        // Test with null user ID
        BorrowingRulesService.ValidationResult result =
                borrowingRulesService.canUserBorrow(null);
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("User not found"));

        // Test with empty user ID
        result = borrowingRulesService.canUserBorrow("");
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("User not found"));
    }

    @Test
    void testUpdateBorrowingRulesWithNullRules() {
        when(mockAuthService.isLoggedIn()).thenReturn(true);

        // Should handle null gracefully
        boolean result = borrowingRulesService.updateBorrowingRules(null, mockAuthService);
        assertTrue(result); // Current implementation doesn't check for null

        // Rules should be set to null (or handle gracefully)
        // This depends on your implementation
    }

    @Test
    void testIntegrationWithRealRepositories() {
        // Create fresh repositories
        MediaRepository mediaRepo = new MediaRepository();
        LoanRepository realLoanRepo = new LoanRepository(mediaRepo);
        FineRepository realFineRepo = new FineRepository();
        UserRepository realUserRepo = new UserRepository();

        // Reset user U005 to clean state
        User userU005 = realUserRepo.findUserById("U005");
        if (userU005 != null) {
            userU005.setActive(true);
            userU005.setCanBorrow(true);
            realUserRepo.updateUser(userU005);
        }

        BorrowingRulesService realService = new BorrowingRulesService(
                realLoanRepo, realFineRepo, realUserRepo
        );

        BorrowingRulesService.ValidationResult result =
                realService.canUserBorrow("U005");

        // Debug output if test fails
        if (!result.isValid()) {
            System.out.println("U005 validation failed: " + result.getMessage());
            System.out.println("User active: " + userU005.isActive());
            System.out.println("User canBorrow: " + userU005.canBorrow());
        }

        assertTrue(result.isValid(),
                "U005 should be able to borrow after reset: " + result.getMessage());
    }

    @Test
    void testBorrowingRulesSettersAndGetters() {
        BorrowingRules rules = new BorrowingRules();

        // Test all getters with default values
        assertEquals(5, rules.getMaxBooksPerUser());
        assertEquals(28, rules.getLoanPeriodDays());
        assertTrue(rules.isRestrictBorrowingForOverdue());
        assertTrue(rules.isRestrictBorrowingForUnpaidFines());

        // Test setters
        rules.setMaxBooksPerUser(10);
        rules.setLoanPeriodDays(14);
        rules.setRestrictBorrowingForOverdue(false);
        rules.setRestrictBorrowingForUnpaidFines(false);

        // Verify changes
        assertEquals(10, rules.getMaxBooksPerUser());
        assertEquals(14, rules.getLoanPeriodDays());
        assertFalse(rules.isRestrictBorrowingForOverdue());
        assertFalse(rules.isRestrictBorrowingForUnpaidFines());

        // Test toString
        String str = rules.toString();
        assertNotNull(str);
        assertTrue(str.contains("Max Books Per User: 10"));
        assertTrue(str.contains("Loan Period: 14 days"));
    }
}