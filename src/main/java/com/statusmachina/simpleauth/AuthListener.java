package com.statusmachina.simpleauth;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component; // Mojang mapping

public class AuthListener implements ServerPlayConnectionEvents.Join {

    private final SimpleAuth mod;

    public AuthListener(SimpleAuth mod) {
        this.mod = mod;
    }

    @Override
    public void onPlayReady(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = handler.getPlayer();
        String username = player.getName().getString();
        String ipAddress = player.getIpAddress();

        // Check for valid remember session
        if (mod.getDatabase().hasValidRememberSession(username, ipAddress)) {
            // Auto-authenticate and restore default game mode (no need to save/restore since we're not locking)
            mod.authenticate(player.getUUID());
            player.sendSystemMessage(Component.literal("§a✓ Automatically authenticated! (remembered from " + ipAddress + ")"));
            return;
        }

        // Mark as unauthenticated
        mod.unauthenticate(player.getUUID());

        // Save original game mode and set to spectator
        mod.saveGameMode(player.getUUID(), player.gameMode.getGameModeForPlayer());
        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);

        // Schedule timeout (kick after 90 seconds if not authenticated)
        mod.scheduleTimeout(player.getUUID(), () -> {
            if (!mod.isAuthenticated(player.getUUID())) {
                player.connection.disconnect(Component.literal("§cAuthentication timeout! You took too long to login."));
            }
        });

        // Send auth message
        int timeoutSeconds = mod.getDatabase().getAuthTimeout();
        if (mod.getDatabase().hasPassword(username)) {
            player.sendSystemMessage(Component.literal("§eWelcome back! Please login: §6/login <password>"));
            player.sendSystemMessage(Component.literal("§7You have " + timeoutSeconds + " seconds to authenticate."));
        } else {
            player.sendSystemMessage(Component.literal("§eFirst time here! §cAsk an admin to set your password!"));
            player.sendSystemMessage(Component.literal("§7You have " + timeoutSeconds + " seconds to authenticate."));
        }
    }
}
