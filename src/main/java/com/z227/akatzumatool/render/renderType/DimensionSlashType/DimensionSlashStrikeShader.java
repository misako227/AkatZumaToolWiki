package com.z227.akatzumatool.render.renderType.DimensionSlashType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

// DimensionSlashStrikeShader 管理次元斩连续斩击 core shader。
public class DimensionSlashStrikeShader {
    public static ShaderInstance shader; // 当前 shader 实例。
    public static AbstractUniform effectParams; // x=时间，y=bloom强度，z=保留，w=保留。
    public static AbstractUniform uView; // 世界到视图矩阵。

    // 创建 Minecraft core shader，顶点格式使用 POSITION_COLOR_TEX。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "dimension_slash_strike"),
                DefaultVertexFormat.POSITION_COLOR_TEX
        );
    }

    // shader 热重载完成后缓存 uniform。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        effectParams = shaderInstance.safeGetUniform("EffectParams");
        uView = shaderInstance.safeGetUniform("uView");
    }

    // 渲染批次开始前写入时间和 bloom 强度。
    public static void setEffectParams(float time, float bloomStrength) {
        effectParams.set(time, bloomStrength, 0.0F, 0.0F);
    }

    // 渲染批次开始前写入视图矩阵。
    public static void setView(Matrix4f view) {
        uView.set(view);
    }

    public static ShaderInstance getShader() {
        return shader;
    }

    public static boolean isLoaded() {
        return shader != null;
    }
}
