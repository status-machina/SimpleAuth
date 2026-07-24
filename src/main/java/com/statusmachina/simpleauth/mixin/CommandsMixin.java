package com.statusmachina.simpleauth.mixin;

import com.mojang.brigadier.ParseResults;
import com.statusmachina.simpleauth.SimpleAuth;
import net.minecraft.commands.CommandSourceStack; // Mojang mapping
import net.minecraft.commands.Commands; // Mojang mapping
import net.minecraft.network.chat.Component; // Mojang mapping
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * Blocks command execution for players who have not yet authenticated.
 *
 * <p>Putting an unauthenticated player in spectator mode does NOT stop them from
 * running commands (spectator only restricts world interaction). A player who is an
 * operator by name — which happens on offline/cracked servers — can therefore run
 * /op, /gamemode, /summon, etc. during the pre-auth window. This gate is the actual
 * lock: every command whose executing entity is an unauthenticated player is cancelled,
 * with the sole exception of /login so they can still authenticate.
 *
 * <p>Commands with no executing entity (console, command blocks, functions) are never
 * affected — {@code getEntity()} is null for those.
 */
@Mixin(Commands.class)
public class CommandsMixin {

    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void simpleauth$gateUnauthenticated(ParseResults<CommandSourceStack> parseResults,
                                                String command,
                                                CallbackInfo ci) {
        CommandSourceStack source = parseResults.getContext().getSource();
        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return; // console / command block / non-player source — leave untouched
        }

        SimpleAuth mod = SimpleAuth.getInstance();
        if (mod == null || mod.isAuthenticated(player.getUUID())) {
            return;
        }

        // Allow only /login while unauthenticated (the password is still verified by AuthCommand).
        String root = simpleauth$rootLiteral(command);
        if (root.equals("login")) {
            return;
        }

        player.sendSystemMessage(Component.literal(
            "§cYou must authenticate first: §6/login <password>"));
        SimpleAuth.LOGGER.warn("AUTH_BLOCKED_COMMAND: user={} ip={} command={}",
                               player.getName().getString(),
                               player.getIpAddress(),
                               root);
        ci.cancel();
    }

    private static String simpleauth$rootLiteral(String command) {
        String c = command.strip();
        if (c.startsWith("/")) {
            c = c.substring(1);
        }
        int space = c.indexOf(' ');
        return (space == -1 ? c : c.substring(0, space)).toLowerCase(Locale.ROOT);
    }
}
