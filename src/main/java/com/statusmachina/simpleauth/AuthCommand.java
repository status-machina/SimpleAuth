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

        // Check rate limit
        if (!mod.getRateLimiter().checkAttempt(player.getUUID())) {
            int remainingSeconds = mod.getRateLimiter().getRemainingCooldown(player.getUUID());
            player.sendSystemMessage(Component.literal("§c✗ Too many failed attempts! Wait " +
                                                      remainingSeconds + " seconds."));
            return 0;
        }

        String password = StringArgumentType.getString(context, "password");

        // Validate password input (prevent DOS with extremely long inputs)
        String passwordError = InputValidator.validatePassword(password);
        if (passwordError != null) {
            player.sendSystemMessage(Component.literal("§c✗ " + passwordError));
            return 0;
        }

        if (!mod.getDatabase().hasPassword(player.getName().getString())) {
            player.sendSystemMessage(Component.literal("§cNo password set! Ask an admin to run: §e/setpassword " +
                player.getName().getString() + " <password>"));
            return 0;
        }

        if (mod.getDatabase().verifyPassword(player.getName().getString(), password)) {
            // Success!
            String username = player.getName().getString();
            String ipAddress = player.getIpAddress();

            SimpleAuth.LOGGER.info("AUTH_SUCCESS: user={} ip={} method=password", username, ipAddress);

            mod.authenticate(player.getUUID());
            mod.getRateLimiter().clearAttempts(player.getUUID()); // Clear rate limit on success
            mod.getDatabase().updateLastLogin(username);
            mod.getDatabase().saveSession(username, ipAddress);

            // Restore original game mode
            player.setGameMode(mod.getOriginalGameMode(player.getUUID()));

            player.sendSystemMessage(Component.literal("§a✓ Successfully authenticated!"));

            int rememberDays = mod.getDatabase().getRememberDuration();
            if (rememberDays > 0) {
                player.sendSystemMessage(Component.literal("§7You will be remembered for " + rememberDays + " days."));
            }

            return 1;
        } else {
            SimpleAuth.LOGGER.warn("AUTH_FAILURE: user={} ip={} reason=invalid_password",
                                  player.getName().getString(),
                                  player.getIpAddress());

            mod.getRateLimiter().recordFailure(player.getUUID());

            int remainingSeconds = mod.getRateLimiter().getRemainingCooldown(player.getUUID());
            if (remainingSeconds > 0) {
                player.sendSystemMessage(Component.literal("§c✗ Too many failed attempts! Wait " +
                                                          remainingSeconds + " seconds before trying again."));
            } else {
                player.sendSystemMessage(Component.literal("§c✗ Invalid password!"));
            }
            return 0;
        }
    }
}
