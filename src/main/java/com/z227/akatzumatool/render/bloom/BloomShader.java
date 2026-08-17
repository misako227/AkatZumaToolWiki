package com.z227.akatzumatool.render.bloom;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.render.frameBuffer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;

public class BloomShader extends ShaderProgram {

    private static final ResourceLocation VERTEX_FILE = new ResourceLocation(AkatZumaTool.MODID, "shaders/post/bloom_blur.vsh"); // Bloom 顶点 shader 资源。
    private static final ResourceLocation FRAGMENT_FILE = new ResourceLocation(AkatZumaTool.MODID, "shaders/post/bloom_blur.fsh"); // Bloom 片元 shader 资源。

    private int location_inputTexture; // 输入 Bloom 纹理采样器。
    private int location_direction; // 当前单向模糊方向。
    private int location_blurRadius; // 当前采样半径倍率。

    public BloomShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }

    @Override
    public void getAllUniformLocations() {
        location_inputTexture = super.getUniformLocation("inputTexture");
        location_direction = super.getUniformLocation("direction");
        location_blurRadius = super.getUniformLocation("BlurRadius");
    }

    public void loadTextureUnit() {
        super.loadInt(location_inputTexture, 0);
    }

    public void loadDirection(float x, float y) {
        super.loadVector(location_direction, new Vector2f(x, y));
    }

    // 写入单向高斯模糊的采样半径倍率。
    public void loadBlurRadius(float blurRadius) {
        super.loadFloat(location_blurRadius, blurRadius);
    }

    @Override
    public void bindAttributes() {
        super.bindAttribute(0, "position");
    }
}
