package com.library.service;

import com.library.model.Book;
import com.library.model.CD;
import com.library.model.Media;
import com.library.repository.MediaRepository;
import java.util.List;

/**
 * Service for handling media-related operations
 * @author Library Team
 * @version 1.0
 */
public class MediaService {
    private MediaRepository mediaRepository;

    /**
     * Constructor that initializes the media repository
     */
    public MediaService(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    public MediaService() {
        this(new MediaRepository());
    }

    /**
     * Adds a new book to the library (admin only)
     */
    public boolean addBook(String title, String author, String isbn, AuthService authService) {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to add books.");
            return false;
        }

        mediaRepository.addBook(title, author, isbn);
        System.out.println("Book added successfully: " + title);
        return true;
    }

    /**
     * Adds a new CD to the library (admin only)
     */
    public boolean addCD(String title, String artist, String catalogNumber,
                         String genre, int trackCount, AuthService authService) {
        if (!authService.isLoggedIn()) {
            System.out.println("Error: Admin login required to add CDs.");
            return false;
        }

        mediaRepository.addCD(title, artist, catalogNumber, genre, trackCount);
        System.out.println("CD added successfully: " + title);
        return true;
    }

    /**
     * Searches for media by query
     */
    public List<Media> searchMedia(String query) {
        return mediaRepository.searchMedia(query);
    }

    /**
     * Gets all media in the library
     */
    public List<Media> getAllMedia() {
        return mediaRepository.getAllMedia();
    }

    /**
     * Gets all books
     */
    public List<Book> getAllBooks() {
        return mediaRepository.getAllBooks();
    }

    /**
     * Gets all CDs
     */
    public List<CD> getAllCDs() {
        return mediaRepository.getAllCDs();
    }

    /**
     * Finds media by identifier
     */
    public Media findMediaById(String identifier) {
        return mediaRepository.findMediaById(identifier);
    }

    public MediaRepository getMediaRepository() {
        return mediaRepository;
    }

    /**
     * Displays all media
     */
    public void displayAllMedia() {
        List<Media> media = getAllMedia();
        System.out.println("\n" + "=".repeat(120));
        System.out.println("LIBRARY MEDIA COLLECTION");
        System.out.println("=".repeat(120));

        if (media.isEmpty()) {
            System.out.println("No media available in the library.");
        } else {
            for (int i = 0; i < media.size(); i++) {
                System.out.println((i + 1) + ". " + media.get(i));
            }
        }
        System.out.println("=".repeat(120));
    }

    /**
     * Displays all books
     */
    public void displayAllBooks() {
        List<Book> books = getAllBooks();
        System.out.println("\n" + "=".repeat(120));
        System.out.println("BOOK COLLECTION");
        System.out.println("=".repeat(120));

        if (books.isEmpty()) {
            System.out.println("No books available in the library.");
        } else {
            for (int i = 0; i < books.size(); i++) {
                System.out.println((i + 1) + ". " + books.get(i));
            }
        }
        System.out.println("=".repeat(120));
    }

    /**
     * Displays all CDs
     */
    public void displayAllCDs() {
        List<CD> cds = getAllCDs();
        System.out.println("\n" + "=".repeat(120));
        System.out.println("CD COLLECTION");
        System.out.println("=".repeat(120));

        if (cds.isEmpty()) {
            System.out.println("No CDs available in the library.");
        } else {
            for (int i = 0; i < cds.size(); i++) {
                System.out.println((i + 1) + ". " + cds.get(i));
            }
        }
        System.out.println("=".repeat(120));
    }
}