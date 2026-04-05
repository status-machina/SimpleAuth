package com.statusmachina.simpleauth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack; // Mojang mapping
import net.minecraft.commands.Commands; // Mojang mapping
import net.minecraft.network.chat.Component; // Mojang mapping

public class ConsoleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, SimpleAuth mod) {
        dispatcher.register(Commands.literal("setpassword")
            .then(Commands.argument("player", StringArgumentType.string())
                .then(Commands.argument("password", StringArgumentType.string())
                    .executes(context -> executeSetPassword(context, mod)))));

        dispatcher.register(Commands.literal("setrememberusers")
            .then(Commands.argument("duration", StringArgumentType.string())
                .executes(context -> executeSetRemember(context, mod))));

        dispatcher.register(Commands.literal("setauthtimeout")
            .then(Commands.argument("seconds", StringArgumentType.string())
                .executes(context -> executeSetAuthTimeout(context, mod))));
    }

    private static int executeSetPassword(CommandContext<CommandSourceStack> context, SimpleAuth mod) {
        CommandSourceStack source = context.getSource();

        // Only allow from console (server source with no entity)
        if (source.getEntity() != null) {
            source.sendFailure(Component.literal("§cThis command can only be run from console!"));
            return 0;
        }

        String username = StringArgumentType.getString(context, "player");
        String password = StringArgumentType.getString(context, "password");

        try {
            mod.getDatabase().setPassword(username, password);
            source.sendSuccess(() -> Component.literal("§a✓ Password set for " + username), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c✗ Failed to set password: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeSetRemember(CommandContext<CommandSourceStack> context, SimpleAuth mod) {
        CommandSourceStack source = context.getSource();

        // Only allow from console (server source with no entity)
        if (source.getEntity() != null) {
            source.sendFailure(Component.literal("§cThis command can only be run from console!"));
            return 0;
        }

        String duration = StringArgumentType.getString(context, "duration");

        try {
            // Parse duration (e.g., "7d", "30d", "0d" to disable)
            if (!duration.endsWith("d")) {
                source.sendFailure(Component.literal("§c✗ Invalid format! Use: setrememberusers <days>d (e.g., 7d)"));
                return 0;
            }

            int days = Integer.parseInt(duration.substring(0, duration.length() - 1));

            if (days < 0) {
                source.sendFailure(Component.literal("§c✗ Days must be 0 or greater!"));
                return 0;
            }

            mod.getDatabase().setRememberDuration(days);

            if (days == 0) {
                source.sendSuccess(() -> Component.literal("§a✓ Remember users disabled"), true);
            } else {
                source.sendSuccess(() -> Component.literal("§a✓ Users will be remembered for " + days + " days"), true);
            }

            // Clean up expired sessions
            mod.getDatabase().cleanExpiredSessions();

            return 1;
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("§c✗ Invalid number! Use: setrememberusers <days>d (e.g., 7d)"));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c✗ Failed to set remember duration: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeSetAuthTimeout(CommandContext<CommandSourceStack> context, SimpleAuth mod) {
        CommandSourceStack source = context.getSource();

        // Only allow from console (server source with no entity)
        if (source.getEntity() != null) {
            source.sendFailure(Component.literal("§cThis command can only be run from console!"));
            return 0;
        }

        String secondsStr = StringArgumentType.getString(context, "seconds");

        try {
            int seconds = Integer.parseInt(secondsStr);

            if (seconds < 10) {
                source.sendFailure(Component.literal("§c✗ Timeout must be at least 10 seconds!"));
                return 0;
            }

            if (seconds > 600) {
                source.sendFailure(Component.literal("§c✗ Timeout cannot exceed 600 seconds (10 minutes)!"));
                return 0;
            }

            mod.getDatabase().setAuthTimeout(seconds);
            source.sendSuccess(() -> Component.literal("§a✓ Authentication timeout set to " + seconds + " seconds"), true);

            return 1;
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("§c✗ Invalid number! Use: setauthtimeout <seconds>"));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c✗ Failed to set auth timeout: " + e.getMessage()));
            return 0;
        }
    }
}
