package com.library.service;

import com.library.model.CD;
import com.library.model.Loan;
import com.library.repository.MediaRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MediaService
 * @author Library Team
 * @version 1.0
 */
class MediaServiceTest {
    private MediaService mediaService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService();
        authService = new AuthService();
    }

    @Test
    void testAddCDWithAdmin() {
        authService.login("admin", "admin123");

        boolean result = mediaService.addCD("Test CD", "Test Artist", "TEST-001", "Test", 5, authService);
        assertTrue(result);

        // Verify CD was added
        var cds = mediaService.getAllCDs();
        boolean found = cds.stream()
                .anyMatch(cd -> cd.getTitle().equals("Test CD") && cd.getAuthor().equals("Test Artist"));

        assertTrue(found);
    }

    @Test
    void testAddCDWithoutAdmin() {
        // Not logged in
        boolean result = mediaService.addCD("Test CD", "Test Artist", "TEST-001", "Test", 5, authService);
        assertFalse(result);
    }

    @Test
    void testGetAllCDs() {
        var cds = mediaService.getAllCDs();
        assertFalse(cds.isEmpty());

        // Sample CD should exist
        boolean hasThriller = cds.stream().anyMatch(cd -> cd.getTitle().equals("Thriller"));
        assertTrue(hasThriller);
    }

    @Test
    void testSearchMediaFindsCDs() {
        var results = mediaService.searchMedia("Thriller");
        assertFalse(results.isEmpty());

        boolean foundCD = results.stream()
                .anyMatch(media -> media.getTitle().equals("Thriller") && media.getMediaType().equals("CD"));
        assertTrue(foundCD);
    }
}