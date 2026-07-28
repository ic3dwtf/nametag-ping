package com.ic3dwtf.pingnametag.mixin;

import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextDisplayEntityRenderState.class)
public interface TextDisplayEntityRenderStateAccessor {
    @Accessor("cachedInfo")
    Display.TextDisplay.CachedInfo ping_nametag$getTextLines();

    @Accessor("cachedInfo")
    void ping_nametag$setTextLines(Display.TextDisplay.CachedInfo textLines);
}