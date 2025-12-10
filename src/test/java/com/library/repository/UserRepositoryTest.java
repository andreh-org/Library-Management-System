package com.library.repository;

import com.library.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for UserRepository
 * @author Library Team
 * @version 1.0
 */
class UserRepositoryTest {
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
    }

    @Test
    void testGetAllUsers() {
        List<User> allUsers = userRepository.getAllUsers();
        assertFalse(allUsers.isEmpty());
        assertEquals(5, allUsers.size()); // We have 5 sample users

        // Verify it returns a copy, not the original list
        int originalSize = allUsers.size();
        // Can't directly add to users list, but verify getAllUsers returns new ArrayList
        List<User> allUsersAgain = userRepository.getAllUsers();
        assertEquals(originalSize, allUsersAgain.size());
    }

    @Test
    void testUserDataIntegrity() {
        List<User> users = userRepository.getAllUsers();
        User firstUser = users.get(0);

        assertNotNull(firstUser.getUserId());
        assertNotNull(firstUser.getName());
        assertNotNull(firstUser.getEmail());

        // Check all users have valid data
        for (User user : users) {
            assertNotNull(user.getUserId());
            assertNotNull(user.getName());
            assertNotNull(user.getEmail());
            assertTrue(user.getUserId().startsWith("U"));
        }
    }

    @Test
    void testFindUserById() {
        // Find existing user
        User user = userRepository.findUserById("U001");
        assertNotNull(user);
        assertEquals("U001", user.getUserId());
        assertEquals("John Smith", user.getName());
        assertEquals("john.smith@email.com", user.getEmail());
        assertTrue(user.canBorrow()); // Should be able to borrow (no fines)
        assertTrue(user.isActive()); // Should be active

        // Find another user
        User user2 = userRepository.findUserById("U002");
        assertNotNull(user2);
        assertEquals("U002", user2.getUserId());
        assertEquals("Emma Johnson", user2.getName());
        assertFalse(user2.canBorrow()); // Has borrowing restrictions
    }

    @Test
    void testFindUserByIdNotFound() {
        User user = userRepository.findUserById("NONEXISTENT");
        assertNull(user);

        User user2 = userRepository.findUserById("");
        assertNull(user2);

        User user3 = userRepository.findUserById(null);
        assertNull(user3);
    }

    @Test
    void testFindUserByIdCaseSensitive() {
        // User IDs are case-sensitive
        User user = userRepository.findUserById("u001"); // lowercase
        assertNull(user); // Should not find with different case

        User user2 = userRepository.findUserById("U001"); // correct case
        assertNotNull(user2);
    }

    @Test
    void testUpdateUserSuccess() {
        // Get existing user
        User originalUser = userRepository.findUserById("U001");
        assertNotNull(originalUser);
        assertTrue(originalUser.canBorrow());

        // Create updated user
        User updatedUser = new User("U001", "John Smith Updated", "john.updated@email.com");
        updatedUser.setCanBorrow(false);
        updatedUser.setActive(false);

        // Update user
        boolean success = userRepository.updateUser(updatedUser);
        assertTrue(success);

        // Verify update
        User retrievedUser = userRepository.findUserById("U001");
        assertNotNull(retrievedUser);
        assertEquals("John Smith Updated", retrievedUser.getName());
        assertEquals("john.updated@email.com", retrievedUser.getEmail());
        assertFalse(retrievedUser.canBorrow());
        assertFalse(retrievedUser.isActive());

        // Other fields should remain
        assertEquals("U001", retrievedUser.getUserId());
    }

    @Test
    void testUpdateUserPartialUpdate() {
        // Test updating only some fields
        User originalUser = userRepository.findUserById("U003");
        assertNotNull(originalUser);

        // Create user with only name changed
        User updatedUser = new User("U003", "Michael Brown Updated", originalUser.getEmail());
        updatedUser.setCanBorrow(originalUser.canBorrow());
        updatedUser.setActive(originalUser.isActive());
        updatedUser.setCurrentLoans(originalUser.getCurrentLoans());

        boolean success = userRepository.updateUser(updatedUser);
        assertTrue(success);

        User retrievedUser = userRepository.findUserById("U003");
        assertEquals("Michael Brown Updated", retrievedUser.getName());
        assertEquals(originalUser.getEmail(), retrievedUser.getEmail());
        assertEquals(originalUser.canBorrow(), retrievedUser.canBorrow());
    }

    @Test
    void testUpdateUserNotFound() {
        // Try to update non-existent user
        User nonExistentUser = new User("NONEXISTENT", "Test User", "test@email.com");
        boolean success = userRepository.updateUser(nonExistentUser);
        assertFalse(success);

        // Try with null user ID
        User userWithNullId = new User(null, "Null User", "null@email.com");
        success = userRepository.updateUser(userWithNullId);
        assertFalse(success);

        // Try with empty user ID
        User userWithEmptyId = new User("", "Empty User", "empty@email.com");
        success = userRepository.updateUser(userWithEmptyId);
        assertFalse(success);
    }

    @Test
    void testUpdateUserMultipleTimes() {
        // Update same user multiple times
        User user = userRepository.findUserById("U005");
        assertNotNull(user);

        // First update
        User update1 = new User("U005", "David Wilson 1", "david1@email.com");
        assertTrue(userRepository.updateUser(update1));
        assertEquals("David Wilson 1", userRepository.findUserById("U005").getName());

        // Second update
        User update2 = new User("U005", "David Wilson 2", "david2@email.com");
        assertTrue(userRepository.updateUser(update2));
        assertEquals("David Wilson 2", userRepository.findUserById("U005").getName());

        // Third update
        User update3 = new User("U005", "David Wilson 3", "david3@email.com");
        assertTrue(userRepository.updateUser(update3));
        assertEquals("David Wilson 3", userRepository.findUserById("U005").getName());
    }

    @Test
    void testSampleUsersInitialization() {
        List<User> users = userRepository.getAllUsers();

        // Check specific users exist
        boolean hasJohnSmith = users.stream()
                .anyMatch(u -> "John Smith".equals(u.getName()) && "U001".equals(u.getUserId()));
        boolean hasEmmaJohnson = users.stream()
                .anyMatch(u -> "Emma Johnson".equals(u.getName()) && "U002".equals(u.getUserId()));
        boolean hasMichaelBrown = users.stream()
                .anyMatch(u -> "Michael Brown".equals(u.getName()) && "U003".equals(u.getUserId()));

        assertTrue(hasJohnSmith);
        assertTrue(hasEmmaJohnson);
        assertTrue(hasMichaelBrown);

        // Check borrowing restrictions are set correctly
        User emma = userRepository.findUserById("U002");
        assertNotNull(emma);
        assertFalse(emma.canBorrow()); // Has borrowing restrictions

        User sarah = userRepository.findUserById("U004");
        assertNotNull(sarah);
        assertFalse(sarah.canBorrow()); // Has borrowing restrictions

        // Other users should be able to borrow
        User john = userRepository.findUserById("U001");
        assertTrue(john.canBorrow());

        User michael = userRepository.findUserById("U003");
        assertTrue(michael.canBorrow());

        User david = userRepository.findUserById("U005");
        assertTrue(david.canBorrow());
    }

    @Test
    void testInitializeSampleUsersCalledOnce() {
        // Constructor should call initializeSampleUsers once
        UserRepository newRepo = new UserRepository();
        List<User> users = newRepo.getAllUsers();
        assertFalse(users.isEmpty());
        assertEquals(5, users.size());

        // Create another repository - should still have 5 users
        UserRepository anotherRepo = new UserRepository();
        assertEquals(5, anotherRepo.getAllUsers().size());
    }

    @Test
    void testUserFieldsPreserved() {
        User user = userRepository.findUserById("U001");
        assertNotNull(user);

        // Test all getters
        assertEquals("U001", user.getUserId());
        assertEquals("John Smith", user.getName());
        assertEquals("john.smith@email.com", user.getEmail());
        assertTrue(user.canBorrow());
        assertTrue(user.isActive());
        assertNotNull(user.getCurrentLoans());
        assertTrue(user.getCurrentLoans().isEmpty()); // New users have no loans
    }

    @Test
    void testUpdateUserWithSameData() {
        // Update user with same data
        User originalUser = userRepository.findUserById("U001");
        assertNotNull(originalUser);

        boolean success = userRepository.updateUser(originalUser);
        assertTrue(success); // Should succeed even with same data

        // User should still be findable
        User retrievedUser = userRepository.findUserById("U001");
        assertNotNull(retrievedUser);
        assertEquals(originalUser.getName(), retrievedUser.getName());
    }

    @Test
    void testUserListImmutableFromOutside() {
        List<User> users = userRepository.getAllUsers();
        int originalSize = users.size();

        // Try to clear the list (should not affect repository)
        try {
            users.clear();
            // If we get here, the list wasn't immutable
            // Check if repository was affected
            assertEquals(originalSize, userRepository.getAllUsers().size());
        } catch (UnsupportedOperationException e) {
            // Expected if list is immutable/unmodifiable
        }

        // Try to add to the list
        try {
            users.add(new User("TEST", "Test", "test@email.com"));
            assertEquals(originalSize, userRepository.getAllUsers().size());
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    void testFindUserByEmailNotSupported() {
        // This method doesn't exist, but we can test that findUserById works with email
        // Actually, we should test edge cases for findUserById

        // Test with very long user ID
        User user = userRepository.findUserById("VERY_LONG_USER_ID_THAT_DOESNT_EXIST");
        assertNull(user);

        // Test with special characters
        user = userRepository.findUserById("U001@#$");
        assertNull(user);
    }

    @Test
    void testUserRepositorySingletonInitialization() {
        // Test that initialization happens only once per instance
        UserRepository repo1 = new UserRepository();
        UserRepository repo2 = new UserRepository();

        // Both should have independent data
        assertEquals(5, repo1.getAllUsers().size());
        assertEquals(5, repo2.getAllUsers().size());

        // Update user in repo1 shouldn't affect repo2
        User user1 = repo1.findUserById("U001");
        User updatedUser = new User("U001", "Updated Name", user1.getEmail());
        repo1.updateUser(updatedUser);

        User user2 = repo2.findUserById("U001");
        assertEquals("John Smith", user2.getName()); // Should still be original name
    }

    @Test
    void testGetAllUsersReturnsDifferentInstances() {
        // Verify getAllUsers returns new ArrayList each time
        List<User> users1 = userRepository.getAllUsers();
        List<User> users2 = userRepository.getAllUsers();

        assertNotSame(users1, users2); // Should be different instances
        assertEquals(users1.size(), users2.size());

        // Contents should be equal
        for (int i = 0; i < users1.size(); i++) {
            assertEquals(users1.get(i).getUserId(), users2.get(i).getUserId());
            assertEquals(users1.get(i).getName(), users2.get(i).getName());
        }
    }

    @Test
    void testUserWithEmptyOrNullFields() {
        // Test that repository handles edge cases
        // Try to find user with null ID
        User user = userRepository.findUserById(null);
        assertNull(user);

        // Try to update with null user
        // Note: updateUser doesn't check for null parameter - would throw NPE
        // We should handle this gracefully

        // Create a user with empty fields and try to update
        User emptyUser = new User("", "", "");
        boolean success = userRepository.updateUser(emptyUser);
        assertFalse(success); // Should not find user with empty ID
    }

    @Test
    void testUpdateUserPreservesOtherFields() {
        // When updating a user, make sure other users are not affected
        User originalU001 = userRepository.findUserById("U001");
        User originalU002 = userRepository.findUserById("U002");

        assertNotNull(originalU001);
        assertNotNull(originalU002);

        // Update U001
        User updatedU001 = new User("U001", "John Updated", "john.updated@email.com");
        userRepository.updateUser(updatedU001);

        // Verify U002 is unchanged
        User currentU002 = userRepository.findUserById("U002");
        assertEquals(originalU002.getName(), currentU002.getName());
        assertEquals(originalU002.getEmail(), currentU002.getEmail());
        assertEquals(originalU002.canBorrow(), currentU002.canBorrow());
    }
}