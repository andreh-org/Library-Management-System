package com.library.service;

import com.library.model.User;
import com.library.repository.FineRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import java.util.List;

/**
 * Service for managing user registration and unregistration
 * @author Library Team
 * @version 1.0
 */
public class UserManagementService {
    private UserRepository userRepository;
    private LoanRepository loanRepository;
    private FineRepository fineRepository;

    public UserManagementService(UserRepository userRepository, LoanRepository loanRepository,
                                 FineRepository fineRepository) {
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
        this.fineRepository = fineRepository;
    }

    public UserManagementService() {
        this.userRepository = new UserRepository();
        this.loanRepository = new LoanRepository();
        this.fineRepository = new FineRepository();
    }

    /**
     * Unregisters a user (admin only)
     * @param userId the user ID to unregister
     * @param authService authentication service to verify admin access
     * @return UnregistrationResult with status and message
     */
    public UnregistrationResult unregisterUser(String userId, AuthService authService) {
        UnregistrationResult result = new UnregistrationResult();

        // Check admin authentication
        if (!authService.isLoggedIn()) {
            result.setSuccess(false);
            result.setMessage("❌ Error: Admin login required to unregister users.");
            return result;
        }

        // Check if user exists
        User user = userRepository.findUserById(userId);
        if (user == null) {
            result.setSuccess(false);
            result.setMessage("❌ Error: User not found.");
            return result;
        }

        // Check if user already inactive
        if (!user.isActive()) {
            result.setSuccess(false);
            result.setMessage("⚠️ User " + userId + " is already inactive.");
            return result;
        }

        // Check if user has active loans
        boolean hasActiveLoans = loanRepository.getActiveLoans().stream()
                .anyMatch(loan -> loan.getUserId().equals(userId));

        if (hasActiveLoans) {
            result.setSuccess(false);
            result.setMessage("❌ Error: Cannot unregister user " + userId +
                    " because they have active loans.");
            return result;
        }

        // Check if user has unpaid fines
        double unpaidFines = fineRepository.getUnpaidFinesByUser(userId).stream()
                .mapToDouble(fine -> fine.getRemainingBalance())
                .sum();

        if (unpaidFines > 0) {
            result.setSuccess(false);
            result.setMessage("❌ Error: Cannot unregister user " + userId +
                    " because they have unpaid fines of $" +
                    String.format("%.2f", unpaidFines));
            return result;
        }

        // All checks passed - unregister the user
        user.setActive(false);
        user.setCanBorrow(false);
        boolean updated = userRepository.updateUser(user);

        if (updated) {
            result.setSuccess(true);
            result.setMessage("✅ User " + userId + " (" + user.getName() +
                    ") has been successfully unregistered.");
        } else {
            result.setSuccess(false);
            result.setMessage("❌ Error: Failed to update user record.");
        }

        return result;
    }

    /**
     * Gets all active users
     * @return list of active users
     */
    public List<User> getActiveUsers() {
        return userRepository.getAllUsers().stream()
                .filter(User::isActive)
                .toList();
    }

    /**
     * Gets all inactive users
     * @return list of inactive users
     */
    public List<User> getInactiveUsers() {
        return userRepository.getAllUsers().stream()
                .filter(user -> !user.isActive())
                .toList();
    }

    /**
     * Reactivates a user account (admin only)
     * @param userId the user ID to reactivate
     * @param authService authentication service to verify admin access
     * @return true if successful, false otherwise
     */
    public boolean reactivateUser(String userId, AuthService authService) {
        if (!authService.isLoggedIn()) {
            System.out.println("❌ Error: Admin login required to reactivate users.");
            return false;
        }

        User user = userRepository.findUserById(userId);
        if (user == null) {
            System.out.println("❌ Error: User not found.");
            return false;
        }

        if (user.isActive()) {
            System.out.println("⚠️ User " + userId + " is already active.");
            return false;
        }

        // Check if user has any unpaid fines before reactivating
        double unpaidFines = fineRepository.getUnpaidFinesByUser(userId).stream()
                .mapToDouble(fine -> fine.getRemainingBalance())
                .sum();

        user.setActive(true);
        user.setCanBorrow(unpaidFines == 0); // Can borrow only if no unpaid fines

        boolean updated = userRepository.updateUser(user);
        if (updated) {
            System.out.println("✅ User " + userId + " (" + user.getName() +
                    ") has been reactivated.");
            if (unpaidFines > 0) {
                System.out.println("⚠️ Note: User still has unpaid fines of $" +
                        String.format("%.2f", unpaidFines) +
                        ". Borrowing is restricted until fines are paid.");
            }
            return true;
        }

        return false;
    }

    /**
     * Inner class to represent unregistration result
     */
    public static class UnregistrationResult {
        private boolean success;
        private String message;

        public UnregistrationResult() {
            this.success = false;
            this.message = "";
        }

        public UnregistrationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        @Override
        public String toString() {
            return String.format("Success: %s | Message: %s", success ? "Yes" : "No", message);
        }
    }
}