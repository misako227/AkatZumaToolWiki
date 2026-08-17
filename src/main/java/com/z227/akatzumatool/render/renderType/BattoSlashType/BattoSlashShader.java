package com.z227.akatzumatool.render.renderType.BattoSlashType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

// BattoSlashShader 管理拔刀斩 core shader 和拔刀斩专用 uniform。
public class BattoSlashShader {
    public static ShaderInstance shader; // 当前拔刀斩 shader 实例。
    public static AbstractUniform materialParams; // x=时间，y=bloom强度，z=UV扰动强度，w=总强度。
    public static AbstractUniform pannerParams; // xy=管线A滚动速度，zw=管线B滚动速度。
    public static AbstractUniform mainSpriteUV; // sword1 主纹理图集 UV 范围。
    public static AbstractUniform texBSpriteUV; // daoguang 暖色自发光贴图图集 UV 范围。
    public static AbstractUniform maskSpriteUV; // daoguang 透明遮罩贴图图集 UV 范围。
    public static AbstractUniform uView; // 世界到视图矩阵。

    // 创建 Minecraft core shader，顶点格式使用 POSITION_COLOR_TEX。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "batto_slash"),
                DefaultVertexFormat.POSITION_COLOR_TEX
        );
    }

    // shader 热重载完成后缓存 uniform。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        materialParams = shaderInstance.safeGetUniform("MaterialParams");
        pannerParams = shaderInstance.safeGetUniform("PannerParams");
        mainSpriteUV = shaderInstance.safeGetUniform("MainSpriteUV");
        texBSpriteUV = shaderInstance.safeGetUniform("TexBSpriteUV");
        maskSpriteUV = shaderInstance.safeGetUniform("MaskSpriteUV");
        uView = shaderInstance.safeGetUniform("uView");
    }

    // 写入 daoguang 材质参数。
    public static void setMaterialParams(float time, float bloomStrength, float noiseStrength, float intensity) {
        materialParams.set(time, bloomStrength, noiseStrength, intensity);
    }

    // 写入两条 daoguang panner 管线的滚动速度。
    public static void setPannerParams(float speedAX, float speedAY, float speedBX, float speedBY) {
        pannerParams.set(speedAX, speedAY, speedBX, speedBY);
    }

    // 写入 daoguang 主纹理、暖色纹理和遮罩贴图的图集 UV 范围。
    public static void setSpriteUVs(float mainU0, float mainV0, float mainU1, float mainV1,
                                    float texBU0, float texBV0, float texBU1, float texBV1,
                                    float maskU0, float maskV0, float maskU1, float maskV1) {
        mainSpriteUV.set(mainU0, mainV0, mainU1, mainV1);
        texBSpriteUV.set(texBU0, texBV0, texBU1, texBV1);
        maskSpriteUV.set(maskU0, maskV0, maskU1, maskV1);
    }

    // 写入当前视图矩阵。
    public static void setView(Matrix4f view) {
        uView.set(view);
    }

    // 为拔刀斩 shader 绑定自定义图集。
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
