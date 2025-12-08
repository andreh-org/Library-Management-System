package com.library.repository;

import com.library.model.Loan;
import com.library.model.Media;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LoanRepository
 * @author Library Team
 * @version 1.0
 */
class LoanRepositoryTest {
    private LoanRepository loanRepository;
    private MediaRepository mediaRepository;

    @BeforeEach
    void setUp() {
        mediaRepository = new MediaRepository();
        loanRepository = new LoanRepository(mediaRepository);
    }

    @Test
    void testCreateBookLoan() {
        LocalDate borrowDate = LocalDate.now();
        Loan loan = loanRepository.createBookLoan("U001", "978-0451524935", borrowDate);

        assertNotNull(loan);
        assertEquals("U001", loan.getUserId());
        assertEquals("978-0451524935", loan.getMediaId());
        assertEquals("BOOK", loan.getMediaType());
        assertEquals(borrowDate, loan.getBorrowDate());
        assertEquals(borrowDate.plusDays(28), loan.getDueDate());
        assertNull(loan.getReturnDate());
        assertFalse(loan.isOverdue());
        assertTrue(loan.getLoanId().startsWith("L"));
    }

    @Test
    void testCreateCDLoan() {
        LocalDate borrowDate = LocalDate.now();
        Loan loan = loanRepository.createCDLoan("U001", "CD-001", borrowDate);

        assertNotNull(loan);
        assertEquals("U001", loan.getUserId());
        assertEquals("CD-001", loan.getMediaId());
        assertEquals("CD", loan.getMediaType());
        assertEquals(borrowDate, loan.getBorrowDate());
        assertEquals(borrowDate.plusDays(7), loan.getDueDate());
        assertNull(loan.getReturnDate());
        assertFalse(loan.isOverdue());
        assertTrue(loan.getLoanId().startsWith("L"));
    }

    @Test
    void testCreateLoan() {
        LocalDate borrowDate = LocalDate.now();
        Loan loan = loanRepository.createLoan("U001", "978-0451524935", "BOOK", borrowDate);

        assertNotNull(loan);
        assertEquals("U001", loan.getUserId());
        assertEquals("978-0451524935", loan.getMediaId());
        assertEquals("BOOK", loan.getMediaType());
    }

    @Test
    void testCreateLoanWithInvalidMediaType() {
        LocalDate borrowDate = LocalDate.now();
        // Should default to BOOK loan period (28 days)
        Loan loan = loanRepository.createLoan("U001", "978-0451524935", "INVALID", borrowDate);

        assertNotNull(loan);
        assertEquals(borrowDate.plusDays(28), loan.getDueDate()); // Defaults to book period
    }

    @Test
    void testCreateLoanMarksMediaUnavailable() {
        assertTrue(mediaRepository.findMediaById("978-0451524935").isAvailable());
        Loan loan = loanRepository.createBookLoan("U001", "978-0451524935", LocalDate.now());
        assertNotNull(loan);
        assertFalse(mediaRepository.findMediaById("978-0451524935").isAvailable());
    }

    @Test
    void testFindLoansByUser() {
        loanRepository.createBookLoan("U001", "978-0451524935", LocalDate.now());
        loanRepository.createCDLoan("U001", "CD-001", LocalDate.now());

        List<Loan> userLoans = loanRepository.findLoansByUser("U001");
        assertFalse(userLoans.isEmpty());
        assertEquals(3, userLoans.size()); // 2 new + 1 sample (L0003)
    }

    @Test
    void testFindLoansByUserNotFound() {
        List<Loan> userLoans = loanRepository.findLoansByUser("NONEXISTENT");
        assertTrue(userLoans.isEmpty());
    }

    @Test
    void testFindLoansByMedia() {
        loanRepository.createBookLoan("U001", "978-0451524935", LocalDate.now());
        List<Loan> mediaLoans = loanRepository.findLoansByMedia("978-0451524935");
        assertFalse(mediaLoans.isEmpty());
    }

