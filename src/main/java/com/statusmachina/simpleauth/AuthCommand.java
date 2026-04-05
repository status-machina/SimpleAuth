package com.statusmachina.simpleauth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack; // Mojang mapping
import net.minecraft.commands.Commands; // Mojang mapping
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component; // Mojang mapping

public class AuthCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, SimpleAuth mod) {
        dispatcher.register(Commands.literal("login")
            .then(Commands.argument("password", StringArgumentType.string())
                .executes(context -> execute(context, mod))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, SimpleAuth mod) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cThis command is for players only!"));
            return 0;
        }

        if (mod.isAuthenticated(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§aYou are already authenticated!"));
            return 1;
        }

        String password = StringArgumentType.getString(context, "password");

        if (!mod.getDatabase().hasPassword(player.getName().getString())) {
            player.sendSystemMessage(Component.literal("§cNo password set! Ask an admin to run: §e/setpassword " +
                player.getName().getString() + " <password>"));
            return 0;
        }

        if (mod.getDatabase().checkPassword(player.getName().getString(), password)) {
            // Success!
            mod.authenticate(player.getUUID());
            mod.getDatabase().updateLastLogin(player.getName().getString());

            // Unlock the player - restore abilities based on game mode
            player.setInvulnerable(false);
            player.getAbilities().flying = false;
            // Keep mayfly if in creative/spectator, remove if survival/adventure
            player.getAbilities().mayfly = player.isCreative() || player.isSpectator();
            player.onUpdateAbilities();

            player.sendSystemMessage(Component.literal("§a✓ Successfully authenticated!"));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c✗ Invalid password!"));
            return 0;
        }
    }
}
