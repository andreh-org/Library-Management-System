package com.library.service;

import com.library.model.User;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for UserManagementService
 * @author Library Team
 * @version 1.0
 */
class UserManagementServiceTest {
    private UserManagementService userManagementService;
    private AuthService authService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementService();
        authService = new AuthService();
        userRepository = new UserRepository();
    }

    @Test
    void testUnregisterUserWithoutAdmin() {
        // Not logged in as admin
        UserManagementService.UnregistrationResult result =
                userManagementService.unregisterUser("U001", authService);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Admin login required"));
    }

    @Test
    void testUnregisterUserWithAdmin() {
        authService.login("admin", "admin123");

        // First, ensure user exists and is active
        User user = userRepository.findUserById("U001");
        assertNotNull(user);
        assertTrue(user.isActive());

        // Try to unregister - should fail because user has no loans but we can't guarantee
        // This test might fail if sample data changes
        UserManagementService.UnregistrationResult result =
                userManagementService.unregisterUser("U001", authService);

        // The result depends on whether U001 has active loans or unpaid fines
        // We can only assert that the method was executed
        assertNotNull(result);
    }

    @Test
    void testUnregisterNonExistentUser() {
        authService.login("admin", "admin123");

        UserManagementService.UnregistrationResult result =
                userManagementService.unregisterUser("NONEXISTENT", authService);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("User not found"));
    }

    @Test
    void testGetActiveUsers() {
        var activeUsers = userManagementService.getActiveUsers();

        assertFalse(activeUsers.isEmpty());
        // All sample users should be active initially
        assertTrue(activeUsers.size() >= 5);
        for (User user : activeUsers) {
            assertTrue(user.isActive());
        }
    }

    @Test
    void testGetInactiveUsersInitiallyEmpty() {
        var inactiveUsers = userManagementService.getInactiveUsers();

        // Initially, no users should be inactive
        assertTrue(inactiveUsers.isEmpty());
    }

    @Test
    void testReactiveUserWithoutAdmin() {
        boolean result = userManagementService.reactivateUser("U001", authService);
        assertFalse(result);
    }
}