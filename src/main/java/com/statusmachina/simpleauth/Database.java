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

            // Create tables if they don't exist
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid TEXT PRIMARY KEY,
                        username TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        last_login INTEGER
                    )
                    """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS remember_sessions (
                        username TEXT NOT NULL,
                        ip_address TEXT NOT NULL,
                        expires_at INTEGER NOT NULL,
                        PRIMARY KEY (username, ip_address)
                    )
                    """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS config (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void setPassword(String username, String password) {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        try {
            connection.setAutoCommit(false);

            // Update password
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT OR REPLACE INTO players (uuid, username, password_hash) VALUES (?, ?, ?)")) {
                // Use username as temporary UUID for console-set passwords
                stmt.setString(1, username.toLowerCase());
                stmt.setString(2, username);
                stmt.setString(3, hash);
                stmt.executeUpdate();
            }

            // Invalidate all remember sessions for this user
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM remember_sessions WHERE username = ? COLLATE NOCASE")) {
                stmt.setString(1, username);
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    SimpleAuth.LOGGER.info("Invalidated {} remember session(s) for user: {}", deleted, username);
                }
            }

            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                SimpleAuth.LOGGER.error("Failed to rollback transaction", ex);
            }
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
            SimpleAuth.LOGGER.error("Database error checking password existence for {}: {}", username, e.getMessage());
            return false;
        }
    }

    public boolean verifyPassword(String username, String password) {
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
            SimpleAuth.LOGGER.error("Database error verifying password for {}: {}", username, e.getMessage());
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
            // Best-effort operation - log warning but don't fail authentication
            SimpleAuth.LOGGER.warn("Failed to update last login for {}: {}", username, e.getMessage());
        }
    }

    // Remember Sessions
    public void setRememberDuration(int days) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO config (key, value) VALUES ('remember_days', ?)")) {
            stmt.setString(1, String.valueOf(days));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set remember duration", e);
        }
    }

    public int getRememberDuration() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT value FROM config WHERE key = 'remember_days'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Integer.parseInt(rs.getString("value"));
                }
            }
        } catch (SQLException e) {
            SimpleAuth.LOGGER.warn("Failed to get remember duration, using default: {}", e.getMessage());
        }
        return 0;
    }

    // Auth Timeout
    public void setAuthTimeout(int seconds) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO config (key, value) VALUES ('auth_timeout_seconds', ?)")) {
            stmt.setString(1, String.valueOf(seconds));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set auth timeout", e);
        }
    }

    public int getAuthTimeout() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT value FROM config WHERE key = 'auth_timeout_seconds'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Integer.parseInt(rs.getString("value"));
                }
            }
        } catch (SQLException e) {
            SimpleAuth.LOGGER.warn("Failed to get auth timeout, using default: {}", e.getMessage());
        }
        return 45; // Default 45 seconds
    }

    public void saveSession(String username, String ipAddress) {
        int days = getRememberDuration();
        if (days <= 0) return;

        long expiresAt = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L);

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO remember_sessions (username, ip_address, expires_at) VALUES (?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, ipAddress);
            stmt.setLong(3, expiresAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Best-effort operation - log warning but don't fail authentication
            SimpleAuth.LOGGER.warn("Failed to save session for {} from {}: {}", username, ipAddress, e.getMessage());
        }
    }

    public boolean hasValidSession(String username, String ipAddress) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT expires_at FROM remember_sessions WHERE username = ? COLLATE NOCASE AND ip_address = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, ipAddress);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long expiresAt = rs.getLong("expires_at");
                    return System.currentTimeMillis() < expiresAt;
                }
            }
        } catch (SQLException e) {
            SimpleAuth.LOGGER.error("Database error checking session for {} from {}: {}", username, ipAddress, e.getMessage());
            return false;
        }
        return false;
    }

    public void cleanExpiredSessions() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM remember_sessions WHERE expires_at < ?")) {
            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                SimpleAuth.LOGGER.info("Cleaned up {} expired remember session(s)", deleted);
            }
        } catch (SQLException e) {
            // Best-effort operation - log warning but continue
            SimpleAuth.LOGGER.warn("Failed to clean expired sessions: {}", e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                SimpleAuth.LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            SimpleAuth.LOGGER.error("Error closing database connection", e);
        }
    }
}
