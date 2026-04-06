package com.statusmachina.simpleauth;

/**
 * Validates user input for usernames and passwords.
 */
public class InputValidator {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 16;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 128;

    /**
     * Validates a username.
     * @return null if valid, error message if invalid
     */
    public static String validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "Username cannot be empty";
        }

        if (username.length() < MIN_USERNAME_LENGTH) {
            return "Username must be at least " + MIN_USERNAME_LENGTH + " characters";
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            return "Username cannot exceed " + MAX_USERNAME_LENGTH + " characters";
        }

        // Minecraft usernames are alphanumeric + underscore
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "Username can only contain letters, numbers, and underscores";
        }

        return null; // Valid
    }

    /**
     * Validates a password.
     * @return null if valid, error message if invalid
     */
    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            return "Password cannot exceed " + MAX_PASSWORD_LENGTH + " characters";
        }

        // Check for printable ASCII characters only (prevent control characters)
        if (!password.matches("^[\\x20-\\x7E]+$")) {
            return "Password contains invalid characters (use only printable ASCII)";
        }

        return null; // Valid
    }
}
