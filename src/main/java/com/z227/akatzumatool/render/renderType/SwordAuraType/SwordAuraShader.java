package com.z227.akatzumatool.render.renderType.SwordAuraType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

// SwordAuraShader 管理飞剑剑气 OBJ 渲染用 core shader 和少量全局 uniform。
public class SwordAuraShader {
    private static ShaderInstance shader; // 当前 shader 实例。
    private static AbstractUniform globalParams; // x=时间，y=bloom倍率，z=保留，w=保留。
    private static AbstractUniform swordSpriteUV; // sword1 sprite 在 AkatZuma 图集中的 UV 范围。
    private static AbstractUniform gradientSpriteUV; // multi_gradient sprite 在 AkatZuma 图集中的 UV 范围。
    private static AbstractUniform blueGradientSpriteUV; // blue_gradient sprite 在 AkatZuma 图集中的 UV 范围。
    private static AbstractUniform uView; // 当前视图矩阵。
    private static AbstractUniform projectionMat; // 当前投影矩阵，直接 GL 绘制时需要手动写入。

    // 创建 Minecraft core shader，顶点格式只描述静态 mesh 的 Position/UV，实例 attribute 由 VAO 手动绑定。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "sword_aura"),
                DefaultVertexFormat.POSITION_TEX
        );
    }

    // shader 热重载完成后缓存 uniform。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        globalParams = shaderInstance.safeGetUniform("GlobalParams");
        swordSpriteUV = shaderInstance.safeGetUniform("SwordSpriteUV");
        gradientSpriteUV = shaderInstance.safeGetUniform("GradientSpriteUV");
        blueGradientSpriteUV = shaderInstance.safeGetUniform("BlueGradientSpriteUV");
        uView = shaderInstance.safeGetUniform("uView");
        projectionMat = shaderInstance.safeGetUniform("ProjMat");
    }

    // 写入全局时间和 bloom 倍率。
    public static void setGlobalParams(float time, float bloomScale) {
        globalParams.set(time, bloomScale, 0.0F, 0.0F);
    }

    // 写入 sword1、multi_gradient 和 blue_gradient 三个 sprite 的图集 UV 范围。
    public static void setSpriteUVs(float swordU0, float swordV0, float swordU1, float swordV1,
                                    float gradientU0, float gradientV0, float gradientU1, float gradientV1,
                                    float blueU0, float blueV0, float blueU1, float blueV1) {
        swordSpriteUV.set(swordU0, swordV0, swordU1, swordV1);
        gradientSpriteUV.set(gradientU0, gradientV0, gradientU1, gradientV1);
        blueGradientSpriteUV.set(blueU0, blueV0, blueU1, blueV1);
    }

    // 写入视图矩阵。
    public static void setView(Matrix4f view) {
        uView.set(view);
    }

    // 写入当前投影矩阵，补齐绕过 RenderType 后缺失的 Minecraft 默认 uniform。
    public static void setProjection(Matrix4f projection) {
        projectionMat.set(projection);
    }

    public static ShaderInstance getShader() {
        return shader;
    }

    public static boolean isLoaded() {
        return shader != null;
    }
}
