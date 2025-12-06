package com.library.strategy;

/**
 * Fine strategy for books
 * @author Library Team
 * @version 1.0
 */
public class BookFineStrategy implements FineStrategy {
    private static final String MEDIA_TYPE = "BOOK";
    private static final double FLAT_FINE = 10.00; // $10 flat fine for books
    private static final double DAILY_FINE_RATE = 0.25; // $0.25 per day

    @Override
    public double calculateFine(int overdueDays) {
        // Books can use either flat fine or daily rate
        // For now, using flat fine as per requirements
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