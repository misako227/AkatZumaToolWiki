package com.z227.akatzumatool.render.renderType.ShockwaveType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinLightningVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

// ShockwaveShader 管理独立冲击波 core shader、图集采样器和每批渲染所需 uniform。
public class ShockwaveShader {
    public static ShaderInstance shader; // 当前冲击波 shader 实例。
    public static AbstractUniform effectParams; // x=时间，y=bloom强度，z=预留旧径向倍率，w=自发光强度。
    public static AbstractUniform tintParams; // xyz=UE5 冲击波蓝色自发光基调，w=预留。
    public static AbstractUniform radialParams; // x=角度倍率，y=半径倍率，z=半径归一化，w=角度偏移。
    public static AbstractUniform uvAnimParams; // x=横向偏移，y=时间速度，z=纵向偏移，w=预留。
    public static AbstractUniform shapeParams; // x=边缘淡出开始，y=边缘淡出结束，z=透明度倍率，w=预留。
    public static AbstractUniform revealParams; // x=径向出现起点，y=径向出现终点，z=起点柔化，w=终点柔化。
    public static AbstractUniform shockwaveSpriteUV; // trail_2 sprite 在 AkatZumaTool 自定义图集中的 UV 范围。
    public static AbstractUniform uView; // 世界到视图矩阵。

    // 创建 Minecraft core shader，顶点格式复用闪电的 Position/UV0/Color/BloomColor 布局。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "shockwave"),
                CoinLightningVertexFormat.FORMAT
        );
    }

    // shader 热重载完成后缓存 uniform，避免每帧按名称查找。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        effectParams = shaderInstance.safeGetUniform("EffectParams");
        tintParams = shaderInstance.safeGetUniform("TintParams");
        radialParams = shaderInstance.safeGetUniform("RadialParams");
        uvAnimParams = shaderInstance.safeGetUniform("UvAnimParams");
        shapeParams = shaderInstance.safeGetUniform("ShapeParams");
        revealParams = shaderInstance.safeGetUniform("RevealParams");
        shockwaveSpriteUV = shaderInstance.safeGetUniform("ShockwaveSpriteUV");
        uView = shaderInstance.safeGetUniform("uView");
    }

    // 写入时间、bloom 强度、预留旧径向倍率和自发光强度。
    public static void setEffectParams(float time, float bloomStrength, float reservedRadialScale, float intensity) {
        effectParams.set(time, bloomStrength, reservedRadialScale, intensity);
    }

    // 写入 UE5 材质反推得到的蓝色自发光基调。
    public static void setTintParams(float r, float g, float b, float reserved) {
        tintParams.set(r, g, b, reserved);
    }

    // 写入 VectorToRadialValue 后的二维 mask，x 控制纹理精度，y 控制圆环数量。
    public static void setRadialParams(float angleScale, float radiusScale, float radiusNormalize, float angleOffset) {
        radialParams.set(angleScale, radiusScale, radiusNormalize, angleOffset);
    }

    // 写入冲击波2材质的 AppendVector 偏移和时间滚动速度。
    public static void setUvAnimParams(float uvOffsetX, float timeSpeed, float uvOffsetY, float reserved) {
        uvAnimParams.set(uvOffsetX, timeSpeed, uvOffsetY, reserved);
    }

    // 写入外缘柔化参数，避免 billboard 顶点边缘出现明显硬边。
    public static void setShapeParams(float edgeFadeStart, float edgeFadeEnd, float opacityScale, float reserved) {
        shapeParams.set(edgeFadeStart, edgeFadeEnd, opacityScale, reserved);
    }

    // 写入径向可见窗口参数，控制冲击波图案从哪里出现、到哪里柔和结束。
    public static void setRevealParams(float visibleStart, float visibleEnd, float startSoftness, float endSoftness) {
        revealParams.set(visibleStart, visibleEnd, startSoftness, endSoftness);
    }

    // 写入 trail_2 sprite 在自定义图集中的 UV 范围。
    public static void setShockwaveSpriteUV(float u0, float v0, float u1, float v1) {
        shockwaveSpriteUV.set(u0, v0, u1, v1);
    }

    // 写入 FinalRender 传入的视图矩阵。
    public static void setView(Matrix4f view) {
        uView.set(view);
    }

    // 绑定 AkatZumaTool 自定义图集，冲击波 trail_2 sprite 从 Sampler0 采样。
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
