package com.z227.akatzumatool.mixin;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// RenderTypeCompositeRenderTypeAccessor 读取 CompositeRenderType 的内部状态，用于解析实体原纹理。
@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
public interface RenderTypeCompositeRenderTypeAccessor {
    @Accessor("state")
    RenderType.CompositeState akatzumatool$getState();
}