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
                    .executes(context -> execute(context, mod)))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, SimpleAuth mod) {
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
}
