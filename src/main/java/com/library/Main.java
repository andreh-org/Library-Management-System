package com.library;

import com.library.model.Book;
import com.library.service.LibraryService;
import java.util.List;
import java.util.Scanner;

/**
 * Main class for the Library Management System
 * @author Library Team
 * @version 1.0
 */
public class Main {
    private static LibraryService libraryService = new LibraryService();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== Library Management System ===");
        showMainMenu();
    }

    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("1. View All Books");
            System.out.println("2. Search Books");
            System.out.println("3. Borrow Book");
            System.out.println("4. Return Book");
            System.out.println("5. Pay Fine");
            System.out.println("6. View My Loans");
            System.out.println("7. Admin Login");
            System.out.println("8. Exit");
            System.out.print("Choose an option: ");

            int choice = getIntInput();

            switch (choice) {
                case 1:
                    libraryService.displayAllBooks();
                    break;
                case 2:
                    searchBooks();
                    break;
                case 3:
                    libraryService.borrowBook();
                    break;
                case 4:
                    libraryService.returnBook();
                    break;
                case 5:
                    libraryService.payFine();
                    break;
                case 6:
                    libraryService.displayUserLoans();
                    break;
                case 7:
                    adminLogin();
                    break;
                case 8:
                    System.out.println("Thank you for using Library Management System. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void showAdminMenu() {
        while (libraryService.getAuthService().isLoggedIn()) {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("1. View All Books");
            System.out.println("2. View All Users");
            System.out.println("3. Add New Book");
            System.out.println("4. Search Books");
            System.out.println("5. View Overdue Books");
            System.out.println("6. Send Overdue Reminders");
            System.out.println("7. User Management"); // Renumbered from 8 to 7
            System.out.println("8. Logout"); // Renumbered from 9 to 8
            System.out.print("Choose an option: ");

            int choice = getIntInput();

            switch (choice) {
                case 1:
                    libraryService.displayAllBooks();
                    break;
                case 2:
                    libraryService.displayAllUsers();
                    break;
                case 3:
                    addNewBook();
                    break;
                case 4:
                    searchBooks();
                    break;
                case 5:
                    libraryService.displayOverdueBooks();
                    break;
                case 6:
                    libraryService.sendOverdueReminders();
                    break;
                case 7: // Now User Management (was Manage Borrowing Rules)
                    libraryService.manageUsers();
                    break;
                case 8: // Now Logout (was User Management)
                    libraryService.getAuthService().logout();
                    System.out.println("Logged out successfully.");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // ... rest of existing methods remain the same ...
    private static void searchBooks() {
        System.out.print("\nEnter search query (title, author, or ISBN): ");
        String query = scanner.nextLine().trim();

        if (query.isEmpty()) {
            System.out.println("Search query cannot be empty.");
            return;
        }

        List<Book> results = libraryService.getBookService().searchBooks(query);

        System.out.println("\n" + "=".repeat(100));
        System.out.println("SEARCH RESULTS for: '" + query + "'");
        System.out.println("=".repeat(100));

        if (results.isEmpty()) {
            System.out.println("No books found matching your search.");
        } else {
            for (int i = 0; i < results.size(); i++) {
                System.out.println((i + 1) + ". " + results.get(i));
            }
        }
        System.out.println("=".repeat(100));
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

    private static void addNewBook() {
        System.out.println("\n=== ADD NEW BOOK ===");
        System.out.print("Enter book title: ");
        String title = scanner.nextLine().trim();

        System.out.print("Enter author: ");
        String author = scanner.nextLine().trim();

        System.out.print("Enter ISBN: ");
        String isbn = scanner.nextLine().trim();

        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            System.out.println("Error: All fields are required.");
            return;
        }

        libraryService.getBookService().addBook(title, author, isbn, libraryService.getAuthService());
    }

    public static int getIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}