    @Test
    void testFindLoansByMediaNotFound() {
        List<Loan> mediaLoans = loanRepository.findLoansByMedia("INVALID_MEDIA");
        assertTrue(mediaLoans.isEmpty());
    }

    @Test
    void testGetActiveLoans() {
        List<Loan> activeLoans = loanRepository.getActiveLoans();
        assertFalse(activeLoans.isEmpty());
        for (Loan loan : activeLoans) {
            assertNull(loan.getReturnDate());
        }
    }

    @Test
    void testGetOverdueLoans() {
        List<Loan> overdueLoans = loanRepository.getOverdueLoans(LocalDate.now());
        assertFalse(overdueLoans.isEmpty());
        for (Loan loan : overdueLoans) {
            assertTrue(loan.isOverdue());
            assertNull(loan.getReturnDate());
        }
    }

    @Test
    void testGetOverdueLoansWithFutureDate() {
        LocalDate futureDate = LocalDate.now().plusYears(1);
        List<Loan> overdueLoans = loanRepository.getOverdueLoans(futureDate);
        // All sample loans should be overdue in the future
        assertTrue(overdueLoans.size() >= 4);
    }

    @Test
    void testGetOverdueLoansForUser() {
        List<Loan> overdueLoans = loanRepository.getOverdueLoansForUser("U002", LocalDate.now());
        assertFalse(overdueLoans.isEmpty());
        assertEquals(2, overdueLoans.size()); // U002 has 2 overdue books
        for (Loan loan : overdueLoans) {
            assertEquals("U002", loan.getUserId());
            assertTrue(loan.isOverdue());
        }
    }

    @Test
    void testGetOverdueLoansForUserNotFound() {
        List<Loan> overdueLoans = loanRepository.getOverdueLoansForUser("NONEXISTENT", LocalDate.now());
        assertTrue(overdueLoans.isEmpty());
    }

    @Test
    void testReturnMediaSuccess() {
        Loan loan = loanRepository.createBookLoan("U001", "978-0451524935", LocalDate.now());
        String loanId = loan.getLoanId();

        assertFalse(mediaRepository.findMediaById("978-0451524935").isAvailable());
        boolean returnSuccess = loanRepository.returnMedia(loanId, LocalDate.now());

        assertTrue(returnSuccess);
        assertTrue(mediaRepository.findMediaById("978-0451524935").isAvailable());

        Loan returnedLoan = loanRepository.findLoanById(loanId);
        assertNotNull(returnedLoan.getReturnDate());
        assertEquals(LocalDate.now(), returnedLoan.getReturnDate());
        assertFalse(returnedLoan.isOverdue());
    }

    @Test
    void testReturnMediaNotFound() {
        boolean returnSuccess = loanRepository.returnMedia("INVALID_LOAN_ID", LocalDate.now());
        assertFalse(returnSuccess);
    }

    @Test
    void testReturnMediaAlreadyReturned() {
        Loan loan = loanRepository.createBookLoan("U001", "978-0451524935", LocalDate.now());
        loanRepository.returnMedia(loan.getLoanId(), LocalDate.now());
        boolean secondReturn = loanRepository.returnMedia(loan.getLoanId(), LocalDate.now());
        assertFalse(secondReturn);
    }

    @Test
    void testFindLoanById() {
        Loan newLoan = loanRepository.createBookLoan("U001", "978-0451524935", LocalDate.now());
        Loan foundLoan = loanRepository.findLoanById(newLoan.getLoanId());

        assertNotNull(foundLoan);
        assertEquals(newLoan.getLoanId(), foundLoan.getLoanId());
        assertEquals("U001", foundLoan.getUserId());
        assertEquals("978-0451524935", foundLoan.getMediaId());
        assertEquals("BOOK", foundLoan.getMediaType());
    }

    @Test
    void testFindLoanByIdNotFound() {
        Loan foundLoan = loanRepository.findLoanById("NONEXISTENT_LOAN");
        assertNull(foundLoan);
    }

