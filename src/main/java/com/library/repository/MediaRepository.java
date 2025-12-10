package com.library.repository;

import com.library.model.Book;
import com.library.model.CD;
import com.library.model.Media;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for managing all media items (books, CDs, etc.)
 * @author Library Team
 * @version 1.0
 */
public class MediaRepository {
    private List<Media> mediaItems;

    /**
     * Constructor that initializes with sample media
     */
    public MediaRepository() {
        this.mediaItems = new ArrayList<>();
        initializeSampleMedia();
    }

    /**
     * Adds sample media to the repository
     */
    private void initializeSampleMedia() {
        // Sample books
        mediaItems.add(new Book("The Great Gatsby", "F. Scott Fitzgerald", "978-0743273565"));
        mediaItems.add(new Book("To Kill a Mockingbird", "Harper Lee", "978-0061120084"));
        mediaItems.add(new Book("1984", "George Orwell", "978-0451524935"));
        mediaItems.add(new Book("Pride and Prejudice", "Jane Austen", "978-0141439518"));
        mediaItems.add(new Book("The Catcher in the Rye", "J.D. Salinger", "978-0316769174"));
        mediaItems.add(new Book("The Hobbit", "J.R.R. Tolkien", "978-0547928227"));
        mediaItems.add(new Book("Harry Potter and the Sorcerer's Stone", "J.K. Rowling", "978-0590353427"));

        // Sample CDs
        mediaItems.add(new CD("Thriller", "Michael Jackson", "CD-001", "Pop", 9));
        mediaItems.add(new CD("The Dark Side of the Moon", "Pink Floyd", "CD-002", "Progressive Rock", 10));
        mediaItems.add(new CD("Back in Black", "AC/DC", "CD-003", "Hard Rock", 10));
        mediaItems.add(new CD("The Beatles", "The Beatles", "CD-004", "Rock", 17));
        mediaItems.add(new CD("Rumours", "Fleetwood Mac", "CD-005", "Soft Rock", 11));
    }

    /**
     * Adds new media to the repository
     * @param media the media to add
     */
    public void addMedia(Media media) {
        mediaItems.add(media);
    }

    /**
     * Searches media by title, author, or identifier
     * @param query the search query
     * @return list of matching media
     */
    public List<Media> searchMedia(String query) {
        return mediaItems.stream()
                .filter(media -> media.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        media.getAuthor().toLowerCase().contains(query.toLowerCase()) ||
                        media.getIdentifier().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Gets all media in the repository
     * @return list of all media
     */
    public List<Media> getAllMedia() {
        return new ArrayList<>(mediaItems);
    }

    /**
     * Finds a media item by identifier
     * @param identifier the media identifier
     * @return the media, or null if not found
     */
    public Media findMediaById(String identifier) {
        return mediaItems.stream()
                .filter(media -> media.getIdentifier().equals(identifier))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a media item by identifier and type
     * @param identifier the media identifier
     * @param mediaType the media type ("BOOK" or "CD")
     * @return the media, or null if not found
     */
    public Media findMediaByIdAndType(String identifier, String mediaType) {
        return mediaItems.stream()
                .filter(media -> media.getIdentifier().equals(identifier) &&
                        media.getMediaType().equals(mediaType))
                .findFirst()
                .orElse(null);
    }

    /**
     * Updates a media's availability status
     * @param identifier the media identifier
     * @param available the availability status
     * @return true if successful, false otherwise
     */
    public boolean updateMediaAvailability(String identifier, boolean available) {
        Media media = findMediaById(identifier);
        if (media != null) {
            media.setAvailable(available);
            return true;
        }
        return false;
    }

    /**
     * Gets all books
     * @return list of books
     */
    public List<Book> getAllBooks() {
        return mediaItems.stream()
                .filter(media -> media instanceof Book)
                .map(media -> (Book) media)
                .collect(Collectors.toList());
    }

    /**
     * Gets all CDs
     * @return list of CDs
     */
    public List<CD> getAllCDs() {
        return mediaItems.stream()
                .filter(media -> media instanceof CD)
                .map(media -> (CD) media)
                .collect(Collectors.toList());
    }

    /**
     * Adds a new book (convenience method)
     */
    public void addBook(String title, String author, String isbn) {
        addMedia(new Book(title, author, isbn));
    }

    /**
     * Adds a new CD (convenience method)
     */
    public void addCD(String title, String artist, String catalogNumber, String genre, int trackCount) {
        addMedia(new CD(title, artist, catalogNumber, genre, trackCount));
    }
}