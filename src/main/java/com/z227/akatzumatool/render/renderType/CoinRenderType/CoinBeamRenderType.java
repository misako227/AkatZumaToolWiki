package com.z227.akatzumatool.render.renderType.CoinRenderType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * 电磁炮光束 RenderType。
 * 这里完全走 Minecraft 的 RenderType 管线，几何模式固定为 QUADS。
 */
public class CoinBeamRenderType extends RenderType {
    private static final RenderStateShard.ShaderStateShard COIN_BEAM_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(CoinBeamShader::getShader);

    // 光束是自发光叠加效果，使用 ONE + ONE 让 CA0 和 CA1 都按亮度叠加。
    private static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "coin_beam_additive_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            );

    // 使用 POSITION_TEX_COLOR，把 per-beam 数据塞进顶点，减少 per-beam uniform 写入。
    private static final RenderType COIN_BEAM_RENDER_TYPE = create(
            "coin_beam_render_type",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(COIN_BEAM_SHADER_STATE)
                    .setTextureState(NO_TEXTURE)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );

    public CoinBeamRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                              boolean affectsCrumbling, boolean sortOnUpload,
                              Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    /**
     * BloomRenderQueue 通过这里获取共享 RenderType。
     */
    public static RenderType getRenderType() {
        return COIN_BEAM_RENDER_TYPE;
    }
}