    @Test
    void testGetAllLoans() {
        List<Loan> allLoans = loanRepository.getAllLoans();
        assertFalse(allLoans.isEmpty());
        assertTrue(allLoans.size() >= 4); // At least 4 sample loans
    }

    @Test
    void testLoanIdIncrement() {
        Loan loan1 = loanRepository.createBookLoan("U001", "978-0451524935", LocalDate.now());
        Loan loan2 = loanRepository.createBookLoan("U001", "978-0141439518", LocalDate.now());
        Loan loan3 = loanRepository.createCDLoan("U001", "CD-001", LocalDate.now());

        assertNotEquals(loan1.getLoanId(), loan2.getLoanId());
        assertNotEquals(loan2.getLoanId(), loan3.getLoanId());
        assertNotEquals(loan1.getLoanId(), loan3.getLoanId());

        // Check IDs are sequential
        int id1 = Integer.parseInt(loan1.getLoanId().substring(1));
        int id2 = Integer.parseInt(loan2.getLoanId().substring(1));
        int id3 = Integer.parseInt(loan3.getLoanId().substring(1));

        assertEquals(id1 + 1, id2);
        assertEquals(id2 + 1, id3);
    }

    @Test
    void testGetMediaRepository() {
        MediaRepository repo = loanRepository.getMediaRepository();
        assertNotNull(repo);
        assertEquals(mediaRepository, repo);
    }

    @Test
    void testCalculateTotalFinesForUser() {
        // U002 has 2 overdue books in sample data
        double totalFines = loanRepository.calculateTotalFinesForUser("U002", LocalDate.now());
        assertEquals(20.00, totalFines, 0.001); // 2 books * $10 each
    }

    @Test
    void testCalculateTotalFinesForUserNoOverdue() {
        double totalFines = loanRepository.calculateTotalFinesForUser("U003", LocalDate.now());
        assertEquals(0.0, totalFines, 0.001); // U003 has no overdue items
    }

    @Test
    void testCalculateTotalFinesForUserNotFound() {
        double totalFines = loanRepository.calculateTotalFinesForUser("NONEXISTENT", LocalDate.now());
        assertEquals(0.0, totalFines, 0.001);
    }

    @Test
    void testGetIntegratedOverdueReport() {
        LoanRepository.IntegratedOverdueReport report =
                loanRepository.getIntegratedOverdueReport("U002", LocalDate.now());

        assertNotNull(report);
        assertEquals("U002", report.getUserId());
        assertEquals(LocalDate.now(), report.getReportDate());
        assertFalse(report.getAllActiveLoans().isEmpty());
        assertFalse(report.getOverdueActiveLoans().isEmpty());

        // U002 has 2 overdue active loans
        assertEquals(2, report.getOverdueActiveLoans().size());
        assertEquals(20.00, report.getActiveFinesTotal(), 0.001); // 2 books * $10
        assertEquals(20.00, report.getTotalFine(), 0.001);
    }

    @Test
    void testIntegratedOverdueReportToString() {
        LoanRepository.IntegratedOverdueReport report =
                loanRepository.getIntegratedOverdueReport("U002", LocalDate.now());

        String reportString = report.toString();
        assertNotNull(reportString);
        assertTrue(reportString.contains("U002"));
        assertTrue(reportString.contains("OVERDUE REPORT"));
        assertTrue(reportString.contains("$20.00"));
    }

    @Test
    void testGetFineForLoan() {
        LoanRepository.IntegratedOverdueReport report =
                loanRepository.getIntegratedOverdueReport("U002", LocalDate.now());

        List<Loan> overdueLoans = report.getOverdueActiveLoans();
        if (!overdueLoans.isEmpty()) {
            Loan loan = overdueLoans.get(0);
            double fine = report.getFineForLoan(loan);
            assertEquals(10.00, fine, 0.001); // Book fine
        }
    }

