package com.z227.akatzumatool.render.finalRender;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.entity.FlySwordEntity;
import com.z227.akatzumatool.item.FlySwordHeldItemRenderer;
import com.z227.akatzumatool.entity.bow.MagicBowParticleEffectEntity;
import com.z227.akatzumatool.entity.coin.ColorfulCoinEntity;
import com.z227.akatzumatool.entity.coin.RailgunBeamEntity;
import com.z227.akatzumatool.entity.sword.BattoSlashEntity;
import com.z227.akatzumatool.entity.sword.DimensionSlashStrikeEntity;
import com.z227.akatzumatool.entity.sword.SwordAuraEntity;
import com.z227.akatzumatool.render.finalRender.bloomQueue.BattoSlashQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.CircleShockwaveQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.CoinLightningQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.ColorfulCoinQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.DimensionSlashStrikeQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.ExcaliburSpiralQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.FlySwordHeldModelQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.GoldenSpiralEffectQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.RailgunBeamQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.ShockwaveQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.SmokeParticleQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.StarJudgementCircleQueue;
import com.z227.akatzumatool.render.finalRender.bloomQueue.SwordAuraQueue;
import com.z227.akatzumatool.render.finalRender.miaoOutline.MiaoOutlineQueue;
import com.z227.akatzumatool.render.finalRender.miaoOutline.MiaoOutlineStyle;
import com.z227.akatzumatool.render.finalRender.queue.EntityQueue;
import com.z227.akatzumatool.render.finalRender.queue.EntityQueueRegistration;
import com.z227.akatzumatool.render.finalRender.queue.FlySwordQueue;
import com.z227.akatzumatool.render.finalRender.queue.QueueAddAction;
import com.z227.akatzumatool.render.finalRender.task.CircleShockwavePostQueue;
import com.z227.akatzumatool.render.finalRender.task.ExcaliburSpiralPostQueue;
import com.z227.akatzumatool.render.finalRender.task.FlySwordHeldModelPostQueue;
import com.z227.akatzumatool.render.finalRender.task.FlySwordHeldModelTask;
import com.z227.akatzumatool.render.finalRender.task.GoldenSpiralEffectPostQueue;
import com.z227.akatzumatool.render.finalRender.task.LightningPostQueue;
import com.z227.akatzumatool.render.finalRender.task.PostRenderQueueType;
import com.z227.akatzumatool.render.finalRender.task.PostRenderTask;
import com.z227.akatzumatool.render.finalRender.task.PostRenderTaskQueue;
import com.z227.akatzumatool.render.finalRender.task.PostRenderTaskRenderContext;
import com.z227.akatzumatool.render.finalRender.task.ShockwavePostQueue;
import com.z227.akatzumatool.render.finalRender.task.SmokeParticlePostQueue;
import com.z227.akatzumatool.render.shader.post.FinalShader;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

// FinalRender 是最终渲染调度器，统一管理 phase 队列、闪电队列、描边队列和最终合成。
public class FinalRender<T extends Entity> {
    private FinalShader shader;
    public final Map<Class<? extends Entity>, EntityQueueRegistration<? extends Entity>> queueRegistrations; // 按实体 class 查找队列注册项。
    public final Map<PostRenderPhase, List<EntityQueue<? extends Entity>>> queuesByPhase; // 按后处理阶段保存全部实体队列。
    public final Map<PostRenderPhase, List<EntityQueue<? extends Entity>>> activeQueuesByPhase; // 按后处理阶段保存本帧有内容的实体队列。
    public final Map<PostRenderQueueType, PostRenderTaskQueue<? extends PostRenderTask>> taskQueueRegistrations; // 按无实体任务类型查找队列适配器。
    public final Map<PostRenderPhase, List<PostRenderTaskQueue<? extends PostRenderTask>>> taskQueuesByPhase; // 按后处理阶段保存无实体任务队列。
    public final Map<PostRenderPhase, List<PostRenderTaskQueue<? extends PostRenderTask>>> activeTaskQueuesByPhase; // 按后处理阶段保存当前仍活跃的无实体任务队列。
    public final Set<PostRenderTaskQueue<? extends PostRenderTask>> activeTaskQueueSet; // 按对象身份去重，避免同一无实体队列重复进入 active list。
    public final MultiBufferSource.BufferSource fboBuffer;

