package com.z227.akatzumatool.render.renderType.TrailRibbonType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class TrailRibbonRenderType extends RenderType{
    private static final RenderStateShard.ShaderStateShard TrailRibbon_SHADER_STATE = new RenderStateShard.ShaderStateShard(() -> TrailRibbonShader.TrailRibbonShader);
    private static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
            "additive_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    );

    static RenderType TrailRibbon_RenderType;

    static {
        TrailRibbon_RenderType = create(
                "trail_ribbon_render_type",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256,
                true,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(TrailRibbon_SHADER_STATE)
                        .setTextureState(new RenderStateShard.TextureStateShard(AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS_LOCATION, false, true))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(true)
        );
    }

    public TrailRibbonRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                    boolean affectsCrumbling, boolean sortOnUpload,
                    Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getRenderType() {
        return TrailRibbon_RenderType;
    }
}
