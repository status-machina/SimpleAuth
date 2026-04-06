package com.statusmachina.simpleauth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter to prevent brute-force login attempts.
 * Tracks failed attempts per player and enforces cooldown after threshold.
 */
public class RateLimiter {

    private static final int MAX_ATTEMPTS = 3;
    private static final long COOLDOWN_MS = 10_000; // 10 seconds
    private static final long RESET_MS = 60_000; // 1 minute

    private final Map<UUID, AttemptTracker> attempts = new ConcurrentHashMap<>();

    private static class AttemptTracker {
        int failedAttempts;
        long lastAttemptTime;
        long cooldownUntil;

        AttemptTracker() {
            this.failedAttempts = 0;
            this.lastAttemptTime = System.currentTimeMillis();
            this.cooldownUntil = 0;
        }
    }

    /**
     * Check if player is currently rate-limited.
     * @return true if player should be allowed to attempt login, false if on cooldown
     */
    public boolean checkAttempt(UUID playerUuid) {
        long now = System.currentTimeMillis();
        AttemptTracker tracker = attempts.get(playerUuid);

        if (tracker == null) {
            return true; // First attempt
        }

        // Check if cooldown expired
        if (tracker.cooldownUntil > now) {
            return false; // Still on cooldown
        }

        // Check if reset window expired (no attempts in last minute)
        if (now - tracker.lastAttemptTime > RESET_MS) {
            attempts.remove(playerUuid); // Reset tracker
            return true;
        }

        return true; // Not on cooldown
    }

    /**
     * Get remaining cooldown time in seconds (0 if not on cooldown).
     */
    public int getRemainingCooldown(UUID playerUuid) {
        long now = System.currentTimeMillis();
        AttemptTracker tracker = attempts.get(playerUuid);

        if (tracker == null || tracker.cooldownUntil <= now) {
            return 0;
        }

        return (int) Math.ceil((tracker.cooldownUntil - now) / 1000.0);
    }

    /**
     * Record a failed login attempt. Applies cooldown if threshold exceeded.
     */
    public void recordFailure(UUID playerUuid) {
        long now = System.currentTimeMillis();

        attempts.compute(playerUuid, (key, tracker) -> {
            if (tracker == null) {
                tracker = new AttemptTracker();
            }

            // Reset if outside reset window
            if (now - tracker.lastAttemptTime > RESET_MS) {
                tracker.failedAttempts = 0;
            }

            tracker.failedAttempts++;
            tracker.lastAttemptTime = now;

            // Apply cooldown if threshold exceeded
            if (tracker.failedAttempts >= MAX_ATTEMPTS) {
                tracker.cooldownUntil = now + COOLDOWN_MS;
                SimpleAuth.LOGGER.warn("RATE_LIMIT: player={} attempts={} cooldown_seconds={}",
                                      key, tracker.failedAttempts, COOLDOWN_MS / 1000);
            }

            return tracker;
        });
    }

    /**
     * Clear rate limit for player (called on successful authentication).
     */
    public void clearAttempts(UUID playerUuid) {
        attempts.remove(playerUuid);
    }

    /**
     * Clean up expired entries (call periodically).
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry ->
            now - entry.getValue().lastAttemptTime > RESET_MS
        );
    }
}
