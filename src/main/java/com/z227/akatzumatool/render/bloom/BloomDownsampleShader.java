package com.z227.akatzumatool.render.bloom;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.render.frameBuffer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;

// BloomDownsampleShader 负责 Bloom 纹理的中心加权 5-tap 重采样，可用于降采样和远景 Bloom 回叠。
public class BloomDownsampleShader extends ShaderProgram {
    private static final ResourceLocation VERTEX_FILE = new ResourceLocation(AkatZumaTool.MODID, "shaders/post/bloom_downsample.vsh"); // 降采样顶点 shader 资源。
    private static final ResourceLocation FRAGMENT_FILE = new ResourceLocation(AkatZumaTool.MODID, "shaders/post/bloom_downsample.fsh"); // Bloom 重采样片元 shader 资源。

    private int location_inputTexture; // 输入 Bloom 纹理采样器。

    public BloomDownsampleShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }

    @Override
    public void getAllUniformLocations() {
        location_inputTexture = super.getUniformLocation("inputTexture");
    }

    // 把输入 Bloom 纹理固定绑定到纹理槽 0。
    public void loadTextureUnit() {
        super.loadInt(location_inputTexture, 0);
    }

    @Override
    public void bindAttributes() {
        super.bindAttribute(0, "position");
    }
}
