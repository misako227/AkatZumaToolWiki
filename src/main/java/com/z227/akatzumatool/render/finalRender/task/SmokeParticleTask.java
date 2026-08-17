package com.z227.akatzumatool.render.finalRender.task;

import net.minecraft.world.phys.Vec3;

// SmokeParticleTask 收敛所有无实体烟雾粒子提交入口。
public class SmokeParticleTask implements PostRenderTask {
    public enum EffectType {
        RING, // 测试烟雾环。
        SINGLE, // 单个测试烟雾粒子。
        CLOUD, // 云团测试烟雾。
        CLOUD_RING, // 横向圆环云。
        HEAVENLY_THUNDER_CLOUD_RING // 天雷法阵横向旋转云环。
    }

    public final EffectType effectType; // 烟雾效果类型。
    public final Vec3 center; // 烟雾中心。
    public final Vec3 normal; // 烟雾方向或环面法线。
    public final long seed; // 随机种子。
    public final float lifeTime; // 自定义生命周期。
    public final float ringRotation; // 云环整体初始旋转角。

    public SmokeParticleTask(EffectType effectType, Vec3 center, Vec3 normal, long seed, float lifeTime, float ringRotation) {
        this.effectType = effectType;
        this.center = center;
        this.normal = normal;
        this.seed = seed;
        this.lifeTime = lifeTime;
        this.ringRotation = ringRotation;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.SMOKE_PARTICLE;
    }

    // 创建测试烟雾环任务。
    public static SmokeParticleTask ring(Vec3 center, Vec3 normal, long seed) {
        return new SmokeParticleTask(EffectType.RING, center, normal, seed, 0.0F, 0.0F);
    }

    // 创建单个烟雾粒子任务。
    public static SmokeParticleTask single(Vec3 center, long seed) {
        return new SmokeParticleTask(EffectType.SINGLE, center, null, seed, 0.0F, 0.0F);
    }

    // 创建云团烟雾任务。
    public static SmokeParticleTask cloud(Vec3 center, long seed) {
        return new SmokeParticleTask(EffectType.CLOUD, center, null, seed, 0.0F, 0.0F);
    }

    // 创建默认旋转的横向圆环云任务。
    public static SmokeParticleTask cloudRing(Vec3 center, long seed) {
        return cloudRing(center, seed, 0.0F);
    }

    // 创建带整体随机旋转角的横向圆环云任务。
    public static SmokeParticleTask cloudRing(Vec3 center, long seed, float ringRotation) {
        return new SmokeParticleTask(EffectType.CLOUD_RING, center, null, seed, 0.0F, ringRotation);
    }

    // 创建默认旋转的天雷法阵云环任务。
    public static SmokeParticleTask heavenlyThunderCloudRing(Vec3 center, long seed, float lifeTime) {
        return heavenlyThunderCloudRing(center, seed, lifeTime, 0.0F);
    }

    // 创建带整体随机旋转角的天雷法阵云环任务。
    public static SmokeParticleTask heavenlyThunderCloudRing(Vec3 center, long seed, float lifeTime, float ringRotation) {
        return new SmokeParticleTask(EffectType.HEAVENLY_THUNDER_CLOUD_RING, center, null, seed, lifeTime, ringRotation);
    }
}
