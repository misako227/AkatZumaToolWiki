package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.render.finalRender.PostRenderPhase;
import com.z227.akatzumatool.render.finalRender.bloomQueue.FlySwordHeldModelQueue;

// FlySwordHeldModelPostQueue 把统一任务接口适配到现有手持飞剑透明模型队列。
public class FlySwordHeldModelPostQueue implements PostRenderTaskQueue<FlySwordHeldModelTask> {
    public final FlySwordHeldModelQueue queue; // 现有手持飞剑透明模型队列。

    public FlySwordHeldModelPostQueue(FlySwordHeldModelQueue queue) {
        this.queue = queue;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.FLY_SWORD_HELD_MODEL;
    }

    public PostRenderPhase phase() {
        return PostRenderPhase.DEPTH_TESTED_WORLD;
    }

    public void add(FlySwordHeldModelTask task) {
        if (task == null || queue == null) return;
        queue.submit(task.model, task.modelViewMatrix, task.plusSword, task.gameTime, task.flowParams);
    }

    public boolean hasActive() {
        return queue != null && queue.hasActive();
    }

    public void render(PostRenderTaskRenderContext context) {
        if (queue == null || context == null) return;
        queue.render(context.fboBuffer, context.partialTick,
                context.sceneColorTextureId, context.screenWidth, context.screenHeight);
    }

    public void clear() {
        if (queue == null) return;
        queue.clear();
    }
}
