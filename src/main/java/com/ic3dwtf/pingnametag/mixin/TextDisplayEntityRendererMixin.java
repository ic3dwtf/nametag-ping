package com.ic3dwtf.pingnametag.mixin;

import com.ic3dwtf.pingnametag.config.PingNametagConfig;
import com.ic3dwtf.pingnametag.config.PingNametagConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.regex.Matcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public abstract class TextDisplayEntityRendererMixin {

    @Shadow
    protected abstract Display.TextDisplay.CachedInfo splitLines(Component text, int width);

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Display$TextDisplay;Lnet/minecraft/client/renderer/entity/state/TextDisplayEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void ping_nametag$appendPingToPlayerMountedTextDisplay(Display.TextDisplay entity, TextDisplayEntityRenderState renderState, float tickProgress, CallbackInfo ci) {
        PingNametagConfig config = PingNametagConfigManager.get();

        if (!config.enabled) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return;
        }

        Component baseText = entity.getText();
        String baseTextValue = baseText == null ? "" : baseText.getString();
        if (baseTextValue.isEmpty()) {
            return;
        }

        int firstNewline = baseTextValue.indexOf('\n');
        String firstLine = firstNewline >= 0 ? baseTextValue.substring(0, firstNewline) : baseTextValue;

        PlayerInfo entry = null;
        Entity vehicle = entity.getVehicle();

        if (vehicle instanceof Player vehiclePlayer) {
            if (firstNewline < 0 && !firstLine.contains(vehiclePlayer.getScoreboardName())) {
                return;
            }

            if (!config.showOwnPing && vehiclePlayer.getUUID().equals(client.player.getUUID())) {
                return;
            }

            entry = client.getConnection().getPlayerInfo(vehiclePlayer.getUUID());
        } else {
            if (!firstLine.isEmpty()) {
                for (PlayerInfo candidate : client.getConnection().getOnlinePlayers()) {
                    String name = candidate.getProfile().name();

                    if (name != null && !name.isEmpty() && firstLine.contains(name)) {
                        if (!config.showOwnPing && candidate.getProfile().id().equals(client.player.getUUID())) {
                            return;
                        }

                        entry = candidate;
                        break;
                    }
                }
            }

            if (entry == null) {
                return;
            }
        }

        MutableComponent suffix;

        if (entry == null) {
            String textFormat = config.textFormat.replace("%ping%", "??");
            suffix = Component.literal(textFormat).setStyle(Style.EMPTY.withColor(0xAAAAAA));
        } else {
            int latency = Math.max(0, entry.getLatency());
            String textFormat = config.textFormat.replace("%ping%", String.valueOf(latency));
            suffix = Component.literal(textFormat).setStyle(Style.EMPTY.withColor(config.colorForPing(latency)));
        }

        Component modifiedText = ping_nametag$appendSuffixToTopLine(baseText, suffix);

        ((TextDisplayEntityRenderStateAccessor) renderState).ping_nametag$setTextLines(
                splitLines(modifiedText, entity.getLineWidth())
        );
    }

    private static Component ping_nametag$appendSuffixToTopLine(Component baseText, MutableComponent suffix) {
        if (!baseText.getString().contains("\n")) {
            return baseText.copy().append(suffix);
        }

        boolean[] inserted = {false};
        return ping_nametag$buildWithSuffixInserted(baseText, suffix, inserted);
    }

    private static MutableComponent ping_nametag$buildWithSuffixInserted(Component node, MutableComponent suffix, boolean[] inserted) {
        String ownStr = ping_nametag$ownLiteralString(node);
        MutableComponent result;

        if (!inserted[0] && ownStr.contains("\n")) {
            int nl = ownStr.indexOf('\n');

            result = Component.literal(ownStr.substring(0, nl)).setStyle(node.getStyle());
            result.append(suffix);
            result.append(Component.literal(ownStr.substring(nl)).setStyle(node.getStyle()));

            inserted[0] = true;

            for (Component sibling : node.getSiblings()) {
                result.append(sibling);
            }
        } else {
            result = MutableComponent.create(node.getContents()).setStyle(node.getStyle());

            for (Component sibling : node.getSiblings()) {
                if (inserted[0]) {
                    result.append(sibling);
                } else {
                    result.append(ping_nametag$buildWithSuffixInserted(sibling, suffix, inserted));
                }
            }

            if (!inserted[0]) {
                result.append(suffix);
                inserted[0] = true;
            }
        }

        return result;
    }

    private static String ping_nametag$ownLiteralString(Component node) {
        if (node.getContents() instanceof PlainTextContents.LiteralContents literal) {
            return literal.text();
        }

        return "";
    }
}