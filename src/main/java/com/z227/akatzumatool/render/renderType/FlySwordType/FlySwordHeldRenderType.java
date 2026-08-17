package com.z227.akatzumatool.render.renderType.FlySwordType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

// FlySwordHeldRenderType 定义手持飞剑后处理透明模型的 MRT 渲染状态。
public class FlySwordHeldRenderType extends RenderType {
    public static final RenderStateShard.ShaderStateShard SHADER_STATE =
            new RenderStateShard.ShaderStateShard(FlySwordHeldShader::getShader); // 手持飞剑透明模型 shader 状态。

    public static final RenderStateShard.MultiTextureStateShard HELD_TEXTURES =
            RenderStateShard.MultiTextureStateShard.builder()
                    .add(AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS_LOCATION, false, false)
                    .build(); // 槽位 0 供 Sampler0 采样三张飞剑噪声，场景颜色在 draw 前动态绑定到槽位 1。

    public static final RenderStateShard.TransparencyStateShard STANDARD_ALPHA_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "fly_sword_held_standard_alpha_transparency",
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
            ); // 使用普通 alpha 混合，保持手中透明模型可控且不过曝。

    public static final RenderType RENDER_TYPE = create(
            "fly_sword_held_render_type",
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
            VertexFormat.Mode.QUADS,
            16384,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(SHADER_STATE)
                    .setTextureState(HELD_TEXTURES)
                    .setTransparencyState(STANDARD_ALPHA_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ); // 保持 NO_CULL 双面提交，透明边缘正反面行为与当前画面一致。

    public FlySwordHeldRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                  boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getRenderType() {
        return RENDER_TYPE;
    }
}
