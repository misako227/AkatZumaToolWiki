package com.z227.akatzumatool.render.renderType.GoldenSpiralType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

// GoldenSpiralRenderType 定义金色三噪声螺旋光效的 QUADS 渲染状态。
public class GoldenSpiralRenderType extends RenderType {
    public static final RenderStateShard.ShaderStateShard GOLDEN_SPIRAL_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(GoldenSpiralShader::getShader); // 金色螺旋 shader 状态。
    public static final RenderStateShard.TextureStateShard AKATZUMA_ATLAS_TEXTURE =
            new RenderStateShard.TextureStateShard(AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS_LOCATION, false, false); // 三张噪声都来自自定义图集。
    public static final RenderStateShard.TransparencyStateShard STANDARD_ALPHA_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "golden_spiral_standard_alpha_transparency",
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
            ); // 第一版使用标准 alpha，方便观察圆形 mask 和柔边。

    public static final RenderType RENDER_TYPE = create(
            "golden_spiral_render_type",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            8192,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(GOLDEN_SPIRAL_SHADER_STATE)
                    .setTextureState(AKATZUMA_ATLAS_TEXTURE)
                    .setTransparencyState(STANDARD_ALPHA_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ); // 光效写 CA0 可见层和 CA1 bloom source，保持深度测试避免穿墙过强。

    public GoldenSpiralRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                  boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getRenderType() {
        return RENDER_TYPE;
    }
}
