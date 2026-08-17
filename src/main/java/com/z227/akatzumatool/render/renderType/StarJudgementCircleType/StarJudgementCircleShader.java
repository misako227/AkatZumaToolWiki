package com.z227.akatzumatool.render.renderType.StarJudgementCircleType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

// StarJudgementCircleShader 管理星辰裁决法阵 core shader 和 uniform 缓存。
public class StarJudgementCircleShader {
    private static ShaderInstance shader; // 当前已加载的 shader 实例。
    private static AbstractUniform effectParams; // x=时间，y=生命周期进度，z=中心展开，w=外围展开。
    private static AbstractUniform strikeParams; // x=裁决进度，y=半径，z=bloom 强度，w=保留。
    private static AbstractUniform uView; // 当前相机 view 矩阵。

    // 创建 Minecraft core shader，顶点格式使用 POSITION_COLOR_TEX。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "star_judgement_circle"),
                DefaultVertexFormat.POSITION_COLOR_TEX
        );
    }

    // shader 热重载完成后缓存 uniform。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        effectParams = shaderInstance.safeGetUniform("EffectParams");
        strikeParams = shaderInstance.safeGetUniform("StrikeParams");
        uView = shaderInstance.safeGetUniform("uView");
    }

    // 写入法阵动画参数。
    public static void setEffectParams(float time, float ageProgress, float centerProgress, float outerProgress) {
        effectParams.set(time, ageProgress, centerProgress, outerProgress);
    }

    // 写入最终裁决和 bloom 参数。
    public static void setStrikeParams(float strikeProgress, float radius, float bloomStrength, float reserved) {
        strikeParams.set(strikeProgress, radius, bloomStrength, reserved);
    }

    // 写入当前 view 矩阵。
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
