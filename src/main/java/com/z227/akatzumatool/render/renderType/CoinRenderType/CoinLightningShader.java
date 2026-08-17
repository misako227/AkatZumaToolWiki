package com.z227.akatzumatool.render.renderType.CoinRenderType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

// CoinLightningShader 管理闪电 core shader、单图集 sampler 和每批渲染使用的 uniform。
public class CoinLightningShader {
    public static ShaderInstance shader; // 当前闪电 shader 实例。
    public static AbstractUniform effectParams; // x=时间，y=bloom强度，z=UV扰动强度，w=自发光强度。
    public static AbstractUniform renderFlags; // x=效果类型，y=bloom开关，zw=预留。
    public static AbstractUniform pannerParams; // xy=噪声滚动速度，z=闪烁强度，w=预留。
    public static AbstractUniform bloomParams; // x=条带 Bloom alpha，y=条带 Bloom 颜色，z=核心 Bloom alpha 兜底，w=核心 Bloom 颜色兜底。
    public static AbstractUniform lightningSpriteUV; // 闪电主纹理 sprite 在自定义图集中的 UV 范围。
    public static AbstractUniform noiseSpriteUV; // 第一张闪电噪声 sprite 在自定义图集中的 UV 范围。
    public static AbstractUniform noiseSpriteUVAlt; // 第二张闪电噪声 sprite 在自定义图集中的 UV 范围。
    public static AbstractUniform uView; // 世界到视图矩阵。

    // 创建 Minecraft core shader，顶点格式与 RenderType 的闪电专用格式保持一致。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "coin_lightning"),
                CoinLightningVertexFormat.FORMAT
        );
    }

    // shader 热重载完成后缓存 uniform，避免每帧按名称查找。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        effectParams = shaderInstance.safeGetUniform("EffectParams");
        renderFlags = shaderInstance.safeGetUniform("RenderFlags");
        pannerParams = shaderInstance.safeGetUniform("PannerParams");
        bloomParams = shaderInstance.safeGetUniform("BloomParams");
        lightningSpriteUV = shaderInstance.safeGetUniform("LightningSpriteUV");
        noiseSpriteUV = shaderInstance.safeGetUniform("NoiseSpriteUV");
        noiseSpriteUVAlt = shaderInstance.safeGetUniform("NoiseSpriteUVAlt");
        uView = shaderInstance.safeGetUniform("uView");
    }

    // 写入时间、bloom 强度、UV 扰动强度和自发光强度。
    public static void setEffectParams(float time, float bloomStrength, float noiseStrength, float intensity) {
        effectParams.set(time, bloomStrength, noiseStrength, intensity);
    }

    // 写入噪声滚动速度和闪烁强度。
    public static void setPannerParams(float speedX, float speedY, float flickerStrength, float reserved) {
        pannerParams.set(speedX, speedY, flickerStrength, reserved);
    }

    // 写入 Bloom 边缘环参数。
    public static void setBloomParams(float ribbonAlpha, float ribbonColor, float coreAlphaFallback, float coreColorFallback) {
        bloomParams.set(ribbonAlpha, ribbonColor, coreAlphaFallback, coreColorFallback);
    }

    // 写入渲染模式和 bloom 开关，当前闪电固定使用黑底亮度 mask 主纹理。
    public static void setRenderFlags(int effectType, int bloomEnabled, int reserved0, int reserved1) {
        renderFlags.set(effectType, bloomEnabled, reserved0, reserved1);
    }

    // 写入闪电主纹理 sprite 在自定义图集中的 UV 范围。
    public static void setLightningSpriteUV(float u0, float v0, float u1, float v1) {
        lightningSpriteUV.set(u0, v0, u1, v1);
    }

    // 写入第一张闪电噪声 sprite 在自定义图集中的 UV 范围。
    public static void setNoiseSpriteUV(float u0, float v0, float u1, float v1) {
        noiseSpriteUV.set(u0, v0, u1, v1);
    }

    // 写入第二张闪电噪声 sprite 在自定义图集中的 UV 范围。
    public static void setNoiseSpriteUVAlt(float u0, float v0, float u1, float v1) {
        noiseSpriteUVAlt.set(u0, v0, u1, v1);
    }

    // 写入 FinalRender 传入的视图矩阵。
    public static void setView(Matrix4f view) {
        uView.set(view);
    }

    // 绑定 AkatZumaTool 自定义图集，闪电主纹理和噪声 sprite 都从 Sampler0 采样。
    public static void setSamplers(int atlasTextureId) {
        if (shader == null) return;
        shader.setSampler("Sampler0", atlasTextureId);
    }

    public static ShaderInstance getShader() {
        return shader;
    }

    public static boolean isLoaded() {
        return shader != null;
    }
}
