package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.render.finalRender.bloomQueue.CoinLightningQueue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

// LightningTask 收敛所有无实体闪电提交参数，避免 PostProcessing 和 FinalRender 继续增加重载。
public class LightningTask implements PostRenderTask {
    public enum Kind {
        DATA, // 已经构造好的闪电数据。
        CHARGING // 玩家蓄力闪电，需要队列按玩家状态生成多条路径闪电。
    }

    public final Kind kind; // 闪电任务类型。
    public final CoinLightningQueue.LightningData lightning; // 单条闪电数据。
    public final Player player; // 蓄力闪电所属玩家。
    public final float chargeProgress; // 玩家蓄力进度。
    public final float chargingPartialTick; // 蓄力闪电采样插值。
    public final boolean colorful; // 是否使用彩色蓄力闪电。

    public LightningTask(CoinLightningQueue.LightningData lightning) {
        this.kind = Kind.DATA;
        this.lightning = lightning;
        this.player = null;
        this.chargeProgress = 0.0F;
        this.chargingPartialTick = 0.0F;
        this.colorful = false;
    }

    public LightningTask(Player player, float chargeProgress, float partialTick, boolean colorful) {
        this.kind = Kind.CHARGING;
        this.lightning = null;
        this.player = player;
        this.chargeProgress = chargeProgress;
        this.chargingPartialTick = partialTick;
        this.colorful = colorful;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.LIGHTNING;
    }

    // 创建旧 lifetime 接口路径闪电任务，内部转换成 grow/hold/fade 三段时间。
    public static LightningTask startToEnd(Vec3 start, Vec3 end, float lifetime, float width, long seed,
                                           float coreR, float coreG, float coreB,
                                           float bloomR, float bloomG, float bloomB) {
        float safeLifetime = Math.max(CoinLightningQueue.MIN_TIME * 3.0F, lifetime);
        return path(start, end, safeLifetime * 0.65F, safeLifetime * 0.25F, safeLifetime * 0.10F, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB);
    }

    // 创建默认几何抖动的路径闪电任务。
    public static LightningTask path(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                     float width, long seed,
                                     float coreR, float coreG, float coreB,
                                     float bloomR, float bloomG, float bloomB) {
        return path(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, CoinLightningQueue.DEFAULT_PATH_JITTER_SCALE);
    }

    // 创建可控制几何抖动倍率的路径闪电任务。
    public static LightningTask path(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                     float width, long seed,
                                     float coreR, float coreG, float coreB,
                                     float bloomR, float bloomG, float bloomB, float jitterScale) {
        return path(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale, 0);
    }

    // 创建可控制几何抖动和末端回弹的路径闪电任务。
    public static LightningTask path(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                     float width, long seed,
                                     float coreR, float coreG, float coreB,
                                     float bloomR, float bloomG, float bloomB, float jitterScale, int terminalBounceCount) {
        float noiseIndex = CoinLightningQueue.noiseIndexFromSeed(seed);
        return path(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale, terminalBounceCount,
                noiseIndex, CoinLightningQueue.DEFAULT_NOISE_STRENGTH);
    }

    // 创建可显式控制噪声图和噪声强度的路径闪电任务。
    public static LightningTask path(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                     float width, long seed,
                                     float coreR, float coreG, float coreB,
                                     float bloomR, float bloomG, float bloomB, float jitterScale, int terminalBounceCount,
                                     float noiseIndex, float noiseStrength) {
        return path(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale, terminalBounceCount,
                noiseIndex, noiseStrength, 0.0F);
    }

    // 创建带开始延迟的路径闪电任务。
    public static LightningTask path(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                     float width, long seed,
                                     float coreR, float coreG, float coreB,
                                     float bloomR, float bloomG, float bloomB, float jitterScale, int terminalBounceCount,
                                     float noiseIndex, float noiseStrength, float startDelay) {
        CoinLightningQueue.LightningStyle style = new CoinLightningQueue.LightningStyle(coreR, coreG, coreB, bloomR, bloomG, bloomB);
        return new LightningTask(CoinLightningQueue.LightningData.path(start, end, seed, growTime, holdTime, fadeTime, width,
                style, jitterScale, terminalBounceCount, noiseIndex, noiseStrength, startDelay));
    }

    // 创建整段可见的闪电任务。
    public static LightningTask burst(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime,
                                      float width, long seed,
                                      float coreR, float coreG, float coreB,
                                      float bloomR, float bloomG, float bloomB) {
        Random random = new Random(seed);
        CoinLightningQueue.LightningStyle style = new CoinLightningQueue.LightningStyle(coreR, coreG, coreB, bloomR, bloomG, bloomB);
        return new LightningTask(CoinLightningQueue.LightningData.burst(start, end, seed, growTime, holdTime, fadeTime, width,
                3 + random.nextInt(3), style));
    }

    // 创建地面圆形扩散闪电任务。
    public static LightningTask ring(Vec3 center, Vec3 normal, float startRadius, float endRadius,
                                     float growTime, float holdTime, float fadeTime, float width, long seed,
                                     float coreR, float coreG, float coreB,
                                     float bloomR, float bloomG, float bloomB) {
        CoinLightningQueue.LightningStyle style = new CoinLightningQueue.LightningStyle(coreR, coreG, coreB, bloomR, bloomG, bloomB);
        return new LightningTask(CoinLightningQueue.LightningData.ring(center, normal, seed, startRadius, endRadius,
                growTime, holdTime, fadeTime, width, style));
    }

    // 创建玩家蓄力闪电任务。
    public static LightningTask charging(Player player, float chargeProgress, float partialTick, boolean colorful) {
        return new LightningTask(player, chargeProgress, partialTick, colorful);
    }
}
