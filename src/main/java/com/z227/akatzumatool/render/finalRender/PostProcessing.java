package com.z227.akatzumatool.render.finalRender;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.GLBuffers.Loader;
import com.z227.akatzumatool.common.GLBuffers.RawModel;
import com.z227.akatzumatool.common.MathUtil;
import com.z227.akatzumatool.entity.FlySwordEntity;
import com.z227.akatzumatool.entity.sword.DimensionSlashDomainEntity;
import com.z227.akatzumatool.item.FlySwordHeldItemRenderer;
import com.z227.akatzumatool.render.bloom.BloomRender;
import com.z227.akatzumatool.render.frameBuffer.FBO;
import com.z227.akatzumatool.render.frameBuffer.GlStateSnapshot;
import com.z227.akatzumatool.render.frameBuffer.fbos.MainFBORender;
import com.z227.akatzumatool.render.finalRender.miaoOutline.MiaoOutlineRender;
import com.z227.akatzumatool.render.finalRender.miaoOutline.MiaoOutlineStyle;
import com.z227.akatzumatool.render.finalRender.miaoOutline.MiaoOutlineTask;
import com.z227.akatzumatool.render.finalRender.task.PostRenderTask;
import com.z227.akatzumatool.render.finalRender.task.PostRenderTaskRenderContext;
import com.z227.akatzumatool.render.finalRender.task.PostRenderTaskSubmitter;
import com.z227.akatzumatool.render.gpu.ParticleEmitTask;
import com.z227.akatzumatool.render.gpu.ParticleSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.List;
import java.util.Map;

/**
 * 后处理总入口。
 * 这里维护 Minecraft 主画面拷贝、模组 mainFBO、bloom blur 和最终合成流程。
 */
public class PostProcessing {
    private static final float[] POSITIONS = { -1, 1, -1, -1, 1, 1, 1, -1 };
    private static final float MAX_PARTICLE_DELTA_SECONDS = 0.05f;

    private final RawModel quad;
    private final FinalRender<Entity> finalRender;
    private final BloomRender bloomRender;
    private final MainFBORender mainFBO;
    private final FBO mcFBO;
    private final ParticleSystem particleSystem;
    private final GlStateSnapshot snapshot;
    private final DimensionSlashScreenEffect dimensionSlashScreenEffect;
    private final MiaoOutlineRender miaoOutlineRender; // Miao UE5 风格径向深度描边后处理器。
    private final PostRenderContext postRenderContext; // 后处理内部 GL 状态缓存。
    private final PostRenderTaskSubmitter taskSubmitter; // 无实体后处理效果语义化提交入口。
    private final ScreenDarkeningEffect screenDarkeningEffect; // 通用屏幕暗化效果，先压暗原版场景再渲染本模组特效。

    private float partialTick;
    private Camera camera;
    private Matrix4f viewMatrix;

    private int lastWidth;
    private int lastHeight;
    private float lastFrameClientTime = -1f;

    private boolean isRendering = false;

    /**
     * 初始化所有后处理资源。
     */
    public PostProcessing(Loader loader) {
        quad = loader.loadToVAO(POSITIONS, 2);
        finalRender = new FinalRender<>();
        bloomRender = new BloomRender();

        int width = Minecraft.getInstance().getWindow().getWidth();
        int height = Minecraft.getInstance().getWindow().getHeight();
        FrameBufferUtil.FboDepthSpec depthSpec = FrameBufferUtil.chooseCompatibleDepthSpec(Minecraft.getInstance().getMainRenderTarget());
        mainFBO = new MainFBORender(depthSpec.depthBufferType, depthSpec.depthInternalFormat);
        mcFBO = new FBO(width, height, depthSpec.depthBufferType, 1, depthSpec.depthInternalFormat);
        lastWidth = width;
        lastHeight = height;

        snapshot = new GlStateSnapshot();
        particleSystem = new ParticleSystem();
        dimensionSlashScreenEffect = new DimensionSlashScreenEffect();
        miaoOutlineRender = new MiaoOutlineRender();
        postRenderContext = new PostRenderContext();
        taskSubmitter = new PostRenderTaskSubmitter(this);
        screenDarkeningEffect = new ScreenDarkeningEffect(width, height);
    }

