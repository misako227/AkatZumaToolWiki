package com.z227.akatzumatool.render.finalRender.task;

import net.minecraft.world.phys.Vec3;

// GoldenSpiralEffectTask 提交一个无实体金色三噪声螺旋光效。
public class GoldenSpiralEffectTask implements PostRenderTask {
    public final Vec3 center; // 光效底部中心。
    public final long seed; // 随机种子。

    public GoldenSpiralEffectTask(Vec3 center, long seed) {
        this.center = center;
        this.seed = seed;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.GOLDEN_SPIRAL_EFFECT;
    }

    // 创建金色螺旋光效任务。
    public static GoldenSpiralEffectTask create(Vec3 center, long seed) {
        return new GoldenSpiralEffectTask(center, seed);
    }
}
