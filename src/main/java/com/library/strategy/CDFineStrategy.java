package com.library.strategy;

/**
 * Fine strategy for CDs
 * @author Library Team
 * @version 1.0
 */
public class CDFineStrategy implements FineStrategy {
    private static final String MEDIA_TYPE = "CD";
    private static final double FLAT_FINE = 20.00; // $20 flat fine for CDs
    private static final double DAILY_FINE_RATE = 0.50; // $0.50 per day

    @Override
    public double calculateFine(int overdueDays) {
        // CDs can use either flat fine or daily rate
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