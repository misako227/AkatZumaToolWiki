package com.z227.akatzumatool.mixin;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

// RenderStateShardEmptyTextureAccessor 调用纹理状态的 cutoutTexture，取得实体 RenderType 使用的主纹理。
@Mixin(RenderStateShard.EmptyTextureStateShard.class)
public interface RenderStateShardEmptyTextureAccessor {
    @Invoker("cutoutTexture")
    Optional<ResourceLocation> akatzumatool$cutoutTexture();
}