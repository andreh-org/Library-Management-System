package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;
import java.util.List;

/**
 * Service for handling book-related operations
 * @author Library Team
 * @version 1.0
 */
public class BookService {
    private BookRepository bookRepository;

    /**
     * Constructor that initializes the book repository
     */
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public BookService() {
        this(new BookRepository());
    }
    /**
     * Adds a new book to the library (admin only)
     * @param title the book title
     * @param author the book author
     * @param isbn the book ISBN
     * @param authService authentication service to verify admin access
     * @return true if book added successfully, false otherwise
     */
    public boolean addBook(String title, String author, String isbn, AuthService authService) {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to add books.");
            return false;
        }

        Book newBook = new Book(title, author, isbn);
        bookRepository.addBook(newBook);
        System.out.println("Book added successfully: " + title);
        return true;
    }

    /**
     * Searches for books by query
     * @param query the search query (title, author, or ISBN)
     * @return list of matching books
     */
    public List<Book> searchBooks(String query) {
        return bookRepository.searchBooks(query);
    }

    /**
     * Gets all books in the library
     * @return list of all books
     */
    public List<Book> getAllBooks() {
        return bookRepository.getAllBooks();
    }

    public BookRepository getBookRepository() {
        return bookRepository;
    }
}