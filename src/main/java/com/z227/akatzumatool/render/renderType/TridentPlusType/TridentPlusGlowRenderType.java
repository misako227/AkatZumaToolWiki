package com.z227.akatzumatool.render.renderType.TridentPlusType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

// TridentPlusGlowRenderType 定义天雷战戟蓄力蓝色模型覆盖层的加法渲染状态。
public class TridentPlusGlowRenderType extends RenderType {
    private static final RenderStateShard.ShaderStateShard TRIDENT_PLUS_GLOW_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(TridentPlusGlowShader::getShader); // 战戟蓝光 shader 状态。

    private static final RenderStateShard.TextureStateShard TRIDENT_TEXTURE =
            new RenderStateShard.TextureStateShard(TridentModel.TEXTURE, false, false); // 原版三叉戟模型纹理。

    private static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "trident_plus_glow_additive_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            ); // 蓝光覆盖层使用加法混合叠在原版模型之上。

    private static final RenderType TRIDENT_PLUS_GLOW_RENDER_TYPE = create(
            "trident_plus_glow_render_type",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(TRIDENT_PLUS_GLOW_SHADER_STATE)
                    .setTextureState(TRIDENT_TEXTURE)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ); // 共享战戟蓝光 RenderType。

    public TridentPlusGlowRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                     boolean affectsCrumbling, boolean sortOnUpload,
                                     Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getRenderType() {
        return TRIDENT_PLUS_GLOW_RENDER_TYPE;
    }
}
