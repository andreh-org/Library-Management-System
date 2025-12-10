package com.library.service;

import com.library.model.Admin;

/**
 * Service for handling authentication operations
 * @author Library Team
 * @version 1.0
 */
public class AuthService {
    private Admin admin;
    private boolean loggedIn;

    /**
     * Constructor that initializes admin credentials
     */
    public AuthService() {
        // Default admin credentials
        this.admin = new Admin("admin", "admin123");
        this.loggedIn = false;
    }

    /**
     * Attempts to log in with provided credentials
     * @param username the username
     * @param password the password
     * @return true if login successful, false otherwise
     */
    public boolean login(String username, String password) {
        if (admin.getUsername().equals(username) && admin.getPassword().equals(password)) {
            loggedIn = true;
            return true;
        }
        return false;
    }

    /**
     * Logs out the current admin
     */
    public void logout() {
        loggedIn = false;
    }

    /**
     * Checks if an admin is currently logged in
     * @return true if logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }
}