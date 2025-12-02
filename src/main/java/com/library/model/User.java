package com.library.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user in the library system
 * @author Library Team
 * @version 1.0
 */
public class User {
    private String userId;
    private String name;
    private String email;
    private List<String> currentLoans;
    private boolean canBorrow;
    private boolean isActive; // NEW FIELD: User registration status

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.currentLoans = new ArrayList<>();
        this.canBorrow = true;
        this.isActive = true; // New users are active by default
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

    public boolean canBorrow() { return canBorrow; }
    public void setCanBorrow(boolean canBorrow) { this.canBorrow = canBorrow; }

    public boolean isActive() { return isActive; } // NEW GETTER
    public void setActive(boolean active) { isActive = active; } // NEW SETTER

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

    /**
     * Checks if user can be unregistered
     * @return true if user has no active loans and no unpaid fines, false otherwise
     */
    public boolean canBeUnregistered() {
        return !hasCurrentLoans();
    }

    @Override
    public String toString() {
        return String.format("ID: %-10s | Name: %-20s | Email: %-25s | Can Borrow: %s | Active: %s",
                userId, name, email, canBorrow ? "Yes" : "No", isActive ? "Yes" : "No");
    }
}