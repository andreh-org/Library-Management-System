package com.library.repository;

import com.library.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BookRepository
 * @author Library Team
 * @version 1.0
 */
class BookRepositoryTest {
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository = new BookRepository();
    }

    @Test
    void testAddBook() {
        Book newBook = new Book("New Book", "New Author", "9999999999");
        bookRepository.addBook(newBook);

        List<Book> results = bookRepository.searchBooks("New Book");
        assertFalse(results.isEmpty());
        assertEquals("New Book", results.get(0).getTitle());
        assertEquals("New Author", results.get(0).getAuthor());
        assertEquals("9999999999", results.get(0).getIsbn());
    }

    @Test
    void testAddMultipleBooks() {
        int initialSize = bookRepository.getAllBooks().size();

        Book book1 = new Book("Book 1", "Author 1", "1111111111");
        Book book2 = new Book("Book 2", "Author 2", "2222222222");
        Book book3 = new Book("Book 3", "Author 3", "3333333333");

        bookRepository.addBook(book1);
        bookRepository.addBook(book2);
        bookRepository.addBook(book3);

        List<Book> allBooks = bookRepository.getAllBooks();
        assertEquals(initialSize + 3, allBooks.size());
    }

    @Test
    void testAddDuplicateBook() {
        Book book = new Book("Duplicate Book", "Author", "1234567890");
        bookRepository.addBook(book);

        // Add same book again (should be allowed in this implementation)
        bookRepository.addBook(book);

        List<Book> results = bookRepository.searchBooks("Duplicate Book");
        assertEquals(2, results.size()); // Both copies should be found
    }

    @Test
    void testSearchBooksByTitle() {
        List<Book> results = bookRepository.searchBooks("Great Gatsby");
        assertFalse(results.isEmpty());
        assertEquals("The Great Gatsby", results.get(0).getTitle());
        assertEquals("F. Scott Fitzgerald", results.get(0).getAuthor());
        assertEquals("978-0743273565", results.get(0).getIsbn());
    }

    @Test
    void testSearchBooksByAuthor() {
        List<Book> results = bookRepository.searchBooks("Fitzgerald");
        assertFalse(results.isEmpty());
        assertEquals("F. Scott Fitzgerald", results.get(0).getAuthor());
        assertEquals("The Great Gatsby", results.get(0).getTitle());
    }

    @Test
    void testSearchBooksByISBN() {
        List<Book> results = bookRepository.searchBooks("978-0743273565");
        assertFalse(results.isEmpty());
        assertEquals("978-0743273565", results.get(0).getIsbn());
        assertEquals("The Great Gatsby", results.get(0).getTitle());
    }

    @Test
    void testSearchBooksCaseInsensitive() {
        List<Book> results = bookRepository.searchBooks("gReAt gAtSbY");
        assertFalse(results.isEmpty());
        assertEquals("The Great Gatsby", results.get(0).getTitle());

        results = bookRepository.searchBooks("FITZGERALD");
        assertFalse(results.isEmpty());
        assertEquals("F. Scott Fitzgerald", results.get(0).getAuthor());

        results = bookRepository.searchBooks("978-0743273565");
        assertFalse(results.isEmpty());
        assertEquals("978-0743273565", results.get(0).getIsbn());
    }

    @Test
    void testSearchBooksPartialMatch() {
        // Partial title
        List<Book> results = bookRepository.searchBooks("Gatsby");
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getTitle().contains("Gatsby"));

        // Partial author
        results = bookRepository.searchBooks("Scott");
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getAuthor().contains("Scott"));

        // Partial ISBN
        results = bookRepository.searchBooks("074327");
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getIsbn().contains("074327"));
    }

    @Test
    void testSearchBooksNoResults() {
        List<Book> results = bookRepository.searchBooks("Nonexistent Book Title");
        assertTrue(results.isEmpty());

        results = bookRepository.searchBooks("Unknown Author");
        assertTrue(results.isEmpty());

        results = bookRepository.searchBooks("0000000000");
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchBooksEmptyQuery() {
        List<Book> results = bookRepository.searchBooks("");
        // Empty query should return all books (contains empty string)
        assertFalse(results.isEmpty());
        assertEquals(bookRepository.getAllBooks().size(), results.size());
    }

    @Test
    void testSearchBooksNullQuery() {
        // Should handle null gracefully (treat as empty string)
        List<Book> results = bookRepository.searchBooks(null);
        assertNotNull(results);
        // In current implementation, null.toLowerCase() would throw NPE
        // We need to test if it handles null
    }

    @Test
    void testGetAllBooks() {
        List<Book> allBooks = bookRepository.getAllBooks();
        assertFalse(allBooks.isEmpty());
        assertTrue(allBooks.size() >= 7); // We have 7 sample books

        // Verify it returns a copy, not the original list
        int originalSize = allBooks.size();
        bookRepository.addBook(new Book("Test", "Test", "9999999999"));

        List<Book> allBooksAgain = bookRepository.getAllBooks();
        assertEquals(originalSize + 1, allBooksAgain.size());
        assertEquals(originalSize, allBooks.size()); // Original list unchanged
    }

    @Test
    void testFindBookByIsbn() {
        Book book = bookRepository.findBookByIsbn("978-0743273565");
        assertNotNull(book);
        assertEquals("The Great Gatsby", book.getTitle());
        assertEquals("F. Scott Fitzgerald", book.getAuthor());
        assertEquals("978-0743273565", book.getIsbn());
        assertTrue(book.isAvailable()); // New books should be available
    }

    @Test
    void testFindBookByIsbnNotFound() {
        Book book = bookRepository.findBookByIsbn("0000000000");
        assertNull(book);
    }

    @Test
    void testFindBookByIsbnCaseSensitive() {
        // ISBN search should be exact match (case-sensitive for any letters)
        Book book = bookRepository.findBookByIsbn("978-0743273565");
        assertNotNull(book);

        // Test with exactly the same ISBN (uppercase doesn't change numbers/hyphens)
        Book book2 = bookRepository.findBookByIsbn("978-0743273565".toUpperCase());
        // This will NOT be null because uppercase of "978-0743273565" is "978-0743273565"
        assertNotNull(book2);
        assertEquals(book, book2);

        // Better test: Create a book with letters in ISBN to test case sensitivity
        Book bookWithLetters = new Book("Test Book", "Test Author", "ABC-123-DEF");
        bookRepository.addBook(bookWithLetters);

        // Lowercase search should NOT find it (case-sensitive)
        Book found = bookRepository.findBookByIsbn("abc-123-def");
        assertNull(found); // Case doesn't match

        // Exact case search should find it
        found = bookRepository.findBookByIsbn("ABC-123-DEF");
        assertNotNull(found);
        assertEquals("Test Book", found.getTitle());
    }

    @Test
    void testUpdateBookAvailabilitySuccess() {
        // First verify book is available
        Book book = bookRepository.findBookByIsbn("978-0743273565");
        assertTrue(book.isAvailable());

        // Mark as unavailable
        boolean success = bookRepository.updateBookAvailability("978-0743273565", false);
        assertTrue(success);

        // Verify update
        Book updatedBook = bookRepository.findBookByIsbn("978-0743273565");
        assertFalse(updatedBook.isAvailable());

        // Mark as available again
        success = bookRepository.updateBookAvailability("978-0743273565", true);
        assertTrue(success);

        // Verify update
        updatedBook = bookRepository.findBookByIsbn("978-0743273565");
        assertTrue(updatedBook.isAvailable());
    }

    @Test
    void testUpdateBookAvailabilityNotFound() {
        boolean success = bookRepository.updateBookAvailability("0000000000", false);
        assertFalse(success);
    }

    @Test
    void testUpdateBookAvailabilityMultipleTimes() {
        String isbn = "978-0743273565";

        // Toggle availability multiple times
        assertTrue(bookRepository.updateBookAvailability(isbn, false));
        assertFalse(bookRepository.findBookByIsbn(isbn).isAvailable());

        assertTrue(bookRepository.updateBookAvailability(isbn, true));
        assertTrue(bookRepository.findBookByIsbn(isbn).isAvailable());

        assertTrue(bookRepository.updateBookAvailability(isbn, false));
        assertFalse(bookRepository.findBookByIsbn(isbn).isAvailable());
    }

    @Test
    void testSampleBooksInitialization() {
        List<Book> books = bookRepository.getAllBooks();

        // Check specific sample books exist
        boolean hasGreatGatsby = books.stream()
                .anyMatch(b -> "The Great Gatsby".equals(b.getTitle()));
        boolean hasMockingbird = books.stream()
                .anyMatch(b -> "To Kill a Mockingbird".equals(b.getTitle()));
        boolean has1984 = books.stream()
                .anyMatch(b -> "1984".equals(b.getTitle()));

        assertTrue(hasGreatGatsby);
        assertTrue(hasMockingbird);
        assertTrue(has1984);

        // Check all sample books are available initially
        for (Book book : books) {
            assertTrue(book.isAvailable(), "Book " + book.getTitle() + " should be available");
        }
    }

    @Test
    void testBookFieldsPreserved() {
        Book book = bookRepository.findBookByIsbn("978-0743273565");
        assertNotNull(book);

        // Test all getters
        assertEquals("The Great Gatsby", book.getTitle());
        assertEquals("F. Scott Fitzgerald", book.getAuthor());
        assertEquals("978-0743273565", book.getIsbn());
        assertEquals("BOOK", book.getMediaType());
        assertEquals(28, book.getLoanPeriodDays());
        assertEquals(10.00, book.getOverdueFine(), 0.001);
        assertTrue(book.isAvailable());
    }

    @Test
    void testSearchBooksMultipleMatches() {
        // Add another book by same author
        Book additionalBook = new Book("Tender Is the Night", "F. Scott Fitzgerald", "9999999999");
        bookRepository.addBook(additionalBook);

        List<Book> results = bookRepository.searchBooks("Fitzgerald");
        assertEquals(2, results.size()); // Great Gatsby + Tender Is the Night

        // Both should be by Fitzgerald
        for (Book book : results) {
            assertTrue(book.getAuthor().contains("Fitzgerald"));
        }
    }

    @Test
    void testSearchBooksOverlappingTerms() {
        // Search for term that appears in multiple fields
        List<Book> results = bookRepository.searchBooks("978");
        // Should find all books (all have ISBNs starting with 978)
        assertEquals(bookRepository.getAllBooks().size(), results.size());
    }

    @Test
    void testImmutableGetAllBooks() {
        List<Book> books = bookRepository.getAllBooks();
        int originalSize = books.size();

        // Try to modify the returned list (should not affect repository)
        try {
            books.add(new Book("Should Not Add", "Author", "0000000000"));
            // If we reach here, the list wasn immutable
            // Check if repository was affected
            assertEquals(originalSize, bookRepository.getAllBooks().size());
        } catch (UnsupportedOperationException e) {
            // Expected if list is immutable
        }
    }

    @Test
    void testFindBookByIsbnWithSpecialCharacters() {
        // Test with ISBN containing hyphens
        Book book = bookRepository.findBookByIsbn("978-0743273565");
        assertNotNull(book);

        // Test with spaces (should not match)
        Book book2 = bookRepository.findBookByIsbn("978 0743273565");
        assertNull(book2);
    }

    @Test
    void testInitializeSampleBooksCalledOnce() {
        // Constructor should call initializeSampleBooks
        BookRepository newRepo = new BookRepository();
        List<Book> books = newRepo.getAllBooks();
        assertFalse(books.isEmpty());
        assertTrue(books.size() >= 7);
    }
}