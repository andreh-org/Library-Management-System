package com.library.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CD model
 * @author Library Team
 * @version 1.0
 */
class CDTest {

    @Test
    void testCDCreation() {
        CD cd = new CD("Test Album", "Test Artist", "CD-999", "Rock", 12);

        assertEquals("Test Album", cd.getTitle());
        assertEquals("Test Artist", cd.getAuthor());
        assertEquals("CD-999", cd.getCatalogNumber());
        assertEquals("Rock", cd.getGenre());
        assertEquals(12, cd.getTrackCount());
        assertEquals("CD", cd.getMediaType());
        assertEquals(7, cd.getLoanPeriodDays());
        assertTrue(cd.isAvailable());
    }

    @Test
    void testCDToString() {
        CD cd = new CD("Thriller", "Michael Jackson", "CD-001", "Pop", 9);
        String toStringResult = cd.toString();

        assertTrue(toStringResult.contains("Thriller"));
        assertTrue(toStringResult.contains("Michael Jackson"));
        assertTrue(toStringResult.contains("CD-001"));
        assertTrue(toStringResult.contains("Pop"));
        assertTrue(toStringResult.contains("9"));
        assertTrue(toStringResult.contains("[CD]"));
    }

    @Test
    void testCDSetters() {
        CD cd = new CD("Initial", "Initial Artist", "CD-001", "Initial Genre", 5);

        cd.setTitle("Updated Title");
        cd.setAuthor("Updated Artist");
        cd.setCatalogNumber("CD-999");
        cd.setGenre("Updated Genre");
        cd.setTrackCount(10);
        cd.setAvailable(false);

        assertEquals("Updated Title", cd.getTitle());
        assertEquals("Updated Artist", cd.getAuthor());
        assertEquals("CD-999", cd.getCatalogNumber());
        assertEquals("Updated Genre", cd.getGenre());
        assertEquals(10, cd.getTrackCount());
        assertFalse(cd.isAvailable());
    }

    @Test
    void testCDOverdueFine() {
        CD cd = new CD("Test Album", "Test Artist", "CD-001", "Rock", 12);
        assertEquals(20.00, cd.getOverdueFine(), 0.001);
    }
}