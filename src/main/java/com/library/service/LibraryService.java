package com.library.service;

import com.library.model.*;
import com.library.repository.LoanRepository;
import com.library.repository.MediaRepository;
import com.library.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Main service for library operations
 * @author Library Team
 * @version 2.1
 */
public class LibraryService {
    private AuthService authService;
    private MediaService mediaService;
    private UserRepository userRepository;
    private LoanService loanService;
    private FineService fineService;
    private ReminderService reminderService;
    private UserManagementService userManagementService;
    private Scanner scanner;
    private static final Logger logger = Logger.getLogger(LibraryService.class.getName());

    // Dependency injection for better testability
    public LibraryService() {
        this(new AuthService(), new UserRepository(), new Scanner(System.in));
    }

    // Package-private constructor for testing with full dependency injection
    LibraryService(AuthService authService, UserRepository userRepository, Scanner scanner) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.scanner = scanner;

        // Create shared MediaRepository first
        MediaRepository sharedMediaRepository = new MediaRepository();
        this.mediaService = new MediaService(sharedMediaRepository);

        // Create FineService first (without LoanService dependency)
        this.fineService = new FineService(userRepository);

        // Create LoanService with the FineService
        this.loanService = new LoanService(fineService, userRepository, sharedMediaRepository);

        // Set the LoanService dependency in FineService
        this.fineService.setLoanService(loanService);

        // Create UserManagementService
        LoanRepository loanRepository = new LoanRepository(sharedMediaRepository);
        this.userManagementService = new UserManagementService(userRepository, loanRepository,
                fineService.getFineRepository());

        EmailService emailService = new EmailService();
        this.reminderService = new ReminderService(emailService, loanRepository, userRepository, true);

