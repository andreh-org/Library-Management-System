package com.library.service;

import com.library.model.Book;
import com.library.model.Fine;
import com.library.model.Loan;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import static com.library.Main.getIntInput;

/**
 * Main service for library operations
 * @author Library Team
 * @version 1.0
 */
public class LibraryService {
    private AuthService authService;
    private BookService bookService;
    private UserRepository userRepository;
    private LoanService loanService;
    private FineService fineService;
    private ReminderService reminderService;
    private UserManagementService userManagementService;
    private Scanner scanner;

    public LibraryService() {
        this.authService = new AuthService();
        this.userRepository = new UserRepository();

        // Create shared BookRepository first
        BookRepository sharedBookRepository = new BookRepository();
        this.bookService = new BookService(sharedBookRepository);

        // Create FineService first (without LoanService dependency)
        this.fineService = new FineService(userRepository);

        // Create LoanService with the FineService
        this.loanService = new LoanService(fineService, userRepository, sharedBookRepository);

        // Set the LoanService dependency in FineService
        this.fineService.setLoanService(loanService);

        // Create UserManagementService
        LoanRepository loanRepository = new LoanRepository(sharedBookRepository);
        this.userManagementService = new UserManagementService(userRepository, loanRepository,
                fineService.getFineRepository());

        EmailService emailService = new EmailService();
        this.reminderService = new ReminderService(emailService, loanRepository, userRepository);

        this.scanner = new Scanner(System.in);
    }

    public void sendOverdueReminders() {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to send reminders.");
            return;
        }

        System.out.println("\n=== SEND OVERDUE REMINDERS ===");
        System.out.println("1. Send reminder to specific user");
        System.out.println("2. Cancel");
        System.out.print("Choose an option: ");