    int pPackedLight = 15728864;

    public final FlySwordQueue flySwordQueue; // 飞剑拖尾队列，使用 AFTER_ENTITIES 阶段矩阵统一入队。
    public final List<FlySwordEntity> pendingFlySwordTrails; // 本帧飞剑 renderer 已确认需要拖尾的实体。
    public final CoinLightningQueue lightningQueue;
    public final ShockwaveQueue shockwaveQueue; // 独立无实体冲击波队列。
    public final CircleShockwaveQueue circleShockwaveQueue; // 法阵冲击波队列。
    public final SmokeParticleQueue smokeParticleQueue; // 无实体实例化烟雾粒子队列。
    public final FlySwordHeldModelQueue flySwordHeldModelQueue; // 手持飞剑透明模型矩阵缓存队列。
    public final GoldenSpiralEffectQueue goldenSpiralEffectQueue; // 金色三噪声螺旋光效队列。
    public final ExcaliburSpiralQueue excaliburSpiralQueue; // 咖喱棒玩家中心向上螺旋光效队列。
    public final MiaoOutlineQueue miaoOutlineQueue; // Miao UE5 风格描边任务队列。

    public FinalRender() {
        // 初始化最终合成 shader，用于把 mcFBO、mainFBO 和 bloom 纹理合成回主画面。
        shader = new FinalShader();
        shader.start();
        shader.loadUniforms();
        shader.stop();

        // 初始化队列索引，activeQueuesByPhase 只记录本帧真正有内容的队列。
        queueRegistrations = new HashMap<>();
        queuesByPhase = new EnumMap<>(PostRenderPhase.class);
        activeQueuesByPhase = new EnumMap<>(PostRenderPhase.class);
        taskQueueRegistrations = new EnumMap<>(PostRenderQueueType.class);
        taskQueuesByPhase = new EnumMap<>(PostRenderPhase.class);
        activeTaskQueuesByPhase = new EnumMap<>(PostRenderPhase.class);
        activeTaskQueueSet = Collections.newSetFromMap(new IdentityHashMap<>());
        fboBuffer = MultiBufferSource.immediate(new BufferBuilder(8192));
        flySwordQueue = new FlySwordQueue();
        pendingFlySwordTrails = new ArrayList<>();

        // 飞剑拖尾只通过 request/flush 两段式入队，避免通用入口误用实体 renderer 的 PoseStack。
        queuesByPhase.computeIfAbsent(flySwordQueue.getPhase(), phase -> new ArrayList<>()).add(flySwordQueue);

        // 注册实体队列，新增实体只需要在这里登记 class、队列、过滤条件和特殊入队动作。
        registerQueue(RailgunBeamEntity.class, new RailgunBeamQueue());
        registerQueue(ColorfulCoinEntity.class, new ColorfulCoinQueue());
        registerQueue(MagicBowParticleEffectEntity.class, new StarJudgementCircleQueue(), MagicBowParticleEffectEntity::isStarJudgementVisual);
        registerQueue(SwordAuraEntity.class, new SwordAuraQueue());
        registerQueue(DimensionSlashStrikeEntity.class, new DimensionSlashStrikeQueue());
        registerQueue(BattoSlashEntity.class, new BattoSlashQueue());

        // 无实体闪电队列由 FinalRender 统一调度，复用 fboBuffer 和 viewMatrix。
        lightningQueue = new CoinLightningQueue();
        shockwaveQueue = new ShockwaveQueue();
        circleShockwaveQueue = new CircleShockwaveQueue();
        smokeParticleQueue = new SmokeParticleQueue();
        flySwordHeldModelQueue = new FlySwordHeldModelQueue();
        goldenSpiralEffectQueue = new GoldenSpiralEffectQueue();
        excaliburSpiralQueue = new ExcaliburSpiralQueue();
        miaoOutlineQueue = new MiaoOutlineQueue();

        // 注册无实体任务队列，新增无实体效果优先扩展 task + queue adapter，不再增加 PostProcessing 透传链。
        registerTaskQueue(new LightningPostQueue(lightningQueue));
        registerTaskQueue(new ShockwavePostQueue(shockwaveQueue));
        registerTaskQueue(new CircleShockwavePostQueue(circleShockwaveQueue));
        registerTaskQueue(new SmokeParticlePostQueue(smokeParticleQueue));
        registerTaskQueue(new FlySwordHeldModelPostQueue(flySwordHeldModelQueue));
        registerTaskQueue(new GoldenSpiralEffectPostQueue(goldenSpiralEffectQueue));
        registerTaskQueue(new ExcaliburSpiralPostQueue(excaliburSpiralQueue));
    }

