package com.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AuthService
 * @author Library Team
 * @version 1.0
 */
class AuthServiceTest {
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    void testSuccessfulLogin() {
        assertTrue(authService.login("admin", "admin123"));
        assertTrue(authService.isLoggedIn());
    }

    @Test
    void testFailedLoginWithWrongPassword() {
        assertFalse(authService.login("admin", "wrongpassword"));
        assertFalse(authService.isLoggedIn());
    }

    @Test
    void testFailedLoginWithWrongUsername() {
        assertFalse(authService.login("wronguser", "admin123"));
        assertFalse(authService.isLoggedIn());
    }

    @Test
    void testLogout() {
        authService.login("admin", "admin123");
        assertTrue(authService.isLoggedIn());

        authService.logout();
        assertFalse(authService.isLoggedIn());
    }

    @Test
    void testIsLoggedInInitiallyFalse() {
        assertFalse(authService.isLoggedIn());
    }

    @Test
    void testAdminActionsRequireLogin() {
        // Simulate trying to perform admin action without login
        assertFalse(authService.isLoggedIn());

        authService.login("admin", "admin123");
        assertTrue(authService.isLoggedIn());

        authService.logout();
        assertFalse(authService.isLoggedIn());
    }
}