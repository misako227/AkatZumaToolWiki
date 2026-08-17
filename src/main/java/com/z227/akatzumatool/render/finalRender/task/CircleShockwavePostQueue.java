package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.render.finalRender.PostRenderPhase;
import com.z227.akatzumatool.render.finalRender.bloomQueue.CircleShockwaveQueue;

// CircleShockwavePostQueue 把统一任务接口适配到现有法阵冲击波队列。
public class CircleShockwavePostQueue implements PostRenderTaskQueue<ShockwaveTask> {
    public final CircleShockwaveQueue queue; // 现有法阵冲击波队列。

    public CircleShockwavePostQueue(CircleShockwaveQueue queue) {
        this.queue = queue;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.CIRCLE_SHOCKWAVE;
    }

    public PostRenderPhase phase() {
        return PostRenderPhase.DEPTH_TESTED_WORLD;
    }

    public void add(ShockwaveTask task) {
        if (task == null || queue == null) return;
        queue.add(task.center, task.normal, task.startRadius, task.endRadius,
                task.growTime, task.holdTime, task.fadeTime, task.width, task.seed, task.alpha);
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
