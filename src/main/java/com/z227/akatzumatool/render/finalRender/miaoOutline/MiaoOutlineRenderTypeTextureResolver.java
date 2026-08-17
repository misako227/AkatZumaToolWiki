package com.z227.akatzumatool.render.finalRender.miaoOutline;

import com.z227.akatzumatool.mixin.RenderStateShardEmptyTextureAccessor;
import com.z227.akatzumatool.mixin.RenderTypeCompositeRenderTypeAccessor;
import com.z227.akatzumatool.mixin.RenderTypeCompositeStateAccessor;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

// MiaoOutlineRenderTypeTextureResolver 从实体原 RenderType 中解析主纹理，供深度 mask 采样 alpha。
public class MiaoOutlineRenderTypeTextureResolver {
    // 解析 RenderType 的 cutoutTexture，失败时返回空并让调用方走纯色 mask。
    public static Optional<ResourceLocation> resolve(RenderType renderType) {
        if (!(renderType instanceof RenderTypeCompositeRenderTypeAccessor compositeAccessor)) {
            return Optional.empty();
        }
        RenderType.CompositeState state = compositeAccessor.akatzumatool$getState();
        if (!((Object) state instanceof RenderTypeCompositeStateAccessor stateAccessor)) {
            return Optional.empty();
        }
        RenderStateShard.EmptyTextureStateShard textureState = stateAccessor.akatzumatool$getTextureState();
        if (!(textureState instanceof RenderStateShardEmptyTextureAccessor textureAccessor)) {
            return Optional.empty();
        }
        return textureAccessor.akatzumatool$cutoutTexture();
    }
}
