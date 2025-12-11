package com.library.service;

import com.library.model.*;
import com.library.repository.LoanRepository;
import com.library.repository.MediaRepository;
import com.library.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for handling loan-related operations
 * @author Library Team
 * @version 1.0
 */
public class LoanService {
    private LoanRepository loanRepository;
    private MediaRepository mediaRepository;
    private UserRepository userRepository;
    private FineService fineService;

    // Constructor with all dependencies
    public LoanService(FineService fineService, UserRepository userRepository, MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.fineService = fineService;
        this.loanRepository = new LoanRepository(mediaRepository);
    }

    /**
     * Gets simple mixed media overdue report showing only fines for books and CDs
     * @param userId the user ID
     * @param currentDate the current date
     * @return formatted simple mixed media overdue report
     */
    public String getSimpleMixedMediaReport(String userId, LocalDate currentDate) {
        checkAndApplyOverdueFines(userId, currentDate);

        User user = userRepository.findUserById(userId);
        if (user == null) {
            return "❌ Error: User not found.";
        }

        List<Fine> fines = fineService.getUserUnpaidFines(userId);
        
        StringBuilder report = new StringBuilder();
        buildReportHeader(report, user, userId, currentDate);
        
        if (fines.isEmpty()) {
            report.append("\n✅ No unpaid fines found.");
        } else {
            buildFinesSection(report, fines);
        }
        
        report.append("\n").append("-".repeat(60));
        return report.toString();
    }

    /**
     * Builds the report header section
     */
    private void buildReportHeader(StringBuilder report, User user, String userId, LocalDate currentDate) {
        report.append("\n=== MIXED MEDIA OVERDUE REPORT ===");
        report.append("\nUser: ").append(user.getName()).append(" (").append(userId).append(")");
        report.append("\nReport Date: ").append(currentDate);
        report.append("\n").append("-".repeat(60));
    }

    /**
     * Builds the fines section with totals by media type
     */
    private void buildFinesSection(StringBuilder report, List<Fine> fines) {
        MediaFinesSummary summary = calculateFinesByMediaType(fines);
        
        report.append("\n📊 UNPAID FINES BY MEDIA TYPE:");
        report.append("\n").append("-".repeat(60));
        
        appendFineDetails(report, fines);
        appendFinesTotals(report, summary);
    }

    /**
     * Calculates totals for fines by media type
     */
    private MediaFinesSummary calculateFinesByMediaType(List<Fine> fines) {
        MediaFinesSummary summary = new MediaFinesSummary();
        
        for (Fine fine : fines) {
            if (fine.getLoanId() != null) {
                Loan loan = loanRepository.findLoanById(fine.getLoanId());
                if (loan != null) {
                    summary.addFine(loan.getMediaType(), fine.getRemainingBalance());
                }
            }
        }
        
        return summary;
    }

    /**
     * Appends individual fine details to the report
     */
    private void appendFineDetails(StringBuilder report, List<Fine> fines) {
        for (Fine fine : fines) {
            if (fine.getLoanId() != null) {
                Loan loan = loanRepository.findLoanById(fine.getLoanId());
                if (loan != null) {
                    appendSingleFineDetail(report, loan, fine);
                }
            }
        }
    }

    /**
     * Appends a single fine detail line
     */
    private void appendSingleFineDetail(StringBuilder report, Loan loan, Fine fine) {
        if ("BOOK".equals(loan.getMediaType())) {
            report.append(String.format("\n📚 Book: %-15s | Loan: %-6s | Fine: $%.2f",
                    loan.getMediaId(), loan.getLoanId(), fine.getRemainingBalance()));
        } else if ("CD".equals(loan.getMediaType())) {
            report.append(String.format("\n💿 CD: %-15s | Loan: %-6s | Fine: $%.2f",
                    loan.getMediaId(), loan.getLoanId(), fine.getRemainingBalance()));
        }
    }

    /**
     * Appends the totals section by media type
     */
    private void appendFinesTotals(StringBuilder report, MediaFinesSummary summary) {
        report.append("\n").append("-".repeat(60));
        
        if (summary.bookCount > 0) {
            report.append(String.format("\n📚 BOOKS: %d item(s) | Total: $%.2f", 
                    summary.bookCount, summary.bookFinesTotal));
        }
        if (summary.cdCount > 0) {
            report.append(String.format("\n💿 CDs: %d item(s) | Total: $%.2f", 
                    summary.cdCount, summary.cdFinesTotal));
        }
        
        double totalFines = summary.bookFinesTotal + summary.cdFinesTotal;
        report.append("\n").append("-".repeat(60));
        report.append(String.format("\n💰 TOTAL UNPAID FINES: $%.2f", totalFines));
    }