        logger.info("LibraryService initialized successfully");
    }

    /**
     * Constructor with Scanner only for backward compatibility
     */
    protected LibraryService(Scanner scanner) {
        this(new AuthService(), new UserRepository(), scanner);
        logger.info("LibraryService initialized with custom scanner for testing");
    }

    /**
     * Helper method to get integer input from user with validation
     * @return the integer input or -1 if invalid
     */
    protected int getIntInput() {
        return getIntInput("", -1);
    }

    /**
     * Overloaded helper method to get integer input with custom prompt and default value
     * @param prompt The prompt to display
     * @param defaultValue The default value to return on empty input
     * @return the integer input or defaultValue if empty/invalid
     */
    protected int getIntInput(String prompt, int defaultValue) {
        if (!prompt.isEmpty()) {
            System.out.print(prompt);
        }

        try {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            logger.warning("Invalid integer input: " + e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Helper method to get double input from user with validation
     * @return the double input or -1 if invalid
     */
    protected double getDoubleInput() {
        return getDoubleInput("", -1);
    }

    /**
     * Overloaded helper method to get double input with custom prompt and default value
     * @param prompt The prompt to display
     * @param defaultValue The default value to return on empty input
     * @return the double input or defaultValue if empty/invalid
     */
    protected double getDoubleInput(String prompt, double defaultValue) {
        if (!prompt.isEmpty()) {
            System.out.print(prompt);
        }

        try {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            logger.warning("Invalid double input: " + e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Helper method to get string input with validation
     * @param prompt The prompt to display
     * @param allowEmpty Whether empty input is allowed
     * @return the string input or null if validation fails
     */
    protected String getStringInput(String prompt, boolean allowEmpty) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();

        if (!allowEmpty && input.isEmpty()) {
            logger.warning("Empty input received when not allowed");
            return null;
        }

        return input;
    }

    /**
     * Helper method to get confirmation from user with more robust validation
     * @param prompt the confirmation prompt
     * @return true if confirmed, false otherwise
     */
    protected boolean getConfirmation(String prompt) {
        return getConfirmation(prompt, false);
    }

    /**
     * Overloaded helper method to get confirmation with default behavior
     * @param prompt the confirmation prompt
     * @param defaultValue the default value if empty input
     * @return true if confirmed, false otherwise (or defaultValue if empty)
     */
    protected boolean getConfirmation(String prompt, boolean defaultValue) {
        System.out.print(prompt + " (y/n)" + (defaultValue ? " [Y]: " : " [N]: "));
        String input = scanner.nextLine().trim().toLowerCase();

        if (input.isEmpty()) {
            return defaultValue;
        }

        return input.startsWith("y");
    }

    // The rest of the methods remain the same but will benefit from the improved helper methods...

    /**
     * Display simple mixed media overdue report (US5.3)
     */
    public void displayMixedMediaOverdueReport() {
        System.out.println("\n=== MIXED MEDIA OVERDUE REPORT ===");

        String userId = getStringInput("Enter User ID: ", false);
        if (userId == null) {
            System.out.println("❌ Error: User ID cannot be empty.");
            logger.warning("Empty user ID entered for mixed media report");
            return;
        }

        // Generate and display the simple report
        try {
            String report = loanService.getSimpleMixedMediaReport(userId, LocalDate.now());
            System.out.println(report);
            logger.info("Mixed media report generated for user: " + userId);
        } catch (Exception e) {
            System.out.println("❌ Error generating report: " + e.getMessage());
            logger.severe("Error generating mixed media report for user " + userId + ": " + e.getMessage());
        }
    }

    public void payFine() {
        System.out.println("\n=== PAY FINE ===");

        String userId = getStringInput("Enter User ID: ", false);
        if (userId == null) {
            System.out.println("❌ Error: User ID cannot be empty.");
            logger.warning("Empty user ID entered for pay fine");
            return;
        }

        // First, check and apply any overdue fines
        loanService.checkAndApplyOverdueFines(userId, LocalDate.now());

        fineService.displayUserFines(userId);

        List<Fine> unpaidFines = fineService.getUserUnpaidFines(userId);
        if (unpaidFines.isEmpty()) {
            System.out.println("✅ No unpaid fines found.");
            logger.info("No unpaid fines found for user: " + userId);
            return;
        }

        String fineId = getStringInput("Enter Fine ID to pay: ", false);
        if (fineId == null) {
            System.out.println("❌ Error: Fine ID cannot be empty.");
            return;
        }

        double paymentAmount = getDoubleInput("Enter payment amount: ", -1);
        if (paymentAmount <= 0) {
            System.out.println("❌ Error: Payment amount must be positive.");
            return;
        }

        try {
            boolean success = fineService.payFine(fineId, paymentAmount);
            if (success) {
                System.out.println("✅ Payment successful!");
                logger.info("Payment processed successfully for fine " + fineId);
            } else {
                System.out.println("❌ Payment failed.");
                logger.warning("Payment failed for fine " + fineId);
            }
        } catch (Exception e) {
            System.out.println("❌ Error processing payment: " + e.getMessage());
            logger.severe("Error processing payment for fine " + fineId + ": " + e.getMessage());
        }
    }

    public void displayUserLoans() {
        System.out.println("\n=== USER LOANS ===");

        String userId = getStringInput("Enter User ID: ", false);
        if (userId == null) {
            System.out.println("❌ Error: User ID cannot be empty.");
            return;
        }

        List<Loan> userLoans = loanService.getUserActiveLoans(userId);
        System.out.println("\n" + "=".repeat(100));
        System.out.println("ACTIVE LOANS FOR USER: " + userId);
        System.out.println("=".repeat(100));

        if (userLoans.isEmpty()) {
            System.out.println("No active loans found.");
            logger.info("No active loans found for user: " + userId);
        } else {
            for (Loan loan : userLoans) {
                System.out.println(loan);
            }

            long overdueCount = userLoans.stream().filter(Loan::isOverdue).count();
            if (overdueCount > 0) {
                System.out.println("\n⚠️ User has " + overdueCount + " overdue item(s) that must be returned:");
                System.out.println("1. Return all overdue items first");
                System.out.println("2. Then you can pay fines for returned items");
                System.out.println("3. Only after all fines are paid can you borrow new items");
                logger.info("User " + userId + " has " + overdueCount + " overdue items");
            }
        }
        System.out.println("=".repeat(100));
    }

    /**
     * Display all users (admin only)
     */
    public void displayAllUsers() {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to view users.");
            logger.warning("Attempt to view all users without admin access");
            return;
        }

        List<User> users = userRepository.getAllUsers();
        System.out.println("\n" + "=".repeat(80));
        System.out.println("REGISTERED USERS (ALL)");
        System.out.println("=".repeat(80));

        if (users.isEmpty()) {
            System.out.println("No users registered in the system.");
            logger.info("No users found in the system");
        } else {
            for (int i = 0; i < users.size(); i++) {
                System.out.println((i + 1) + ". " + users.get(i));
            }
            logger.info("Displayed " + users.size() + " users");
        }
        System.out.println("=".repeat(80));
    }

    /**
     * Send overdue reminders
     */
    public void sendOverdueReminders() {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to send reminders.");
            logger.warning("Attempt to send reminders without admin access");
            return;
        }

        System.out.println("\n=== SEND OVERDUE REMINDERS ===");
        System.out.println("1. Send reminder to specific user");
        System.out.println("2. Send reminders to all users with overdue items");
        System.out.println("3. Cancel");

        int choice = getIntInput("Choose an option: ", 3);
        switch (choice) {
            case 1:
                sendReminderToSpecificUser();
                break;
            case 2:
                boolean confirmed = getConfirmation("Send reminders to ALL users with overdue items?", false);
                if (confirmed) {
                    reminderService.sendOverdueRemindersToAllUsers();
                    System.out.println("Reminders sent to all users with overdue items.");
                    logger.info("Reminders sent to all users with overdue items");
                } else {
                    System.out.println("Operation cancelled.");
                    logger.info("Reminder sending cancelled by user");
                }
                break;
            case 3:
                System.out.println("Operation cancelled.");
                logger.info("Reminder sending cancelled");
                break;
            default:
                System.out.println("Invalid option.");
                logger.warning("Invalid option selected in sendOverdueReminders: " + choice);
        }
    }

    private void sendReminderToSpecificUser() {
        String userId = getStringInput("Enter User ID: ", false);
        if (userId == null) {
            System.out.println("❌ Error: User ID cannot be empty.");
            return;
        }

        List<Loan> userActiveLoans = loanService.getUserActiveLoans(userId);
        long overdueCount = userActiveLoans.stream()
                .filter(Loan::isOverdue)
                .count();

        if (overdueCount == 0) {
            System.out.println("User " + userId + " has no overdue items.");
            logger.info("User " + userId + " has no overdue items");
            return;
        }

        System.out.println("User " + userId + " has " + overdueCount + " overdue item(s).");

        if (getConfirmation("Send reminder to user " + userId + "?", false)) {
            reminderService.sendOverdueReminderToUser(userId, (int) overdueCount);
            System.out.println("Reminder sent successfully!");
            logger.info("Reminder sent to user " + userId + " for " + overdueCount + " overdue items");
        } else {
            System.out.println("Operation cancelled.");
            logger.info("Reminder sending cancelled for user " + userId);
        }
    }

    /**
     * Manage users (admin only)
     */
    public void manageUsers() {
        if (!authService.isLoggedIn()) {
            System.out.println("❌ Error: Admin login required to manage users.");
            logger.warning("Attempt to manage users without admin access");
            return;
        }

        boolean continueManaging = true;
        while (continueManaging) {
            System.out.println("\n=== USER MANAGEMENT ===");
            System.out.println("1. Unregister user");
            System.out.println("2. View active users");
            System.out.println("3. View inactive users");
            System.out.println("4. Reactivate user");
            System.out.println("5. Check if user can be unregistered");
            System.out.println("6. Back to main menu");

            int choice = getIntInput("Choose an option: ", 6);
            switch (choice) {
                case 1:
                    unregisterUser();
                    break;
                case 2:
                    displayActiveUsers();
                    break;
                case 3:
                    displayInactiveUsers();
                    break;
                case 4:
                    reactivateUser();
                    break;
                case 5:
                    checkUserUnregistration();
                    break;
                case 6:
                    System.out.println("Returning to main menu...");
                    continueManaging = false;
                    break;
                default:
                    System.out.println("Invalid option.");
                    logger.warning("Invalid option selected in manageUsers: " + choice);
            }
        }
    }

    private void unregisterUser() {
        System.out.println("\n=== UNREGISTER USER ===");

        // Display active users first
        List<User> activeUsers = userManagementService.getActiveUsers();
        if (activeUsers.isEmpty()) {
            System.out.println("No active users found.");
            logger.info("No active users found for unregistration");
            return;
        }

        System.out.println("\nActive Users:");
        System.out.println("=".repeat(80));
        for (int i = 0; i < activeUsers.size(); i++) {
            System.out.println((i + 1) + ". " + activeUsers.get(i));
        }
        System.out.println("=".repeat(80));

        String userId = getStringInput("Enter User ID to unregister: ", false);
        if (userId == null) {
            System.out.println("❌ Error: User ID cannot be empty.");
            return;
        }

        // Check if user can be unregistered first
        UserManagementService.ValidationResult validation =
                userManagementService.canUserBeUnregistered(userId);

        if (!validation.isValid()) {
            System.out.println(validation.getMessage());
            logger.warning("User " + userId + " cannot be unregistered: " + validation.getMessage());
            return;
        }

        if (getConfirmation("Are you sure you want to unregister user " + userId + "?", false)) {
            UserManagementService.UnregistrationResult result =
                    userManagementService.unregisterUser(userId, authService);
            System.out.println(result.getMessage());
            logger.info("Unregistration result for user " + userId + ": " + result.getMessage());
        } else {
            System.out.println("Operation cancelled.");
            logger.info("Unregistration cancelled for user " + userId);
        }
    }

    private void displayActiveUsers() {
        List<User> activeUsers = userManagementService.getActiveUsers();
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ACTIVE USERS");
        System.out.println("=".repeat(80));

        if (activeUsers.isEmpty()) {
            System.out.println("No active users found.");
            logger.info("No active users found");
        } else {
            for (int i = 0; i < activeUsers.size(); i++) {
                System.out.println((i + 1) + ". " + activeUsers.get(i));
            }
            logger.info("Displayed " + activeUsers.size() + " active users");
        }
        System.out.println("=".repeat(80));
    }

    private void displayInactiveUsers() {
        List<User> inactiveUsers = userManagementService.getInactiveUsers();
        System.out.println("\n" + "=".repeat(80));
        System.out.println("INACTIVE USERS");
        System.out.println("=".repeat(80));

        if (inactiveUsers.isEmpty()) {
            System.out.println("No inactive users found.");
            logger.info("No inactive users found");
        } else {
            for (int i = 0; i < inactiveUsers.size(); i++) {
                System.out.println((i + 1) + ". " + inactiveUsers.get(i));
            }
            logger.info("Displayed " + inactiveUsers.size() + " inactive users");
        }
        System.out.println("=".repeat(80));
    }

    private void reactivateUser() {
        System.out.println("\n=== REACTIVATE USER ===");

        List<User> inactiveUsers = userManagementService.getInactiveUsers();
        if (inactiveUsers.isEmpty()) {
            System.out.println("No inactive users found.");
            logger.info("No inactive users found for reactivation");
            return;
        }

        System.out.println("\nInactive Users:");
        System.out.println("=".repeat(80));
        for (int i = 0; i < inactiveUsers.size(); i++) {
            System.out.println((i + 1) + ". " + inactiveUsers.get(i));
        }
        System.out.println("=".repeat(80));

        String userId = getStringInput("Enter User ID to reactivate: ", false);
        if (userId == null) {
            System.out.println("❌ Error: User ID cannot be empty.");
            return;
        }

        boolean checkFines = getConfirmation("Check for unpaid fines before reactivating?", true);
        boolean success = userManagementService.reactivateUser(userId, authService, checkFines);

        if (success) {
            System.out.println("✅ User reactivated successfully!");
            logger.info("User " + userId + " reactivated successfully");
        } else {
            System.out.println("❌ Failed to reactivate user.");
            logger.warning("Failed to reactivate user " + userId);
        }
    }

    private void checkUserUnregistration() {
        System.out.println("\n=== CHECK USER UNREGISTRATION ===");

        String userId = getStringInput("Enter User ID to check: ", false);
        if (userId == null) {
            System.out.println("❌ Error: User ID cannot be empty.");
            return;
        }

        UserManagementService.ValidationResult result =
                userManagementService.canUserBeUnregistered(userId);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("UNREGISTRATION CHECK FOR USER: " + userId);
        System.out.println("=".repeat(80));
        System.out.println(result.getMessage());
        System.out.println("=".repeat(80));

        logger.info("Unregistration check for user " + userId + ": " + result.getMessage());
    }

    /**
     * Borrow media (book or CD)
     */
    public void borrowMedia() {
        System.out.println("\n=== BORROW MEDIA ===");
        System.out.println("1. Borrow Book");
        System.out.println("2. Borrow CD");
        System.out.println("3. Cancel");

        int mediaTypeChoice = getIntInput("Choose media type: ", 3);
        if (mediaTypeChoice == 3) {
            System.out.println("Operation cancelled.");
            logger.info("Borrow media operation cancelled");
            return;
        }

        if (mediaTypeChoice < 1 || mediaTypeChoice > 2) {
            System.out.println("Invalid choice.");
            logger.warning("Invalid media type choice: " + mediaTypeChoice);
            return;
        }

        String userId = getStringInput("Enter User ID: ", false);
        if (userId == null) {
            System.out.println("❌ Error: User ID cannot be empty.");
            return;
        }

        User user = userRepository.findUserById(userId);
        if (user == null) {
            System.out.println("Error: User not found.");
            logger.warning("User not found for borrowing: " + userId);
            return;
        }

        // Check if user is active
        if (!user.isActive()) {
            System.out.println("❌ Error: User account is not active.");
            System.out.println("Please contact administrator to reactivate your account.");
            logger.warning("Inactive user attempted to borrow: " + userId);
            return;
        }

        // Check if user can borrow (no unpaid fines)
        if (!user.canBorrow()) {
            System.out.println("❌ Error: User cannot borrow new items.");
            System.out.println("Reason: User has unpaid fines or other restrictions.");
            logger.warning("User " + userId + " attempted to borrow but has restrictions");
            return;
        }

        if (mediaTypeChoice == 1) {
            // Borrow book
            String isbn = getStringInput("Enter Book ISBN: ", false);
            if (isbn == null) {
                System.out.println("❌ Error: ISBN cannot be empty.");
                return;
            }
            borrowBook(userId, isbn);
        } else {
            // Borrow CD
            String catalogNumber = getStringInput("Enter CD Catalog Number: ", false);
            if (catalogNumber == null) {
                System.out.println("❌ Error: Catalog number cannot be empty.");
                return;
            }
            borrowCD(userId, catalogNumber);
        }
    }

    /**
     * Borrow a book
     */
    private void borrowBook(String userId, String isbn) {
        try {
            Loan loan = loanService.borrowBook(userId, isbn, LocalDate.now());
            if (loan != null) {
                System.out.println("✅ Book borrowed successfully!");
                System.out.println("Due date: " + loan.getDueDate());
                System.out.println("Loan period: 28 days");
                logger.info("Book borrowed successfully: user=" + userId + ", isbn=" + isbn + ", loan=" + loan.getLoanId());
            } else {
                System.out.println("❌ Failed to borrow book.");
                logger.warning("Book borrowing failed: user=" + userId + ", isbn=" + isbn);
            }
        } catch (Exception e) {
            System.out.println("❌ Error borrowing book: " + e.getMessage());
            logger.severe("Error borrowing book for user " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Borrow a CD
     */
    private void borrowCD(String userId, String catalogNumber) {
        try {
            Loan loan = loanService.borrowCD(userId, catalogNumber, LocalDate.now());
            if (loan != null) {
                System.out.println("✅ CD borrowed successfully!");
                System.out.println("Due date: " + loan.getDueDate());
                System.out.println("Loan period: 7 days");
                logger.info("CD borrowed successfully: user=" + userId + ", catalog=" + catalogNumber + ", loan=" + loan.getLoanId());
            } else {
                System.out.println("❌ Failed to borrow CD.");
                logger.warning("CD borrowing failed: user=" + userId + ", catalog=" + catalogNumber);
            }
        } catch (Exception e) {
            System.out.println("❌ Error borrowing CD: " + e.getMessage());
            logger.severe("Error borrowing CD for user " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Add new media (book or CD)
     */
    public void addNewMedia() {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to add media.");
            logger.warning("Attempt to add media without admin access");
            return;
        }

        System.out.println("\n=== ADD NEW MEDIA ===");
        System.out.println("1. Add Book");
        System.out.println("2. Add CD");
        System.out.println("3. Cancel");

        int choice = getIntInput("Choose media type: ", 3);
        switch (choice) {
            case 1:
                addNewBook();
                break;
            case 2:
                addNewCD();
                break;
            case 3:
                System.out.println("Operation cancelled.");
                logger.info("Add media operation cancelled");
                break;
            default:
                System.out.println("Invalid choice.");
                logger.warning("Invalid choice in addNewMedia: " + choice);
        }
    }

    /**
     * Add a new book
     */
    private void addNewBook() {
        System.out.println("\n=== ADD NEW BOOK ===");

        String title = getStringInput("Enter book title: ", false);
        String author = getStringInput("Enter author: ", false);
        String isbn = getStringInput("Enter ISBN: ", false);

        if (title == null || author == null || isbn == null) {
            System.out.println("Error: All fields are required.");
            logger.warning("Empty fields when adding book");
            return;
        }

        if (!getConfirmation("Add book '" + title + "' by " + author + "?", false)) {
            System.out.println("Operation cancelled.");
            logger.info("Book addition cancelled: " + title);
            return;
        }

        boolean success = mediaService.addBook(title, author, isbn, authService);
        if (success) {
            System.out.println("✅ Book added successfully!");
            logger.info("Book added successfully: " + title + " by " + author + " (ISBN: " + isbn + ")");
        } else {
            System.out.println("❌ Failed to add book.");
            logger.warning("Failed to add book: " + title);
        }
    }

    /**
     * Add a new CD
     */
    private void addNewCD() {
        System.out.println("\n=== ADD NEW CD ===");

        String title = getStringInput("Enter CD title: ", false);
        String artist = getStringInput("Enter artist: ", false);
        String catalogNumber = getStringInput("Enter catalog number: ", false);
        String genre = getStringInput("Enter genre: ", false);
        int trackCount = getIntInput("Enter track count: ", -1);

        if (title == null || artist == null || catalogNumber == null || genre == null || trackCount <= 0) {
            System.out.println("Error: All fields are required and track count must be positive.");
            logger.warning("Invalid fields when adding CD");
            return;
        }

        if (!getConfirmation("Add CD '" + title + "' by " + artist + "?", false)) {
            System.out.println("Operation cancelled.");
            logger.info("CD addition cancelled: " + title);
            return;
        }

        boolean success = mediaService.addCD(title, artist, catalogNumber, genre, trackCount, authService);
        if (success) {
            System.out.println("✅ CD added successfully!");
            logger.info("CD added successfully: " + title + " by " + artist + " (Catalog: " + catalogNumber + ")");
        } else {
            System.out.println("❌ Failed to add CD.");
            logger.warning("Failed to add CD: " + title);
        }
    }

    /**
     * Search media
     */
    public void searchMedia() {
        String query = getStringInput("\nEnter search query (title, author, or ID): ", true);
        if (query == null || query.isEmpty()) {
            System.out.println("Search query cannot be empty.");
            logger.warning("Empty search query");
            return;
        }

        List<Media> results = mediaService.searchMedia(query);

        System.out.println("\n" + "=".repeat(120));
        System.out.println("SEARCH RESULTS for: '" + query + "'");
        System.out.println("=".repeat(120));

        if (results.isEmpty()) {
            System.out.println("No media found matching your search.");
            logger.info("No search results for query: " + query);
        } else {
            System.out.println("Found " + results.size() + " result(s):");
            for (int i = 0; i < results.size(); i++) {
                System.out.println((i + 1) + ". " + results.get(i));
            }
            logger.info("Search found " + results.size() + " results for query: " + query);
        }
        System.out.println("=".repeat(120));
    }

    /**
     * Display all media
     */
    public void displayAllMedia() {
        try {
            mediaService.displayAllMedia();
            logger.info("Displayed all media");
        } catch (Exception e) {
            System.out.println("❌ Error displaying media: " + e.getMessage());
            logger.severe("Error displaying all media: " + e.getMessage());
        }
    }

    /**
     * Display all books
     */
    public void displayAllBooks() {
        try {
            mediaService.displayAllBooks();
            logger.info("Displayed all books");
        } catch (Exception e) {
            System.out.println("❌ Error displaying books: " + e.getMessage());
            logger.severe("Error displaying all books: " + e.getMessage());
        }
    }

    /**
     * Display all CDs
     */
    public void displayAllCDs() {
        try {
            mediaService.displayAllCDs();
            logger.info("Displayed all CDs");
        } catch (Exception e) {
            System.out.println("❌ Error displaying CDs: " + e.getMessage());
            logger.severe("Error displaying all CDs: " + e.getMessage());
        }
    }

    /**
     * Return media
     */
    public void returnBook() {
        System.out.println("\n=== RETURN MEDIA ===");

        String loanId = getStringInput("Enter Loan ID: ", false);
        if (loanId == null) {
            System.out.println("❌ Error: Loan ID cannot be empty.");
            return;
        }

        if (!getConfirmation("Return media for loan " + loanId + "?", false)) {
            System.out.println("Operation cancelled.");
            logger.info("Media return cancelled for loan: " + loanId);
            return;
        }

        try {
            boolean success = loanService.returnBook(loanId, LocalDate.now());
            if (success) {
                System.out.println("✅ Media returned successfully!");
                logger.info("Media returned successfully for loan: " + loanId);
            } else {
                System.out.println("❌ Failed to return media.");
                logger.warning("Media return failed for loan: " + loanId);
            }
        } catch (Exception e) {
            System.out.println("❌ Error returning media: " + e.getMessage());
            logger.severe("Error returning media for loan " + loanId + ": " + e.getMessage());
        }
    }

    /**
     * Display overdue items
     */
    public void displayOverdueBooks() {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to view overdue items.");
            logger.warning("Attempt to view overdue items without admin access");
            return;
        }

        try {
            List<Loan> overdueLoans = loanService.getOverdueLoans(LocalDate.now());
            System.out.println("\n" + "=".repeat(120));
            System.out.println("OVERDUE ITEMS (ALL USERS)");
            System.out.println("=".repeat(120));

            if (overdueLoans.isEmpty()) {
                System.out.println("No overdue items found.");
                logger.info("No overdue items found");
            } else {
                double totalFines = 0;
                for (Loan loan : overdueLoans) {
                    System.out.println(loan);
                    totalFines += loan.calculateFine(LocalDate.now());
                }
                System.out.println("-".repeat(120));
                System.out.println(String.format("TOTAL OVERDUE FINES: $%.2f", totalFines));
                System.out.println("Found " + overdueLoans.size() + " overdue item(s)");
                logger.info("Displayed " + overdueLoans.size() + " overdue items with total fines: $" + totalFines);
            }
            System.out.println("=".repeat(120));
        } catch (Exception e) {
            System.out.println("❌ Error displaying overdue items: " + e.getMessage());
            logger.severe("Error displaying overdue items: " + e.getMessage());
        }
    }

    // Getters for testing
    public AuthService getAuthService() { return authService; }
    public MediaService getMediaService() { return mediaService; }
    public UserRepository getUserRepository() { return userRepository; }
    public LoanService getLoanService() { return loanService; }
    public FineService getFineService() { return fineService; }
    public ReminderService getReminderService() { return reminderService; }
    public UserManagementService getUserManagementService() { return userManagementService; }
    public Scanner getScanner() { return scanner; }

    // Package-private setters for testing
    void setAuthService(AuthService authService) { this.authService = authService; }
    void setMediaService(MediaService mediaService) { this.mediaService = mediaService; }
    void setLoanService(LoanService loanService) { this.loanService = loanService; }
    void setFineService(FineService fineService) { this.fineService = fineService; }
    void setReminderService(ReminderService reminderService) { this.reminderService = reminderService; }
    void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }
}