    @Test
    void testGetOverdueSummaryForUser() {
        LoanRepository.OverdueSummary summary =
                loanRepository.getOverdueSummaryForUser("U002", LocalDate.now());

        assertNotNull(summary);
        assertEquals("U002", summary.getUserId());
        assertFalse(summary.getOverdueItems().isEmpty());
        assertEquals(20.00, summary.getTotalFine(), 0.001); // 2 books * $10
    }

    @Test
    void testOverdueSummaryToString() {
        LoanRepository.OverdueSummary summary =
                loanRepository.getOverdueSummaryForUser("U002", LocalDate.now());

        String summaryString = summary.toString();
        assertNotNull(summaryString);
        assertTrue(summaryString.contains("U002"));
        assertTrue(summaryString.contains("OVERDUE SUMMARY"));
    }

    @Test
    void testOverdueSummaryGetMediaTypeBreakdown() {
        LoanRepository.OverdueSummary summary =
                loanRepository.getOverdueSummaryForUser("U002", LocalDate.now());

        String breakdown = summary.getMediaTypeBreakdown();
        assertNotNull(breakdown);
        assertTrue(breakdown.contains("BOOKS"));
        assertTrue(breakdown.contains("$20.00"));
    }

    @Test
    void testOverdueItemCreation() {
        LoanRepository.OverdueSummary.OverdueItem item =
                new LoanRepository.OverdueSummary.OverdueItem("BOOK", "978-0743273565", 10.00, "L0001");

        assertEquals("BOOK", item.getMediaType());
        assertEquals("978-0743273565", item.getMediaId());
        assertEquals(10.00, item.getFine(), 0.001);
        assertEquals("L0001", item.getLoanId());
    }

    @Test
    void testOverdueItemToString() {
        LoanRepository.OverdueSummary.OverdueItem item =
                new LoanRepository.OverdueSummary.OverdueItem("BOOK", "978-0743273565", 10.00, "L0001");

        String itemString = item.toString();
        assertNotNull(itemString);
        assertTrue(itemString.contains("📚"));
        assertTrue(itemString.contains("978-0743273565"));
        assertTrue(itemString.contains("$10.00"));
    }

    @Test
    void testOverdueItemCDToString() {
        LoanRepository.OverdueSummary.OverdueItem item =
                new LoanRepository.OverdueSummary.OverdueItem("CD", "CD-001", 20.00, "L0003");

        String itemString = item.toString();
        assertNotNull(itemString);
        assertTrue(itemString.contains("💿"));
        assertTrue(itemString.contains("CD-001"));
        assertTrue(itemString.contains("$20.00"));
    }

    @Test
    void testGetLoanPeriodForMediaType() {
        // Test private method indirectly through createLoan
        Loan bookLoan = loanRepository.createLoan("U001", "TEST", "BOOK", LocalDate.now());
        assertEquals(LocalDate.now().plusDays(28), bookLoan.getDueDate());

        Loan cdLoan = loanRepository.createLoan("U001", "TEST-CD", "CD", LocalDate.now());
        assertEquals(LocalDate.now().plusDays(7), cdLoan.getDueDate());

        Loan unknownLoan = loanRepository.createLoan("U001", "TEST", "UNKNOWN", LocalDate.now());
        assertEquals(LocalDate.now().plusDays(28), unknownLoan.getDueDate()); // Defaults to book
    }

    @Test
    void testSampleDataInitialization() {
        List<Loan> allLoans = loanRepository.getAllLoans();
        assertTrue(allLoans.size() >= 4);

        // Verify specific sample loans exist
        boolean hasL0001 = allLoans.stream().anyMatch(l -> "L0001".equals(l.getLoanId()));
        boolean hasL0002 = allLoans.stream().anyMatch(l -> "L0002".equals(l.getLoanId()));
        boolean hasL0003 = allLoans.stream().anyMatch(l -> "L0003".equals(l.getLoanId()));
        boolean hasL0004 = allLoans.stream().anyMatch(l -> "L0004".equals(l.getLoanId()));

        assertTrue(hasL0001);
        assertTrue(hasL0002);
        assertTrue(hasL0003);
        assertTrue(hasL0004);

        // Verify loans are marked overdue
        Loan l0001 = allLoans.stream().filter(l -> "L0001".equals(l.getLoanId())).findFirst().orElse(null);
        assertNotNull(l0001);
        assertTrue(l0001.isOverdue());
    }

