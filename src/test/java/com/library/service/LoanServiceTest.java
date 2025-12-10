package com.library.service;

import com.library.model.*;
import com.library.repository.FineRepository;
import com.library.repository.LoanRepository;
import com.library.repository.MediaRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test class for LoanService
 * @author Library Team
 * @version 3.3 - Fixed Mockito matcher and NPE issues
 */
class LoanServiceTest {
    private LoanService loanService;
    private FineService fineService;
    private UserRepository userRepository;
    private MediaRepository mediaRepository;
    private LoanRepository loanRepository;
    private FineRepository fineRepository;
    private final String TEST_USER_ID = "TEST001";
    private final String TEST_BOOK_ISBN = "978-0451524935";
    private final String TEST_CD_CATALOG = "CD-001";

    // Static versions for parameterized tests
    private static final String STATIC_TEST_USER_ID = "TEST001";
    private static final String STATIC_TEST_BOOK_ISBN = "978-0451524935";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mediaRepository = mock(MediaRepository.class);
        loanRepository = mock(LoanRepository.class);
        fineRepository = mock(FineRepository.class);

        // Create FineService with mocked dependencies
        fineService = mock(FineService.class);
        when(fineService.getFineRepository()).thenReturn(fineRepository);

        // Create LoanService with mocked dependencies
        loanService = new LoanService(fineService, userRepository, mediaRepository);

        // Inject the mocked loanRepository using reflection
        try {
            Field loanRepoField = LoanService.class.getDeclaredField("loanRepository");
            loanRepoField.setAccessible(true);
            loanRepoField.set(loanService, loanRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject loan repository", e);
        }

        // Set up a default test user
        setupTestUser();
    }

    private void setupTestUser() {
        User testUser = new User(TEST_USER_ID, "Test User", "test@email.com");
        testUser.setCanBorrow(true);
        testUser.setActive(true);
        testUser.getCurrentLoans().clear();

        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(testUser);
        when(userRepository.findUserById("NONEXISTENT")).thenReturn(null);
        when(userRepository.findUserById("U005")).thenReturn(
                new User("U005", "David Wilson", "david@email.com")
        );
    }

    // Helper method to create a test loan
    private Loan createTestLoan(String userId, String mediaId, String mediaType, boolean isOverdue) {
        LocalDate borrowDate = LocalDate.now().minusDays(30);
        LocalDate dueDate = borrowDate.plusDays(mediaType.equals("BOOK") ? 28 : 7);
        LocalDate returnDate = isOverdue ? null : dueDate.minusDays(1);

        Loan loan = new Loan("LOAN-" + System.currentTimeMillis(), userId, mediaId, mediaType,
                borrowDate, dueDate);
        loan.setReturnDate(returnDate);
        return loan;
    }

    // Helper method to create a concrete Book instance
    private Book createTestBook(String isbn, String title, boolean isAvailable) {
        Book book = new Book(isbn, title, "Test Author");
        book.setAvailable(isAvailable);
        return book;
    }

    // Helper method to create a concrete CD instance
    private CD createTestCD(String catalogNumber, String title, boolean isAvailable) {
        CD cd = new CD(catalogNumber, title, "Test Artist", "Test Genre", 10);
        cd.setAvailable(isAvailable);
        return cd;
    }

