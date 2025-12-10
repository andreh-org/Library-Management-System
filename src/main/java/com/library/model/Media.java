package com.library.model;

/**
 * Base interface for all library media items
 * @author Library Team
 * @version 1.0
 */
public interface Media {
    String getTitle();
    String getAuthor();
    String getIdentifier();
    boolean isAvailable();
    void setAvailable(boolean available);
    String getMediaType();
    int getLoanPeriodDays();
    // getDailyFineRate() REMOVED - Using flat fines via Strategy Pattern
}