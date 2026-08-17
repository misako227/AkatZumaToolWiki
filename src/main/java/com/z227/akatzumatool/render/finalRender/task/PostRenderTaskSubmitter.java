package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.render.finalRender.PostProcessing;
import com.z227.akatzumatool.render.finalRender.bloomQueue.CoinLightningQueue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

// PostRenderTaskSubmitter 保存无实体后处理效果的语义化提交入口，避免 PostProcessing 继续堆具体 add 方法。
public class PostRenderTaskSubmitter {
    public final PostProcessing postProcessing; // 后处理统一 task 提交入口。

    public PostRenderTaskSubmitter(PostProcessing postProcessing) {
        this.postProcessing = postProcessing;
    }

    // 提交旧 lifetime 接口路径闪电任务，内部转换为 grow/hold/fade 三段时间。
    public void addLightningStartToEnd(Vec3 start, Vec3 end, float lifetime, float width, long seed,
                                       float coreR, float coreG, float coreB,
                                       float bloomR, float bloomG, float bloomB) {
        if (postProcessing == null) return;
        postProcessing.submit(LightningTask.startToEnd(start, end, lifetime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB));
    }

    // 提交默认几何抖动的路径闪电任务。
    public void addLightningPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                 float width, long seed,
                                 float coreR, float coreG, float coreB,
                                 float bloomR, float bloomG, float bloomB) {
        if (postProcessing == null) return;
        postProcessing.submit(LightningTask.path(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB));
    }

    // 提交可控制几何抖动倍率的路径闪电任务。
    public void addLightningPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                 float width, long seed,
                                 float coreR, float coreG, float coreB,
                                 float bloomR, float bloomG, float bloomB, float jitterScale) {
        if (postProcessing == null) return;
        postProcessing.submit(LightningTask.path(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale));
    }

    // 提交可控制几何抖动和末端回弹的路径闪电任务。
    public void addLightningPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                 float width, long seed,
                                 float coreR, float coreG, float coreB,
                                 float bloomR, float bloomG, float bloomB, float jitterScale, int terminalBounceCount) {
        if (postProcessing == null) return;
        postProcessing.submit(LightningTask.path(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale, terminalBounceCount));
    }

    // 提交可显式控制噪声图和噪声强度的路径闪电任务。
    public void addLightningPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                 float width, long seed,
                                 float coreR, float coreG, float coreB,
                                 float bloomR, float bloomG, float bloomB, float jitterScale, int terminalBounceCount,
                                 float noiseIndex, float noiseStrength) {
        if (postProcessing == null) return;
        postProcessing.submit(LightningTask.path(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale, terminalBounceCount, noiseIndex, noiseStrength));
    }

    // 提交带开始延迟的路径闪电任务。
    public void addLightningPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                 float width, long seed,
                                 float coreR, float coreG, float coreB,
                                 float bloomR, float bloomG, float bloomB, float jitterScale, int terminalBounceCount,
                                 float noiseIndex, float noiseStrength, float startDelay) {
        if (postProcessing == null) return;
        postProcessing.submit(LightningTask.path(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale, terminalBounceCount,
                noiseIndex, noiseStrength, startDelay));
    }

    // 提交整段可见的常驻闪电任务。
    public void addPersistentLightningStartToEnd(Vec3 start, Vec3 end, float lifetime, float width, long seed,
                                                 float coreR, float coreG, float coreB,
                                                 float bloomR, float bloomG, float bloomB) {
        if (postProcessing == null) return;
        float safeLifetime = Math.max(CoinLightningQueue.MIN_TIME * 3.0F, lifetime);
        postProcessing.submit(LightningTask.burst(start, end, safeLifetime * 0.15F, safeLifetime * 0.65F,
                safeLifetime * 0.20F, width, seed, coreR, coreG, coreB, bloomR, bloomG, bloomB));
    }

    // 提交整段闪电任务。
    public void addLightningBurst(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                  float width, long seed,
                                  float coreR, float coreG, float coreB,
                                  float bloomR, float bloomG, float bloomB) {
        if (postProcessing == null) return;
        postProcessing.submit(LightningTask.burst(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB));
    }

    // 提交地面圆形扩散闪电任务。
    public void addLightningRing(Vec3 center, Vec3 normal, float startRadius, float endRadius,
                                 float growTime, float holdTime, float fadeTime, float width, long seed,
                                 float coreR, float coreG, float coreB,
                                 float bloomR, float bloomG, float bloomB) {
        if (postProcessing == null) return;
        postProcessing.submit(LightningTask.ring(center, normal, startRadius, endRadius, growTime, holdTime,
                fadeTime, width, seed, coreR, coreG, coreB, bloomR, bloomG, bloomB));
    }

    // 提交独立冲击波任务。
    public void addShockwave(Vec3 center, Vec3 normal, float startRadius, float endRadius,
                             float growTime, float holdTime, float fadeTime, float width, long seed, float alpha) {
        if (postProcessing == null) return;
        postProcessing.submit(ShockwaveTask.shockwave(center, normal, startRadius, endRadius,
                growTime, holdTime, fadeTime, width, seed, alpha));
    }

    // 提交法阵冲击波任务。
    public void addCircleShockwave(Vec3 center, Vec3 normal, float startRadius, float endRadius,
                                   float growTime, float holdTime, float fadeTime, float width, long seed, float alpha) {
        if (postProcessing == null) return;
        postProcessing.submit(ShockwaveTask.circleShockwave(center, normal, startRadius, endRadius,
                growTime, holdTime, fadeTime, width, seed, alpha));
    }

    // 提交测试烟雾环任务。
    public void addSmokeRing(Vec3 center, Vec3 normal, long seed) {
        if (postProcessing == null) return;
        postProcessing.submit(SmokeParticleTask.ring(center, normal, seed));
    }

    // 提交单个烟雾粒子任务。
    public void addSmokeParticle(Vec3 center, long seed) {
        if (postProcessing == null) return;
        postProcessing.submit(SmokeParticleTask.single(center, seed));
    }

    // 提交云团烟雾任务。
    public void addSmokeCloud(Vec3 center, long seed) {
        if (postProcessing == null) return;
        postProcessing.submit(SmokeParticleTask.cloud(center, seed));
    }

    // 提交横向圆环云任务。
    public void addSmokeCloudRing(Vec3 center, long seed) {
        if (postProcessing == null) return;
        postProcessing.submit(SmokeParticleTask.cloudRing(center, seed));
    }

    // 提交带整体旋转角的横向圆环云任务。
    public void addSmokeCloudRing(Vec3 center, long seed, float ringRotation) {
        if (postProcessing == null) return;
        postProcessing.submit(SmokeParticleTask.cloudRing(center, seed, ringRotation));
    }

    // 提交天雷法阵横向旋转云环任务。
    public void addHeavenlyThunderCloudRing(Vec3 center, long seed, float lifeTime) {
        if (postProcessing == null) return;
        postProcessing.submit(SmokeParticleTask.heavenlyThunderCloudRing(center, seed, lifeTime));
    }

    // 提交带整体旋转角的天雷法阵横向旋转云环任务。
    public void addHeavenlyThunderCloudRing(Vec3 center, long seed, float lifeTime, float ringRotation) {
        if (postProcessing == null) return;
        postProcessing.submit(SmokeParticleTask.heavenlyThunderCloudRing(center, seed, lifeTime, ringRotation));
    }

    // 提交玩家蓄力闪电任务。
    public void addChargingLightning(Player player, float chargeProgress, float partialTick, boolean colorful) {
        if (postProcessing == null) return;
        postProcessing.submit(LightningTask.charging(player, chargeProgress, partialTick, colorful));
    }

    // 提交金色三噪声螺旋光效任务。
    public void addGoldenSpiralEffect(Vec3 center, long seed) {
        if (postProcessing == null) return;
        postProcessing.submit(GoldenSpiralEffectTask.create(center, seed));
    }
}
