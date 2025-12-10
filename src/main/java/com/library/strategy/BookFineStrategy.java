package com.library.strategy;

/**
 * Fine strategy for books
 * @author Library Team
 * @version 1.0
 */
public class BookFineStrategy implements FineStrategy {
    private static final String MEDIA_TYPE = "BOOK";
    private static final double FLAT_FINE = 10.00; // $10 flat fine for books

    @Override
    public double calculateFine(int overdueDays) {
        // Always return flat fine - no daily calculation
        return FLAT_FINE;
    }

    @Override
    public double getFlatFine() {
        return FLAT_FINE;
    }

    @Override
    public String getMediaType() {
        return MEDIA_TYPE;
    }
}