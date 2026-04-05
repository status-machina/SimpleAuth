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

        // Mark as unauthenticated
        mod.unauthenticate(player.getUUID());

        // Lock the player
        player.setInvulnerable(true);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        // Send auth message
        if (mod.getDatabase().hasPassword(player.getName().getString())) {
            player.sendSystemMessage(Component.literal("§eWelcome back! Please login: §6/login <password>"));
        } else {
            player.sendSystemMessage(Component.literal("§eFirst time here! §cAsk an admin to set your password!"));
        }
    }
}