        int choice = getIntInput();
        switch (choice) {
            case 1:
                sendReminderToSpecificUser();
                break;
            case 2:
                System.out.println("Operation cancelled.");
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private void sendReminderToSpecificUser() {
        System.out.print("Enter User ID: ");
        String userId = scanner.nextLine().trim();

        // Get user's overdue books count
        List<Loan> userActiveLoans = loanService.getUserActiveLoans(userId);
        long overdueCount = userActiveLoans.stream()
                .filter(Loan::isOverdue)
                .count();

        if (overdueCount == 0) {
            System.out.println("User " + userId + " has no overdue books.");
            return;
        }

        System.out.println("User " + userId + " has " + overdueCount + " overdue book(s).");
        System.out.print("Send reminder? (y/n): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();

        if (confirmation.equals("y") || confirmation.equals("yes")) {
            reminderService.sendOverdueReminderToUser(userId, (int) overdueCount);
            System.out.println("Reminder sent successfully!");
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    // User Management method
    public void manageUsers() {
        if (!authService.isLoggedIn()) {
            System.out.println("❌ Error: Admin login required to manage users.");
            return;
        }

        System.out.println("\n=== USER MANAGEMENT ===");
        System.out.println("1. Unregister user");
        System.out.println("2. View active users");
        System.out.println("3. View inactive users");
        System.out.println("4. Reactivate user");
        System.out.println("5. Back to main menu");
        System.out.print("Choose an option: ");

        int choice = getIntInput();
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
                System.out.println("Returning to main menu...");
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private void unregisterUser() {
        System.out.println("\n=== UNREGISTER USER ===");

        // Display active users first
        List<User> activeUsers = userManagementService.getActiveUsers();
        if (activeUsers.isEmpty()) {
            System.out.println("No active users found.");
            return;
        }

        System.out.println("\nActive Users:");
        System.out.println("=".repeat(80));
        for (int i = 0; i < activeUsers.size(); i++) {
            System.out.println((i + 1) + ". " + activeUsers.get(i));
        }
        System.out.println("=".repeat(80));

        System.out.print("Enter User ID to unregister: ");
        String userId = scanner.nextLine().trim();

        System.out.print("Are you sure you want to unregister user " + userId + "? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();

        if (confirmation.equals("yes") || confirmation.equals("y")) {
            UserManagementService.UnregistrationResult result =
                    userManagementService.unregisterUser(userId, authService);
            System.out.println(result.getMessage());
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    private void displayActiveUsers() {
        List<User> activeUsers = userManagementService.getActiveUsers();
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ACTIVE USERS");
        System.out.println("=".repeat(80));

        if (activeUsers.isEmpty()) {
            System.out.println("No active users found.");
        } else {
            for (int i = 0; i < activeUsers.size(); i++) {
                System.out.println((i + 1) + ". " + activeUsers.get(i));
            }
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
        } else {
            for (int i = 0; i < inactiveUsers.size(); i++) {
                System.out.println((i + 1) + ". " + inactiveUsers.get(i));
            }
        }
        System.out.println("=".repeat(80));
    }

    private void reactivateUser() {
        System.out.println("\n=== REACTIVATE USER ===");

        List<User> inactiveUsers = userManagementService.getInactiveUsers();
        if (inactiveUsers.isEmpty()) {
            System.out.println("No inactive users found.");
            return;
        }

        System.out.println("\nInactive Users:");
        System.out.println("=".repeat(80));
        for (int i = 0; i < inactiveUsers.size(); i++) {
            System.out.println((i + 1) + ". " + inactiveUsers.get(i));
        }
        System.out.println("=".repeat(80));

        System.out.print("Enter User ID to reactivate: ");
        String userId = scanner.nextLine().trim();

        boolean success = userManagementService.reactivateUser(userId, authService);
        if (!success) {
            System.out.println("Failed to reactivate user.");
        }
    }

    public void borrowBook() {
        System.out.println("\n=== BORROW BOOK ===");

        System.out.print("Enter User ID: ");
        String userId = scanner.nextLine().trim();

        System.out.print("Enter Book ISBN: ");
        String bookIsbn = scanner.nextLine().trim();

        User user = userRepository.findUserById(userId);
        if (user == null) {
            System.out.println("Error: User not found.");
            return;
        }

        // Check if user is active
        if (!user.isActive()) {
            System.out.println("❌ Error: User account is not active.");
            System.out.println("Please contact administrator to reactivate your account.");
            return;
        }

        Loan loan = loanService.borrowBook(userId, bookIsbn, LocalDate.now());
        if (loan != null) {
            System.out.println("✅ Book borrowed successfully!");
            System.out.println("Due date: " + loan.getDueDate());
        }
    }

    public void returnBook() {
        System.out.println("\n=== RETURN BOOK ===");

        System.out.print("Enter Loan ID: ");
        String loanId = scanner.nextLine().trim();

        boolean success = loanService.returnBook(loanId, LocalDate.now());
        if (success) {
            System.out.println("✅ Book returned successfully!");
        }
    }

    public void payFine() {
        System.out.println("\n=== PAY FINE ===");

        System.out.print("Enter User ID: ");
        String userId = scanner.nextLine().trim();

        fineService.displayUserFines(userId);

        List<Fine> unpaidFines = fineService.getUserUnpaidFines(userId);
        if (unpaidFines.isEmpty()) {
            System.out.println("✅ No unpaid fines found.");
            return;
        }

        System.out.print("Enter Fine ID to pay: ");
        String fineId = scanner.nextLine().trim();

        System.out.print("Enter payment amount: ");
        try {
            double paymentAmount = Double.parseDouble(scanner.nextLine().trim());
            fineService.payFine(fineId, paymentAmount);
        } catch (NumberFormatException e) {
            System.out.println("❌ Error: Invalid payment amount.");
        }
    }

    public void displayOverdueBooks() {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to view overdue books.");
            return;
        }

        List<Loan> overdueLoans = loanService.getOverdueLoans(LocalDate.now());
        System.out.println("\n" + "=".repeat(100));
        System.out.println("OVERDUE BOOKS");
        System.out.println("=".repeat(100));

        if (overdueLoans.isEmpty()) {
            System.out.println("No overdue books found.");
        } else {
            for (Loan loan : overdueLoans) {
                System.out.println(loan);
            }
        }
        System.out.println("=".repeat(100));
    }

    public void displayUserLoans() {
        System.out.println("\n=== USER LOANS ===");

        System.out.print("Enter User ID: ");
        String userId = scanner.nextLine().trim();

        List<Loan> userLoans = loanService.getUserActiveLoans(userId);
        System.out.println("\n" + "=".repeat(100));
        System.out.println("ACTIVE LOANS FOR USER: " + userId);
        System.out.println("=".repeat(100));

        if (userLoans.isEmpty()) {
            System.out.println("No active loans found.");
        } else {
            for (Loan loan : userLoans) {
                System.out.println(loan);
            }

            // Show summary
            long overdueCount = userLoans.stream().filter(Loan::isOverdue).count();
            if (overdueCount > 0) {
                System.out.println("\n⚠️ User has " + overdueCount + " overdue book(s) that must be returned before paying fines or borrowing new books.");
            }
        }
        System.out.println("=".repeat(100));
    }

    public ReminderService getReminderService() {
        return reminderService;
    }

    // Getter for UserManagementService
    public UserManagementService getUserManagementService() { return userManagementService; }

    public void displayAllBooks() {
        List<Book> books = bookService.getAllBooks();
        System.out.println("\n" + "=".repeat(100));
        System.out.println("LIBRARY BOOK COLLECTION");
        System.out.println("=".repeat(100));

        if (books.isEmpty()) {
            System.out.println("No books available in the library.");
        } else {
            for (int i = 0; i < books.size(); i++) {
                System.out.println((i + 1) + ". " + books.get(i));
            }
        }
        System.out.println("=".repeat(100));
    }

    public void displayAllUsers() {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to view users.");
            return;
        }

        List<User> users = userRepository.getAllUsers();
        System.out.println("\n" + "=".repeat(80));
        System.out.println("REGISTERED USERS (ALL)");
        System.out.println("=".repeat(80));

        if (users.isEmpty()) {
            System.out.println("No users registered in the system.");
        } else {
            for (int i = 0; i < users.size(); i++) {
                System.out.println((i + 1) + ". " + users.get(i));
            }
        }
        System.out.println("=".repeat(80));
    }

    public AuthService getAuthService() { return authService; }
    public BookService getBookService() { return bookService; }
    public UserRepository getUserRepository() { return userRepository; }
    public LoanService getLoanService() { return loanService; }
    public FineService getFineService() { return fineService; }
}