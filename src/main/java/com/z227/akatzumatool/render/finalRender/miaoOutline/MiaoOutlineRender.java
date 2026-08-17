package com.z227.akatzumatool.render.finalRender.miaoOutline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.z227.akatzumatool.common.GLBuffers.RawModel;
import com.z227.akatzumatool.common.MathUtil;
import com.z227.akatzumatool.render.frameBuffer.FBO;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

// MiaoOutlineRender 把 CA2.R/G 转换成 CA0 可见描边和 CA1 bloom source。
public class MiaoOutlineRender {
    private final MiaoOutlineShader shader; // Miao 全屏描边 shader。

    public MiaoOutlineRender() {
        shader = new MiaoOutlineShader();
        shader.start();
        shader.loadTextureUnits();
        shader.stop();
    }

    // 采样目标深度 mask 并执行 UE5 风格径向边缘检测。
    public void render(FBO mainFBO, RawModel quad, MiaoOutlineStyle style, float partialTick) {
        if (mainFBO == null || quad == null || style == null) return;

        int width = mainFBO.getWidth();
        int height = mainFBO.getHeight();
        float time = MathUtil.getClientTime(partialTick);

        mainFBO.setDrawBuffers(0, 1);
        BufferUploader.reset();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        GL30.glBindVertexArray(quad.getVaoID());
        GL20.glEnableVertexAttribArray(0);

        shader.start();
        shader.loadUniforms(width, height, time, style);
        loadAtlasSpriteUvs();
        bindTextures(mainFBO);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, quad.getVertexCount());
        shader.stop();

        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
    }

    // 绑定 CA2 目标深度 mask 和 AkatZuma 自定义 atlas。
    public void bindTextures(FBO mainFBO) {
        int atlasTextureId = AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS == null ? 0 : AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.getId();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainFBO.getColourTexture(2));
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlasTextureId);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    // 写入噪声和火焰渐变在同一张 AkatZuma atlas 中的 UV 范围。
    public void loadAtlasSpriteUvs() {
        TextureAtlasSprite noise = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.noise_002_128x);
        TextureAtlasSprite gradient = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.yellow_gradient);
        shader.loadNoiseSpriteUv(noise.getU0(), noise.getV0(), noise.getU1(), noise.getV1());
        shader.loadGradientSpriteUv(gradient.getU0(), gradient.getV0(), gradient.getU1(), gradient.getV1());
    }

    public void cleanUp() {
        shader.cleanUp();
    }
}
