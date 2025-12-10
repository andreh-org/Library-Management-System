package com.library.repository;

import com.library.model.Book;
import com.library.model.CD;
import com.library.model.Media;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MediaRepository
 * @author Library Team
 * @version 1.0
 */
class MediaRepositoryTest {
    private MediaRepository mediaRepository;

    @BeforeEach
    void setUp() {
        mediaRepository = new MediaRepository();
    }

    @Test
    void testGetAllMedia() {
        List<Media> allMedia = mediaRepository.getAllMedia();
        assertFalse(allMedia.isEmpty());

        // Should contain both books and CDs
        boolean hasBooks = allMedia.stream().anyMatch(m -> m instanceof Book);
        boolean hasCDs = allMedia.stream().anyMatch(m -> m instanceof CD);

        assertTrue(hasBooks);
        assertTrue(hasCDs);
    }

    @Test
    void testGetAllBooks() {
        List<Book> books = mediaRepository.getAllBooks();
        assertFalse(books.isEmpty());

        for (Book book : books) {
            assertEquals("BOOK", book.getMediaType());
            assertEquals(28, book.getLoanPeriodDays());

        }
    }

    @Test
    void testGetAllCDs() {
        List<CD> cds = mediaRepository.getAllCDs();
        assertFalse(cds.isEmpty());

        for (CD cd : cds) {
            assertEquals("CD", cd.getMediaType());
            assertEquals(7, cd.getLoanPeriodDays());

        }
    }

    @Test
    void testAddBook() {
        mediaRepository.addBook("New Book", "New Author", "999-9999999");

        List<Book> books = mediaRepository.getAllBooks();
        boolean found = books.stream()
                .anyMatch(b -> b.getTitle().equals("New Book") && b.getAuthor().equals("New Author"));

        assertTrue(found);
    }

    @Test
    void testAddCD() {
        mediaRepository.addCD("New CD", "New Artist", "CD-999", "Jazz", 8);

        List<CD> cds = mediaRepository.getAllCDs();
        boolean found = cds.stream()
                .anyMatch(c -> c.getTitle().equals("New CD") && c.getAuthor().equals("New Artist"));

        assertTrue(found);
    }

    @Test
    void testSearchMedia() {
        // Search for a book
        List<Media> bookResults = mediaRepository.searchMedia("Gatsby");
        assertFalse(bookResults.isEmpty());
        assertEquals("The Great Gatsby", bookResults.get(0).getTitle());

        // Search for a CD
        List<Media> cdResults = mediaRepository.searchMedia("Thriller");
        assertFalse(cdResults.isEmpty());
        assertEquals("Thriller", cdResults.get(0).getTitle());
    }

    @Test
    void testFindMediaById() {
        // Find a book by ISBN
        Media book = mediaRepository.findMediaById("978-0743273565");
        assertNotNull(book);
        assertTrue(book instanceof Book);
        assertEquals("The Great Gatsby", book.getTitle());

        // Find a CD by catalog number
        Media cd = mediaRepository.findMediaById("CD-001");
        assertNotNull(cd);
        assertTrue(cd instanceof CD);
        assertEquals("Thriller", cd.getTitle());
    }
}