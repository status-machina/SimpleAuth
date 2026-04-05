package com.statusmachina.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SimpleAuth implements DedicatedServerModInitializer {
    public static final String MOD_ID = "simpleauth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SimpleAuth instance;
    private Database database;
    private final Set<UUID> authenticatedPlayers = new HashSet<>();
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

    public Set<UUID> getAuthenticatedPlayers() {
        return authenticatedPlayers;
    }

    public boolean isAuthenticated(UUID playerUuid) {
        return authenticatedPlayers.contains(playerUuid);
    }

    public void authenticate(UUID playerUuid) {
        authenticatedPlayers.add(playerUuid);
    }

    public void unauthenticate(UUID playerUuid) {
        authenticatedPlayers.remove(playerUuid);
    }
}
