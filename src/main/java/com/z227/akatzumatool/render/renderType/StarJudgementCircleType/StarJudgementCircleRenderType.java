package com.z227.akatzumatool.render.renderType.StarJudgementCircleType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

// StarJudgementCircleRenderType 定义星辰裁决法阵的叠加渲染状态。
public class StarJudgementCircleRenderType extends RenderType {
    private static final RenderStateShard.ShaderStateShard STAR_JUDGEMENT_CIRCLE_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(StarJudgementCircleShader::getShader); // 法阵专用 shader。

    private static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "star_judgement_circle_additive_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            ); // 法阵为自发光效果，使用加法混合写入 CA0/CA1。

    private static final RenderType STAR_JUDGEMENT_CIRCLE_RENDER_TYPE = create(
            "star_judgement_circle_render_type",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            2048,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(STAR_JUDGEMENT_CIRCLE_SHADER_STATE)
                    .setTextureState(NO_TEXTURE)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ); // 法阵只需要少量四边形，shader 内部负责细节。

    public StarJudgementCircleRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                         boolean affectsCrumbling, boolean sortOnUpload,
                                         Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getRenderType() {
        return STAR_JUDGEMENT_CIRCLE_RENDER_TYPE;
    }
}