    @Test
    void testMixedMediaLoans() {
        Loan bookLoan = loanRepository.createBookLoan("U001", "978-0451524935", LocalDate.now());
        Loan cdLoan = loanRepository.createCDLoan("U001", "CD-001", LocalDate.now());

        assertNotNull(bookLoan);
        assertNotNull(cdLoan);
        assertEquals("BOOK", bookLoan.getMediaType());
        assertEquals("CD", cdLoan.getMediaType());
        assertEquals(LocalDate.now().plusDays(28), bookLoan.getDueDate());
        assertEquals(LocalDate.now().plusDays(7), cdLoan.getDueDate());
    }

    @Test
    void testReturnMediaUpdatesOverdueStatus() {
        // Create a loan through the repository (not manually)
        Loan loan = loanRepository.createBookLoan("U001", "978-0451524935", LocalDate.now().minusDays(30));

        // Manually set it as overdue for testing
        loan.setOverdue(true);

        // Return the loan
        boolean success = loanRepository.returnMedia(loan.getLoanId(), LocalDate.now());
        assertTrue(success);

        // Verify overdue status is reset
        Loan returnedLoan = loanRepository.findLoanById(loan.getLoanId());
        assertFalse(returnedLoan.isOverdue());
    }

    @Test
    void testCalculateFlatFineForCD() {
        // Create a CD loan that's overdue
        LocalDate pastDate = LocalDate.now().minusDays(10);
        Loan cdLoan = new Loan("CD-TEST", "U001", "CD-001", "CD",
                pastDate, pastDate.plusDays(7));
        cdLoan.setOverdue(true);

        // Add to repository
        loanRepository.getAllLoans().add(cdLoan);

        // Get report and check CD fine
        LoanRepository.IntegratedOverdueReport report =
                loanRepository.getIntegratedOverdueReport("U001", LocalDate.now());

        double cdFine = report.getFineForLoan(cdLoan);
        assertEquals(20.00, cdFine, 0.001); // CD flat fine
    }

    @Test
    void testReturnedOverdueLoansInReport() {
        // First, return any existing loans for U005 to clean state
        List<Loan> existingLoans = loanRepository.findLoansByUser("U005");
        for (Loan loan : existingLoans) {
            if (loan.getReturnDate() == null) {
                loanRepository.returnMedia(loan.getLoanId(), LocalDate.now());
            }
        }

        // Create a new loan for U005 with past date
        Loan newLoan = loanRepository.createBookLoan("U005", "978-0451524935",
                LocalDate.now().minusDays(30));

        // Return it immediately (overdue)
        boolean returned = loanRepository.returnMedia(newLoan.getLoanId(), LocalDate.now());
        assertTrue(returned);

        // Get report
        LoanRepository.IntegratedOverdueReport report =
                loanRepository.getIntegratedOverdueReport("U005", LocalDate.now());

        // Should have returned overdue loans with fine
        assertEquals(1, report.getReturnedOverdueLoans().size());
        assertEquals(10.00, report.getReturnedFinesTotal(), 0.001);
    }

    @Test
    void testDefaultConstructor() {
        // Test the default constructor
        LoanRepository defaultRepo = new LoanRepository();
        assertNotNull(defaultRepo);
        assertNotNull(defaultRepo.getMediaRepository());
        assertFalse(defaultRepo.getAllLoans().isEmpty());
    }

