package com.statusmachina.simpleauth;

import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.sql.*;

public class Database {

    private Connection connection;

    public Database(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        try {
            File dbFile = new File(dataFolder, "auth.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            // Create table if it doesn't exist
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid TEXT PRIMARY KEY,
                        username TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        last_login INTEGER
                    )
                    """);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void setPassword(String username, String password) {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, username, password_hash) VALUES (?, ?, ?)")) {
            // Use username as temporary UUID for console-set passwords
            stmt.setString(1, username.toLowerCase());
            stmt.setString(2, username);
            stmt.setString(3, hash);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set password for " + username, e);
        }
    }

    public boolean hasPassword(String username) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT password_hash FROM players WHERE username = ? COLLATE NOCASE")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean checkPassword(String username, String password) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT password_hash FROM players WHERE username = ? COLLATE NOCASE")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    return BCrypt.checkpw(password, hash);
                }
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    public void updateLastLogin(String username) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE players SET last_login = ? WHERE username = ? COLLATE NOCASE")) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Log but don't fail auth on this
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // Already closing, ignore
        }
    }
}