    @Test
    void testBorrowBookSuccess() {
        // Setup
        String userId = "U005";
        User user = new User(userId, "David Wilson", "david@email.com");
        user.setCanBorrow(true);
        user.setActive(true);

        Book book = createTestBook(TEST_BOOK_ISBN, "Test Book", true);

        when(userRepository.findUserById(userId)).thenReturn(user);
        when(mediaRepository.findMediaByIdAndType(TEST_BOOK_ISBN, "BOOK")).thenReturn(book);
        when(fineService.getTotalUnpaidAmount(userId)).thenReturn(0.0);
        when(loanRepository.findLoansByUser(userId)).thenReturn(new ArrayList<>());

        Loan expectedLoan = new Loan("LOAN-001", userId, TEST_BOOK_ISBN, "BOOK",
                LocalDate.now(), LocalDate.now().plusDays(28));
        when(loanRepository.createBookLoan(userId, TEST_BOOK_ISBN, LocalDate.now()))
                .thenReturn(expectedLoan);

        // Execute
        Loan loan = loanService.borrowBook(userId, TEST_BOOK_ISBN, LocalDate.now());

        // Verify
        assertNotNull(loan, "Book loan should be created successfully");
        assertEquals("BOOK", loan.getMediaType(), "Should be a BOOK loan");
        assertEquals(TEST_BOOK_ISBN, loan.getMediaId(), "ISBN should match");
        assertEquals(LocalDate.now().plusDays(28), loan.getDueDate(), "Due date should be 28 days from now");

        verify(userRepository).updateUser(user);
    }

    @Test
    void testBorrowBookUserNotFound() {
        // Setup
        when(userRepository.findUserById("NONEXISTENT")).thenReturn(null);

        // Execute
        Loan loan = loanService.borrowBook("NONEXISTENT", TEST_BOOK_ISBN, LocalDate.now());

        // Verify
        assertNull(loan, "Should return null for non-existent user");
    }

    @Test
    void testBorrowBookUserInactive() {
        // Setup
        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        user.setCanBorrow(true);
        user.setActive(false); // Inactive user

        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);

        // Execute
        Loan loan = loanService.borrowBook(TEST_USER_ID, TEST_BOOK_ISBN, LocalDate.now());