    @Test
    void testGetLoanPeriodForMediaTypeAllBranches() {
        // Test BOOK branch
        Loan bookLoan = loanRepository.createLoan("U001", "TEST1", "BOOK", LocalDate.now());
        assertEquals(LocalDate.now().plusDays(28), bookLoan.getDueDate());

        // Test CD branch
        Loan cdLoan = loanRepository.createLoan("U001", "TEST2", "CD", LocalDate.now());
        assertEquals(LocalDate.now().plusDays(7), cdLoan.getDueDate());

        // Test default branch (unknown media type)
        Loan unknownLoan = loanRepository.createLoan("U001", "TEST3", "UNKNOWN", LocalDate.now());
        assertEquals(LocalDate.now().plusDays(28), unknownLoan.getDueDate()); // Defaults to book period

        // Test case-insensitive
        Loan lowerCaseBook = loanRepository.createLoan("U001", "TEST4", "book", LocalDate.now());
        assertEquals(LocalDate.now().plusDays(28), lowerCaseBook.getDueDate());

        Loan lowerCaseCD = loanRepository.createLoan("U001", "TEST5", "cd", LocalDate.now());
        assertEquals(LocalDate.now().plusDays(7), lowerCaseCD.getDueDate());
    }

    @Test
    void testIntegratedOverdueReportCalculateFlatFineAllBranches() {
        String testUserId = "BRANCH-TEST-USER";

        // Create loans with PAST dates so they're actually overdue
        // Book loan: 30 days ago, due 28 days ago = 2 days overdue
        LocalDate bookBorrowDate = LocalDate.now().minusDays(30);
        Loan bookLoan = new Loan("TEST-BOOK-LOAN", testUserId, "978-0451524935", "BOOK",
                bookBorrowDate, bookBorrowDate.plusDays(28));
        bookLoan.checkOverdue(LocalDate.now()); // This will mark it as overdue
        assertTrue(bookLoan.isOverdue(), "Book loan should be overdue");

        // CD loan: 10 days ago, due 7 days ago = 3 days overdue
        LocalDate cdBorrowDate = LocalDate.now().minusDays(10);
        Loan cdLoan = new Loan("TEST-CD-LOAN", testUserId, "CD-001", "CD",
                cdBorrowDate, cdBorrowDate.plusDays(7));
        cdLoan.checkOverdue(LocalDate.now()); // This will mark it as overdue
        assertTrue(cdLoan.isOverdue(), "CD loan should be overdue");

        // We need to add these to the repository's internal list for testing
        // Since we can't access private loans list, we'll test differently

        // Instead, let's test the calculateFlatFine logic directly through existing sample data
        // U001 has an overdue CD in sample data
        LoanRepository.IntegratedOverdueReport report =
                loanRepository.getIntegratedOverdueReport("U001", LocalDate.now());

        // Get U001's overdue CD loan from sample data
        List<Loan> u001Loans = loanRepository.findLoansByUser("U001");
        Loan u001CDLoan = u001Loans.stream()
                .filter(l -> "CD".equals(l.getMediaType()))
                .filter(Loan::isOverdue)
                .findFirst()
                .orElse(null);

        assertNotNull(u001CDLoan, "U001 should have an overdue CD loan");
        double cdFine = report.getFineForLoan(u001CDLoan);
        assertEquals(20.00, cdFine, 0.001, "CD should have $20 flat fine");

        // Test BOOK fine using U002's overdue books
        LoanRepository.IntegratedOverdueReport u002Report =
                loanRepository.getIntegratedOverdueReport("U002", LocalDate.now());

        List<Loan> u002Loans = loanRepository.findLoansByUser("U002");
        Loan u002BookLoan = u002Loans.stream()
                .filter(l -> "BOOK".equals(l.getMediaType()))
                .filter(Loan::isOverdue)
                .findFirst()
                .orElse(null);

        assertNotNull(u002BookLoan, "U002 should have an overdue book loan");
        double bookFine = u002Report.getFineForLoan(u002BookLoan);
        assertEquals(10.00, bookFine, 0.001, "Book should have $10 flat fine");
    }

