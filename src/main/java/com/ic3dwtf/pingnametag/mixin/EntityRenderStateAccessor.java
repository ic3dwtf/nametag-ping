package com.ic3dwtf.pingnametag.mixin;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderState.class)
public interface EntityRenderStateAccessor {
    @Accessor("nameTag")
    Component ping_nametag$getDisplayName();

    @Accessor("nameTag")
    void ping_nametag$setDisplayName(Component displayName);
}