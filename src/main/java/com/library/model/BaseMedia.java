package com.library.model;

/**
 * Abstract base class for all library media items
 * @author Library Team
 * @version 1.0
 */
public abstract class BaseMedia implements Media {
    private String title;
    private String author;
    private String identifier;
    private boolean available;
    private final String mediaType;
    private final int loanPeriodDays;

    public BaseMedia(String title, String author, String identifier,
                     String mediaType, int loanPeriodDays) {
        this.title = title;
        this.author = author;
        this.identifier = identifier;
        this.available = true;
        this.mediaType = mediaType;
        this.loanPeriodDays = loanPeriodDays;
    }

    @Override
    public String getTitle() { return title; }

    @Override
    public String getAuthor() { return author; }

    @Override
    public String getIdentifier() { return identifier; }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String getMediaType() { return mediaType; }

    @Override
    public int getLoanPeriodDays() { return loanPeriodDays; }

    // Remove getDailyFineRate() method completely

    // Add setters for the fields
    public void setTitle(String title) { this.title = title; }

    public void setAuthor(String author) { this.author = author; }

    public void setIdentifier(String identifier) { this.identifier = identifier; }

    @Override
    public String toString() {
        return String.format("[%s] Title: %-30s | Author: %-20s | ID: %-15s | Available: %s",
                mediaType, title, author, identifier, available ? "Yes" : "No");
    }
}