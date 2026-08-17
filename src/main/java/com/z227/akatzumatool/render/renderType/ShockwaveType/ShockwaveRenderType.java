package com.z227.akatzumatool.render.renderType.ShockwaveType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinLightningVertexFormat;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

// ShockwaveRenderType 定义独立冲击波 billboard 的 QUADS 渲染状态。
public class ShockwaveRenderType extends RenderType {
    public static final RenderStateShard.ShaderStateShard SHOCKWAVE_SHADER_STATE = new RenderStateShard.ShaderStateShard(ShockwaveShader::getShader); // 冲击波 shader 状态。
    public static final RenderStateShard.TextureStateShard AKATZUMA_ATLAS_TEXTURE = new RenderStateShard.TextureStateShard(AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS_LOCATION, false, false); // 冲击波使用 AkatZumaTool 自定义图集。
    public static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
            "shockwave_additive_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    ); // 冲击波是自发光短生命周期效果，使用加法混合接入 bloom。

    public static final RenderType SHOCKWAVE_RENDER_TYPE = create(
            "shockwave_render_type",
            CoinLightningVertexFormat.FORMAT,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(SHOCKWAVE_SHADER_STATE)
                    .setTextureState(AKATZUMA_ATLAS_TEXTURE)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ); // 共享冲击波 RenderType。

    public ShockwaveRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                               boolean affectsCrumbling, boolean sortOnUpload,
                               Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getRenderType() {
        return SHOCKWAVE_RENDER_TYPE;
    }
}
