package com.z227.akatzumatool.render.finalRender;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.z227.akatzumatool.render.frameBuffer.FBO;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.Arrays;

// PostRenderContext 缓存后处理内部常用 GL 状态，减少重复切换并集中表达阶段状态。
public class PostRenderContext {
    public int boundFramebuffer = -1; // 当前后处理认为已绑定的 GL_FRAMEBUFFER。
    public int[] drawAttachments = new int[0]; // 当前后处理认为已启用的颜色附件。
    public boolean depthTestEnabled = false; // 当前深度测试是否开启。
    public boolean depthMaskEnabled = false; // 当前深度写入是否开启。
    public int depthFunc = GL11.GL_LEQUAL; // 当前深度比较函数。
    public int minecraftVao = 0; // 当前帧 Minecraft / RenderType 上传路径使用的 VAO。

    // 绑定后处理 FBO，并按需要清理颜色附件和深度。
    public void bindFrameBuffer(FBO fbo, boolean clearDepth, int... colorAttachmentsToClear) {
        if (fbo == null) return;
        fbo.bindFrameBuffer(clearDepth, colorAttachmentsToClear);
        boundFramebuffer = fbo.getFrameBuffer();
        drawAttachments = colorAttachmentsToClear == null ? new int[0] : Arrays.copyOf(colorAttachmentsToClear, colorAttachmentsToClear.length);
    }

    // 绑定回 Minecraft 主 RenderTarget。
    public void bindMinecraftFrameBuffer(FBO currentFbo, RenderTarget renderTarget) {
        if (currentFbo == null || renderTarget == null) return;
        currentFbo.unbindFrameBuffer(renderTarget.frameBufferId);
        boundFramebuffer = renderTarget.frameBufferId;
        drawAttachments = new int[0];
    }

    // 设置单个颜色附件为绘制目标。
    public void setDrawBuffer(FBO fbo, int attachment) {
        if (fbo == null) return;
        if (drawAttachments.length == 1 && drawAttachments[0] == attachment) return;
        fbo.setDrawBuffer(attachment);
        drawAttachments = new int[]{attachment};
    }

    // 设置多个颜色附件为 MRT 绘制目标。
    public void setDrawBuffers(FBO fbo, int... attachments) {
        if (fbo == null || attachments == null) return;
        if (Arrays.equals(drawAttachments, attachments)) return;
        fbo.setDrawBuffers(attachments);
        drawAttachments = Arrays.copyOf(attachments, attachments.length);
    }

    // 清空指定颜色附件，清理后记录当前 draw buffer 已被切到该附件。
    public void clearColorAttachment(FBO fbo, int attachment, float red, float green, float blue, float alpha) {
        if (fbo == null) return;
        fbo.clearColorAttachment(attachment, red, green, blue, alpha);
        drawAttachments = new int[]{attachment};
    }

    // 设置当前阶段深度状态，深度写入和比较函数每次显式写入，避免外部 GL 状态未知导致漏设。
    public void setDepthState(boolean depthTest, boolean depthMask, int newDepthFunc) {
        // RenderType.clearRenderState 会绕过本缓存修改真实 GL 状态，因此不能根据缓存跳过 enable/disable。
        if (depthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDepthMask(depthMask);
        GL11.glDepthFunc(newDepthFunc);
        depthTestEnabled = depthTest;
        depthMaskEnabled = depthMask;
        depthFunc = newDepthFunc;
    }

    // 记录当前帧进入后处理前的 Minecraft VAO，供 RenderType 批处理阶段恢复使用。
    public void beginFrame(int minecraftVao) {
        this.minecraftVao = minecraftVao;
    }

    // 准备执行 Minecraft RenderType / VertexConsumer 批处理，避免 VAO 0 导致 Array object is not active。
    public void prepareMinecraftBufferSource() {
        GL20.glUseProgram(0);
        if (minecraftVao != 0) {
            GL30.glBindVertexArray(minecraftVao);
        }
    }

    // 准备进入 RenderType 阶段，清掉自管实例化绘制残留的 shader、VAO、blend 和 depth 状态。
    public void prepareRenderTypePhase(boolean depthTest, boolean depthMask, int newDepthFunc) {
        prepareMinecraftBufferSource();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        setDepthState(depthTest, depthMask, newDepthFunc);
    }

    // 兼容旧调用名，语义改为恢复 Minecraft BufferSource 需要的 VAO。
    public void resetForMinecraftBufferSource() {
        prepareMinecraftBufferSource();
    }
}
