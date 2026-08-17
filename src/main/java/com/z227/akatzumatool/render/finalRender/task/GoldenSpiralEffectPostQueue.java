package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.render.finalRender.PostRenderPhase;
import com.z227.akatzumatool.render.finalRender.bloomQueue.GoldenSpiralEffectQueue;

// GoldenSpiralEffectPostQueue 把统一任务接口适配到金色螺旋光效队列。
public class GoldenSpiralEffectPostQueue implements PostRenderTaskQueue<GoldenSpiralEffectTask> {
    public final GoldenSpiralEffectQueue queue; // 金色螺旋光效队列。

    public GoldenSpiralEffectPostQueue(GoldenSpiralEffectQueue queue) {
        this.queue = queue;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.GOLDEN_SPIRAL_EFFECT;
    }

    public PostRenderPhase phase() {
        return PostRenderPhase.DEPTH_TESTED_WORLD;
    }

    public void add(GoldenSpiralEffectTask task) {
        if (task == null || queue == null) return;
        queue.add(task.center, task.seed);
    }

    public boolean hasActive() {
        return queue != null && queue.hasActive();
    }

    public void render(PostRenderTaskRenderContext context) {
        if (queue == null || context == null) return;
        queue.render(context.fboBuffer, context.camera, context.partialTick, context.viewMatrix);
    }

    public void clear() {
        if (queue == null) return;
        queue.clear();
    }
}
