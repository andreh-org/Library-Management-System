package com.library.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Admin model
 * @author Library Team
 * @version 1.0
 */
class AdminTest {
//andgeh ^_^
    @Test
    void testAdminCreation() {
        Admin admin = new Admin("admin", "admin123");

        assertEquals("admin", admin.getUsername());
        assertEquals("admin123", admin.getPassword());
    }

    @Test
    void testAdminGetters() {
        Admin admin = new Admin("librarian", "securepassword");

        // Test that getters return the correct values
        assertEquals("librarian", admin.getUsername());
        assertEquals("securepassword", admin.getPassword());
    }

    @Test
    void testAdminWithSpecialCharacters() {
        Admin admin = new Admin("admin@library", "p@ssw0rd!");

        assertEquals("admin@library", admin.getUsername());
        assertEquals("p@ssw0rd!", admin.getPassword());
    }

    @Test
    void testAdminWithLongCredentials() {
        String longUsername = "superadministratoruser";
        String longPassword = "verylongpasswordthatexceedsnormallength";

        Admin admin = new Admin(longUsername, longPassword);

        assertEquals(longUsername, admin.getUsername());
        assertEquals(longPassword, admin.getPassword());
    }
}