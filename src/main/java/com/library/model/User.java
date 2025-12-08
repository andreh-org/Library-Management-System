package com.library.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user in the library system
 * @author Library Team
 * @version 1.1
 */
public class User {
    private String userId;
    private String name;
    private String email;
    private List<String> currentLoans;
    private boolean canBorrow;
    private boolean isActive; // User registration status
    private double unpaidFines; // NEW: Track unpaid fines

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.currentLoans = new ArrayList<>();
        this.canBorrow = true;
        this.isActive = true; // New users are active by default
        this.unpaidFines = 0.0; // Initialize with no fines
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<String> getCurrentLoans() { return currentLoans; }
    public void setCurrentLoans(List<String> currentLoans) { this.currentLoans = currentLoans; }

    public boolean canBorrow() {
        // User can borrow only if active AND no overdue books AND no unpaid fines
        return isActive && canBorrow && unpaidFines <= 0;
    }

    public void setCanBorrow(boolean canBorrow) {
        this.canBorrow = canBorrow;
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public double getUnpaidFines() { return unpaidFines; } // NEW GETTER
    public void setUnpaidFines(double unpaidFines) { this.unpaidFines = unpaidFines; } // NEW SETTER

    /**
     * Adds a fine to the user's unpaid balance
     * @param amount the amount to add (must be positive)
     */
    public void addFine(double amount) {
        if (amount > 0) {
            this.unpaidFines += amount;
            // Auto-update borrow permission based on fines
            if (this.unpaidFines > 0) {
                this.canBorrow = false;
            }
        }
    }

    /**
     * Pays a portion or all of the unpaid fines
     * @param amount the amount to pay (must be positive)
     * @return the remaining unpaid balance
     */
    public double payFine(double amount) {
        if (amount <= 0) {
            return this.unpaidFines;
        }

        if (amount >= this.unpaidFines) {
            this.unpaidFines = 0;
            this.canBorrow = true; // Restore borrow permission when fully paid
            return 0;
        } else {
            this.unpaidFines -= amount;
            return this.unpaidFines;
        }
    }

    public void addLoan(String loanId) {
        if (!currentLoans.contains(loanId)) {
            currentLoans.add(loanId);
        }
    }

    public void removeLoan(String loanId) {
        currentLoans.remove(loanId);
    }

    public boolean hasCurrentLoans() {
        return !currentLoans.isEmpty();
    }

    public boolean hasUnpaidFines() {
        return unpaidFines > 0;
    }

    /**
     * Checks if user can be unregistered based on project requirements
     * @return true if user has no active loans AND no unpaid fines, false otherwise
     */
    public boolean canBeUnregistered() {
        return !hasCurrentLoans() && !hasUnpaidFines();
    }

    @Override
    public String toString() {
        return String.format("ID: %-10s | Name: %-20s | Email: %-25s | Active: %s | Can Borrow: %s | Unpaid Fines: %.2f",
                userId, name, email,
                isActive ? "Yes" : "No",
                canBorrow() ? "Yes" : "No", // Use the method, not the field
                unpaidFines);
    }
}