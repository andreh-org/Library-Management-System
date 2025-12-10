package com.library;

import com.library.service.LibraryService;
import java.util.Scanner;

/**
 * Main class for the Library Management System with Design Patterns
 * @author Library Team
 * @version 2.0
 */
public class Main {
    private static LibraryService libraryService = new LibraryService();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== Library Management System with Design Patterns ===");
        System.out.println("✅ Strategy Pattern: Fine calculation for different media types");
        System.out.println("✅ Observer Pattern: Notification system for events");
        showMainMenu();
    }

    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("1. View All Media");
            System.out.println("2. View Books Only");
            System.out.println("3. View CDs Only");
            System.out.println("4. Search Media");
            System.out.println("5. Borrow Media");
            System.out.println("6. Return Media");
            System.out.println("7. Pay Fine");
            System.out.println("8. View My Loans");
            System.out.println("9. View Mixed Media Overdue Report");
            System.out.println("10. Demo Design Patterns");
            System.out.println("11. Admin Login");
            System.out.println("12. Exit");
            System.out.print("Choose an option: ");

            int choice = getIntInput();

            switch (choice) {
                case 1:
                    libraryService.displayAllMedia();
                    break;
                case 2:
                    libraryService.displayAllBooks();
                    break;
                case 3:
                    libraryService.displayAllCDs();
                    break;
                case 4:
                    libraryService.searchMedia();
                    break;
                case 5:
                    libraryService.borrowMedia();
                    break;
                case 6:
                    libraryService.returnBook();
                    break;
                case 7:
                    libraryService.payFine();
                    break;
                case 8:
                    libraryService.displayUserLoans();
                    break;
                case 9:
                    libraryService.displayMixedMediaOverdueReport();
                    break;
                case 10:
                    demoDesignPatterns();
                    break;
                case 11:
                    adminLogin();
                    break;
                case 12:
                    System.out.println("Thank you for using Library Management System. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void demoDesignPatterns() {
        System.out.println("\n=== DESIGN PATTERNS DEMO ===");
        System.out.println("1. Strategy Pattern Demo - Fine Calculation");
        System.out.println("2. Observer Pattern Demo - Notifications");
        System.out.println("3. Back to Main Menu");
        System.out.print("Choose demo: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                demoStrategyPattern();
                break;
            case 2:
                demoObserverPattern();
                break;
            case 3:
                return;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void demoStrategyPattern() {
        System.out.println("\n🎯 STRATEGY PATTERN DEMO - Fine Calculation");
        System.out.println("Different media types have different fine calculation strategies:");

        var fineContext = libraryService.getFineService().getFineContext();
        String[] mediaTypes = fineContext.getRegisteredMediaTypes();

        for (String mediaType : mediaTypes) {
            double fine = fineContext.calculateFine(mediaType, 5);
            System.out.printf("  • %s: $%.2f flat fine%n", mediaType, fine);
        }

        System.out.println("\n✨ Benefits of Strategy Pattern:");
        System.out.println("  • Easy to add new media types (e.g., DVDs, e-books)");
        System.out.println("  • Fine calculation logic is encapsulated in strategies");
        System.out.println("  • Can switch strategies at runtime");
        System.out.println("  • Follows Open/Closed Principle");
    }

    private static void demoObserverPattern() {
        System.out.println("\n👁️ OBSERVER PATTERN DEMO - Notifications");
        System.out.println("Multiple notification channels can be attached:");
        System.out.println("  ✓ Email Notifications");
        System.out.println("  ✓ Console Logging");
        System.out.println("  ✓ File Logging");
        System.out.println("  ✓ (Future: SMS, Push Notifications)");

        System.out.println("\n✨ Benefits of Observer Pattern:");
        System.out.println("  • Loose coupling between subject and observers");
        System.out.println("  • Easy to add new notification channels");
        System.out.println("  • Observers can be attached/detached at runtime");
        System.out.println("  • Follows Single Responsibility Principle");
    }

    private static void showAdminMenu() {
        while (libraryService.getAuthService().isLoggedIn()) {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("1. View All Media");
            System.out.println("2. View All Users");
            System.out.println("3. Add New Media");
            System.out.println("4. Search Media");
            System.out.println("5. View Overdue Items");
            System.out.println("6. Send Overdue Reminders");
            System.out.println("7. User Management");
            System.out.println("8. Logout");
            System.out.print("Choose an option: ");

            int choice = getIntInput();

            switch (choice) {
                case 1:
                    libraryService.displayAllMedia();
                    break;
                case 2:
                    libraryService.displayAllUsers();
                    break;
                case 3:
                    libraryService.addNewMedia();
                    break;
                case 4:
                    libraryService.searchMedia();
                    break;
                case 5:
                    libraryService.displayOverdueBooks();
                    break;
                case 6:
                    libraryService.sendOverdueReminders();
                    break;
                case 7:
                    libraryService.manageUsers();
                    break;
                case 8:
                    libraryService.getAuthService().logout();
                    System.out.println("Logged out successfully.");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void adminLogin() {
        System.out.println("\n=== ADMIN LOGIN ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (libraryService.getAuthService().login(username, password)) {
            System.out.println("Login successful! Welcome, Admin.");
            showAdminMenu();
        } else {
            System.out.println("Login failed! Invalid credentials.");
        }
    }

    public static int getIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}