package com.z227.akatzumatool.render.renderType.MiaoOutlineType;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

// MiaoOutlineDepthMaskRenderType 定义 Miao 深度 mask 写入 CA2 的 RenderType 状态。
public class MiaoOutlineDepthMaskRenderType extends RenderType {
    public static final RenderStateShard.ShaderStateShard SHADER_STATE =
            new RenderStateShard.ShaderStateShard(MiaoOutlineDepthMaskShader::getShader); // Miao 深度 mask shader 状态。
    public static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("minecraft", "textures/block/white_wool.png"); // 无纹理 fallback 采样用白图。
    public static final Map<ResourceLocation, RenderType> CACHE = new HashMap<>(); // 按实体原纹理缓存 RenderType。

    public MiaoOutlineDepthMaskRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                          boolean affectsCrumbling, boolean sortOnUpload,
                                          Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    // 按纹理返回共享 RenderType，fragment shader 会用该纹理 alpha 剔除透明像素。
    public static RenderType getRenderType(ResourceLocation texture) {
        ResourceLocation safeTexture = texture == null ? WHITE_TEXTURE : texture;
        return CACHE.computeIfAbsent(safeTexture, MiaoOutlineDepthMaskRenderType::createRenderType);
    }

    // 创建指定纹理的 depth mask RenderType，关闭 cull 以避免模型背面缺失。
    public static RenderType createRenderType(ResourceLocation texture) {
        return create(
                "miao_outline_depth_mask_render_type",
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
                VertexFormat.Mode.TRIANGLES,
                4096,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(SHADER_STATE)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
    }
}
