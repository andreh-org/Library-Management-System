package com.library.strategy;

/**
 * Fine strategy for CDs
 * @author Library Team
 * @version 1.0
 */
public class CDFineStrategy implements FineStrategy {
    private static final String MEDIA_TYPE = "CD";
    private static final double FLAT_FINE = 20.00; // $20 flat fine for CDs

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