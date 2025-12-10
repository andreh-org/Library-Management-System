package com.library.model;

/**
 * Represents an administrator in the library system
 * @author Library Team
 * @version 1.0
 */
public class Admin {
    private String username;
    private String password;

    /**
     * Constructor for creating an admin
     * @param username the admin username
     * @param password the admin password
     */
    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}