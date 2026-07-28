package com.ic3dwtf.pingnametag.mixin;

import com.ic3dwtf.pingnametag.config.PingNametagConfig;
import com.ic3dwtf.pingnametag.config.PingNametagConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.regex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ping_nametag$appendPingOverlayToFinalLabel(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        PingNametagConfig config = PingNametagConfigManager.get();

        if (!config.enabled) {
            return;
        }

        if (!(entity instanceof AbstractClientPlayer self)) {
            return;
        }

        Component originalText = ((EntityRenderStateAccessor) state).ping_nametag$getDisplayName();
        if (originalText == null || originalText.getString().isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return;
        }

        if (!config.showOwnPing && self.getUUID().equals(client.player.getUUID())) {
            return;
        }

        PlayerInfo entry = client.getConnection().getPlayerInfo(self.getUUID());
        if (entry == null) {
            String textFormat = config.textFormat.replaceAll("%ping%", Matcher.quoteReplacement("??"));
            MutableComponent unknownSuffix = Component.literal(textFormat).setStyle(Style.EMPTY.withColor(0xAAAAAA));
            ((EntityRenderStateAccessor) state).ping_nametag$setDisplayName(
                    ping_nametag$baseLabelWithoutPingSuffix(originalText).append(unknownSuffix)
            );
            return;
        }

        int latency = Math.max(0, entry.getLatency());
        String textFormat = config.textFormat.replaceAll("%ping%", Matcher.quoteReplacement(String.valueOf(latency)));
        MutableComponent suffix = Component.literal(textFormat)
                .setStyle(Style.EMPTY.withColor(config.colorForPing(latency)));

        ((EntityRenderStateAccessor) state).ping_nametag$setDisplayName(
                ping_nametag$baseLabelWithoutPingSuffix(originalText).append(suffix)
        );
    }

    private static MutableComponent ping_nametag$baseLabelWithoutPingSuffix(Component originalText) {
        MutableComponent baseText = originalText.copy();
        var siblings = baseText.getSiblings();
        if (siblings.isEmpty()) {
            return baseText;
        }

        Component lastSibling = siblings.get(siblings.size() - 1);
        if (ping_nametag$isPingSuffix(lastSibling.getString())) {
            siblings.remove(siblings.size() - 1);
        }

        return baseText;
    }

    private static boolean ping_nametag$isPingSuffix(String text) {
        if (!text.startsWith(" (") || !text.endsWith("ms)")) {
            return false;
        }

        String value = text.substring(2, text.length() - 3);
        if ("??".equals(value)) {
            return true;
        }

        if (value.isEmpty()) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}