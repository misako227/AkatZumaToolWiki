package com.z227.akatzumatool.render.renderType.TridentPlusType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;

// TridentPlusGlowShader 管理天雷战戟蓄力蓝色覆盖层 shader 和动态蓄力参数。
public class TridentPlusGlowShader {
    private static ShaderInstance shader; // 当前战戟蓝光 shader 实例。
    private static AbstractUniform glowParams; // x=时间，y=蓄力进度，z=蓝光强度，w=满蓄力标记。

    // 创建 Minecraft core shader，顶点格式匹配 TridentModel.renderToBuffer 写出的实体模型顶点。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "trident_plus/trident_plus_glow"),
                DefaultVertexFormat.NEW_ENTITY
        );
    }

    // shader 热重载完成后缓存 uniform，避免每帧按名称查找。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        glowParams = shaderInstance.safeGetUniform("GlowParams");
    }

    // 写入蓄力蓝光时间、进度、强度和满蓄力标记。
    public static void setGlowParams(float time, float chargeProgress, float glowStrength, float fullyCharged) {
        glowParams.set(time, chargeProgress, glowStrength, fullyCharged);
    }

    public static ShaderInstance getShader() {
        return shader;
    }

    public static boolean isLoaded() {
        return shader != null;
    }
}