    /**
     * 窗口大小变化时重建 FBO，避免 bloom 纹理和主窗口尺寸不一致。
     */
    public void onFramebufferResize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (width == lastWidth && height == lastHeight) return;

        lastWidth = width;
        lastHeight = height;
        FrameBufferUtil.FboDepthSpec depthSpec = FrameBufferUtil.chooseCompatibleDepthSpec(Minecraft.getInstance().getMainRenderTarget());
        mcFBO.resize(width, height, depthSpec.depthBufferType, depthSpec.depthInternalFormat);
        mainFBO.resize(width, height, depthSpec.depthBufferType, depthSpec.depthInternalFormat);
        screenDarkeningEffect.resize(width, height);
        bloomRender.resize(width, height);
    }

    // 主 RenderTarget 可能被其他模组在运行中重建，逐帧确认本模组 FBO 深度格式仍然匹配。
    public void syncDepthSpec(RenderTarget renderTarget) {
        FrameBufferUtil.FboDepthSpec depthSpec = FrameBufferUtil.chooseCompatibleDepthSpec(renderTarget);
        if (depthSpec.matches(mcFBO) && depthSpec.matches(mainFBO.getFbo())) return;
        mcFBO.resize(lastWidth, lastHeight, depthSpec.depthBufferType, depthSpec.depthInternalFormat);
        mainFBO.resize(lastWidth, lastHeight, depthSpec.depthBufferType, depthSpec.depthInternalFormat);
    }

    /**
     * 执行完整后处理：拷贝原画面、写入模组效果、模糊 bloom、最终合成。
     */
    public void doPostProcessing() {
        if (!shouldRender()) return;
        RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
        syncDepthSpec(renderTarget);
        snapshot.save();
        postRenderContext.beginFrame(snapshot.prevVao);

        FrameBufferUtil.copyFBO(renderTarget, mcFBO);
        int sceneTexture = screenDarkeningEffect.renderIfNeeded(mcFBO, quad);
        buildBuffer(renderTarget);

        int bloomTexture = bloomRender.render(mainFBO.getFbo().getColourTexture(1), quad, renderTarget.frameBufferId);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        start();
        finalRender.render(sceneTexture, mainFBO.getFbo().getColourTexture(), bloomTexture, dimensionSlashScreenEffect);
        end();
        dimensionSlashScreenEffect.clearFrame();

        snapshot.restore();
        isRendering = false;

    }

    // 判断当前帧是否需要执行后处理，兼顾新任务和跨帧存活效果。
    public boolean shouldRender() {
        return isRendering || particleSystem.hasActiveParticles() || finalRender.hasActiveEffects() || screenDarkeningEffect.hasActive();
    }

    private void start() {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL30.glBindVertexArray(quad.getVaoID());
        GL20.glEnableVertexAttribArray(0);
    }

    private void end() {
        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        GL11.glDepthMask(true);
        snapshot.setToMCVao();
    }

    /**
     * 将飞剑拖尾加入需要深度测试的 phase 队列。
     */
    public void add(Entity entity, PoseStack pose) {
        finalRender.addBloomQueue(entity, pose, RenderSystem.getModelViewMatrix());
        isRendering = true;
    }

    // 记录飞剑 renderer 已经确认需要拖尾的实体，等待 AFTER_ENTITIES 阶段补正确矩阵。
    public void requestFlySwordTrail(FlySwordEntity entity) {
        finalRender.requestFlySwordTrail(entity);
    }

    // 在 AFTER_ENTITIES 阶段使用事件 PoseStack 批量提交本帧飞剑拖尾。
    public void flushFlySwordTrailPose(PoseStack pose) {
        if (!finalRender.hasPendingFlySwordTrails()) return;
        finalRender.flushFlySwordTrails(new Matrix4f(pose.last().pose()));
        isRendering = true;
    }

    /**
     * 将 GPU 粒子发射任务提交到粒子系统。
     */
    public void addParticle(ParticleEmitTask task) {
        AkatZumaTool.submitAkatTask(() -> particleSystem.emit(task));
        isRendering = true;
    }

    // 提交通用屏幕暗化请求，多个请求同帧只取最大暗化强度。
    public void addScreenDarkening(float strength, int lifeTicks, int fadeInTicks, int fadeOutTicks) {
        screenDarkeningEffect.add(strength, lifeTicks, fadeInTicks, fadeOutTicks);
        isRendering = true;
    }

    public void addBloomTask(Entity entity, PoseStack pose){
        finalRender.addBloomQueue(entity, pose, RenderSystem.getModelViewMatrix());
        isRendering = true;
    }

    // 提交次元斩领域屏幕后处理效果。
    public void addDimensionSlashField(DimensionSlashDomainEntity entity, float partialTick) {
        camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        dimensionSlashScreenEffect.add(entity, camera, partialTick);
        isRendering = true;
    }

    // 提交实体 Miao 描边，输出会写入 CA0 可见层和 CA1 bloom source。
    public void addMiaoOutline(Entity entity, MiaoOutlineStyle style) {
        finalRender.addMiaoOutline(entity, style);
        isRendering = true;
    }

    // 统一提交无实体后处理任务，新增无实体效果优先走该入口。
    public void submit(PostRenderTask task) {
        if (task == null) return;
        finalRender.submit(task);
        isRendering = true;
    }

    // 返回无实体后处理效果语义化提交入口，调用方不再直接在 PostProcessing 堆 add 方法。
    public PostRenderTaskSubmitter effects() {
        return taskSubmitter;
    }

    // 缓存手持飞剑模型真实矩阵，供后处理阶段重放透明模型和 bloom source。
    public void submitFlySwordHeldModel(BakedModel model, Matrix4f modelViewMatrix,
                                        boolean plusSword, long gameTime, FlySwordHeldItemRenderer.FlySwordFlowParams flowParams) {
        finalRender.submitFlySwordHeldModel(model, modelViewMatrix, plusSword, gameTime, flowParams);
        isRendering = true;
    }

    /**
     * 写入 mainFBO：CA0 保存可见模组效果，CA1 保存 bloom source。
     */
    public void buildBuffer(RenderTarget renderTarget) {
        if (camera == null) {
            camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        }
        viewMatrix = MathUtil.createViewMatrix(camera);

        // 每帧先同步主画面深度，再清理颜色附件，避免光影下 mainFBO 深度为空导致光束被深度测试挡掉。
        FrameBufferUtil.copyFBODepth(renderTarget, mainFBO.getFbo());
        postRenderContext.bindFrameBuffer(mainFBO.getFbo(), false, 0, 1);

        // 粒子帧时间同时提供给 phase/bloom 队列和 GPU 粒子，本次保持每帧计算，避免时间跳变。
        float frameDelta = getParticleFrameDeltaSeconds() * 3;
        PostRenderTaskRenderContext taskContext = new PostRenderTaskRenderContext(postRenderContext, finalRender.fboBuffer,
                camera, partialTick, viewMatrix, frameDelta, mcFBO.getColourTexture(), mcFBO.getDepthTexture(),
                mcFBO.getWidth(), mcFBO.getHeight());
        boolean hasDepthBloomQueues = hasPhaseQueues(PostRenderPhase.DEPTH_TESTED_WORLD);
        boolean hasParticles = particleSystem.hasActiveParticles();
        boolean hasAlwaysVisibleQueues = hasPhaseQueues(PostRenderPhase.ALWAYS_VISIBLE_WORLD);

        // 需要场景深度的 phase/bloom 队列和 GPU 粒子共用同一个深度测试阶段。
        boolean hasDepthWorldPhase = hasDepthBloomQueues || hasParticles;
        if (hasDepthWorldPhase) {
            postRenderContext.setDrawBuffers(mainFBO.getFbo(), 0, 1);
            postRenderContext.setDepthState(true, false, GL11.GL_LEQUAL);

            // RenderType 队列前恢复稳定状态，避免上一个自管 VAO 阶段留下 VAO 0 或 blend 状态。
            if (hasDepthBloomQueues) {
                finalRender.renderTaskQueuesByPhase(PostRenderPhase.DEPTH_TESTED_WORLD, taskContext);
                postRenderContext.prepareRenderTypePhase(true, false, GL11.GL_LEQUAL);
                finalRender.renderBloomQueuesByPhase(PostRenderPhase.DEPTH_TESTED_WORLD, camera, partialTick, viewMatrix, frameDelta);
            }

            // 最后一个 RenderType.endBatch 可能关闭真实深度测试，GPU 粒子前重新建立深度和 MRT 契约。
            if (hasParticles) {
                postRenderContext.setDepthState(true, false, GL11.GL_LEQUAL);
                postRenderContext.setDrawBuffers(mainFBO.getFbo(), 0, 1);
                particleSystem.updateAndRender(frameDelta, RenderSystem.getProjectionMatrix(), camera);
            }
        }

        // 常显世界空间效果集中在关闭深度测试后渲染，拔刀斩前必须恢复 Minecraft VAO。
        if (hasAlwaysVisibleQueues) {
            postRenderContext.prepareRenderTypePhase(false, false, GL11.GL_ALWAYS);
            BufferUploader.reset();
            postRenderContext.setDrawBuffers(mainFBO.getFbo(), 0, 1);
            finalRender.renderTaskQueuesByPhase(PostRenderPhase.ALWAYS_VISIBLE_WORLD, taskContext);
            postRenderContext.prepareRenderTypePhase(false, false, GL11.GL_ALWAYS);
            finalRender.renderBloomQueuesByPhase(PostRenderPhase.ALWAYS_VISIBLE_WORLD, camera, partialTick, viewMatrix, frameDelta);
        }

        // Miao 描边按类型分组渲染，每个类型单独写 CA2 并使用自己的后处理参数。
        if (finalRender.hasMiaoOutlineTasks()) {
            postRenderContext.prepareRenderTypePhase(false, false, GL11.GL_ALWAYS);
            BufferUploader.reset();
            for (Map.Entry<MiaoOutlineStyle.Kind, List<MiaoOutlineTask>> entry : finalRender.getMiaoOutlineQueue().groupTasksByKind().entrySet()) {
                MiaoOutlineStyle style = MiaoOutlineStyle.create(entry.getKey());
                // CA2.R 保存归一化 view depth，CA2.G 保存目标 mask，每个类型独立清理避免互相串参数。
                postRenderContext.clearColorAttachment(mainFBO.getFbo(), 2, 0f, 0f, 0f, 0f);
                finalRender.getMiaoOutlineQueue().renderDepthMask(finalRender.fboBuffer, entry.getValue(), camera, partialTick, viewMatrix, style);
                miaoOutlineRender.render(mainFBO.getFbo(), quad, style, partialTick);
            }
            finalRender.clearMiaoOutlineTasks();
        }
        postRenderContext.prepareRenderTypePhase(true, false, GL11.GL_LEQUAL);
        postRenderContext.bindMinecraftFrameBuffer(mainFBO.getFbo(), renderTarget);
    }



    // 判断指定后处理阶段是否存在实体队列或无实体任务队列，避免 buildBuffer 中重复拼装条件。
    public boolean hasPhaseQueues(PostRenderPhase phase) {
        return finalRender.hasTaskQueuesByPhase(phase) || finalRender.hasBloomQueuesByPhase(phase);
    }

    public float getParticleFrameDeltaSeconds() {
        float now = MathUtil.getClientTime(partialTick);
        if (lastFrameClientTime < 0f) {
            lastFrameClientTime = now;
            return 0f;
        }

        float dt = now - lastFrameClientTime;
        lastFrameClientTime = now;
        return Math.min(Math.max(dt, 0f), MAX_PARTICLE_DELTA_SECONDS);
    }

    /**
     * 记录当前帧插值和相机，供队列生成世界空间几何。
     */
    public void setPartialTick(float partialTick, Camera camera) {
        this.partialTick = partialTick;
        this.camera = camera;
    }

    /**
     * 释放后处理资源。
     */
    public void cleanUp() {
        finalRender.cleanUp();
        bloomRender.cleanUp();
        particleSystem.cleanUp();
        miaoOutlineRender.cleanUp();
        screenDarkeningEffect.cleanUp();
    }

    public FinalRender<Entity> getFinalRender() {
        return finalRender;
    }
}
