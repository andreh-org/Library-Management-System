package com.library.model;

/**
 * Represents a CD in the library system
 * @author Library Team
 * @version 1.0
 */
public class CD extends BaseMedia {
    private static final String MEDIA_TYPE = "CD";
    private static final int LOAN_PERIOD_DAYS = 7;
    private static final double OVERDUE_FINE = 20.00; // $20 flat fine for overdue CD

    private String genre;
    private int trackCount;

    /**
     * Constructor for creating a new CD
     * @param title the title of the CD
     * @param artist the artist/author of the CD
     * @param catalogNumber the catalog number of the CD
     * @param genre the genre of the CD
     * @param trackCount the number of tracks on the CD
     */
    public CD(String title, String artist, String catalogNumber, String genre, int trackCount) {
        super(title, artist, catalogNumber, MEDIA_TYPE, LOAN_PERIOD_DAYS);
        this.genre = genre;
        this.trackCount = trackCount;
    }

    // Getters and setters
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getTrackCount() { return trackCount; }
    public void setTrackCount(int trackCount) { this.trackCount = trackCount; }

    // Convenience getter for catalog number (alias for getIdentifier())
    public String getCatalogNumber() {
        return getIdentifier();
    }

    // Convenience setter for catalog number (alias for setIdentifier())
    public void setCatalogNumber(String catalogNumber) {
        setIdentifier(catalogNumber);
    }

    /**
     * Gets the flat overdue fine for CDs
     * @return $20.00 fine
     */
    public double getOverdueFine() {
        return OVERDUE_FINE;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Genre: %-15s | Tracks: %-3d", genre, trackCount);
    }
}