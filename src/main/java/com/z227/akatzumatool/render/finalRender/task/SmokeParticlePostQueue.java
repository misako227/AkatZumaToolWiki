package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.render.finalRender.PostRenderPhase;
import com.z227.akatzumatool.render.finalRender.bloomQueue.SmokeParticleQueue;

// SmokeParticlePostQueue 把统一任务接口适配到现有烟雾粒子队列。
public class SmokeParticlePostQueue implements PostRenderTaskQueue<SmokeParticleTask> {
    public final SmokeParticleQueue queue; // 现有无实体烟雾粒子队列。

    public SmokeParticlePostQueue(SmokeParticleQueue queue) {
        this.queue = queue;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.SMOKE_PARTICLE;
    }

    public PostRenderPhase phase() {
        return PostRenderPhase.DEPTH_TESTED_WORLD;
    }

    public void add(SmokeParticleTask task) {
        if (task == null || queue == null) return;
        switch (task.effectType) {
            case RING -> queue.addRing(task.center, task.normal, task.seed);
            case SINGLE -> queue.addSingleParticle(task.center, task.seed);
            case CLOUD -> queue.addCloud(task.center, task.seed);
            case CLOUD_RING -> queue.addCloudRing(task.center, task.seed, task.ringRotation);
            case HEAVENLY_THUNDER_CLOUD_RING -> queue.addHeavenlyThunderCloudRing(task.center, task.seed, task.lifeTime, task.ringRotation);
        }
    }

    public boolean hasActive() {
        return queue != null && queue.hasActive();
    }

    public void render(PostRenderTaskRenderContext context) {
        if (queue == null || context == null) return;
        queue.render(context.camera, context.partialTick, context.viewMatrix,
                context.sceneDepthTextureId, context.screenWidth, context.screenHeight);
    }

    public void clear() {
        if (queue == null) return;
        queue.clear();
    }
}
