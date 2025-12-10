package com.library.strategy;

/**
 * Strategy interface for calculating fines based on media type
 * Follows Strategy Pattern from refactoring.guru
 * @author Library Team
 * @version 1.0
 */
public interface FineStrategy {
    /**
     * Calculate fine amount for overdue days
     * @param overdueDays number of days overdue
     * @return fine amount
     */
    double calculateFine(int overdueDays);

    /**
     * Get the flat fine amount (if applicable)
     * @return flat fine amount
     */
    double getFlatFine();

    /**
     * Get the media type this strategy applies to
     * @return media type identifier
     */
    String getMediaType();
}