    // 执行最终全屏合成。
    public void render(int mcTexture, int mainTexture, int bloomTexture, DimensionSlashScreenEffect screenEffect) {
        shader.start();
        shader.loadDimensionSlashEffect(screenEffect);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, mcTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, mainTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, bloomTexture);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        shader.stop();
    }

    // 注册普通实体队列，默认不过滤并直接 add 到队列。
    public <E extends Entity> void registerQueue(Class<E> entityClass, EntityQueue<E> queue) {
        registerQueue(entityClass, queue, entity -> true);
    }

    // 注册带过滤条件的实体队列，过滤通过后直接 add 到队列。
    public <E extends Entity> void registerQueue(Class<E> entityClass, EntityQueue<E> queue, Predicate<E> filter) {
        registerQueue(entityClass, queue, filter, (targetQueue, entity, pose, modelViewMatrix) -> targetQueue.add(entity));
    }

    // 注册完整实体队列，支持自定义过滤和特殊入队动作。
    public <E extends Entity> void registerQueue(Class<E> entityClass, EntityQueue<E> queue, Predicate<E> filter, QueueAddAction<E> addAction) {
        EntityQueueRegistration<E> registration = new EntityQueueRegistration<>(entityClass, queue, filter, addAction);
        queueRegistrations.put(entityClass, registration);
        queuesByPhase.computeIfAbsent(queue.getPhase(), phase -> new ArrayList<>()).add(queue);
    }

    // 注册一个无实体任务队列适配器，FinalRender 后续只按任务类型分发。
    public <Q extends PostRenderTask> void registerTaskQueue(PostRenderTaskQueue<Q> queue) {
        if (queue == null) return;
//        if (taskQueueRegistrations.containsKey(queue.queueType())) {
//            throw new IllegalStateException("Duplicate post render task queue type: " + queue.queueType());
//        }
        taskQueueRegistrations.put(queue.queueType(), queue);
        taskQueuesByPhase.computeIfAbsent(queue.phase(), phase -> new ArrayList<>()).add(queue);
    }

    // 统一提交无实体后处理任务，避免 PostProcessing 和 FinalRender 继续为新效果追加重载。
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void submit(PostRenderTask task) {
        if (task == null) return;
        PostRenderTaskQueue queue = taskQueueRegistrations.get(task.queueType());
        if (queue == null) return;
        queue.add(task);
        markTaskQueueActive(queue);
    }

    // 将无实体任务队列加入当前活跃队列索引，后续 phase 判断不再扫描全部注册队列。
    public void markTaskQueueActive(PostRenderTaskQueue<? extends PostRenderTask> queue) {
        if (queue == null) return;
        if (!activeTaskQueueSet.add(queue)) return;
        activeTaskQueuesByPhase.computeIfAbsent(queue.phase(), phase -> new ArrayList<>()).add(queue);
    }

    // 按实体运行时 class 查找队列注册项，当前注册实体均为具体类。
    public EntityQueueRegistration<? extends Entity> findRegistration(Entity entity) {
        return queueRegistrations.get(entity.getClass());
    }

    // 统一提交实体 bloom/phase 队列，飞剑拖尾和普通 bloom 都走这个入口。
    public void addBloomQueue(T entity, PoseStack pose, Matrix4f modelViewMatrix) {
        if (entity == null) return;
        EntityQueueRegistration<? extends Entity> registration = findRegistration(entity);
        if (registration == null) return;
        if (!registration.tryAdd(entity, pose, modelViewMatrix)) return;
        markActive(registration.queue);
    }

    // 记录本帧飞剑 renderer 已经渲染到的移动飞剑，等待 AFTER_ENTITIES 阶段提供正确矩阵。
    public void requestFlySwordTrail(FlySwordEntity entity) {
        if (entity == null) return;
        pendingFlySwordTrails.add(entity);
    }

    // 判断本帧是否存在等待 AFTER_ENTITIES 矩阵的飞剑拖尾。
    public boolean hasPendingFlySwordTrails() {
        return !pendingFlySwordTrails.isEmpty();
    }

    // 使用 AFTER_ENTITIES 阶段矩阵把本帧飞剑拖尾正式加入渲染队列。
    public void flushFlySwordTrails(Matrix4f modelMatrix) {
        if (pendingFlySwordTrails.isEmpty()) return;
        flySwordQueue.updateModelMatrix(modelMatrix);
        for (FlySwordEntity entity : pendingFlySwordTrails) {
            flySwordQueue.add(entity);
        }
        pendingFlySwordTrails.clear();
        markActive(flySwordQueue);
    }

    // 将队列标记为本帧 active，避免同一帧同一队列重复进入 active list。
    public void markActive(EntityQueue<? extends Entity> queue) {
        if (queue.activeInFrame) return;
        activeQueuesByPhase.computeIfAbsent(queue.getPhase(), phase -> new ArrayList<>()).add(queue);
        queue.activeInFrame = true;
    }

    // 缓存 FlySwordHeldItemRenderer 提交的真实手持飞剑模型视图矩阵。
    public void submitFlySwordHeldModel(BakedModel model, Matrix4f modelViewMatrix,
                                        boolean plusSword, long gameTime, FlySwordHeldItemRenderer.FlySwordFlowParams flowParams) {
        submit(new FlySwordHeldModelTask(model, modelViewMatrix, plusSword, gameTime, flowParams));
    }

    // 提交实体 Miao 描边任务，mask 和 bloom 由 PostProcessing 统一调度。
    public void addMiaoOutline(Entity entity, MiaoOutlineStyle style) {
        miaoOutlineQueue.add(entity, style == null ? MiaoOutlineStyle.AUTO_TRACKING_RED : style);
    }

    // 返回 Miao 描边队列，PostProcessing 用它完成 CA2 深度 mask 和径向后处理。
    public MiaoOutlineQueue getMiaoOutlineQueue() {
        return miaoOutlineQueue;
    }

    // 判断当前帧是否存在 Miao 描边任务。
    public boolean hasMiaoOutlineTasks() {
        return miaoOutlineQueue.hasTasks();
    }

    // 清空当前帧 Miao 描边任务。
    public void clearMiaoOutlineTasks() {
        miaoOutlineQueue.clear();
    }

    // 判断指定 phase 是否存在实体队列内容。
    public boolean hasBloomQueuesByPhase(PostRenderPhase phase) {
        List<EntityQueue<? extends Entity>> activeQueues = activeQueuesByPhase.get(phase);
        return activeQueues != null && !activeQueues.isEmpty();
    }

    // 判断指定 phase 是否存在无实体任务队列内容。
    public boolean hasTaskQueuesByPhase(PostRenderPhase phase) {
        List<PostRenderTaskQueue<? extends PostRenderTask>> activeQueues = activeTaskQueuesByPhase.get(phase);
        return activeQueues != null && !activeQueues.isEmpty();
    }

    // 按后处理阶段渲染无实体任务队列，阶段状态在每个队列前恢复一次，隔离自管 GL 状态。
    public void renderTaskQueuesByPhase(PostRenderPhase phase, PostRenderTaskRenderContext context) {
        List<PostRenderTaskQueue<? extends PostRenderTask>> activeQueues = activeTaskQueuesByPhase.get(phase);
        if (activeQueues == null || activeQueues.isEmpty()) return;
        int stableSize = activeQueues.size();
        for (int i = 0; i < stableSize; i++) {
            PostRenderTaskQueue<? extends PostRenderTask> queue = activeQueues.get(i);
            if (context != null) context.prepareRenderTypePhase(phase);
            queue.render(context);
        }
        compactActiveTaskQueues(phase);
    }

    // 渲染后压缩 active list，保留生命周期未结束的跨帧无实体队列。
    public void compactActiveTaskQueues(PostRenderPhase phase) {
        List<PostRenderTaskQueue<? extends PostRenderTask>> activeQueues = activeTaskQueuesByPhase.get(phase);
        if (activeQueues == null || activeQueues.isEmpty()) return;
        int write = 0;
        int originalSize = activeQueues.size();
        for (int read = 0; read < originalSize; read++) {
            PostRenderTaskQueue<? extends PostRenderTask> queue = activeQueues.get(read);
            if (queue.hasActive()) {
                activeQueues.set(write, queue);
                write++;
                continue;
            }
            activeTaskQueueSet.remove(queue);
        }
        while (activeQueues.size() > write) {
            activeQueues.remove(activeQueues.size() - 1);
        }
    }

    // 按后处理阶段渲染本帧 active 的 bloom 队列，让 PostProcessing 可以在阶段边界统一设置 GL 状态。
    public void renderBloomQueuesByPhase(PostRenderPhase phase, Camera camera, float partialTick, Matrix4f viewMatrix, float frameDeltaSeconds) {
        List<EntityQueue<? extends Entity>> activeQueues = activeQueuesByPhase.get(phase);
        if (activeQueues == null || activeQueues.isEmpty()) return;

        for (EntityQueue<? extends Entity> bloomQueue : activeQueues) {
            if (!bloomQueue.entities.isEmpty()) {
                bloomQueue.render(fboBuffer, camera, partialTick, viewMatrix, frameDeltaSeconds);
                bloomQueue.entities.clear();
            }
            bloomQueue.activeInFrame = false;
        }
        activeQueues.clear();
    }

    // 判断最终渲染器是否仍有跨帧播放的效果。
    public boolean hasActiveEffects() {
        return hasTaskQueuesByPhase(PostRenderPhase.DEPTH_TESTED_WORLD)
                || hasTaskQueuesByPhase(PostRenderPhase.ALWAYS_VISIBLE_WORLD)
                || hasBloomQueuesByPhase(PostRenderPhase.DEPTH_TESTED_WORLD)
                || hasBloomQueuesByPhase(PostRenderPhase.ALWAYS_VISIBLE_WORLD)
                || miaoOutlineQueue.hasTasks();
    }

    // 释放最终合成 shader，并清空实体队列、无实体 task 队列和特殊 GPU 资源。
    public void cleanUp() {
        for (PostRenderTaskQueue<? extends PostRenderTask> queue : taskQueueRegistrations.values()) {
            queue.clear();
        }
        smokeParticleQueue.cleanUp();
        miaoOutlineQueue.clear();
        pendingFlySwordTrails.clear();
        for (List<EntityQueue<? extends Entity>> queues : queuesByPhase.values()) {
            for (EntityQueue<? extends Entity> bloomQueue : queues) {
                if (bloomQueue instanceof SwordAuraQueue swordAuraQueue) {
                    swordAuraQueue.cleanUp();
                }
                bloomQueue.entities.clear();
                bloomQueue.activeInFrame = false;
            }
        }
        activeQueuesByPhase.clear();
        activeTaskQueuesByPhase.clear();
        activeTaskQueueSet.clear();
        shader.cleanUp();
    }
}
