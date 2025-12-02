package com.library.service;

import com.library.model.BorrowingRules;
import com.library.model.Fine;
import com.library.model.Loan;
import com.library.model.User;
import com.library.repository.FineRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for handling borrowing rules and validations
 * @author Library Team
 * @version 1.0
 */
public class BorrowingRulesService {
    private BorrowingRules borrowingRules;
    private LoanRepository loanRepository;
    private FineRepository fineRepository;
    private UserRepository userRepository;

    public BorrowingRulesService(LoanRepository loanRepository, FineRepository fineRepository,
                                 UserRepository userRepository) {
        this.borrowingRules = new BorrowingRules();
        this.loanRepository = loanRepository;
        this.fineRepository = fineRepository;
        this.userRepository = userRepository;
    }

    public BorrowingRulesService() {
        this.borrowingRules = new BorrowingRules();
        this.loanRepository = new LoanRepository(); // FIXED: Removed .getLoanRepository()
        this.fineRepository = new FineRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * Checks if a user can borrow a new book
     * @param userId the user ID
     * @return ValidationResult with status and message
     */
    public ValidationResult canUserBorrow(String userId) {
        ValidationResult result = new ValidationResult();

        // Check if user exists and is active
        User user = userRepository.findUserById(userId);
        if (user == null) {
            result.setValid(false);
            result.setMessage("❌ Error: User not found.");
            return result;
        }

        if (!user.isActive()) {
            result.setValid(false);
            result.setMessage("❌ Error: User account is not active.");
            return result;
        }

        // Check max books per user
        List<Loan> activeLoans = loanRepository.getActiveLoans().stream()
                .filter(loan -> loan.getUserId().equals(userId))
                .toList();

        if (activeLoans.size() >= borrowingRules.getMaxBooksPerUser()) {
            result.setValid(false);
            result.setMessage("❌ Error: User has reached the maximum limit of " +
                    borrowingRules.getMaxBooksPerUser() + " books.");
            return result;
        }

        // Check for overdue books (if restriction is enabled)
        if (borrowingRules.isRestrictBorrowingForOverdue()) {
            boolean hasOverdue = activeLoans.stream()
                    .anyMatch(loan -> loan.checkOverdue(LocalDate.now()));

            if (hasOverdue) {
                result.setValid(false);
                result.setMessage("❌ Error: User has overdue books that must be returned first.");
                return result;
            }
        }

        // Check for unpaid fines (if restriction is enabled)
        if (borrowingRules.isRestrictBorrowingForUnpaidFines()) {
            double unpaidFines = fineRepository.getUnpaidFinesByUser(userId).stream()
                    .mapToDouble(Fine::getRemainingBalance)
                    .sum();

            if (unpaidFines > 0) {
                result.setValid(false);
                result.setMessage("❌ Error: User has unpaid fines of $" +
                        String.format("%.2f", unpaidFines) +
                        ". Please pay all fines before borrowing.");
                return result;
            }
        }

        // Check user's borrowing flag
        if (!user.canBorrow()) {
            result.setValid(false);
            result.setMessage("❌ Error: User account has borrowing restrictions.");
            return result;
        }

        result.setValid(true);
        result.setMessage("✅ User can borrow books.");
        return result;
    }

    /**
     * Gets the current borrowing rules
     * @return BorrowingRules object
     */
    public BorrowingRules getBorrowingRules() {
        return borrowingRules;
    }

    /**
     * Updates borrowing rules (admin only)
     * @param newRules the new borrowing rules
     * @param authService authentication service to verify admin access
     * @return true if successful, false otherwise
     */
    public boolean updateBorrowingRules(BorrowingRules newRules, AuthService authService) {
        if (!authService.isLoggedIn()) {
            System.out.println("❌ Error: Admin login required to update borrowing rules.");
            return false;
        }

        this.borrowingRules = newRules;
        System.out.println("✅ Borrowing rules updated successfully.");
        System.out.println("New rules: " + newRules);
        return true;
    }

    /**
     * Displays current borrowing rules
     */
    public void displayBorrowingRules() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("CURRENT BORROWING RULES");
        System.out.println("=".repeat(100));
        System.out.println(borrowingRules);
        System.out.println("=".repeat(100));
    }

    /**
     * Inner class to represent validation result
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;

        public ValidationResult() {
            this.valid = false;
            this.message = "";
        }

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        @Override
        public String toString() {
            return String.format("Valid: %s | Message: %s", valid ? "Yes" : "No", message);
        }
    }
}