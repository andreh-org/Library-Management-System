package com.library.strategy;

import java.util.HashMap;
import java.util.Map;

/**
 * Context class that uses the FineStrategy
 * Follows Strategy Pattern from refactoring.guru
 * @author Library Team
 * @version 1.0
 */
public class FineContext {
    private Map<String, FineStrategy> strategies;

    public FineContext() {
        strategies = new HashMap<>();
        // Register default strategies
        strategies.put("BOOK", new BookFineStrategy());
        strategies.put("CD", new CDFineStrategy());
    }

    /**
     * Register a new fine strategy
     * @param mediaType the media type
     * @param strategy the strategy to use
     */
    public void registerStrategy(String mediaType, FineStrategy strategy) {
        strategies.put(mediaType.toUpperCase(), strategy);
    }

    /**
     * Calculate fine for given media type and overdue days
     * @param mediaType the media type
     * @param overdueDays number of days overdue
     * @return calculated fine amount
     */
    public double calculateFine(String mediaType, int overdueDays) {
        FineStrategy strategy = strategies.get(mediaType.toUpperCase());
        if (strategy != null) {
            return strategy.calculateFine(overdueDays);
        }
        // Default to book strategy if not found
        return strategies.get("BOOK").calculateFine(overdueDays);
    }

    /**
     * Get flat fine for media type
     * @param mediaType the media type
     * @return flat fine amount
     */
    public double getFlatFine(String mediaType) {
        FineStrategy strategy = strategies.get(mediaType.toUpperCase());
        if (strategy != null) {
            return strategy.getFlatFine();
        }
        return strategies.get("BOOK").getFlatFine();
    }

    /**
     * Get all registered media types
     * @return array of media types
     */
    public String[] getRegisteredMediaTypes() {
        return strategies.keySet().toArray(new String[0]);
    }
}