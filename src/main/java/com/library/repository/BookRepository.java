package com.library.repository;

import com.library.model.Book;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing book data
 * @author Library Team
 * @version 1.0
 */
public class BookRepository {
    private List<Book> books;

    /**
     * Constructor that initializes with sample books
     */
    public BookRepository() {
        this.books = new ArrayList<>();
        initializeSampleBooks();
    }

    /**
     * Adds sample books to the repository
     */
    private void initializeSampleBooks() {
        books.add(new Book("The Great Gatsby", "F. Scott Fitzgerald", "978-0743273565"));
        books.add(new Book("To Kill a Mockingbird", "Harper Lee", "978-0061120084"));
        books.add(new Book("1984", "George Orwell", "978-0451524935"));
        books.add(new Book("Pride and Prejudice", "Jane Austen", "978-0141439518"));
        books.add(new Book("The Catcher in the Rye", "J.D. Salinger", "978-0316769174"));
        books.add(new Book("The Hobbit", "J.R.R. Tolkien", "978-0547928227"));
        books.add(new Book("Harry Potter and the Sorcerer's Stone", "J.K. Rowling", "978-0590353427"));
    }

    /**
     * Adds a new book to the repository
     * @param book the book to add
     */
    public void addBook(Book book) {
        books.add(book);
    }

    /**
     * Searches books by title, author, or ISBN
     * @param query the search query
     * @return list of matching books
     */
public List<Book> searchBooks(String query) {
        if (query == null) {
            return new ArrayList<>(); // Return empty list for null query
        }

        String lowerQuery = query.toLowerCase();
        return books.stream()
                .filter(book -> book.getTitle().toLowerCase().contains(lowerQuery) ||
                        book.getAuthor().toLowerCase().contains(lowerQuery) ||
                        book.getIsbn().toLowerCase().contains(lowerQuery))
                .toList();
    }

    /**
     * Gets all books in the repository
     * @return list of all books
     */
    public List<Book> getAllBooks() {
        return new ArrayList<>(books);
    }

    /**
     * Finds a book by ISBN
     * @param isbn the book ISBN
     * @return the book, or null if not found
     */
    public Book findBookByIsbn(String isbn) {
        return books.stream()
                .filter(book -> book.getIsbn().equals(isbn))
                .findFirst()
                .orElse(null);
    }

    /**
     * Updates a book's availability status
     * @param isbn the book ISBN
     * @param available the availability status
     * @return true if successful, false otherwise
     */
    public boolean updateBookAvailability(String isbn, boolean available) {
        Book book = findBookByIsbn(isbn);
        if (book != null) {
            book.setAvailable(available);
            return true;
        }
        return false;
    }
}
