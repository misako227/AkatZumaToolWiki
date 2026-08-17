package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.render.finalRender.PostRenderPhase;
import com.z227.akatzumatool.render.finalRender.bloomQueue.ExcaliburSpiralQueue;

// ExcaliburSpiralPostQueue 把统一任务接口适配到咖喱棒随机短螺旋队列。
public class ExcaliburSpiralPostQueue implements PostRenderTaskQueue<ExcaliburSpiralTask> {
    public final ExcaliburSpiralQueue queue; // 咖喱棒螺旋队列。

    public ExcaliburSpiralPostQueue(ExcaliburSpiralQueue queue) {
        this.queue = queue;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.EXCALIBUR_SPIRAL;
    }

    public PostRenderPhase phase() {
        return PostRenderPhase.ALWAYS_VISIBLE_WORLD;
    }

    public void add(ExcaliburSpiralTask task) {
        if (task == null || queue == null) return;
        queue.add(task);
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
