package com.z227.akatzumatool.render.renderType.BattoSlashType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

// BattoSlashRenderType 定义拔刀斩的普通透明混合渲染状态。
public class BattoSlashRenderType extends RenderType {
    public static final RenderStateShard.ShaderStateShard SHADER_STATE = new RenderStateShard.ShaderStateShard(BattoSlashShader::getShader); // 拔刀斩 shader 状态。
    public static final RenderStateShard.TextureStateShard AKATZUMA_ATLAS_TEXTURE = new RenderStateShard.TextureStateShard(AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS_LOCATION, false, false); // 拔刀斩使用 AkatZumaTool 自定义图集。
    public static final RenderStateShard.TransparencyStateShard STANDARD_ALPHA_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
            "batto_slash_standard_alpha_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                );
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    ); // 使用普通 alpha 混合，先保证拔刀斩本体稳定可见。
    public static final RenderType RENDER_TYPE = create(
            "batto_slash_render_type",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            16384,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(SHADER_STATE)
                    .setTextureState(AKATZUMA_ATLAS_TEXTURE)
                    .setTransparencyState(STANDARD_ALPHA_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ); // 共享拔刀斩 RenderType。

    public BattoSlashRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getRenderType() {
        return RENDER_TYPE;
    }
}
