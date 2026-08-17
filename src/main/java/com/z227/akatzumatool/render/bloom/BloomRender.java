package com.z227.akatzumatool.render.bloom;

import com.z227.akatzumatool.common.GLBuffers.RawModel;
import com.z227.akatzumatool.render.frameBuffer.FBO;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class BloomRender {

    public static final int DEFAULT_ITERATIONS = 3; // 默认近景高斯模糊迭代次数。
    public static final int DEFAULT_FAR_ITERATIONS = 4; // 默认远景高斯模糊迭代次数，用于生成更大范围柔光。
    public static final float BLOOM_SCALE = 0.5F; // 近景 Bloom Ping-Pong 缓冲相对窗口的分辨率比例。
    public static final float FAR_BLOOM_SCALE = 0.25F; // 远景 Bloom Ping-Pong 缓冲相对窗口的分辨率比例。
    public static final float DEFAULT_SCREEN_BLUR_RADIUS = 1.35F; // 目标屏幕空间采样半径，1 表示约一个全分辨率像素。
    public static final float DEFAULT_BLUR_RADIUS = DEFAULT_SCREEN_BLUR_RADIUS * BLOOM_SCALE; // 按近景 Bloom 分辨率补偿后的 shader 采样半径。
    public static final float DEFAULT_FAR_BLUR_RADIUS = 2.0F; // 远景 Bloom 在 1/4 分辨率上直接使用更大采样半径。

    private final BloomShader shader; // 单向高斯模糊 shader。
    private final BloomDownsampleShader downsampleShader; // 全分辨率 Bloom source 的预过滤降采样 shader。
    private final FBO blurFboA; // 半分辨率 Ping-Pong 缓冲 A。
    private final FBO blurFboB; // 半分辨率 Ping-Pong 缓冲 B。
    private final FBO farBlurFboA; // 四分之一分辨率远景 Bloom Ping-Pong 缓冲 A。
    private final FBO farBlurFboB; // 四分之一分辨率远景 Bloom Ping-Pong 缓冲 B。

    private int resultTexture; // 当前最终 Bloom 纹理。
    private int iterations = DEFAULT_ITERATIONS; // 当前高斯模糊迭代次数。
    private int farIterations = DEFAULT_FAR_ITERATIONS; // 当前远景高斯模糊迭代次数。
    private float blurRadius = DEFAULT_BLUR_RADIUS; // 当前高斯采样半径倍率。
    private float farBlurRadius = DEFAULT_FAR_BLUR_RADIUS; // 当前远景高斯采样半径倍率。

    public BloomRender() {
        int width = Minecraft.getInstance().getWindow().getWidth();
        int height = Minecraft.getInstance().getWindow().getHeight();
        shader = new BloomShader();
        shader.start();
        shader.loadTextureUnit();
        shader.stop();
        downsampleShader = new BloomDownsampleShader();
        downsampleShader.start();
        downsampleShader.loadTextureUnit();
        downsampleShader.stop();
        blurFboA = new FBO(getBloomWidth(width), getBloomHeight(height), FBO.NONE);
        blurFboB = new FBO(getBloomWidth(width), getBloomHeight(height), FBO.NONE);
        farBlurFboA = new FBO(getFarBloomWidth(width), getFarBloomHeight(height), FBO.NONE);
        farBlurFboB = new FBO(getFarBloomWidth(width), getFarBloomHeight(height), FBO.NONE);
        resultTexture = blurFboA.getColourTexture();
    }

    // 先生成半分辨率近景 Bloom，再生成 1/4 分辨率远景 Bloom，并把远景柔光加法合并回近景结果。
    public int render(int sourceTexture, RawModel quad, int preFbo) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_BLEND);
        // Bloom 中间 pass 依赖全屏覆盖写入，先关闭裁剪避免 no-clear 后留下旧像素。
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        BufferUploader.reset();
        GL30.glBindVertexArray(quad.getVaoID());
        GL20.glEnableVertexAttribArray(0);

        // 中心加权 5-tap 预过滤兼顾细闪电清晰度与半分辨率缩小时的稳定性。
        downsampleTo(sourceTexture, blurFboA, quad);

        int nearTexture = blurPasses(blurFboA.getColourTexture(), blurFboA, blurFboB, iterations, blurRadius, quad);
        downsampleTo(nearTexture, farBlurFboA, quad);
        int farTexture = blurPasses(farBlurFboA.getColourTexture(), farBlurFboA, farBlurFboB, farIterations, farBlurRadius, quad);
        addTextureToNearBloom(farTexture, quad);

        resultTexture = blurFboA.getColourTexture();
        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        blurFboA.unbindFrameBuffer(preFbo);

        return resultTexture;
    }

    // 将输入纹理通过中心加权预过滤写入目标 FBO，目标分辨率决定降采样比例。
    public void downsampleTo(int inputTexture, FBO target, RawModel quad) {
        target.bindFrameBuffer(false);
        downsampleShader.start();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, inputTexture);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, quad.getVertexCount());
        downsampleShader.stop();
    }

    // 在指定 Ping-Pong FBO 上执行水平/垂直可分离高斯模糊，并返回最终纹理。
    public int blurPasses(int inputTexture, FBO ping, FBO pong, int passIterations, float passBlurRadius, RawModel quad) {
        int currentTexture = inputTexture;
        boolean horizontal = true;
        shader.start();

        // Ping-Pong 缓冲在水平与垂直方向间交替模糊，偶数 pass 后结果回到 ping。
        for (int i = 0; i < Math.max(1, passIterations) * 2; i++) {
            FBO target = horizontal ? pong : ping;
            target.bindFrameBuffer(false);

            shader.loadDirection(horizontal ? 1f : 0f, horizontal ? 0f : 1f);
            shader.loadBlurRadius(passBlurRadius);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, quad.getVertexCount());

            currentTexture = target.getColourTexture();
            horizontal = !horizontal;
        }

        shader.stop();
        return currentTexture;
    }

    // 把 1/4 远景 Bloom 线性放大并加法叠回半分辨率近景 Bloom，形成更大范围柔光。
    public void addTextureToNearBloom(int farTexture, RawModel quad) {
        blurFboA.bindFrameBuffer(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        downsampleShader.start();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, farTexture);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, quad.getVertexCount());
        downsampleShader.stop();
        GL11.glDisable(GL11.GL_BLEND);
    }

    // 设置高斯模糊迭代次数，至少保留一轮水平和垂直模糊。
    public void setIterations(int iterations) {
        this.iterations = Math.max(1, iterations);
    }

    // 设置高斯采样半径倍率，限制为正数以避免反向或零距离采样。
    public void setBlurRadius(float blurRadius) {
        this.blurRadius = Math.max(0.01F, blurRadius);
    }

    // 设置远景高斯模糊迭代次数，数值越大远处低频柔光越明显。
    public void setFarIterations(int farIterations) {
        this.farIterations = Math.max(1, farIterations);
    }

    // 设置远景高斯采样半径倍率，限制为正数以避免反向或零距离采样。
    public void setFarBlurRadius(float farBlurRadius) {
        this.farBlurRadius = Math.max(0.01F, farBlurRadius);
    }

    // 根据窗口宽度计算 Bloom 半分辨率宽度，奇数尺寸向上取整。
    public int getBloomWidth(int windowWidth) {
        return Math.max(1, (int) Math.ceil(windowWidth * BLOOM_SCALE));
    }

    // 根据窗口高度计算 Bloom 半分辨率高度，奇数尺寸向上取整。
    public int getBloomHeight(int windowHeight) {
        return Math.max(1, (int) Math.ceil(windowHeight * BLOOM_SCALE));
    }

    // 根据窗口宽度计算远景 Bloom 四分之一分辨率宽度，奇数尺寸向上取整。
    public int getFarBloomWidth(int windowWidth) {
        return Math.max(1, (int) Math.ceil(windowWidth * FAR_BLOOM_SCALE));
    }

    // 根据窗口高度计算远景 Bloom 四分之一分辨率高度，奇数尺寸向上取整。
    public int getFarBloomHeight(int windowHeight) {
        return Math.max(1, (int) Math.ceil(windowHeight * FAR_BLOOM_SCALE));
    }

    public int getResultTexture() {
        return resultTexture;
    }

    // 窗口变化时同步重建近景半分辨率和远景四分之一分辨率 Bloom Ping-Pong 缓冲。
    public void resize(int width, int height) {
        blurFboA.resize(getBloomWidth(width), getBloomHeight(height));
        blurFboB.resize(getBloomWidth(width), getBloomHeight(height));
        farBlurFboA.resize(getFarBloomWidth(width), getFarBloomHeight(height));
        farBlurFboB.resize(getFarBloomWidth(width), getFarBloomHeight(height));
        resultTexture = blurFboA.getColourTexture();
    }

    // 释放 Bloom shader 与近景/远景临时纹理。
    public void cleanUp() {
        shader.cleanUp();
        downsampleShader.cleanUp();
        blurFboA.cleanUp();
        blurFboB.cleanUp();
        farBlurFboA.cleanUp();
        farBlurFboB.cleanUp();
    }
}
