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
        // First check and apply any overdue fines
        checkAndApplyOverdueFines(userId, currentDate);

        // Get user info
        User user = userRepository.findUserById(userId);
        if (user == null) {
            return "❌ Error: User not found.";
        }

        // Get all fines for the user
        List<Fine> fines = fineService.getUserUnpaidFines(userId);

        StringBuilder report = new StringBuilder();
        report.append("\n=== MIXED MEDIA OVERDUE REPORT ===");
        report.append("\nUser: ").append(user.getName()).append(" (").append(userId).append(")");
        report.append("\nReport Date: ").append(currentDate);
        report.append("\n").append("-".repeat(60));

        if (fines.isEmpty()) {
            report.append("\n✅ No unpaid fines found.");
        } else {
            // Separate fines by media type
            double bookFinesTotal = 0;
            double cdFinesTotal = 0;
            int bookCount = 0;
            int cdCount = 0;

            report.append("\n📊 UNPAID FINES BY MEDIA TYPE:");
            report.append("\n").append("-".repeat(60));

            for (Fine fine : fines) {
                if (fine.getLoanId() != null) {
                    Loan loan = loanRepository.findLoanById(fine.getLoanId());
                    if (loan != null) {
                        if ("BOOK".equals(loan.getMediaType())) {
                            bookFinesTotal += fine.getRemainingBalance();
                            bookCount++;
                            report.append(String.format("\n📚 Book: %-15s | Loan: %-6s | Fine: $%.2f",
                                    loan.getMediaId(), loan.getLoanId(), fine.getRemainingBalance()));
                        } else if ("CD".equals(loan.getMediaType())) {
                            cdFinesTotal += fine.getRemainingBalance();
                            cdCount++;
                            report.append(String.format("\n💿 CD: %-15s | Loan: %-6s | Fine: $%.2f",
                                    loan.getMediaId(), loan.getLoanId(), fine.getRemainingBalance()));
                        }
                    }
                }
            }

            report.append("\n").append("-".repeat(60));

            // Show totals by media type
            if (bookCount > 0) {
                report.append(String.format("\n📚 BOOKS: %d item(s) | Total: $%.2f", bookCount, bookFinesTotal));
            }
            if (cdCount > 0) {
                report.append(String.format("\n💿 CDs: %d item(s) | Total: $%.2f", cdCount, cdFinesTotal));
            }

            double totalFines = bookFinesTotal + cdFinesTotal;
            report.append("\n").append("-".repeat(60));
            report.append(String.format("\n💰 TOTAL UNPAID FINES: $%.2f", totalFines));
        }

        report.append("\n").append("-".repeat(60));
        return report.toString();
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
                    // Calculate how many days overdue
                    long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), currentDate);

                    if (overdueDays > 0) {
                        // Calculate fine amount - use flat fines now
                        double fineAmount = 0.0;
                        if ("BOOK".equals(loan.getMediaType())) {
                            fineAmount = 10.00; // $10 flat fine for books
                        } else if ("CD".equals(loan.getMediaType())) {
                            fineAmount = 20.00; // $20 flat fine for CDs
                        }

                        // Check if a fine already exists for this loan
                        Fine existingFine = fineService.getFineRepository().findFineByLoanId(loan.getLoanId());

                        if (existingFine == null && fineAmount > 0) {
                            // Create a fine for this overdue loan
                            String reason = String.format("Overdue %s (Loan: %s) - %d days overdue",
                                    loan.getMediaType(), loan.getLoanId(), overdueDays);

                            // Use the new applyFine method with loanId
                            Fine fine = fineService.applyFine(userId, reason, loan.getLoanId());
                            if (fine != null) {
                                System.out.println("⚠️ Overdue fine applied: $" +
                                        String.format("%.2f", fineAmount) +
                                        " for " + loan.getMediaType() + " " + loan.getMediaId());
                            }
                        } else if (existingFine != null) {
                            // Fine already exists, check if it needs updating
                            // Since we're using flat fines now, we don't need to update based on days
                            // Just ensure it's the correct flat amount
                            double expectedFine = "BOOK".equals(loan.getMediaType()) ? 10.00 : 20.00;
                            if (existingFine.getAmount() != expectedFine) {
                                existingFine.setAmount(expectedFine);
                                System.out.println("⚠️ Updated fine for loan " + loan.getLoanId() +
                                        " to $" + String.format("%.2f", expectedFine));
                            }
                        }
                    }
                }
            }
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

        // Check if user is active
        if (!user.isActive()) {
            System.out.println("❌ Error: User account is not active.");
            System.out.println("Please contact administrator to reactivate your account.");
            return null;
        }

        // FIRST: Check and apply any overdue fines before new borrow
        checkAndApplyOverdueFines(userId, borrowDate);

        // Check if user can borrow (no unpaid fines)
        double unpaidFines = fineService.getTotalUnpaidAmount(userId);
        if (unpaidFines > 0) {
            System.out.println("❌ Error: User cannot borrow. Unpaid fines: $" + unpaidFines);
            System.out.println("Please pay all fines before borrowing.");
            return null;
        }

        // Check if user has any overdue items
        List<Loan> userActiveLoans = getUserActiveLoans(userId);
        boolean hasOverdue = userActiveLoans.stream()
                .anyMatch(Loan::isOverdue);

        if (hasOverdue) {
            System.out.println("❌ Error: User cannot borrow. There are overdue items that need to be returned first.");
            System.out.println("Please return all overdue items before borrowing new ones.");
            return null;
        }

        // Get the media item
        Media media = mediaRepository.findMediaByIdAndType(mediaId, mediaType);
        if (media == null) {
            System.out.println("Error: " + mediaType + " not found with ID: " + mediaId);
            return null;
        }

        if (!media.isAvailable()) {
            System.out.println("Error: " + mediaType + " is already borrowed.");
            return null;
        }

        // Create loan
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

            // Check if loan was overdue and apply fine
            if (returnDate.isAfter(loan.getDueDate())) {
                long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
                System.out.println("⚠️ This item was " + overdueDays + " days overdue.");

                // Apply flat fine based on media type
                String fineReason = "Overdue " + loan.getMediaType() + " (Loan: " + loanId + ") - " + overdueDays + " days overdue";

                // Use the new applyFine method with loanId
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
        // First check and apply any overdue fines
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