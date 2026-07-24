package com.statusmachina.simpleauth.mixin;

import com.statusmachina.simpleauth.SimpleAuth;
import net.minecraft.network.chat.Component; // Mojang mapping
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

/**
 * Rejects a login when someone is already authenticated under the same name.
 *
 * <p>On an offline-mode server a username always resolves to the same UUID, so a second
 * connection using an online player's name is treated by vanilla as a duplicate login:
 * it kicks the <em>existing</em> player ("logged in from another location") and hands the
 * slot to the newcomer. That lets a griefer boot a real player at will.
 *
 * <p>{@code canPlayerLogin} runs during the login handshake, before the duplicate-login
 * kick in {@code placeNewPlayer}. Injecting here turns the impostor away at that point,
 * so the authenticated player who is already online is never disconnected.
 *
 * <p>Only authenticated sessions are protected. If the existing session has not yet
 * logged in, vanilla's normal handling is left in place so a real player can still
 * recover from a stale/ghost connection.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void simpleauth$rejectDuplicateLogin(SocketAddress socketAddress,
                                                 NameAndId nameAndId,
                                                 CallbackInfoReturnable<Component> cir) {
        SimpleAuth mod = SimpleAuth.getInstance();
        if (mod == null) {
            return;
        }

        PlayerList self = (PlayerList) (Object) this;
        ServerPlayer existing = self.getPlayer(nameAndId.id());
        if (existing == null) {
            return; // nobody online under this identity
        }
        if (!mod.isAuthenticated(existing.getUUID())) {
            return; // existing session hasn't authenticated yet — let vanilla handle it
        }

        SimpleAuth.LOGGER.warn("AUTH_REJECT_DUPLICATE: user={} existing_ip={} new_addr={}",
                               nameAndId.name(), existing.getIpAddress(), socketAddress);
        cir.setReturnValue(Component.literal("§cBye."));
    }
}
