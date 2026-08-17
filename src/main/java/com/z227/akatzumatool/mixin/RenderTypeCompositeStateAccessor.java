package com.z227.akatzumatool.mixin;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// RenderTypeCompositeStateAccessor 读取 CompositeState 的纹理状态，用于捕获描边透明剔除。
@Mixin(RenderType.CompositeState.class)
public interface RenderTypeCompositeStateAccessor {
    @Accessor("textureState")
    RenderStateShard.EmptyTextureStateShard akatzumatool$getTextureState();
}