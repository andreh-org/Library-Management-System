package com.library.model;

/**
 * Represents borrowing rules and restrictions in the library system
 * @author Library Team
 * @version 1.0
 */
public class BorrowingRules {
    private int maxBooksPerUser;
    private int loanPeriodDays;
    private boolean restrictBorrowingForOverdue;
    private boolean restrictBorrowingForUnpaidFines;

    public BorrowingRules() {
        // Default borrowing rules
        this.maxBooksPerUser = 5;
        this.loanPeriodDays = 28;
        this.restrictBorrowingForOverdue = true;
        this.restrictBorrowingForUnpaidFines = true;
    }

    // Getters and setters
    public int getMaxBooksPerUser() { return maxBooksPerUser; }
    public void setMaxBooksPerUser(int maxBooksPerUser) { this.maxBooksPerUser = maxBooksPerUser; }

    public int getLoanPeriodDays() { return loanPeriodDays; }
    public void setLoanPeriodDays(int loanPeriodDays) { this.loanPeriodDays = loanPeriodDays; }

    // Remove getDailyFineRate() and setDailyFineRate()

    public boolean isRestrictBorrowingForOverdue() { return restrictBorrowingForOverdue; }
    public void setRestrictBorrowingForOverdue(boolean restrictBorrowingForOverdue) {
        this.restrictBorrowingForOverdue = restrictBorrowingForOverdue;
    }

    public boolean isRestrictBorrowingForUnpaidFines() { return restrictBorrowingForUnpaidFines; }
    public void setRestrictBorrowingForUnpaidFines(boolean restrictBorrowingForUnpaidFines) {
        this.restrictBorrowingForUnpaidFines = restrictBorrowingForUnpaidFines;
    }

    // Remove calculateFine() method since we're using flat fines via Strategy Pattern

    @Override
    public String toString() {
        return String.format("Max Books Per User: %d | Loan Period: %d days | " +
                        "Restrict for Overdue: %s | Restrict for Unpaid Fines: %s",
                maxBooksPerUser, loanPeriodDays,
                restrictBorrowingForOverdue ? "Yes" : "No",
                restrictBorrowingForUnpaidFines ? "Yes" : "No");
    }
}