package com.z227.akatzumatool.render.renderType.CoinRenderType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

/**
 * 硬币电磁炮光束 shader 入口。
 * 这里按 Minecraft 的 ShaderInstance 方式注册和保存 uniform 引用，让 RenderType 负责实际绑定。
 */
public class CoinBeamShader {
    private static ShaderInstance shader;

    // 每批光束共用的浮点参数：x=时间，y=bloom强度，z=噪声强度，w=保留。
    private static AbstractUniform effectParams;

    // 多个 int uniform 合并到一个 ivec4：x=效果类型，y=是否写入bloom，z/w=保留。
    private static AbstractUniform renderFlags;
    private static AbstractUniform uView;
    private static AbstractUniform beamCoreColor;
    private static AbstractUniform beamInnerColor;
    private static AbstractUniform beamOuterColor;

    /**
     * 创建 Minecraft core shader，顶点格式与 RenderType 的 POSITION_TEX_COLOR 保持一致。
     */
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "coin_beam"),
                DefaultVertexFormat.POSITION_COLOR_TEX
        );
    }

    /**
     * shader 热重载完成后缓存 uniform，避免每帧按名称查找。
     */
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        effectParams = shaderInstance.safeGetUniform("EffectParams");
        renderFlags = shaderInstance.safeGetUniform("RenderFlags");
        uView = shaderInstance.safeGetUniform("uView");
        beamCoreColor = shaderInstance.safeGetUniform("BeamCoreColor");
        beamInnerColor = shaderInstance.safeGetUniform("BeamInnerColor");
        beamOuterColor = shaderInstance.safeGetUniform("BeamOuterColor");
    }

    /**
     * 渲染批次开始前一次性写入浮点参数。
     */
    public static void setEffectParams(float time, float bloomStrength, float noiseStrength, float reserved) {
        effectParams.set(time, bloomStrength, noiseStrength, reserved);
    }

    /**
     * 渲染批次开始前一次性写入整型标记。
     */
    public static void setRenderFlags(int effectType, int bloomEnabled, int reserved0, int reserved1) {
        renderFlags.set(effectType, bloomEnabled, reserved0, reserved1);
    }

    public static void setView(Matrix4f view) {
        uView.set(view);
    }

    // 渲染批次开始前写入光束核心、内圈和外圈颜色。
    public static void setBeamColors(float coreR, float coreG, float coreB,
                                     float innerR, float innerG, float innerB,
                                     float outerR, float outerG, float outerB) {
        beamCoreColor.set(coreR, coreG, coreB, 1.0f);
        beamInnerColor.set(innerR, innerG, innerB, 1.0f);
        beamOuterColor.set(outerR, outerG, outerB, 1.0f);
    }

    /**
     * RenderType 的 ShaderStateShard 通过这里取得当前 shader。
     */
    public static ShaderInstance getShader() {
        return shader;
    }

    /**
     * 资源还没加载完成时跳过渲染，避免空 shader 被 RenderType 使用。
     */
    public static boolean isLoaded() {
        return shader != null;
    }
}
