package com.library.repository;

import com.library.model.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing user data
 * @author Library Team
 * @version 1.0
 */
public class UserRepository {
    private List<User> users;
    private boolean initialized = false;

    /**
     * Constructor that initializes with sample users
     */
    public UserRepository() {
        this.users = new ArrayList<>();
        if (!initialized) {
            initializeSampleUsers();
            initialized = true;
        }
    }

    /**
     * Adds sample users to the repository
     */
    private void initializeSampleUsers() {
        users.add(new User("U001", "John Smith", "john.smith@email.com"));
        users.add(new User("U002", "Emma Johnson", "emma.johnson@email.com"));
        users.add(new User("U003", "Michael Brown", "michael.brown@email.com"));
        users.add(new User("U004", "Sarah Davis", "sarah.davis@email.com"));
        users.add(new User("U005", "David Wilson", "david.wilson@email.com"));

        // Set two users with borrowing restrictions (they have unpaid fines)
        User userWithFine1 = users.get(1); // Emma Johnson - U002
        userWithFine1.setCanBorrow(false);

        User userWithFine3 = users.get(1); // Emma Johnson - U002
        userWithFine3.setCanBorrow(false);

        User userWithFine2 = users.get(3); // Sarah Davis - U004
        userWithFine2.setCanBorrow(false);
    }

    /**
     * Finds a user by user ID
     * @param userId the user ID to find
     * @return the user, or null if not found
     */
    public User findUserById(String userId) {
        return users.stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Updates a user's information
     * @param updatedUser the updated user object
     * @return true if successful, false otherwise
     */
    public boolean updateUser(User updatedUser) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId().equals(updatedUser.getUserId())) {
                users.set(i, updatedUser);
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all users in the repository
     * @return list of all users
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }
}