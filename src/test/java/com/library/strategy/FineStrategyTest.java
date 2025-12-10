package com.library.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Strategy Pattern implementation
 * @author Library Team
 * @version 1.0
 */
class FineStrategyTest {
    private FineContext fineContext;

    @BeforeEach
    void setUp() {
        fineContext = new FineContext();
    }

    @Test
    void testBookFineStrategy() {
        double bookFine = fineContext.calculateFine("BOOK", 5);
        assertEquals(10.00, bookFine, 0.001);
        assertEquals(10.00, fineContext.getFlatFine("BOOK"), 0.001);
    }

    @Test
    void testCDFineStrategy() {
        double cdFine = fineContext.calculateFine("CD", 5);
        assertEquals(20.00, cdFine, 0.001);
        assertEquals(20.00, fineContext.getFlatFine("CD"), 0.001);
    }

    @Test
    void testDefaultStrategyForUnknownMedia() {
        double defaultFine = fineContext.calculateFine("UNKNOWN", 5);
        assertEquals(10.00, defaultFine, 0.001); // Should default to book strategy
    }

    @Test
    void testRegisterNewStrategy() {
        // Create a custom strategy for journals
        FineStrategy journalStrategy = new FineStrategy() {
            @Override
            public double calculateFine(int overdueDays) {
                return 15.00; // $15 flat fine for journals
            }

            @Override
            public double getFlatFine() {
                return 15.00;
            }

            @Override
            public String getMediaType() {
                return "JOURNAL";
            }
        };

        fineContext.registerStrategy("JOURNAL", journalStrategy);

        double journalFine = fineContext.calculateFine("JOURNAL", 5);
        assertEquals(15.00, journalFine, 0.001);
        assertEquals(15.00, fineContext.getFlatFine("JOURNAL"), 0.001);
    }
}