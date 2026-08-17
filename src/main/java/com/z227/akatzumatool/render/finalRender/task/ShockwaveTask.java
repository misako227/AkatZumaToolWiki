package com.z227.akatzumatool.render.finalRender.task;

import net.minecraft.world.phys.Vec3;

// ShockwaveTask 统一独立冲击波和法阵冲击波的提交参数。
public class ShockwaveTask implements PostRenderTask {
    public final PostRenderQueueType queueType; // 目标冲击波队列类型。
    public final Vec3 center; // 冲击波中心。
    public final Vec3 normal; // 冲击波法线或预留方向。
    public final float startRadius; // 起始半径。
    public final float endRadius; // 结束半径。
    public final float growTime; // 扩散时间。
    public final float holdTime; // 保持时间。
    public final float fadeTime; // 淡出时间。
    public final float width; // 冲击波宽度参数。
    public final long seed; // 随机种子。
    public final float alpha; // 全局透明度倍率。

    public ShockwaveTask(PostRenderQueueType queueType, Vec3 center, Vec3 normal, float startRadius, float endRadius,
                         float growTime, float holdTime, float fadeTime, float width, long seed, float alpha) {
        this.queueType = queueType;
        this.center = center;
        this.normal = normal;
        this.startRadius = startRadius;
        this.endRadius = endRadius;
        this.growTime = growTime;
        this.holdTime = holdTime;
        this.fadeTime = fadeTime;
        this.width = width;
        this.seed = seed;
        this.alpha = alpha;
    }

    public PostRenderQueueType queueType() {
        return queueType;
    }

    // 创建独立冲击波任务。
    public static ShockwaveTask shockwave(Vec3 center, Vec3 normal, float startRadius, float endRadius,
                                          float growTime, float holdTime, float fadeTime, float width, long seed, float alpha) {
        return new ShockwaveTask(PostRenderQueueType.SHOCKWAVE, center, normal, startRadius, endRadius,
                growTime, holdTime, fadeTime, width, seed, alpha);
    }

    // 创建法阵冲击波任务。
    public static ShockwaveTask circleShockwave(Vec3 center, Vec3 normal, float startRadius, float endRadius,
                                                float growTime, float holdTime, float fadeTime, float width, long seed, float alpha) {
        return new ShockwaveTask(PostRenderQueueType.CIRCLE_SHOCKWAVE, center, normal, startRadius, endRadius,
                growTime, holdTime, fadeTime, width, seed, alpha);
    }
}
