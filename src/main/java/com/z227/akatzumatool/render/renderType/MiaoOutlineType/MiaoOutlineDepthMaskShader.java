package com.z227.akatzumatool.render.renderType.MiaoOutlineType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

// MiaoOutlineDepthMaskShader 管理 Miao 深度 mask core shader，负责写 CA2.R/G。
public class MiaoOutlineDepthMaskShader {
    private static ShaderInstance shader; // 已加载的 Minecraft ShaderInstance。
    private static AbstractUniform uView; // fallback AABB 从世界空间转 view-space 的矩阵。
    private static AbstractUniform depthParams; // x=近端深度，y=深度范围，z=mask 值，w=alpha 阈值。

    // 创建 POSITION_TEX_COLOR_NORMAL 格式的 core shader，顶点已经处于 view-space。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "miao_outline_depth_mask"),
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );
    }

    // shader 热重载完成后保存实例和 uniform，后处理阶段通过 RenderType 使用。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        uView = shaderInstance.safeGetUniform("uView");
        depthParams = shaderInstance.safeGetUniform("DepthParams");
    }

    public static ShaderInstance getShader() {
        return shader;
    }

    public static boolean isLoaded() {
        return shader != null;
    }

    // 写入当前相机 view 矩阵，AABB fallback 使用世界空间坐标时需要它。
    public static void setView(Matrix4f viewMatrix) {
        if (uView == null || viewMatrix == null) return;
        uView.set(viewMatrix);
    }

    // 写入深度归一化参数和目标 mask 强度。
    public static void setDepthParams(float nearDepth, float depthRange, float maskValue, float alphaCutoff) {
        if (depthParams == null) return;
        depthParams.set(nearDepth, Math.max(depthRange, 1.0f), maskValue, alphaCutoff);
    }
}