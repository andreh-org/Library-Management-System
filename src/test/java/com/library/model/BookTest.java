package com.library.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Book model
 * @author Library Team
 * @version 1.0
 */
class BookTest {

    @Test
    void testBookCreation() {
        Book book = new Book("Test Book", "Test Author", "1234567890");

        assertEquals("Test Book", book.getTitle());
        assertEquals("Test Author", book.getAuthor());
        assertEquals("1234567890", book.getIsbn());
        assertTrue(book.isAvailable());
    }

    @Test
    void testBookSetters() {
        Book book = new Book("Initial Title", "Initial Author", "1111111111");

        // Use the setter methods that exist
        book.setTitle("Updated Title");
        book.setAuthor("Updated Author");
        book.setIsbn("9999999999");
        book.setAvailable(false);

        assertEquals("Updated Title", book.getTitle());
        assertEquals("Updated Author", book.getAuthor());
        assertEquals("9999999999", book.getIsbn());
        assertFalse(book.isAvailable());
    }

    @Test
    void testBookToString() {
        Book book = new Book("The Great Gatsby", "F. Scott Fitzgerald", "978-0743273565");
        String toStringResult = book.toString();

        assertTrue(toStringResult.contains("The Great Gatsby"));
        assertTrue(toStringResult.contains("F. Scott Fitzgerald"));
        assertTrue(toStringResult.contains("978-0743273565"));
        assertTrue(toStringResult.contains("Yes")); // Available
    }

    @Test
    void testBookAvailability() {
        Book book = new Book("Test Book", "Test Author", "1234567890");

        // Initially available
        assertTrue(book.isAvailable());

        // Set to not available
        book.setAvailable(false);
        assertFalse(book.isAvailable());

        // Set back to available
        book.setAvailable(true);
        assertTrue(book.isAvailable());
    }

    @Test
    void testBookOverdueFine() {
        Book book = new Book("Test Book", "Test Author", "1234567890");
        assertEquals(10.00, book.getOverdueFine(), 0.001);
    }
}