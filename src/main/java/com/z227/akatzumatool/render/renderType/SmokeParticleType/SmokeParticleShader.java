package com.z227.akatzumatool.render.renderType.SmokeParticleType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

// SmokeParticleShader 管理烟雾实例化粒子 core shader 和后处理阶段需要的 uniform。
public class SmokeParticleShader {
    private static ShaderInstance shader; // 当前 shader 实例。
    private static AbstractUniform globalParams; // x=时间，y=播放帧数，z=总列数，w=总行数。
    private static AbstractUniform cameraRight; // 当前相机右方向。
    private static AbstractUniform cameraUp; // 当前相机上方向。
    private static AbstractUniform uView; // 当前视图矩阵。
    private static AbstractUniform projectionMat; // 当前投影矩阵。
    private static AbstractUniform screenSize; // 当前 mainFBO 尺寸。
    private static AbstractUniform smokeMaskParams; // x=alpha 阈值，y=软边范围，z=alpha 曲线，w=底部淡出高度。
    private static AbstractUniform softParticleParams; // x=启用标记，y=深度软化近距离，z=深度软化远距离，w=预留。

    // 创建 Minecraft core shader，顶点格式只描述静态 billboard quad，实例 attribute 由 VAO 手动绑定。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "smoke_particle"),
                DefaultVertexFormat.POSITION_TEX
        );
    }

    // shader 热重载完成后缓存 uniform。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        globalParams = shaderInstance.safeGetUniform("GlobalParams");
        cameraRight = shaderInstance.safeGetUniform("CameraRight");
        cameraUp = shaderInstance.safeGetUniform("CameraUp");
        uView = shaderInstance.safeGetUniform("uView");
        projectionMat = shaderInstance.safeGetUniform("ProjMat");
        screenSize = shaderInstance.safeGetUniform("ScreenSize");
        smokeMaskParams = shaderInstance.safeGetUniform("SmokeMaskParams");
        softParticleParams = shaderInstance.safeGetUniform("SoftParticleParams");
    }

    // 写入烟雾序列帧全局参数。
    public static void setGlobalParams(float time, float playableFrames, float columns, float rows) {
        globalParams.set(time, playableFrames, columns, rows);
    }

    // 写入相机 billboard 基向量。
    public static void setCameraBasis(float rightX, float rightY, float rightZ, float upX, float upY, float upZ) {
        cameraRight.set(rightX, rightY, rightZ, 0.0F);
        cameraUp.set(upX, upY, upZ, 0.0F);
    }

    // 写入视图矩阵。
    public static void setView(Matrix4f view) {
        uView.set(view);
    }

    // 写入当前投影矩阵，补齐绕过 RenderType 后缺失的 Minecraft 默认 uniform。
    public static void setProjection(Matrix4f projection) {
        projectionMat.set(projection);
    }

    // 写入屏幕尺寸，片元 shader 用 gl_FragCoord 换算深度纹理 UV。
    public static void setScreenSize(float width, float height) {
        screenSize.set(width, height, 0.0F, 0.0F);
    }

    // 写入纹理 alpha 曲线参数，控制透明背景边缘软化。
    public static void setSmokeMaskParams(float alphaCutoff, float smokeSoftness, float smokeGamma, float bottomFadeEnd) {
        smokeMaskParams.set(alphaCutoff, smokeSoftness, smokeGamma, bottomFadeEnd);
    }

    // 写入 soft particle 参数，用场景深度软化贴地裁剪。
    public static void setSoftParticleParams(boolean enabled, float nearDistance, float farDistance) {
        softParticleParams.set(enabled ? 1.0F : 0.0F, nearDistance, farDistance, 0.0F);
    }

    public static ShaderInstance getShader() {
        return shader;
    }

    public static boolean isLoaded() {
        return shader != null;
    }
}
