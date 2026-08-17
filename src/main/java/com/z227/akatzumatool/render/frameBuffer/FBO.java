package com.z227.akatzumatool.render.frameBuffer;


import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class FBO {
    public static final int NONE = 0;
    public static final int DEPTH_TEXTURE = 1;
    public static final int DEPTH_RENDER_BUFFER = 2;
    public static final int DEPTH_STENCIL_TEXTURE = 3; // packed depth-stencil 纹理附件类型。
    public static final int DEPTH_STENCIL_RENDER_BUFFER = 4; // packed depth-stencil renderbuffer 附件类型。

    private int width;
    private int height;
    private int w,h;

    private int frameBuffer;
    private int depthBuffer;
    private int[] colourTextures;
    private int colorAttachmentCount;
    private int depthTexture;
    private int depthBufferType;
    private int depthInternalFormat; // 当前深度附件使用的 OpenGL 内部格式。

    public FBO(int width, int height, int depthBufferType) {
        this(width, height, depthBufferType, 1);
    }

    public FBO(int width, int height, int depthBufferType, int colorAttachmentCount) {
        this(width, height, depthBufferType, colorAttachmentCount, defaultInternalFormatForDepthBufferType(depthBufferType));
    }

    public FBO(int width, int height, int depthBufferType, int colorAttachmentCount, int depthInternalFormat) {
        this.width = width;
        this.height = height;
        this.depthBufferType = depthBufferType;
        this.depthInternalFormat = depthInternalFormat;
        this.colorAttachmentCount = Math.max(1, colorAttachmentCount);
        initialiseFrameBuffer(depthBufferType);
        w = Minecraft.getInstance().getWindow().getWidth();
        h = Minecraft.getInstance().getWindow().getHeight();
    }

    /**
     * 重建 FBO 以适应新的窗口尺寸。
     * 清除旧的 GPU 资源并按新尺寸重新创建。
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        resize(newWidth, newHeight, depthBufferType, depthInternalFormat);
    }

    // 按指定深度类型重建 FBO，供主 FBO stencil 状态变化时同步附件格式。
    public void resize(int newWidth, int newHeight, int newDepthBufferType, int newDepthInternalFormat) {
        if (newWidth <= 0 || newHeight <= 0) return;
        cleanUp();
        this.width = newWidth;
        this.height = newHeight;
        this.depthBufferType = newDepthBufferType;
        this.depthInternalFormat = newDepthInternalFormat;
        initialiseFrameBuffer(depthBufferType);
        this.w = Minecraft.getInstance().getWindow().getWidth();
        this.h = Minecraft.getInstance().getWindow().getHeight();
    }

    public void bindFrameBuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
//        configureDrawBuffers();
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClearDepth(1.0D);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glViewport(0, 0, width, height);
    }

    // 绑定 FBO，并只清空指定颜色附件；可选择保留已经拷贝过来的深度。
    public void bindFrameBuffer(boolean clearDepth, int... colorAttachmentsToClear) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
        GL11.glViewport(0, 0, width, height);
        GL11.glClearColor(0f, 0f, 0f, 0f);

        if (colorAttachmentsToClear != null) {
            for (int attachmentIndex : colorAttachmentsToClear) {
                clearColorAttachment(attachmentIndex, 0f, 0f, 0f, 0f);
            }
        }

        if (clearDepth) {
            GL11.glClearDepth(1.0D);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }

        if (colorAttachmentsToClear != null && colorAttachmentsToClear.length > 0) {
            setDrawBuffers(colorAttachmentsToClear);
        }
    }

    public void unbindFrameBuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, w, h);
    }

    public void unbindFrameBuffer(int fbo) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, w, h);
    }

    public void initialiseFrameBuffer(int type) {
        createFrameBuffer();
        createTextureAttachment();

        if (type == DEPTH_RENDER_BUFFER) {
            createDepthBufferAttachment();
        } else if (type == DEPTH_TEXTURE) {
            createDepthTextureAttachment();
        } else if (type == DEPTH_STENCIL_TEXTURE) {
            createDepthStencilTextureAttachment();
        } else if (type == DEPTH_STENCIL_RENDER_BUFFER) {
            createDepthStencilBufferAttachment();
        }
        checkFrameBufferComplete();
        unbindFrameBuffer();
    }

    private void createFrameBuffer() {
        frameBuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
    }

    private void createTextureAttachment() {
        colourTextures = new int[colorAttachmentCount];

        // Each color attachment gets its own texture. Attachment 0 stays compatible with the old single-texture path.
        for (int i = 0; i < colorAttachmentCount; i++) {
            int colourTexture = GL11.glGenTextures();
            colourTextures[i] = colourTexture;

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, colourTexture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                    (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, GL11.GL_TEXTURE_2D,
                    colourTexture, 0);
        }

        configureDrawBuffers();
    }

    public void configureDrawBuffers() {
        IntBuffer buffers = BufferUtils.createIntBuffer(colorAttachmentCount);
        for (int i = 0; i < colorAttachmentCount; i++) {
            buffers.put(GL30.GL_COLOR_ATTACHMENT0 + i);
        }
        buffers.flip();
        GL20.glDrawBuffers(buffers);
    }

    // 将后续片元输出 location 0 写入指定颜色附件。
    public void setDrawBuffer(int attachmentIndex) {
        validateColorAttachmentIndex(attachmentIndex);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0 + attachmentIndex);
    }

    // 将后续 MRT 输出显式映射到指定颜色附件，避免不相关附件被本阶段触碰。
    public void setDrawBuffers(int... attachmentIndices) {
        if (attachmentIndices == null || attachmentIndices.length == 0) {
            configureDrawBuffers();
            return;
        }

        IntBuffer buffers = BufferUtils.createIntBuffer(attachmentIndices.length);
        for (int attachmentIndex : attachmentIndices) {
            validateColorAttachmentIndex(attachmentIndex);
            buffers.put(GL30.GL_COLOR_ATTACHMENT0 + attachmentIndex);
        }
        buffers.flip();
        GL20.glDrawBuffers(buffers);
    }

    // 单独清空某个颜色附件，CA2 只在有描边任务时调用以减少每帧成本。
    public void clearColorAttachment(int attachmentIndex, float red, float green, float blue, float alpha) {
        validateColorAttachmentIndex(attachmentIndex);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0 + attachmentIndex);
        GL11.glClearColor(red, green, blue, alpha);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    // 校验颜色附件索引，避免调用方把 MRT 阶段写到不存在的 attachment。
    public void validateColorAttachmentIndex(int attachmentIndex) {
        if (attachmentIndex < 0 || attachmentIndex >= colorAttachmentCount) {
            throw new IndexOutOfBoundsException("Color attachment index out of range: " + attachmentIndex);
        }
    }

    private void createDepthTextureAttachment() {
        depthTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, depthInternalFormat, width, height, 0, GL11.GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
    }

    private void createDepthBufferAttachment() {
        depthBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, depthInternalFormat, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER,
                depthBuffer);
    }

    // 创建 packed depth-stencil 纹理，和启用 stencil 的 Minecraft 主 RenderTarget 对齐。
    public void createDepthStencilTextureAttachment() {
        depthTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, depthInternalFormat, width, height, 0, GL30.GL_DEPTH_STENCIL,
                getDepthStencilPixelType(depthInternalFormat), (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
    }

    // 创建 packed depth-stencil renderbuffer，保留给只需要附件不采样深度的路径。
    public void createDepthStencilBufferAttachment() {
        depthBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, depthInternalFormat, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER,
                depthBuffer);
    }

    public void cleanUp() {
        GL30.glDeleteFramebuffers(frameBuffer);
        if (colourTextures != null) {
            for (int colourTexture : colourTextures) {
                GL11.glDeleteTextures(colourTexture);
            }
        }
        if (depthTexture > 0) {
            GL11.glDeleteTextures(depthTexture);
            depthTexture = 0;
        }
        if (depthBuffer > 0) {
            GL30.glDeleteRenderbuffers(depthBuffer);
            depthBuffer = 0;
        }
    }

    private void checkFrameBufferComplete() {
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("FBO incomplete: " + status);
        }
    }

    public int getFrameBuffer() {
        return frameBuffer;
    }

    public int getDepthBuffer() {
        return depthBuffer;
    }

    public int getColourTexture() {
        return getColourTexture(0);
    }

    public int getColourTexture(int attachmentIndex) {
        validateColorAttachmentIndex(attachmentIndex);
        return colourTextures[attachmentIndex];
    }

    public int getColorAttachmentCount() {
        return colorAttachmentCount;
    }

    public int getDepthTexture() {
        return depthTexture;
    }

    // 返回 FBO 当前深度附件类型，供后处理判断是否需要重建。
    public int getDepthBufferType() {
        return depthBufferType;
    }

    // 返回本模组 FBO 创建时使用的深度内部格式，供 FBO blit 前做兼容判断。
    public int getDepthInternalFormat() {
        if (depthBufferType == NONE) return GL11.GL_NONE;
        return depthInternalFormat;
    }

    // 根据深度附件类型返回默认内部格式。
    public static int defaultInternalFormatForDepthBufferType(int type) {
        if (type == DEPTH_TEXTURE || type == DEPTH_RENDER_BUFFER) return GL14.GL_DEPTH_COMPONENT24;
        if (type == DEPTH_STENCIL_TEXTURE || type == DEPTH_STENCIL_RENDER_BUFFER) return GL30.GL_DEPTH32F_STENCIL8;
        return GL11.GL_NONE;
    }

    // 判断内部格式是否为 packed depth-stencil。
    public static boolean isDepthStencilInternalFormat(int internalFormat) {
        return internalFormat == GL30.GL_DEPTH24_STENCIL8 || internalFormat == GL30.GL_DEPTH32F_STENCIL8;
    }

    // 根据 packed depth-stencil 内部格式选择上传类型。
    public static int getDepthStencilPixelType(int internalFormat) {
        if (internalFormat == GL30.GL_DEPTH32F_STENCIL8) return GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV;
        return GL30.GL_UNSIGNED_INT_24_8;
    }

    // 返回当前 FBO 宽度，供后处理按像素计算采样偏移。
    public int getWidth() {
        return width;
    }

    // 返回当前 FBO 高度，供后处理按像素计算采样偏移。
    public int getHeight() {
        return height;
    }
}
