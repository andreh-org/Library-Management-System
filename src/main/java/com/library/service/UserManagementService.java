package com.library.service;

import com.library.model.User;
import com.library.repository.FineRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for managing user registration and unregistration
 * @author Library Team
 * @version 1.1
 */
public class UserManagementService {
    private UserRepository userRepository;
    private LoanRepository loanRepository;
    private FineRepository fineRepository;
    private static final Logger logger = Logger.getLogger(UserManagementService.class.getName());

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
            logger.warning("Attempt to unregister user without admin access: " + userId);
            return result;
        }

        // Check if user exists
        User user = userRepository.findUserById(userId);
        if (user == null) {
            result.setSuccess(false);
            result.setMessage("❌ Error: User not found.");
            logger.warning("Attempt to unregister non-existent user: " + userId);
            return result;
        }

        // Check if user already inactive
        if (!user.isActive()) {
            result.setSuccess(false);
            result.setMessage("⚠️ User " + userId + " is already inactive.");
            logger.info("User " + userId + " is already inactive.");
            return result;
        }

        // Check if user has active loans
        boolean hasActiveLoans = loanRepository.getActiveLoans().stream()
                .anyMatch(loan -> loan.getUserId().equals(userId));

        if (hasActiveLoans) {
            result.setSuccess(false);
            result.setMessage("❌ Error: Cannot unregister user " + userId +
                    " because they have active loans.");
            logger.warning("Cannot unregister user " + userId + " - has active loans");
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
            logger.warning("Cannot unregister user " + userId + " - has unpaid fines: $" + unpaidFines);
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
            logger.info("User " + userId + " successfully unregistered.");
        } else {
            result.setSuccess(false);
            result.setMessage("❌ Error: Failed to update user record.");
            logger.severe("Failed to update user record for: " + userId);
        }

        return result;
    }

    /**
     * Gets all active users
     * @return list of active users
     */
    public List<User> getActiveUsers() {
        List<User> activeUsers = userRepository.getAllUsers().stream()
                .filter(User::isActive)
                .toList();
        logger.info("Retrieved " + activeUsers.size() + " active users");
        return activeUsers;
    }

    /**
     * Gets all inactive users
     * @return list of inactive users
     */
    public List<User> getInactiveUsers() {
        List<User> inactiveUsers = userRepository.getAllUsers().stream()
                .filter(user -> !user.isActive())
                .toList();
        logger.info("Retrieved " + inactiveUsers.size() + " inactive users");
        return inactiveUsers;
    }

    /**
     * Reactivates a user account (admin only)
     * @param userId the user ID to reactivate
     * @param authService authentication service to verify admin access
     * @return true if successful, false otherwise
     */
    public boolean reactivateUser(String userId, AuthService authService) {
        return reactivateUser(userId, authService, true);
    }

    /**
     * Reactivates a user account with option to check fines (admin only)
     * @param userId the user ID to reactivate
     * @param authService authentication service to verify admin access
     * @param checkFines whether to check for unpaid fines before reactivating
     * @return true if successful, false otherwise
     */
    public boolean reactivateUser(String userId, AuthService authService, boolean checkFines) {
        if (!authService.isLoggedIn()) {
            System.out.println("❌ Error: Admin login required to reactivate users.");
            logger.warning("Attempt to reactivate user without admin access: " + userId);
            return false;
        }

        User user = userRepository.findUserById(userId);
        if (user == null) {
            System.out.println("❌ Error: User not found.");
            logger.warning("Attempt to reactivate non-existent user: " + userId);
            return false;
        }

        if (user.isActive()) {
            System.out.println("⚠️ User " + userId + " is already active.");
            logger.info("User " + userId + " is already active.");
            return false;
        }

        // Check if user has any unpaid fines before reactivating
        double unpaidFines = 0;
        if (checkFines) {
            unpaidFines = fineRepository.getUnpaidFinesByUser(userId).stream()
                    .mapToDouble(fine -> fine.getRemainingBalance())
                    .sum();
        }

        user.setActive(true);
        user.setCanBorrow(unpaidFines == 0); // Can borrow only if no unpaid fines

        boolean updated = userRepository.updateUser(user);
        if (updated) {
            System.out.println("✅ User " + userId + " (" + user.getName() +
                    ") has been reactivated.");
            logger.info("User " + userId + " successfully reactivated.");

            if (unpaidFines > 0) {
                System.out.println("⚠️ Note: User still has unpaid fines of $" +
                        String.format("%.2f", unpaidFines) +
                        ". Borrowing is restricted until fines are paid.");
                logger.info("User " + userId + " reactivated with unpaid fines: $" + unpaidFines);
            }
            return true;
        } else {
            System.out.println("❌ Error: Failed to update user record.");
            logger.severe("Failed to update user record for reactivation: " + userId);
            return false;
        }
    }

    /**
     * Checks if a user can be unregistered without actually unregistering them
     * @param userId the user ID to check
     * @return ValidationResult with status and message
     */
    public ValidationResult canUserBeUnregistered(String userId) {
        ValidationResult result = new ValidationResult();

        // Check if user exists
        User user = userRepository.findUserById(userId);
        if (user == null) {
            result.setValid(false);
            result.setMessage("❌ Error: User not found.");
            return result;
        }

        // Check if user already inactive
        if (!user.isActive()) {
            result.setValid(false);
            result.setMessage("⚠️ User " + userId + " is already inactive.");
            return result;
        }

        // Check if user has active loans
        boolean hasActiveLoans = loanRepository.getActiveLoans().stream()
                .anyMatch(loan -> loan.getUserId().equals(userId));

        if (hasActiveLoans) {
            result.setValid(false);
            result.setMessage("❌ Error: User " + userId + " has active loans.");
            return result;
        }

        // Check if user has unpaid fines
        double unpaidFines = fineRepository.getUnpaidFinesByUser(userId).stream()
                .mapToDouble(fine -> fine.getRemainingBalance())
                .sum();

        if (unpaidFines > 0) {
            result.setValid(false);
            result.setMessage("❌ Error: User " + userId + " has unpaid fines of $" +
                    String.format("%.2f", unpaidFines));
            return result;
        }

        result.setValid(true);
        result.setMessage("✅ User " + userId + " can be unregistered.");
        return result;
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

    // Getters for testing
    public UserRepository getUserRepository() { return userRepository; }
    public LoanRepository getLoanRepository() { return loanRepository; }
    public FineRepository getFineRepository() { return fineRepository; }
}