    @Test
    void testCalculateReportEdgeCases() {
        // Test with user who has no loans
        LoanRepository.IntegratedOverdueReport report =
                loanRepository.getIntegratedOverdueReport("NONEXISTENT-USER", LocalDate.now());

        assertTrue(report.getAllActiveLoans().isEmpty());
        assertTrue(report.getOverdueActiveLoans().isEmpty());
        assertTrue(report.getReturnedOverdueLoans().isEmpty());
        assertEquals(0.0, report.getActiveFinesTotal(), 0.001);
        assertEquals(0.0, report.getReturnedFinesTotal(), 0.001);
        assertEquals(0.0, report.getTotalFine(), 0.001);

        // Test toString with empty report
        String reportString = report.toString();
        assertNotNull(reportString);
        assertTrue(reportString.contains("No overdue items or unpaid fines"));
    }

    @Test
    void testOverdueSummaryEmptyCase() {
        // User with no overdue items
        LoanRepository.OverdueSummary summary =
                loanRepository.getOverdueSummaryForUser("U003", LocalDate.now()); // U003 has no overdue items

        assertTrue(summary.getOverdueItems().isEmpty());
        assertEquals(0.0, summary.getTotalFine(), 0.001);

        // Test toString for empty summary
        String summaryString = summary.toString();
        assertNotNull(summaryString);
        assertTrue(summaryString.contains("No overdue items"));

        // Test getMediaTypeBreakdown for empty
        String breakdown = summary.getMediaTypeBreakdown();
        assertNotNull(breakdown);
        assertTrue(breakdown.contains("MEDIA TYPE BREAKDOWN"));
    }

    @Test
    void testIntegratedOverdueReportToStringAllBranches() {
        // Test with user who has mixed media (U001 has a CD)
        LoanRepository.IntegratedOverdueReport report =
                loanRepository.getIntegratedOverdueReport("U001", LocalDate.now());

        String reportString = report.toString();
        assertNotNull(reportString);

        // Should contain CD icon
        assertTrue(reportString.contains("💿"));
        // Should contain fine amount
        assertTrue(reportString.contains("$20.00"));

        // Test with user who has only books (U002)
        LoanRepository.IntegratedOverdueReport bookReport =
                loanRepository.getIntegratedOverdueReport("U002", LocalDate.now());

        String bookReportString = bookReport.toString();
        assertNotNull(bookReportString);
        assertTrue(bookReportString.contains("📚")); // Book icon
        assertTrue(bookReportString.contains("$20.00")); // 2 books * $10
    }

    @Test
    void testCalculateTotalFinesForUserEdgeCases() {
        // User with no active loans
        double fines = loanRepository.calculateTotalFinesForUser("U003", LocalDate.now());
        assertEquals(0.0, fines, 0.001);

        // Non-existent user
        fines = loanRepository.calculateTotalFinesForUser("NONEXISTENT", LocalDate.now());
        assertEquals(0.0, fines, 0.001);

        // User with returned loans (should not count)
        // First, return U002's loans
        List<Loan> u002Loans = loanRepository.findLoansByUser("U002");
        for (Loan loan : u002Loans) {
            if (loan.getReturnDate() == null) {
                loanRepository.returnMedia(loan.getLoanId(), LocalDate.now());
            }
        }

        // Now calculate fines - should be 0 because loans are returned
        fines = loanRepository.calculateTotalFinesForUser("U002", LocalDate.now());
        assertEquals(0.0, fines, 0.001);
    }

    @Test
    void testGetOverdueLoansEdgeCases() {
        // Test with future date (no loans should be overdue)
        LocalDate futureDate = LocalDate.now().plusYears(1);
        List<Loan> futureOverdue = loanRepository.getOverdueLoans(futureDate);
        // All sample loans will be overdue in the future
        assertTrue(futureOverdue.size() >= 4);

        // Test with past date before any loans were created
        LocalDate pastDate = LocalDate.now().minusYears(1);
        List<Loan> pastOverdue = loanRepository.getOverdueLoans(pastDate);
        // No loans should be overdue (they didn't exist yet)
        assertTrue(pastOverdue.isEmpty());
    }

