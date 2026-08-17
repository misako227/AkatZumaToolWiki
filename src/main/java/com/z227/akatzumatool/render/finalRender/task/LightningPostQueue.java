package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.render.finalRender.PostRenderPhase;
import com.z227.akatzumatool.render.finalRender.bloomQueue.CoinLightningQueue;

// LightningPostQueue 把统一任务接口适配到现有 CoinLightningQueue。
public class LightningPostQueue implements PostRenderTaskQueue<LightningTask> {
    public final CoinLightningQueue queue; // 现有无实体闪电队列。

    public LightningPostQueue(CoinLightningQueue queue) {
        this.queue = queue;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.LIGHTNING;
    }

    public PostRenderPhase phase() {
        return PostRenderPhase.DEPTH_TESTED_WORLD;
    }

    public void add(LightningTask task) {
        if (task == null || queue == null) return;
        if (task.kind == LightningTask.Kind.CHARGING) {
            queue.addChargingLightning(task.player, task.chargeProgress, task.chargingPartialTick, task.colorful);
            return;
        }
        queue.addLightning(task.lightning);
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
