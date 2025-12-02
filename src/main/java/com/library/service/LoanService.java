package com.library.service;

import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for handling loan-related operations
 * @author Library Team
 * @version 1.0
 */
public class LoanService {
    private LoanRepository loanRepository;
    private BookRepository bookRepository;
    private UserRepository userRepository;
    private FineService fineService;

    // Constructor with all dependencies
    public LoanService(FineService fineService, UserRepository userRepository, BookRepository bookRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.fineService = fineService;
        this.loanRepository = new LoanRepository(bookRepository);
    }

    public LoanService(FineService fineService, UserRepository userRepository) {
        this(fineService, userRepository, new BookRepository());
    }

    public LoanService(FineService fineService) {
        this(fineService, new UserRepository());
    }

    public LoanService() {
        this(new FineService(), new UserRepository());
    }

    public Loan borrowBook(String userId, String bookIsbn, LocalDate borrowDate) {
        User user = userRepository.findUserById(userId);
        if (user == null) {
            System.out.println("Error: User not found.");
            return null;
        }

        // Check if user is active
        if (!user.isActive()) {
            System.out.println("❌ Error: User account is not active.");
            System.out.println("Please contact administrator to reactivate your account.");
            return null;
        }

        // Check if user can borrow (no unpaid fines)
        double unpaidFines = fineService.getTotalUnpaidAmount(userId);
        if (unpaidFines > 0) {
            System.out.println("❌ Error: User cannot borrow books. Unpaid fines: $" + unpaidFines);
            System.out.println("Please pay all fines before borrowing new books.");
            return null;
        }

        // Check if user has any overdue books that need to be returned
        List<Loan> userActiveLoans = getUserActiveLoans(userId);
        boolean hasOverdueBooks = userActiveLoans.stream()
                .anyMatch(Loan::isOverdue);

        if (hasOverdueBooks) {
            System.out.println("❌ Error: User cannot borrow new books. There are overdue books that need to be returned first.");
            System.out.println("Please return all overdue books before borrowing new ones.");
            return null;
        }

        // Double-check the user's canBorrow flag and update if needed
        if (!user.canBorrow() && unpaidFines == 0 && !hasOverdueBooks) {
            // If fines are paid, no overdue books, but user flag is still false, update it
            user.setCanBorrow(true);
            userRepository.updateUser(user);
            System.out.println("User borrowing status updated. User can now borrow books.");
        }

        // Final check - if user still cannot borrow, show error
        if (!user.canBorrow()) {
            System.out.println("Error: User cannot borrow books due to account restrictions.");
            return null;
        }

        Book book = bookRepository.searchBooks(bookIsbn).stream()
                .findFirst()
                .orElse(null);

        if (book == null) {
            System.out.println("Error: Book not found.");
            return null;
        }

        if (!book.isAvailable()) {
            System.out.println("Error: Book is already borrowed.");
            return null;
        }

        Loan loan = loanRepository.createLoan(userId, bookIsbn, borrowDate);
        if (loan != null) {
            // Book availability is already updated in LoanRepository.createLoan()
            user.addLoan(loan.getLoanId());
            userRepository.updateUser(user);
            System.out.println("✅ Book borrowed successfully. Due date: " + loan.getDueDate());
        }

        return loan;
    }

    public boolean returnBook(String loanId, LocalDate returnDate) {
        Loan loan = loanRepository.findLoanById(loanId);
        if (loan == null) {
            System.out.println("❌ Error: Loan not found.");
            return false;
        }

        if (loan.getReturnDate() != null) {
            System.out.println("❌ Error: Book already returned.");
            return false;
        }

        boolean returnSuccess = loanRepository.returnBook(loanId, returnDate);
        if (returnSuccess) {
            User user = userRepository.findUserById(loan.getUserId());
            if (user != null) {
                user.removeLoan(loanId);
                userRepository.updateUser(user);
            }

            System.out.println("✅ Book returned successfully!");

            List<Loan> remainingLoans = getUserActiveLoans(loan.getUserId());
            boolean stillHasOverdue = remainingLoans.stream()
                    .anyMatch(Loan::isOverdue);

            if (!stillHasOverdue) {
                System.out.println("🎉 All overdue books returned! User can now pay fines.");
            }
        }

        return returnSuccess;
    }

    /**
     * Gets all active loans for a user (including overdue ones)
     * @param userId the user ID
     * @return list of active loans with updated overdue status
     */
    public List<Loan> getUserActiveLoans(String userId) {
        List<Loan> userLoans = loanRepository.findLoansByUser(userId).stream()
                .filter(loan -> loan.getReturnDate() == null)
                .collect(java.util.stream.Collectors.toList());

        LocalDate currentDate = LocalDate.now();
        for (Loan loan : userLoans) {
            loan.checkOverdue(currentDate);
        }

        return userLoans;
    }

    /**
     * Checks if user has any overdue books
     * @param userId the user ID
     * @return true if user has overdue books, false otherwise
     */
    public boolean hasOverdueBooks(String userId) {
        List<Loan> activeLoans = getUserActiveLoans(userId);
        return activeLoans.stream().anyMatch(Loan::isOverdue);
    }

    public List<Loan> getOverdueLoans(LocalDate currentDate) {
        return loanRepository.getOverdueLoans(currentDate);
    }

    public void checkAllOverdueLoans(LocalDate currentDate) {
        List<Loan> overdueLoans = loanRepository.getOverdueLoans(currentDate);
        for (Loan loan : overdueLoans) {
            long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), currentDate);
            System.out.println("Overdue loan detected: " + loan.getLoanId() + " - " + overdueDays + " days overdue");
        }
    }

    public LoanRepository getLoanRepository() { return loanRepository; }
    public UserRepository getUserRepository() { return userRepository; }
    public FineService getFineService() { return fineService; }
    public BookRepository getBookRepository() { return bookRepository; }
}