    @Test
    void testReturnMediaAllBranches() {
        // Try to return non-existent loan
        boolean success = loanRepository.returnMedia("NONEXISTENT-LOAN", LocalDate.now());
        assertFalse(success);

        // Try to return already returned loan
        // First create and return a loan
        Loan loan = loanRepository.createBookLoan("TEST-USER", "978-0451524935", LocalDate.now());
        assertTrue(loanRepository.returnMedia(loan.getLoanId(), LocalDate.now()));

        // Try to return again
        boolean secondReturn = loanRepository.returnMedia(loan.getLoanId(), LocalDate.now());
        assertFalse(secondReturn);
    }

    @Test
    void testOverdueItemToStringAllMediaTypes() {
        // Test BOOK OverdueItem
        LoanRepository.OverdueSummary.OverdueItem bookItem =
                new LoanRepository.OverdueSummary.OverdueItem("BOOK", "978-0743273565", 10.00, "L0001");

        String bookString = bookItem.toString();
        assertNotNull(bookString);
        assertTrue(bookString.contains("📚"));
        assertTrue(bookString.contains("BOOK"));
        assertTrue(bookString.contains("$10.00"));

        // Test CD OverdueItem
        LoanRepository.OverdueSummary.OverdueItem cdItem =
                new LoanRepository.OverdueSummary.OverdueItem("CD", "CD-001", 20.00, "L0003");

        String cdString = cdItem.toString();
        assertNotNull(cdString);
        assertTrue(cdString.contains("💿"));
        assertTrue(cdString.contains("CD"));
        assertTrue(cdString.contains("$20.00"));

        // Test unknown media type (should use book icon as default in current logic)
        LoanRepository.OverdueSummary.OverdueItem unknownItem =
                new LoanRepository.OverdueSummary.OverdueItem("UNKNOWN", "UNK-001", 15.00, "L9999");

        String unknownString = unknownItem.toString();
        assertNotNull(unknownString);
        // Current logic uses book icon for non-CD, but should handle gracefully
    }

    @Test
    void testMediaTypeBreakdownAllBranches() {
        // Create a summary with both book and CD items
        LoanRepository.OverdueSummary summary = new LoanRepository.OverdueSummary("TEST-USER");

        summary.addOverdueItem("BOOK", "978-0743273565", 10.00, "L0001");
        summary.addOverdueItem("BOOK", "978-0061120084", 10.00, "L0002");
        summary.addOverdueItem("CD", "CD-001", 20.00, "L0003");
        summary.addOverdueItem("CD", "CD-002", 20.00, "L0004");

        String breakdown = summary.getMediaTypeBreakdown();
        assertNotNull(breakdown);

        // Should contain both book and CD sections
        assertTrue(breakdown.contains("BOOKS (2 items): $20.00"));
        assertTrue(breakdown.contains("CDs (2 items): $40.00"));
        assertTrue(breakdown.contains("TOTAL FINE: $60.00"));

        // Test with only books
        LoanRepository.OverdueSummary booksOnly = new LoanRepository.OverdueSummary("BOOKS-ONLY");
        booksOnly.addOverdueItem("BOOK", "978-0743273565", 10.00, "L0001");
        booksOnly.addOverdueItem("BOOK", "978-0061120084", 10.00, "L0002");

        String booksBreakdown = booksOnly.getMediaTypeBreakdown();
        assertNotNull(booksBreakdown);
        assertTrue(booksBreakdown.contains("BOOKS (2 items): $20.00"));
        assertFalse(booksBreakdown.contains("CDs")); // Should not have CD section

        // Test with only CDs
        LoanRepository.OverdueSummary cdsOnly = new LoanRepository.OverdueSummary("CDS-ONLY");
        cdsOnly.addOverdueItem("CD", "CD-001", 20.00, "L0001");

        String cdsBreakdown = cdsOnly.getMediaTypeBreakdown();
        assertNotNull(cdsBreakdown);
        assertTrue(cdsBreakdown.contains("CDs (1 items): $20.00"));
        assertFalse(cdsBreakdown.contains("BOOKS")); // Should not have book section
    }


}