package com.library.model;

/**
 * Represents borrowing rules and restrictions in the library system
 * @author Library Team
 * @version 1.0
 */
public class BorrowingRules {
    private int maxBooksPerUser;
    private int loanPeriodDays;
    private double dailyFineRate;
    private boolean restrictBorrowingForOverdue;
    private boolean restrictBorrowingForUnpaidFines;

    public BorrowingRules() {
        // Default borrowing rules
        this.maxBooksPerUser = 5;
        this.loanPeriodDays = 28;
        this.dailyFineRate = 0.25; // $0.25 per day
        this.restrictBorrowingForOverdue = true;
        this.restrictBorrowingForUnpaidFines = true;
    }

    // Getters and setters
    public int getMaxBooksPerUser() { return maxBooksPerUser; }
    public void setMaxBooksPerUser(int maxBooksPerUser) { this.maxBooksPerUser = maxBooksPerUser; }

    public int getLoanPeriodDays() { return loanPeriodDays; }
    public void setLoanPeriodDays(int loanPeriodDays) { this.loanPeriodDays = loanPeriodDays; }

    public double getDailyFineRate() { return dailyFineRate; }
    public void setDailyFineRate(double dailyFineRate) { this.dailyFineRate = dailyFineRate; }

    public boolean isRestrictBorrowingForOverdue() { return restrictBorrowingForOverdue; }
    public void setRestrictBorrowingForOverdue(boolean restrictBorrowingForOverdue) {
        this.restrictBorrowingForOverdue = restrictBorrowingForOverdue;
    }

    public boolean isRestrictBorrowingForUnpaidFines() { return restrictBorrowingForUnpaidFines; }
    public void setRestrictBorrowingForUnpaidFines(boolean restrictBorrowingForUnpaidFines) {
        this.restrictBorrowingForUnpaidFines = restrictBorrowingForUnpaidFines;
    }

    /**
     * Calculates fine for overdue days
     * @param overdueDays number of days overdue
     * @return calculated fine amount
     */
    public double calculateFine(int overdueDays) {
        return overdueDays * dailyFineRate;
    }

    @Override
    public String toString() {
        return String.format("Max Books Per User: %d | Loan Period: %d days | Daily Fine Rate: $%.2f | " +
                        "Restrict for Overdue: %s | Restrict for Unpaid Fines: %s",
                maxBooksPerUser, loanPeriodDays, dailyFineRate,
                restrictBorrowingForOverdue ? "Yes" : "No",
                restrictBorrowingForUnpaidFines ? "Yes" : "No");
    }
}