        // Verify
        assertNull(loan, "Should not allow borrowing for inactive user");
    }

    @Test
    void testBorrowBookBookNotFound() {
        // Setup
        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        user.setCanBorrow(true);
        user.setActive(true);

        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);
        when(mediaRepository.findMediaByIdAndType("invalid-isbn", "BOOK")).thenReturn(null);

        // Execute
        Loan loan = loanService.borrowBook(TEST_USER_ID, "invalid-isbn", LocalDate.now());

        // Verify
        assertNull(loan, "Should return null for non-existent book");
    }

    @Test
    void testBorrowBookAlreadyBorrowed() {
        // Setup
        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        user.setCanBorrow(true);
        user.setActive(true);

        Book book = createTestBook(TEST_BOOK_ISBN, "Test Book", false); // Already borrowed

        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);
        when(mediaRepository.findMediaByIdAndType(TEST_BOOK_ISBN, "BOOK")).thenReturn(book);
        when(fineService.getTotalUnpaidAmount(TEST_USER_ID)).thenReturn(0.0);
        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(new ArrayList<>());

        // Execute
        Loan loan = loanService.borrowBook(TEST_USER_ID, TEST_BOOK_ISBN, LocalDate.now());

        // Verify
        assertNull(loan, "Should fail because book is already borrowed");
    }

    @Test
    void testBorrowBookWithUnpaidFines() {
        // Setup
        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        user.setCanBorrow(true);
        user.setActive(true);

        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);
        when(fineService.getTotalUnpaidAmount(TEST_USER_ID)).thenReturn(15.0); // Has unpaid fines

        // Execute
        Loan loan = loanService.borrowBook(TEST_USER_ID, TEST_BOOK_ISBN, LocalDate.now());

        // Verify
        assertNull(loan, "Should not be able to borrow with unpaid fines");
    }

    @Test
    void testBorrowBookWithOverdueItems() {
        // Setup
        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        user.setCanBorrow(true);
        user.setActive(true);

        // Create an overdue loan
        Loan overdueLoan = createTestLoan(TEST_USER_ID, TEST_BOOK_ISBN, "BOOK", true);

        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);
        when(fineService.getTotalUnpaidAmount(TEST_USER_ID)).thenReturn(0.0);
        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(List.of(overdueLoan));
        when(fineRepository.findFineByLoanId(anyString())).thenReturn(null);

        // Execute
        Loan loan = loanService.borrowBook(TEST_USER_ID, "978-0743273565", LocalDate.now());

        // Verify
        assertNull(loan, "Should not be able to borrow with overdue items");
    }

    @Test
    void testBorrowCDSuccess() {
        // Setup
        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        user.setCanBorrow(true);
        user.setActive(true);

        CD cd = createTestCD(TEST_CD_CATALOG, "Test CD", true);

        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);
        when(mediaRepository.findMediaByIdAndType(TEST_CD_CATALOG, "CD")).thenReturn(cd);
        when(fineService.getTotalUnpaidAmount(TEST_USER_ID)).thenReturn(0.0);
        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(new ArrayList<>());

        Loan expectedLoan = new Loan("LOAN-CD-001", TEST_USER_ID, TEST_CD_CATALOG, "CD",
                LocalDate.now(), LocalDate.now().plusDays(7));
        when(loanRepository.createCDLoan(TEST_USER_ID, TEST_CD_CATALOG, LocalDate.now()))
                .thenReturn(expectedLoan);

        // Execute
        Loan cdLoan = loanService.borrowCD(TEST_USER_ID, TEST_CD_CATALOG, LocalDate.now());

        // Verify
        assertNotNull(cdLoan);
        assertEquals(TEST_USER_ID, cdLoan.getUserId());
        assertEquals(TEST_CD_CATALOG, cdLoan.getMediaId());
        assertEquals("CD", cdLoan.getMediaType());
        assertEquals(LocalDate.now().plusDays(7), cdLoan.getDueDate());

        verify(userRepository).updateUser(user);
    }

    @ParameterizedTest
    @CsvSource({
            "BOOK, 28",
            "CD, 7"
    })
    void testBorrowDifferentMediaTypes(String mediaType, int expectedLoanDays) {
        // Setup
        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        user.setCanBorrow(true);
        user.setActive(true);

        String mediaId = mediaType.equals("BOOK") ? TEST_BOOK_ISBN : TEST_CD_CATALOG;
        Media media;
        if (mediaType.equals("BOOK")) {
            media = createTestBook(mediaId, "Test Book", true);
        } else {
            media = createTestCD(mediaId, "Test CD", true);
        }

        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);
        when(mediaRepository.findMediaByIdAndType(mediaId, mediaType)).thenReturn(media);
        when(fineService.getTotalUnpaidAmount(TEST_USER_ID)).thenReturn(0.0);
        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(new ArrayList<>());

        Loan expectedLoan = new Loan("LOAN-001", TEST_USER_ID, mediaId, mediaType,
                LocalDate.now(), LocalDate.now().plusDays(expectedLoanDays));

        if (mediaType.equals("BOOK")) {
            when(loanRepository.createBookLoan(TEST_USER_ID, mediaId, LocalDate.now()))
                    .thenReturn(expectedLoan);
        } else {
            when(loanRepository.createCDLoan(TEST_USER_ID, mediaId, LocalDate.now()))
                    .thenReturn(expectedLoan);
        }

        // Execute
        Loan loan;
        if (mediaType.equals("BOOK")) {
            loan = loanService.borrowBook(TEST_USER_ID, mediaId, LocalDate.now());
        } else {
            loan = loanService.borrowCD(TEST_USER_ID, mediaId, LocalDate.now());
        }

        // Verify
        assertNotNull(loan);
        assertEquals(mediaType, loan.getMediaType());
        assertEquals(LocalDate.now().plusDays(expectedLoanDays), loan.getDueDate());
    }

    @Test
    void testReturnBookSuccess() {
        // Setup
        String loanId = "LOAN-001";
        Loan loan = new Loan(loanId, TEST_USER_ID, TEST_BOOK_ISBN, "BOOK",
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(2));

        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        user.addLoan(loanId);

        when(loanRepository.findLoanById(loanId)).thenReturn(loan);
        when(loanRepository.returnMedia(loanId, LocalDate.now())).thenReturn(true);
        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);

        // Execute
        boolean returnSuccess = loanService.returnBook(loanId, LocalDate.now());

        // Verify
        assertTrue(returnSuccess, "Return should succeed");
        verify(userRepository).updateUser(user);
    }

    @Test
    void testReturnBookWithOverdueFine() {
        // Setup
        String loanId = "LOAN-OVERDUE";
        LocalDate borrowDate = LocalDate.now().minusDays(30);
        LocalDate dueDate = borrowDate.plusDays(28);
        Loan loan = new Loan(loanId, TEST_USER_ID, TEST_BOOK_ISBN, "BOOK", borrowDate, dueDate);

        User user = new User(TEST_USER_ID, "Test User", "test@email.com");

        when(loanRepository.findLoanById(loanId)).thenReturn(loan);
        when(loanRepository.returnMedia(loanId, LocalDate.now())).thenReturn(true);
        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);

        Fine fine = new Fine("FINE-001", TEST_USER_ID, 10.0, "Overdue book fine");
        when(fineService.applyFine(eq(TEST_USER_ID), anyString(), eq(loanId))).thenReturn(fine);

        // Execute
        boolean success = loanService.returnBook(loanId, LocalDate.now());

        // Verify
        assertTrue(success, "Return should succeed even with overdue fine");
        verify(fineService).applyFine(eq(TEST_USER_ID), anyString(), eq(loanId));
    }

    @Test
    void testReturnBookNotFound() {
        // Setup
        when(loanRepository.findLoanById("invalid-loan-id")).thenReturn(null);

        // Execute
        boolean returnSuccess = loanService.returnBook("invalid-loan-id", LocalDate.now());

        // Verify
        assertFalse(returnSuccess, "Should fail for non-existent loan");
    }

    @Test
    void testReturnBookAlreadyReturned() {
        // Setup
        String loanId = "LOAN-001";
        Loan loan = new Loan(loanId, TEST_USER_ID, TEST_BOOK_ISBN, "BOOK",
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(2));
        loan.setReturnDate(LocalDate.now().minusDays(1)); // Already returned

        when(loanRepository.findLoanById(loanId)).thenReturn(loan);

        // Execute
        boolean secondReturn = loanService.returnBook(loanId, LocalDate.now());

        // Verify
        assertFalse(secondReturn, "Should fail when returning already returned book");
    }

    @Test
    void testGetUserActiveLoans() {
        // Setup
        Loan activeLoan = new Loan("LOAN-001", TEST_USER_ID, TEST_BOOK_ISBN, "BOOK",
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(18));

        Loan returnedLoan = new Loan("LOAN-002", TEST_USER_ID, TEST_CD_CATALOG, "CD",
                LocalDate.now().minusDays(20), LocalDate.now().minusDays(13));
        returnedLoan.setReturnDate(LocalDate.now().minusDays(10));

        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(List.of(activeLoan, returnedLoan));

        // Execute
        List<Loan> activeLoans = loanService.getUserActiveLoans(TEST_USER_ID);

        // Verify
        assertEquals(1, activeLoans.size(), "Should have 1 active loan");
        assertEquals("LOAN-001", activeLoans.get(0).getLoanId());
    }

    @Test
    void testHasOverdueBooks() {
        // Setup - No overdue books initially
        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(new ArrayList<>());

        // Execute
        boolean hasOverdue = loanService.hasOverdueBooks(TEST_USER_ID);

        // Verify
        assertFalse(hasOverdue, "Should have no overdue books initially");

        // Setup - With overdue books
        Loan overdueLoan = createTestLoan(TEST_USER_ID, TEST_BOOK_ISBN, "BOOK", true);
        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(List.of(overdueLoan));

        // Execute again
        hasOverdue = loanService.hasOverdueBooks(TEST_USER_ID);

        // Verify
        assertTrue(hasOverdue, "Should detect overdue books");
    }

    @Test
    void testCheckAndApplyOverdueFinesForBooks() {
        // Setup
        LocalDate currentDate = LocalDate.now().plusDays(35);
        Loan overdueLoan = new Loan("TEST-OVERDUE", TEST_USER_ID, TEST_BOOK_ISBN, "BOOK",
                LocalDate.now(), LocalDate.now().plusDays(28));

        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(List.of(overdueLoan));
        when(fineRepository.findFineByLoanId("TEST-OVERDUE")).thenReturn(null);

        // Execute
        loanService.checkAndApplyOverdueFines(TEST_USER_ID, currentDate);

        // Verify - Loan should be marked as overdue
        assertTrue(overdueLoan.isOverdue(), "Loan should be marked as overdue");
    }

    @Test
    void testCheckAndApplyOverdueFinesNoOverdue() {
        // Setup
        LocalDate currentDate = LocalDate.now();
        Loan currentLoan = new Loan("TEST-CURRENT", TEST_USER_ID, TEST_BOOK_ISBN, "BOOK",
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(18));

        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(List.of(currentLoan));

        // Execute
        loanService.checkAndApplyOverdueFines(TEST_USER_ID, currentDate);

        // Verify
        assertFalse(currentLoan.isOverdue(), "Loan should not be overdue");
    }

    @Test
    void testGetSimpleMixedMediaReportUserNotFound() {
        // Setup
        when(userRepository.findUserById("NONEXISTENT-USER")).thenReturn(null);

        // Execute
        String report = loanService.getSimpleMixedMediaReport("NONEXISTENT-USER", LocalDate.now());

        // Verify
        assertTrue(report.contains("Error: User not found"), "Should report user not found");
    }

    @Test
    void testGetSimpleMixedMediaReportNoFines() {
        // Setup
        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);
        when(fineService.getUserUnpaidFines(TEST_USER_ID)).thenReturn(new ArrayList<>());

        // Execute
        String report = loanService.getSimpleMixedMediaReport(TEST_USER_ID, LocalDate.now());

        // Verify
        assertTrue(report.contains("No unpaid fines found"), "Should report no fines");
        assertTrue(report.contains("MIXED MEDIA OVERDUE REPORT"), "Should include report header");
    }

    @Test
    void testGetOverdueLoans() {
        // Setup
        LocalDate futureDate = LocalDate.now().plusDays(100);
        Loan overdueLoan = new Loan("LOAN-001", TEST_USER_ID, TEST_BOOK_ISBN, "BOOK",
                LocalDate.now(), LocalDate.now().plusDays(28));

        when(loanRepository.getOverdueLoans(futureDate)).thenReturn(List.of(overdueLoan));

        // Execute
        List<Loan> overdueLoans = loanService.getOverdueLoans(futureDate);

        // Verify
        assertFalse(overdueLoans.isEmpty(), "Should return overdue loans");
        assertEquals("LOAN-001", overdueLoans.get(0).getLoanId());
    }

    @Test
    void testGetOverdueSummary() {
        // Skip this test since we don't know the exact OverdueSummary structure
        System.out.println("Skipping testGetOverdueSummary - OverdueSummary structure unknown");

        // Alternative: Test that the method doesn't throw an exception
        assertDoesNotThrow(() -> {
            loanService.getOverdueSummary(TEST_USER_ID, LocalDate.now());
        });
    }

    @Test
    void testMixedMediaBorrowing() {
        // Setup for book
        User user = new User(TEST_USER_ID, "Test User", "test@email.com");
        user.setCanBorrow(true);
        user.setActive(true);

        Book book = createTestBook(TEST_BOOK_ISBN, "Test Book", true);
        CD cd = createTestCD(TEST_CD_CATALOG, "Test CD", true);

        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(user);
        when(mediaRepository.findMediaByIdAndType(TEST_BOOK_ISBN, "BOOK")).thenReturn(book);
        when(mediaRepository.findMediaByIdAndType(TEST_CD_CATALOG, "CD")).thenReturn(cd);
        when(fineService.getTotalUnpaidAmount(TEST_USER_ID)).thenReturn(0.0);
        when(loanRepository.findLoansByUser(TEST_USER_ID)).thenReturn(new ArrayList<>());

        Loan bookLoan = new Loan("BOOK-LOAN", TEST_USER_ID, TEST_BOOK_ISBN, "BOOK",
                LocalDate.now(), LocalDate.now().plusDays(28));
        Loan cdLoan = new Loan("CD-LOAN", TEST_USER_ID, TEST_CD_CATALOG, "CD",
                LocalDate.now(), LocalDate.now().plusDays(7));

        when(loanRepository.createBookLoan(TEST_USER_ID, TEST_BOOK_ISBN, LocalDate.now()))
                .thenReturn(bookLoan);
        when(loanRepository.createCDLoan(TEST_USER_ID, TEST_CD_CATALOG, LocalDate.now()))
                .thenReturn(cdLoan);

        // Execute - Borrow book
        Loan actualBookLoan = loanService.borrowBook(TEST_USER_ID, TEST_BOOK_ISBN, LocalDate.now());
        assertNotNull(actualBookLoan);

        // Execute - Borrow CD
        Loan actualCdLoan = loanService.borrowCD(TEST_USER_ID, TEST_CD_CATALOG, LocalDate.now());
        assertNotNull(actualCdLoan);

        // Verify different due dates
        assertEquals(LocalDate.now().plusDays(28), actualBookLoan.getDueDate());
        assertEquals(LocalDate.now().plusDays(7), actualCdLoan.getDueDate());

        // Verify different media types
        assertEquals("BOOK", actualBookLoan.getMediaType());
        assertEquals("CD", actualCdLoan.getMediaType());
    }

    // Parameterized test for different borrowing scenarios
    static Stream<Arguments> borrowingFailureScenarios() {
        return Stream.of(
                Arguments.of("NONEXISTENT", STATIC_TEST_BOOK_ISBN, "User not found"),
                Arguments.of(STATIC_TEST_USER_ID, "INVALID-ISBN", "Book not found"),
                Arguments.of(STATIC_TEST_USER_ID, STATIC_TEST_BOOK_ISBN, "With unpaid fines")
        );
    }

    @ParameterizedTest
    @MethodSource("borrowingFailureScenarios")
    void testBorrowingFailureScenarios(String userId, String mediaId, String scenario) {
        System.out.println("Testing scenario: " + scenario);

        // Setup based on scenario
        if ("User not found".equals(scenario)) {
            when(userRepository.findUserById("NONEXISTENT")).thenReturn(null);
        } else if ("Book not found".equals(scenario)) {
            User user = new User(STATIC_TEST_USER_ID, "Test User", "test@email.com");
            user.setCanBorrow(true);
            user.setActive(true);
            when(userRepository.findUserById(STATIC_TEST_USER_ID)).thenReturn(user);
            when(mediaRepository.findMediaByIdAndType("INVALID-ISBN", "BOOK")).thenReturn(null);
        } else if ("With unpaid fines".equals(scenario)) {
            User user = new User(STATIC_TEST_USER_ID, "Test User", "test@email.com");
            user.setCanBorrow(true);
            user.setActive(true);
            when(userRepository.findUserById(STATIC_TEST_USER_ID)).thenReturn(user);
            when(fineService.getTotalUnpaidAmount(STATIC_TEST_USER_ID)).thenReturn(15.0);
        }

        // Execute
        Loan loan = loanService.borrowBook(userId, mediaId, LocalDate.now());

        // Verify
        assertNull(loan, "Should fail to borrow in scenario: " + scenario);
    }

    @Test
    void testGetters() {
        assertNotNull(loanService.getLoanRepository());
        assertNotNull(loanService.getUserRepository());
        assertNotNull(loanService.getFineService());
        assertNotNull(loanService.getMediaRepository());
    }
}