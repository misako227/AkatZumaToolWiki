package com.z227.akatzumatool.render.renderType.FlySwordType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.item.FlySwordHeldItemRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

// FlySwordHeldShader 管理手持飞剑后处理透明模型 shader 和每次重放所需 uniform。
public class FlySwordHeldShader {
    private static ShaderInstance shader; // 当前手持飞剑透明模型 shader 实例。
    private static AbstractUniform modelViewMat; // item renderer 阶段缓存的真实模型视图矩阵。
    private static AbstractUniform effectParams; // x=时间秒，y=bloom强度。
    private static AbstractUniform mainSpriteUv; // xy=主纹理最小 UV，zw=主纹理最大 UV。
    private static AbstractUniform noise1SpriteUv; // xy=第一张噪声最小 UV，zw=第一张噪声最大 UV。
    private static AbstractUniform noise2SpriteUv; // xy=第二张噪声最小 UV，zw=第二张噪声最大 UV。
    private static AbstractUniform noise3SpriteUv; // xy=第三张 RG UV 扰动噪声最小 UV，zw=最大 UV。
    private static AbstractUniform fresnelParams; // x=菲尼尔指数，y=边缘开始，z=边缘结束。
    private static AbstractUniform emissiveStrength; // 飞剑 Bloom 自发光强度。
    private static AbstractUniform gradientStartColor; // xyz=模型最低点的流动自发光颜色。
    private static AbstractUniform gradientEndColor; // xyz=模型最高点的流动自发光颜色。
    private static AbstractUniform screenSize; // xy=场景颜色纹理的实际像素尺寸。
    private static AbstractUniform noise1FlowParams; // xy=第一张噪声速度，zw=第一张噪声起始相位。
    private static AbstractUniform noise2FlowParams; // xy=第二张噪声速度，zw=第二张噪声起始相位。
    private static int sceneTextureId; // 当前帧 mcFBO 场景颜色纹理 ID。

    // 创建 Minecraft core shader，顶点格式带法线以支持 baked model 的菲尼尔计算。
    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        return new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "fly_sword/fly_sword_held"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );
    }

    // shader 热重载完成后缓存 uniform 引用。
    public static void onLoad(ShaderInstance shaderInstance) {
        shader = shaderInstance;
        modelViewMat = shaderInstance.safeGetUniform("FlySwordModelViewMat");
        effectParams = shaderInstance.safeGetUniform("EffectParams");
        mainSpriteUv = shaderInstance.safeGetUniform("MainSpriteUV");
        noise1SpriteUv = shaderInstance.safeGetUniform("Noise1SpriteUV");
        noise2SpriteUv = shaderInstance.safeGetUniform("Noise2SpriteUV");
        noise3SpriteUv = shaderInstance.safeGetUniform("Noise3SpriteUV");
        fresnelParams = shaderInstance.safeGetUniform("FresnelParams");
        emissiveStrength = shaderInstance.safeGetUniform("EmissiveStrength");
        gradientStartColor = shaderInstance.safeGetUniform("GradientStartColor");
        gradientEndColor = shaderInstance.safeGetUniform("GradientEndColor");
        screenSize = shaderInstance.safeGetUniform("ScreenSize");
        noise1FlowParams = shaderInstance.safeGetUniform("Noise1FlowParams");
        noise2FlowParams = shaderInstance.safeGetUniform("Noise2FlowParams");
    }

    // 写入 item renderer 阶段提交的真实手持模型矩阵。
    public static void setModelViewMat(Matrix4f matrix) {
        modelViewMat.set(matrix);
    }

    // 写入时间和 Bloom 强度；普通与真飞剑颜色差异由独立渐变 uniform 表达。
    public static void setEffectParams(float time, float bloomStrength) {
        effectParams.set(time, bloomStrength);
    }

    // 写入主纹理坐标基准和三张噪声图在图集中的 UV 范围。
    public static void setSpriteUvs(TextureAtlasSprite mainSprite, TextureAtlasSprite noise1Sprite,
                                    TextureAtlasSprite noise2Sprite, TextureAtlasSprite noise3Sprite) {
        setSpriteUv(mainSpriteUv, mainSprite);
        setSpriteUv(noise1SpriteUv, noise1Sprite);
        setSpriteUv(noise2SpriteUv, noise2Sprite);
        setSpriteUv(noise3SpriteUv, noise3Sprite);
    }

    // 写入单张图集 sprite 的最小和最大 UV。
    public static void setSpriteUv(AbstractUniform uniform, TextureAtlasSprite sprite) {
        if (uniform == null || sprite == null) return;
        uniform.set(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
    }

    // 写入菲尼尔指数和纯边缘阈值。
    public static void setFresnelParams(float power, float edgeStart, float edgeEnd) {
        fresnelParams.set(power, edgeStart, edgeEnd);
    }

    // 写入飞剑 Bloom 自发光强度。
    public static void setEmissiveStrength(float strength) {
        emissiveStrength.set(strength);
    }

    // 写入模型最低点使用的渐变起始颜色。
    public static void setGradientStartColor(float red, float green, float blue) {
        gradientStartColor.set(red, green, blue);
    }

    // 写入模型最高点使用的渐变结束颜色。
    public static void setGradientEndColor(float red, float green, float blue) {
        gradientEndColor.set(red, green, blue);
    }

    // 写入当前物品栈固定的两张噪声流动速度和起始相位。
    public static void setNoiseFlowParams(FlySwordHeldItemRenderer.FlySwordFlowParams flowParams) {
        if (flowParams == null) return;
        noise1FlowParams.set(flowParams.noise1SpeedX, flowParams.noise1SpeedY, flowParams.noise1PhaseX, flowParams.noise1PhaseY);
        noise2FlowParams.set(flowParams.noise2SpeedX, flowParams.noise2SpeedY, flowParams.noise2PhaseX, flowParams.noise2PhaseY);
    }

    // 写入 mcFBO 场景颜色纹理和实际尺寸，供片元 shader 进行屏幕空间折射。
    public static void setSceneParams(int textureId, int width, int height) {
        sceneTextureId = textureId;
        // ScreenSize 是 float uniform；显式转换避免调用 Uniform 的 int setter。
        screenSize.set((float) Math.max(width, 1), (float) Math.max(height, 1));
    }

    // 返回本帧用于 Sampler1 的 mcFBO 场景颜色纹理。
    public static int getSceneTextureId() {
        return sceneTextureId;
    }

    public static ShaderInstance getShader() {
        return shader;
    }

    public static boolean isLoaded() {
        return shader != null;
    }
}