    /**
     * Helper class to track fines summary by media type
     */
    private static class MediaFinesSummary {
        double bookFinesTotal = 0;
        double cdFinesTotal = 0;
        int bookCount = 0;
        int cdCount = 0;
        
        void addFine(String mediaType, double amount) {
            if ("BOOK".equals(mediaType)) {
                bookFinesTotal += amount;
                bookCount++;
            } else if ("CD".equals(mediaType)) {
                cdFinesTotal += amount;
                cdCount++;
            }
        }
    }

    /**
     * Check and apply overdue fines for a user
     */
    public void checkAndApplyOverdueFines(String userId, LocalDate currentDate) {
        List<Loan> userLoans = loanRepository.findLoansByUser(userId);

        for (Loan loan : userLoans) {
            if (loan.getReturnDate() == null) {
                loan.checkOverdue(currentDate);

                if (loan.isOverdue()) {
                    long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), currentDate);

                    if (overdueDays > 0) {
                        processOverdueLoan(loan, userId, overdueDays);
                    }
                }
            }
        }
    }

    /**
     * Process a single overdue loan
     */
    private void processOverdueLoan(Loan loan, String userId, long overdueDays) {
        double fineAmount = calculateFlatFine(loan.getMediaType());
        Fine existingFine = fineService.getFineRepository().findFineByLoanId(loan.getLoanId());

        if (existingFine == null && fineAmount > 0) {
            createNewOverdueFine(loan, userId, overdueDays, fineAmount);
        } else if (existingFine != null) {
            updateExistingFine(loan, existingFine, fineAmount);
        }
    }

    /**
     * Calculate flat fine amount based on media type
     */
    private double calculateFlatFine(String mediaType) {
        if ("BOOK".equals(mediaType)) {
            return 10.00;
        } else if ("CD".equals(mediaType)) {
            return 20.00;
        }
        return 0.0;
    }

    /**
     * Create a new overdue fine
     */
    private void createNewOverdueFine(Loan loan, String userId, long overdueDays, double fineAmount) {
        String reason = String.format("Overdue %s (Loan: %s) - %d days overdue",
                loan.getMediaType(), loan.getLoanId(), overdueDays);

        Fine fine = fineService.applyFine(userId, reason, loan.getLoanId());
        if (fine != null) {
            System.out.println("⚠️ Overdue fine applied: $" +
                    String.format("%.2f", fineAmount) +
                    " for " + loan.getMediaType() + " " + loan.getMediaId());
        }
    }

    /**
     * Update existing fine if amount has changed
     */
    private void updateExistingFine(Loan loan, Fine existingFine, double expectedFine) {
        if (existingFine.getAmount() != expectedFine) {
            existingFine.setAmount(expectedFine);
            System.out.println("⚠️ Updated fine for loan " + loan.getLoanId() +
                    " to $" + String.format("%.2f", expectedFine));
        }
    }

    /**
     * Borrow a book
     */
    public Loan borrowBook(String userId, String bookIsbn, LocalDate borrowDate) {
        return borrowMedia(userId, bookIsbn, "BOOK", borrowDate);
    }

    /**
     * Borrow a CD
     */
    public Loan borrowCD(String userId, String cdCatalogNumber, LocalDate borrowDate) {
        return borrowMedia(userId, cdCatalogNumber, "CD", borrowDate);
    }

    /**
     * Generic method to borrow any media
     */
    private Loan borrowMedia(String userId, String mediaId, String mediaType, LocalDate borrowDate) {
        User user = userRepository.findUserById(userId);
        if (user == null) {
            System.out.println("Error: User not found.");
            return null;
        }

        if (!user.isActive()) {
            System.out.println("❌ Error: User account is not active.");
            System.out.println("Please contact administrator to reactivate your account.");
            return null;
        }

        checkAndApplyOverdueFines(userId, borrowDate);

        double unpaidFines = fineService.getTotalUnpaidAmount(userId);
        if (unpaidFines > 0) {
            System.out.println("❌ Error: User cannot borrow. Unpaid fines: $" + unpaidFines);
            System.out.println("Please pay all fines before borrowing.");
            return null;
        }

        List<Loan> userActiveLoans = getUserActiveLoans(userId);
        boolean hasOverdue = userActiveLoans.stream().anyMatch(Loan::isOverdue);

        if (hasOverdue) {
            System.out.println("❌ Error: User cannot borrow. There are overdue items that need to be returned first.");
            System.out.println("Please return all overdue items before borrowing new ones.");
            return null;
        }

        Media media = mediaRepository.findMediaByIdAndType(mediaId, mediaType);
        if (media == null) {
            System.out.println("Error: " + mediaType + " not found with ID: " + mediaId);
            return null;
        }

        if (!media.isAvailable()) {
            System.out.println("Error: " + mediaType + " is already borrowed.");
            return null;
        }

        Loan loan;
        if ("BOOK".equals(mediaType)) {
            loan = loanRepository.createBookLoan(userId, mediaId, borrowDate);
        } else {
            loan = loanRepository.createCDLoan(userId, mediaId, borrowDate);
        }

        if (loan != null) {
            user.addLoan(loan.getLoanId());
            userRepository.updateUser(user);

            String mediaDescription = mediaType.equals("BOOK") ? "Book" : "CD";
            System.out.println("✅ " + mediaDescription + " borrowed successfully. Due date: " + loan.getDueDate());
            System.out.println("Loan period: " + media.getLoanPeriodDays() + " days");
        }

        return loan;
    }

    /**
     * Return media
     */
    public boolean returnBook(String loanId, LocalDate returnDate) {
        Loan loan = loanRepository.findLoanById(loanId);
        if (loan == null) {
            System.out.println("❌ Error: Loan not found.");
            return false;
        }

        if (loan.getReturnDate() != null) {
            System.out.println("❌ Error: Media already returned.");
            return false;
        }

        boolean returnSuccess = loanRepository.returnMedia(loanId, returnDate);
        if (returnSuccess) {
            User user = userRepository.findUserById(loan.getUserId());
            if (user != null) {
                user.removeLoan(loanId);
                userRepository.updateUser(user);
            }

            System.out.println("✅ Media returned successfully!");

            if (returnDate.isAfter(loan.getDueDate())) {
                long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
                System.out.println("⚠️ This item was " + overdueDays + " days overdue.");

                String fineReason = "Overdue " + loan.getMediaType() + " (Loan: " + loanId + ") - " + overdueDays + " days overdue";

                Fine fine = fineService.applyFine(loan.getUserId(), fineReason, loanId);
                if (fine != null) {
                    double fineAmount = "BOOK".equals(loan.getMediaType()) ? 10.00 : 20.00;
                    System.out.println("Fine amount: $" + String.format("%.2f", fineAmount));
                }
            }
        }

        return returnSuccess;
    }

    /**
     * Gets overdue summary for a user
     */
    public LoanRepository.OverdueSummary getOverdueSummary(String userId, LocalDate currentDate) {
        checkAndApplyOverdueFines(userId, currentDate);
        return loanRepository.getOverdueSummaryForUser(userId, currentDate);
    }

    /**
     * Gets all active loans for a user
     */
    public List<Loan> getUserActiveLoans(String userId) {
        List<Loan> userLoans = loanRepository.findLoansByUser(userId).stream()
                .filter(loan -> loan.getReturnDate() == null)
                .toList();

        LocalDate currentDate = LocalDate.now();
        for (Loan loan : userLoans) {
            loan.checkOverdue(currentDate);
        }

        return userLoans;
    }

    /**
     * Checks if user has any overdue items
     */
    public boolean hasOverdueBooks(String userId) {
        List<Loan> activeLoans = getUserActiveLoans(userId);
        return activeLoans.stream().anyMatch(Loan::isOverdue);
    }

    public List<Loan> getOverdueLoans(LocalDate currentDate) {
        return loanRepository.getOverdueLoans(currentDate);
    }

    // Getters
    public LoanRepository getLoanRepository() { return loanRepository; }
    public UserRepository getUserRepository() { return userRepository; }
    public FineService getFineService() { return fineService; }
    public MediaRepository getMediaRepository() { return mediaRepository; }
}
