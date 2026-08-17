package com.z227.akatzumatool.render.renderType.CoinRenderType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

// CoinLightningRenderType 定义闪电纹理材质的 QUADS 渲染状态。
public class CoinLightningRenderType extends RenderType {
    public static final RenderStateShard.ShaderStateShard COIN_LIGHTNING_SHADER_STATE = new RenderStateShard.ShaderStateShard(CoinLightningShader::getShader); // 闪电 shader 状态。
    public static final RenderStateShard.TextureStateShard AKATZUMA_ATLAS_TEXTURE = new RenderStateShard.TextureStateShard(AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS_LOCATION, false, false); // 闪电使用 AkatZumaTool 自定义图集。
    public static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
            "coin_lightning_additive_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    ); // 闪电是纯发光效果，使用加法混合增强瞬时亮度。

    public static final RenderType COIN_LIGHTNING_RENDER_TYPE = create(
            "coin_lightning_render_type",
            CoinLightningVertexFormat.FORMAT,
            VertexFormat.Mode.QUADS,
            8192,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(COIN_LIGHTNING_SHADER_STATE)
                    .setTextureState(AKATZUMA_ATLAS_TEXTURE)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ); // 共享闪电 RenderType。

    public CoinLightningRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                   boolean affectsCrumbling, boolean sortOnUpload,
                                   Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getRenderType() {
        return COIN_LIGHTNING_RENDER_TYPE;
    }
}
