package com.statusmachina.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SimpleAuth implements DedicatedServerModInitializer {
    public static final String MOD_ID = "simpleauth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SimpleAuth instance;
    private Database database;
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();
    private final Map<UUID, GameType> originalGameModes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final RateLimiter rateLimiter = new RateLimiter();
    private ScheduledFuture<?> cleanupTask;
    private MinecraftServer server;

    @Override
    public void onInitializeServer() {
        instance = this;
        LOGGER.info("Initializing SimpleAuth...");

        // Initialize database
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        database = new Database(configDir.toFile());

        // Register events
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerPlayConnectionEvents.JOIN.register(new AuthListener(this));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            unauthenticate(handler.getPlayer().getUUID()); // Mojang mapping: getUUID()
        });

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AuthCommand.register(dispatcher, this);
            ConsoleCommand.register(dispatcher, this);
        });

        LOGGER.info("SimpleAuth initialized! Players will be locked until authenticated.");
    }

    private void onServerStarted(MinecraftServer server) {
        this.server = server;

        // Clean up expired sessions immediately on startup
        database.cleanExpiredSessions();

        // Schedule daily cleanup of expired sessions (runs every 24 hours)
        cleanupTask = scheduler.scheduleAtFixedRate(
            () -> server.execute(() -> database.cleanExpiredSessions()),
            24, // Initial delay: 24 hours
            24, // Period: 24 hours
            TimeUnit.HOURS
        );

        LOGGER.info("Scheduled daily cleanup of expired sessions");
    }

    public static SimpleAuth getInstance() {
        return instance;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public Database getDatabase() {
        return database;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public Set<UUID> getAuthenticatedPlayers() {
        return authenticatedPlayers;
    }

    public boolean isAuthenticated(UUID playerUuid) {
        return authenticatedPlayers.contains(playerUuid);
    }

    public void authenticate(UUID playerUuid) {
        authenticatedPlayers.add(playerUuid);
        cancelTimeout(playerUuid);
    }

    public void unauthenticate(UUID playerUuid) {
        authenticatedPlayers.remove(playerUuid);
        cancelTimeout(playerUuid);
        originalGameModes.remove(playerUuid);
    }

    public void saveGameMode(UUID playerUuid, GameType gameType) {
        originalGameModes.put(playerUuid, gameType);
    }

    public GameType getOriginalGameMode(UUID playerUuid) {
        return originalGameModes.getOrDefault(playerUuid, GameType.SURVIVAL);
    }

    public void scheduleTimeout(UUID playerUuid, Runnable task) {
        int timeoutSeconds = database.getAuthTimeout();
        // Wrap task to run on server thread
        ScheduledFuture<?> future = scheduler.schedule(() -> server.execute(task), timeoutSeconds, TimeUnit.SECONDS);
        timeoutTasks.put(playerUuid, future);
    }

    public void cancelTimeout(UUID playerUuid) {
        ScheduledFuture<?> future = timeoutTasks.remove(playerUuid);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    private void onServerStopping(MinecraftServer server) {
        LOGGER.info("SimpleAuth shutting down...");

        // Cancel all pending timeout tasks
        timeoutTasks.values().forEach(future -> {
            if (!future.isDone()) {
                future.cancel(false);
            }
        });
        timeoutTasks.clear();

        // Cancel daily cleanup task
        if (cleanupTask != null && !cleanupTask.isDone()) {
            cleanupTask.cancel(false);
        }

        // Clean up rate limiter
        rateLimiter.cleanup();

        // Shutdown scheduler (wait up to 5 seconds for tasks to complete)
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close database connection
        database.close();

        LOGGER.info("SimpleAuth shutdown complete");
    }
}
