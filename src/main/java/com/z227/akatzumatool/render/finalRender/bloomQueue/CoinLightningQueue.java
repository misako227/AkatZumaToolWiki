package com.z227.akatzumatool.render.finalRender.bloomQueue;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.z227.akatzumatool.common.MathUtil;
import com.z227.akatzumatool.common.render.RibbonGeometry;
import com.z227.akatzumatool.item.CoinItem;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinLightningRenderType;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinLightningShader;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

// CoinLightningQueue 是无实体闪电队列，负责路径闪电、常驻闪电和地面扩散闪电的具体渲染实现。
public class CoinLightningQueue {
    public static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D); // 世界上方向。
    public static final Vec3 WORLD_RIGHT = new Vec3(1.0D, 0.0D, 0.0D); // 世界右方向，作为退化法线备用。
    public static final Vec3 WORLD_FORWARD = new Vec3(0.0D, 0.0D, 1.0D); // 世界前方向，作为退化方向备用。
    public static final int MODE_PATH = 0; // 起点到终点的单四边形路径闪电。
    public static final int MODE_BURST = 1; // 起点到终点的整段常驻闪电。
    public static final int MODE_RING = 2; // 地面圆形扩散闪电。
    public static final float MIN_TIME = 0.01F; // 最小时间，避免生命周期除零。
    public static final float MIN_WIDTH = 0.05F; // 最小宽度，避免退化四边形。
    public static final float DEFAULT_FADE_TIME = 0.05F; // 旧接口默认淡出时间。
    public static final float DEFAULT_BLOOM_STRENGTH = 0.35F; // 闪电写入 bloom 源的默认强度。
    public static final float DEFAULT_NOISE_STRENGTH = 0.1F; // UE5 风格 UV 扰动强度。
    public static final float RING_NOISE_STRENGTH = 0.2F; // 地面扩散圆形闪电专用顶点噪声扰动强度。
    public static final float MAX_VERTEX_NOISE_STRENGTH = 0.35F; // 顶点噪声强度打包上限，和 shader clamp 保持一致。
    public static final float DEFAULT_INTENSITY = 5.0F; // 纹理自发光整体强度。
    public static final float DEFAULT_PANNER_SPEED_X = 0.10F; // 噪声横向滚动速度。
    public static final float DEFAULT_PANNER_SPEED_Y = 0.90F; // 噪声纵向滚动速度。
    public static final float DEFAULT_FLICKER_STRENGTH = 0.35F; // shader 闪烁强度。
    public static final float DEFAULT_RIBBON_BLOOM_ALPHA_WEIGHT = 0.09F; // 条带 Bloom alpha 权重。
    public static final float DEFAULT_RIBBON_BLOOM_COLOR_WEIGHT = 0.05F; // 条带 Bloom 颜色权重。
    public static final float DEFAULT_CORE_BLOOM_ALPHA_FALLBACK = 0.08F; // 核心 Bloom alpha 兜底。
    public static final float DEFAULT_CORE_BLOOM_COLOR_FALLBACK = 0.04F; // 核心 Bloom 颜色兜底。
    public static final double RING_SURFACE_OFFSET = 0.015D; // 圆环贴地渲染时的轻微抬升。
    public static final float LIGHTNING_U_MIN = 0.016F; // 避开 shader 左端淡出区的最小 U。
    public static final float LIGHTNING_U_MAX = 0.984F; // 避开 shader 右端淡出区的最大 U。
    public static final float PATH_TERMINAL_CAP_START_U = 0.974F; // 末端补段起始 U，继续避开右端淡出区。
    public static final float PATH_TERMINAL_CAP_MIN_LENGTH = 0.08F; // 路径末端补段最小长度。
    public static final float PATH_TERMINAL_CAP_MAX_LENGTH = 0.35F; // 路径末端补段最大长度。
    public static final float PATH_TERMINAL_CAP_WIDTH_FACTOR = 0.35F; // 路径末端补段长度相对半宽的倍率。
    public static final int PATH_TERMINAL_BOUNCE_MIN_BRANCHES = 1; // 路径末端回弹分支最小数量。
    public static final int PATH_TERMINAL_BOUNCE_MAX_BRANCHES = 2; // 路径末端回弹分支最大数量。
    public static final float PATH_TERMINAL_BOUNCE_U_MIN = 0.20F; // 回弹分支起始 U，避开首端淡出。
    public static final float PATH_TERMINAL_BOUNCE_U_MAX = 0.80F; // 回弹分支结束 U，避开尾端淡出。
    public static final double PATH_TERMINAL_BOUNCE_LENGTH = 3.0D; // 回弹分支水平扩展长度，约 3 个方块。
    public static final double PATH_TERMINAL_BOUNCE_HEIGHT = 1.25D; // 回弹分支最高点抬升高度。
    public static final float NOISE_INDEX_PRIMARY = 0.0F; // 顶点噪声索引 0，对应 noise_076_256x。
    public static final float NOISE_INDEX_ALT = 1.0F; // 顶点噪声索引 1，对应 noise_092_256x。
    public static final int NOISE_PARAM_ALT_FLAG = 128; // 顶点噪声参数最高位，1 表示使用备用噪声图。
    public static final int NOISE_PARAM_STRENGTH_MASK = 127; // 顶点噪声参数低 7 位，保存噪声强度量化值。
    public static final int RING_EXTRA_OVERLAP_SEGMENTS = 3; // 圆环首尾闭合处额外重叠补段数量。
    public static final int MIN_RING_SEGMENTS = 24; // 圆环最小分段数。
    public static final int MAX_RING_SEGMENTS = 96; // 圆环最大分段数。

    public static final int MIN_PATH_SEGMENTS = 5; // 路径闪电最小分段数。
    public static final int MAX_PATH_SEGMENTS = 50; // 路径闪电最大分段数。
    public static final float SEGMENTS_PER_BLOCK = 3.5F; // 路径闪电每格长度的基础分段倍率。
    public static final float PATH_SEGMENT_SCALE_MIN = 0.72F; // 单条路径闪电分段随机倍率下限，降低同质化。
    public static final float PATH_SEGMENT_SCALE_RANDOM = 0.68F; // 单条路径闪电分段随机倍率额外范围。
    public static final long PATH_SEGMENT_SCALE_RANDOM_SALT = 0x5E6A1C7L; // 分段随机盐值，避免和折线偏移随机完全同步。
    public static final float PATH_DISTANCE_NEAR = 12.0F; // 近距离完整保留路径分段的距离。
    public static final float PATH_DISTANCE_MID = 32.0F; // 中距离开始进一步降低路径分段的距离。
    public static final float PATH_MID_SEGMENT_FACTOR = 0.8F; // 中距离路径分段倍率。
    public static final float PATH_FAR_SEGMENT_FACTOR = 0.6F; // 远距离路径分段倍率。
    public static final float DEFAULT_PATH_JITTER = 1.25F; // 路径闪电几何偏移相对宽度的倍率。
    public static final float DEFAULT_PATH_JITTER_MIN = 0.03F; // 路径闪电最小几何偏移半径。
    public static final float DEFAULT_PATH_JITTER_MAX = 0.45F; // 路径闪电最大几何偏移半径。
    public static final float DEFAULT_PATH_JITTER_SCALE = 1.0F; // 默认路径几何抖动倍率。

    public final List<LightningData> pendingLightnings = new ArrayList<>(); // 本帧新增闪电。
    public final List<LightningData> activeLightnings = new ArrayList<>(); // 跨帧播放闪电。
    public final Map<UUID, Integer> lastChargingLightningTick = new HashMap<>(); // 玩家蓄力闪电防重复提交 tick。
    public TextureAtlasSprite lightningSprite; // 闪电主纹理 sprite。
    public TextureAtlasSprite lightningNoiseSprite; // 闪电扰动噪声 sprite。
    public TextureAtlasSprite lightningNoiseSpriteAlt; // 闪电备用扰动噪声 sprite。

    // 添加旧接口路径闪电，内部转换成 grow/hold/fade 三段生命周期。
    public void addPath(Vec3 start, Vec3 end, float lifetime, float width, long seed,
                        float coreR, float coreG, float coreB,
                        float bloomR, float bloomG, float bloomB) {
        float safeLifetime = Math.max(MIN_TIME * 3.0F, lifetime);
        addPath(start, end, safeLifetime * 0.65F, safeLifetime * 0.25F, safeLifetime * 0.10F, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB);
    }

    // 添加路径闪电，growTime 控制从头到尾显现，holdTime 控制到达尾部后的保持显示。
    public void addPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime, float width, long seed,
                        float coreR, float coreG, float coreB,
                        float bloomR, float bloomG, float bloomB) {
        addPath(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, DEFAULT_PATH_JITTER_SCALE);
    }

    // 添加可控制几何抖动倍率的路径闪电，给粗落雷单独降低 XZ 折线跳动。
    public void addPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime, float width, long seed,
                        float coreR, float coreG, float coreB,
                        float bloomR, float bloomG, float bloomB, float jitterScale) {
        addPath(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale, 0);
    }

    // 添加可控制几何抖动和末端回弹的路径闪电，战戟下落雷使用 terminalBounceCount 生成落地反弹。
    public void addPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime, float width, long seed,
                        float coreR, float coreG, float coreB,
                        float bloomR, float bloomG, float bloomB, float jitterScale, int terminalBounceCount) {
        float noiseIndex = noiseIndexFromSeed(seed);
        addPath(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale, terminalBounceCount,
                noiseIndex, DEFAULT_NOISE_STRENGTH);
    }

    // 添加可显式控制噪声图和噪声强度的路径闪电，战戟下落雷用它让同批每根随机噪声。
    public void addPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime, float width, long seed,
                        float coreR, float coreG, float coreB,
                        float bloomR, float bloomG, float bloomB, float jitterScale, int terminalBounceCount,
                        float noiseIndex, float noiseStrength) {
        addPath(start, end, growTime, holdTime, fadeTime, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB, jitterScale, terminalBounceCount,
                noiseIndex, noiseStrength, 0.0F);
    }

    // 添加支持延迟显现的路径闪电，同批天雷用它保留 growTime 差异并对齐落地时刻。
    public void addPath(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime, float width, long seed,
                        float coreR, float coreG, float coreB,
                        float bloomR, float bloomG, float bloomB, float jitterScale, int terminalBounceCount,
                        float noiseIndex, float noiseStrength, float startDelay) {
        addLightning(LightningData.path(start, end, seed, growTime, holdTime, fadeTime, width,
                new LightningStyle(coreR, coreG, coreB, bloomR, bloomG, bloomB), jitterScale, terminalBounceCount,
                noiseIndex, noiseStrength, startDelay));
    }

    // 添加旧接口整段闪电，保留兼容入口，内部仍转换成新的三段生命周期。
    public void addBurst(Vec3 start, Vec3 end, float lifetime, float width, long seed,
                         float coreR, float coreG, float coreB,
                         float bloomR, float bloomG, float bloomB) {
        float safeLifetime = Math.max(MIN_TIME * 3.0F, lifetime);
        addBurst(start, end, safeLifetime * 0.15F, safeLifetime * 0.65F, safeLifetime * 0.20F, width, seed,
                coreR, coreG, coreB, bloomR, bloomG, bloomB);
    }

    // 添加整段常驻闪电，用少量平行偏移四边形制造分叉感。
    public void addBurst(Vec3 start, Vec3 end, float growTime, float holdTime, float fadeTime, float width, long seed,
                         float coreR, float coreG, float coreB,
                         float bloomR, float bloomG, float bloomB) {
        Random random = new Random(seed);
        addLightning(LightningData.burst(start, end, seed, growTime, holdTime, fadeTime, width, 3 + random.nextInt(3),
                new LightningStyle(coreR, coreG, coreB, bloomR, bloomG, bloomB)));
    }

    // 添加地面圆形扩散闪电，normal 决定圆环所在平面。
    public void addRing(Vec3 center, Vec3 normal, float startRadius, float endRadius,
                        float growTime, float holdTime, float fadeTime, float width, long seed,
                        float coreR, float coreG, float coreB,
                        float bloomR, float bloomG, float bloomB) {
        addLightning(LightningData.ring(center, normal, seed, startRadius, endRadius, growTime, holdTime, fadeTime, width,
                new LightningStyle(coreR, coreG, coreB, bloomR, bloomG, bloomB)));
    }

    // 添加玩家蓄力闪电，FinalRender 和 PostProcessing 只负责把 add 请求转发到这里。
    public void addChargingLightning(Player player, float chargeProgress, float partialTick, boolean colorful) {
        if (player == null || !player.isAlive()) return;
        Integer lastTick = lastChargingLightningTick.get(player.getUUID());
        if (lastTick != null && lastTick == player.tickCount) return;
        lastChargingLightningTick.put(player.getUUID(), player.tickCount);

        float progress = Mth.clamp(chargeProgress, 0.0F, 1.0F);
        Vec3 handPos = CoinItem.getChargeEffectHandOrigin(player, player.getEyePosition(partialTick), player.getViewVector(partialTick));
        int count = 2 + Mth.floor(progress * 4.0F);
        double radius = 0.25D + progress * 0.45D;
        long baseSeed = player.getUUID().getLeastSignificantBits() ^ (long) player.tickCount * 734287L;
        LightningStyle style = selectChargingStyle(baseSeed, colorful);

        for (int i = 0; i < count; i++) {
            long seed = baseSeed + i * 9973L;
            Vec3 start = handPos.add(randomSphereOffset(seed, radius));
            addPath(start, handPos, 0.04F + progress * 0.02F, 0.03F, DEFAULT_FADE_TIME, 0.048F, seed,
                    style.coreR, style.coreG, style.coreB, style.bloomR, style.bloomG, style.bloomB);
        }
    }

    // 选择蓄力闪电颜色，彩色模式在红、紫、粉之间稳定轮换，普通模式保持蓝色。
    public static LightningStyle selectChargingStyle(long seed, boolean colorful) {
        if (!colorful) return LightningStyle.blueLightning();
        int index = Math.floorMod((int) (seed ^ (seed >>> 32)), 3);
        if (index == 0) return LightningStyle.redLightning();
        if (index == 1) return LightningStyle.purpleLightning();
        return LightningStyle.pinkLightning();
    }

    // 添加已构建的闪电数据，并在这里做统一参数保护。
    public void addLightning(LightningData lightning) {
        if (lightning == null) return;
        if (lightning.mode != MODE_RING && (lightning.start == null || lightning.end == null || lightning.start.distanceToSqr(lightning.end) < 1.0E-6D)) return;
        if (lightning.mode == MODE_RING && lightning.center == null) return;
        pendingLightnings.add(lightning);
    }

    // 队列渲染：统一绑定图集、设置单个 sprite UV，再批量写入全部闪电顶点。
    public void render(MultiBufferSource.BufferSource fboBuffer, Camera camera, float partialTick, Matrix4f viewMatrix) {
        if (!CoinLightningShader.isLoaded()) return;
        if (!activatePending()) return;

        if (AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS == null) return;
        float time = MathUtil.getClientTime(partialTick);
        TextureAtlasSprite sprite = getLightningSprite();
        TextureAtlasSprite noiseSprite = getLightningNoiseSprite();
        TextureAtlasSprite noiseSpriteAlt = getLightningNoiseSpriteAlt();
        if (sprite == null || noiseSprite == null || noiseSpriteAlt == null) return;

        Vec3 cameraPos = camera.getPosition();

        // shader 使用单张自定义图集：主闪电和两张噪声是不同 sprite，但共享同一个 Sampler0。
        CoinLightningShader.setEffectParams(time, DEFAULT_BLOOM_STRENGTH, DEFAULT_NOISE_STRENGTH, DEFAULT_INTENSITY);
        CoinLightningShader.setPannerParams(DEFAULT_PANNER_SPEED_X, DEFAULT_PANNER_SPEED_Y, DEFAULT_FLICKER_STRENGTH, 0.0F);
        CoinLightningShader.setBloomParams(DEFAULT_RIBBON_BLOOM_ALPHA_WEIGHT, DEFAULT_RIBBON_BLOOM_COLOR_WEIGHT,
                DEFAULT_CORE_BLOOM_ALPHA_FALLBACK, DEFAULT_CORE_BLOOM_COLOR_FALLBACK);
        CoinLightningShader.setRenderFlags(1, 1, 0, 0);
        CoinLightningShader.setLightningSpriteUV(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
        CoinLightningShader.setNoiseSpriteUV(noiseSprite.getU0(), noiseSprite.getV0(), noiseSprite.getU1(), noiseSprite.getV1());
        CoinLightningShader.setNoiseSpriteUVAlt(noiseSpriteAlt.getU0(), noiseSpriteAlt.getV0(), noiseSpriteAlt.getU1(), noiseSpriteAlt.getV1());
        CoinLightningShader.setView(viewMatrix);
        CoinLightningShader.setSamplers(AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.getId());

        VertexConsumer consumer = fboBuffer.getBuffer(CoinLightningRenderType.getRenderType());
        Iterator<LightningData> iterator = activeLightnings.iterator();
        while (iterator.hasNext()) {
            LightningData lightning = iterator.next();
            float age = time - lightning.spawnTime;
            if (age > lightning.totalTime()) {
                iterator.remove();
                continue;
            }

            if (lightning.mode == MODE_RING) {
                writeRingLightning(consumer, lightning, age);
            } else if (lightning.mode == MODE_BURST) {
                writeBurstLightning(consumer, lightning, cameraPos, age);
            } else {
                writePathLightning(consumer, lightning, cameraPos, age);
            }
        }
        fboBuffer.endBatch(CoinLightningRenderType.getRenderType());
    }

    // 把 pending 数据推进 active，返回当前是否存在可渲染数据。
    public boolean activatePending() {
        if (!pendingLightnings.isEmpty()) {
            activeLightnings.addAll(pendingLightnings);
            pendingLightnings.clear();
        }
        return !activeLightnings.isEmpty();
    }

    public boolean hasActive() {
        return !pendingLightnings.isEmpty() || !activeLightnings.isEmpty();
    }

    public void clear() {
        pendingLightnings.clear();
        activeLightnings.clear();
        lastChargingLightningTick.clear();
    }

    // 绘制路径闪电：按长度分段后写入多个面向摄像机的四边形，几何长度体现从头到尾的显现。
    public void writePathLightning(VertexConsumer consumer, LightningData lightning, Vec3 cameraPos, float age) {
        float reveal = lightning.reveal(age);
        float alpha = lightning.alpha(age);
        if (reveal <= 0.02F || alpha <= 0.003F) return;
        writeRibbonPathLightning(consumer, lightning, cameraPos, reveal, alpha);
    }

    // 绘制连续条带路径闪电：整条路径先生成共享 left/right 节点，再用相邻节点拼四边形。
    public void writeRibbonPathLightning(VertexConsumer consumer, LightningData lightning, Vec3 cameraPos, float reveal, float alpha) {
        int segments = pathSegmentCount(lightning, cameraPos);
        List<RibbonGeometry.RibbonCenter> centers = buildVisiblePathCenters(lightning, segments, reveal);
        List<RibbonGeometry.RibbonPoint> ribbonPoints = RibbonGeometry.buildBillboardRibbon(centers, cameraPos, lightning.width);
        if (ribbonPoints.size() < 2) return;

        // 相邻四边形复用同一组接缝位置，U 避开 0/1 淡出区，避免末端顶点被 shader 吃掉。
        for (int i = 0; i < ribbonPoints.size() - 1; i++) {
            RibbonGeometry.RibbonPoint start = ribbonPoints.get(i);
            RibbonGeometry.RibbonPoint end = ribbonPoints.get(i + 1);
            float startU = visiblePathU(start.t);
            float endU = visiblePathU(end.t);
            vertex(consumer, start.left, startU, 0.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
            vertex(consumer, start.right, startU, 1.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
            vertex(consumer, end.right, endU, 1.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
            vertex(consumer, end.left, endU, 0.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
        }
    }

    // 保留旧方法名作为内部兼容入口，实际改走连续条带写法。
    public void writeSegmentedPathLightning(VertexConsumer consumer, LightningData lightning, Vec3 cameraPos, float reveal, float alpha) {
        writeRibbonPathLightning(consumer, lightning, cameraPos, reveal, alpha);
    }

    // 根据完整路径点和 reveal 裁切出当前可见的中心线点，并保留每个点的真实路径进度。
    public List<RibbonGeometry.RibbonCenter> buildVisiblePathCenters(LightningData lightning, int segments, float reveal) {
        Vec3[] points = buildPathPoints(lightning, segments);
        List<RibbonGeometry.RibbonCenter> centers = new ArrayList<>();
        centers.add(new RibbonGeometry.RibbonCenter(points[0], 0.0F));

        int fullSegments = Mth.clamp((int) Math.floor(segments * reveal), 0, segments);
        for (int i = 1; i <= fullSegments; i++) {
            centers.add(new RibbonGeometry.RibbonCenter(points[i], (float) i / (float) segments));
        }

        // reveal 落在某段中间时插入裁切点，让闪电仍然从头到尾平滑生长。
        if (fullSegments < segments && reveal > (float) fullSegments / (float) segments) {
            float t0 = (float) fullSegments / (float) segments;
            float t1 = (float) (fullSegments + 1) / (float) segments;
            double localT = (reveal - t0) / (t1 - t0);
            Vec3 clipped = lerp(points[fullSegments], points[fullSegments + 1], localT);
            centers.add(new RibbonGeometry.RibbonCenter(clipped, reveal));
        }
        return centers;
    }

    // 根据闪电长度决定基础分段，并按相机距离对远处闪电降级。
    public int pathSegmentCount(LightningData lightning, Vec3 cameraPos) {
        float length = (float) lightning.start.distanceTo(lightning.end);
        int segments = Mth.clamp((int) Math.ceil(length * SEGMENTS_PER_BLOCK * lightning.segmentScale), MIN_PATH_SEGMENTS, MAX_PATH_SEGMENTS);
        float distance = (float) midpoint(lightning.start, lightning.end).distanceTo(cameraPos);
        float factor = distance < PATH_DISTANCE_NEAR ? 1.0F : distance < PATH_DISTANCE_MID ? PATH_MID_SEGMENT_FACTOR : PATH_FAR_SEGMENT_FACTOR;
        return Mth.clamp((int) Math.ceil(segments * factor), MIN_PATH_SEGMENTS, MAX_PATH_SEGMENTS);
    }

    // 构建固定 seed 的折线路径点，首尾不偏移，中间点在垂直于路径的平面内扰动。
    public Vec3[] buildPathPoints(LightningData lightning, int segments) {
        Vec3[] points = new Vec3[segments + 1];
        Vec3 direction = safeNormalize(lightning.end.subtract(lightning.start), WORLD_FORWARD);
        Vec3 jitterAxisA = safeNormalize(direction.cross(WORLD_UP), WORLD_RIGHT);
        Vec3 jitterAxisB = safeNormalize(direction.cross(jitterAxisA), WORLD_UP);
        float safeJitterScale = Math.max(0.0F, lightning.jitterScale);
        float jitterRadius = safeJitterScale <= 0.0F ? 0.0F : Mth.clamp(lightning.width * DEFAULT_PATH_JITTER * safeJitterScale,
                DEFAULT_PATH_JITTER_MIN * safeJitterScale, DEFAULT_PATH_JITTER_MAX * safeJitterScale);
        Random random = new Random(lightning.seed);

        for (int i = 0; i <= segments; i++) {
            float t = (float) i / (float) segments;
            Vec3 base = lerp(lightning.start, lightning.end, t);
            if (i == 0 || i == segments) {
                points[i] = base;
                continue;
            }

            // sin 端点保护让偏移在中段最强，两个扰动轴都保持对称随机，避免整条 PATH 系统性偏向一侧。
            float endProtect = Mth.sin(t * (float) Math.PI);
            double offsetA = (random.nextDouble() * 2.0D - 1.0D) * jitterRadius * endProtect;
            double offsetB = (random.nextDouble() * 2.0D - 1.0D) * jitterRadius * endProtect;
            points[i] = base.add(jitterAxisA.scale(offsetA)).add(jitterAxisB.scale(offsetB));
        }
        return points;
    }

    // 绘制整段常驻闪电：多条略微偏移的四边形同批提交。
    public void writeBurstLightning(VertexConsumer consumer, LightningData lightning, Vec3 cameraPos, float age) {
        float reveal = lightning.reveal(age);
        float alpha = lightning.alpha(age) * 0.82F;
        if (reveal <= 0.02F || alpha <= 0.003F) return;

        Vec3 visibleEnd = lerp(lightning.start, lightning.end, reveal);
        Vec3 direction = safeNormalize(visibleEnd.subtract(lightning.start), WORLD_FORWARD);
        Vec3 side = normalFrom(direction, cameraPos.subtract(midpoint(lightning.start, visibleEnd)));
        Random random = new Random(lightning.seed);
        for (int branch = 0; branch < lightning.branchCount; branch++) {
            Vec3 offsetVector = side.scale((random.nextDouble() * 2.0D - 1.0D) * lightning.width * 1.8D);
            float width = lightning.width * (0.76F + branch * 0.08F);
            writeBillboardQuad(consumer, lightning.start.add(offsetVector), visibleEnd.add(offsetVector), cameraPos,
                    width, 0.0F, reveal, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
        }
    }

    // 绘制地面圆形扩散闪电：按圆环分段写入贴地四边形条带。
    public void writeRingLightning(VertexConsumer consumer, LightningData lightning, float age) {
        float reveal = lightning.reveal(age);
        float alpha = lightning.alpha(age);
        if (reveal <= 0.02F || alpha <= 0.003F) return;

        Vec3 normal = safeNormalize(lightning.normal, WORLD_UP);
        Vec3 axisA = safeNormalize(normal.cross(WORLD_RIGHT), WORLD_FORWARD);
        Vec3 axisB = safeNormalize(normal.cross(axisA), WORLD_RIGHT);
        float radius = Mth.lerp(reveal, lightning.startRadius, lightning.endRadius);
        float safeRadius = Math.max(radius, lightning.width * 1.5F);
        int segments = Mth.clamp((int) (safeRadius * 18.0F), MIN_RING_SEGMENTS, MAX_RING_SEGMENTS);
        Vec3 center = lightning.center.add(normal.scale(RING_SURFACE_OFFSET));

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / (float) segments;
            float t1 = (float) (i + 1) / (float) segments;
            float u0 = visibleRingU(t0, false);
            float u1 = visibleRingU(t1, i == segments - 1);
            Vec3 dir0 = ringDirection(axisA, axisB, t0);
            Vec3 dir1 = ringDirection(axisA, axisB, t1);
            Vec3 inner0 = center.add(dir0.scale(Math.max(0.0F, safeRadius - lightning.width)));
            Vec3 outer0 = center.add(dir0.scale(safeRadius + lightning.width));
            Vec3 outer1 = center.add(dir1.scale(safeRadius + lightning.width));
            Vec3 inner1 = center.add(dir1.scale(Math.max(0.0F, safeRadius - lightning.width)));
            vertex(consumer, inner0, u0, 0.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
            vertex(consumer, outer0, u0, 1.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
            vertex(consumer, outer1, u1, 1.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
            vertex(consumer, inner1, u1, 0.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
        }

        // 额外绘制多段首尾重叠四边形，不提高圆环高度，直接用覆盖面积补掉闭合缝隙。
//        for (int extra = 0; extra < RING_EXTRA_OVERLAP_SEGMENTS; extra++) {
//            float extraT0 = (float) extra / (float) segments;
//            float extraT1 = (float) (extra + 1) / (float) segments;
//            float extraU0 = visibleRingU(extraT0, false);
//            float extraU1 = visibleRingU(extraT1, false);
//            Vec3 extraDir0 = ringDirection(axisA, axisB, extraT0);
//            Vec3 extraDir1 = ringDirection(axisA, axisB, extraT1);
//            Vec3 extraInner0 = center.add(extraDir0.scale(Math.max(0.0F, safeRadius - lightning.width)));
//            Vec3 extraOuter0 = center.add(extraDir0.scale(safeRadius + lightning.width));
//            Vec3 extraOuter1 = center.add(extraDir1.scale(safeRadius + lightning.width));
//            Vec3 extraInner1 = center.add(extraDir1.scale(Math.max(0.0F, safeRadius - lightning.width)));
//            vertex(consumer, extraInner0, extraU0, 0.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
//            vertex(consumer, extraOuter0, extraU0, 1.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
//            vertex(consumer, extraOuter1, extraU1, 1.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
//            vertex(consumer, extraInner1, extraU1, 0.0F, alpha, lightning.style, lightning.noiseIndex, lightning.noiseStrength);
//        }
    }

    // 写入一个始终面向摄像机的路径闪电四边形。
    public void writeBillboardQuad(VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 cameraPos,
                                   float width, float u0, float u1, float alpha, LightningStyle style, float noiseIndex, float noiseStrength) {
        Vec3 direction = safeNormalize(end.subtract(start), WORLD_FORWARD);
        Vec3 side = normalFrom(direction, cameraPos.subtract(midpoint(start, end))).scale(width);
        vertex(consumer, start.subtract(side), u0, 0.0F, alpha, style, noiseIndex, noiseStrength);
        vertex(consumer, start.add(side), u0, 1.0F, alpha, style, noiseIndex, noiseStrength);
        vertex(consumer, end.add(side), u1, 1.0F, alpha, style, noiseIndex, noiseStrength);
        vertex(consumer, end.subtract(side), u1, 0.0F, alpha, style, noiseIndex, noiseStrength);
    }

    // 写入闪电顶点，颜色留给每条闪电独立控制，UV2 打包 bloom 颜色、噪声索引和噪声强度。
    public static void vertex(VertexConsumer consumer, Vec3 pos, float u, float v, float alpha, LightningStyle style, float noiseIndex, float noiseStrength) {
        consumer.vertex(pos.x, pos.y, pos.z)
                .uv(u, v)
                .color(style.coreR, style.coreG, style.coreB, Mth.clamp(alpha, 0.0F, 1.0F))
                .uv2(packColorPair(style.bloomR, style.bloomG), packColorPair(style.bloomB, packNoiseParams(noiseIndex, noiseStrength) / 255.0F))
                .endVertex();
    }

    // 把噪声图索引和扰动强度压进 8 bit，最高位选图，低 7 位量化强度。
    public static int packNoiseParams(float noiseIndex, float noiseStrength) {
        int indexFlag = noiseIndex > 0.5F ? NOISE_PARAM_ALT_FLAG : 0;
        int strengthByte = Mth.clamp(Math.round(Mth.clamp(noiseStrength, 0.0F, MAX_VERTEX_NOISE_STRENGTH) / MAX_VERTEX_NOISE_STRENGTH * NOISE_PARAM_STRENGTH_MASK), 0, NOISE_PARAM_STRENGTH_MASK);
        return indexFlag | strengthByte;
    }

    public static int packColorPair(float first, float second) {
        int firstByte = Mth.clamp((int) (first * 255.0F), 0, 255);
        int secondByte = Mth.clamp((int) (second * 255.0F), 0, 255);
        return firstByte | (secondByte << 8);
    }

    public static Vec3 normalFrom(Vec3 direction, Vec3 reference) {
        Vec3 side = direction.cross(reference);
        if (side.lengthSqr() < 1.0E-6D) side = direction.cross(WORLD_UP);
        if (side.lengthSqr() < 1.0E-6D) side = direction.cross(WORLD_RIGHT);
        return safeNormalize(side, WORLD_RIGHT);
    }

    public static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        if (vector == null || vector.lengthSqr() < 1.0E-8D) return fallback;
        return vector.normalize();
    }

    public static Vec3 midpoint(Vec3 start, Vec3 end) {
        return new Vec3((start.x + end.x) * 0.5D, (start.y + end.y) * 0.5D, (start.z + end.z) * 0.5D);
    }

    public static Vec3 lerp(Vec3 start, Vec3 end, double t) {
        return new Vec3(Mth.lerp(t, start.x, end.x), Mth.lerp(t, start.y, end.y), Mth.lerp(t, start.z, end.z));
    }

    public static Vec3 ringDirection(Vec3 axisA, Vec3 axisB, float t) {
        double angle = t * Math.PI * 2.0D;
        return axisA.scale(Math.cos(angle)).add(axisB.scale(Math.sin(angle)));
    }

    // 将真实路径进度压缩到 shader 可见 U 区间，避免首尾淡出吞掉末端闪电。
    public static float visiblePathU(float t) {
        return Mth.lerp(Mth.clamp(t, 0.0F, 1.0F), LIGHTNING_U_MIN, LIGHTNING_U_MAX);
    }

    // 将圆环进度压缩到 shader 可见 U 区间，最后一段保持最大 U，避免首尾回绕造成缺口。
    public static float visibleRingU(float t, boolean forceEnd) {
        if (forceEnd) return LIGHTNING_U_MAX;
        return visiblePathU(t - (float) Math.floor(t));
    }

    public static Vec3 randomSphereOffset(long seed, double radius) {
        Random random = new Random(seed);
        return new Vec3((random.nextDouble() * 2.0D - 1.0D) * radius,
                (random.nextDouble() * 2.0D - 1.0D) * radius,
                (random.nextDouble() * 2.0D - 1.0D) * radius);
    }

    public TextureAtlasSprite getLightningSprite() {
        if (lightningSprite == null) lightningSprite = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.LIGHTNING_TEXTURE);
        return lightningSprite;
    }

    public TextureAtlasSprite getLightningNoiseSprite() {
        if (lightningNoiseSprite == null) lightningNoiseSprite = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.LIGHTNING_NOISE_TEXTURE);
        return lightningNoiseSprite;
    }

    public TextureAtlasSprite getLightningNoiseSpriteAlt() {
        if (lightningNoiseSpriteAlt == null) lightningNoiseSpriteAlt = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.LIGHTNING_NOISE_TEXTURE_ALT);
        return lightningNoiseSpriteAlt;
    }

    // 按 seed 为整条闪电选择固定噪声图，0 对应 noise_076_256x，1 对应 noise_092_256x。
    public static float noiseIndexFromSeed(long seed) {
        return (seed & 1L) == 0L ? NOISE_INDEX_PRIMARY : NOISE_INDEX_ALT;
    }

    // 普通 PATH/BURST 默认扰动强度固定为 0.1，噪声图选择不再影响强度。
    public static float noiseStrengthFromIndex(float noiseIndex) {
        return DEFAULT_NOISE_STRENGTH;
    }

    // LightningData 记录单个闪电效果的几何、生命周期、噪声选择和颜色数据。
    public static class LightningData {
        public final int mode; // 闪电模式。
        public final Vec3 start; // 路径起点。
        public final Vec3 end; // 路径终点。
        public final Vec3 center; // 圆环中心。
        public final Vec3 normal; // 圆环法线。
        public final long seed; // 随机种子。
        public final float spawnTime; // 生成时刻。
        public final float startDelay; // 开始显现前的延迟时间。
        public final float growTime; // 从头到尾显现时间。
        public final float holdTime; // 显现完成后的保持时间。
        public final float fadeTime; // 保持结束后的淡出时间。
        public final float width; // 闪电半宽或圆环半宽。
        public final float jitterScale; // 路径几何抖动倍率。
        public final float startRadius; // 圆环起始半径。
        public final float endRadius; // 圆环结束半径。
        public final int branchCount; // 常驻闪电分支数量。
        public final int terminalBounceCount; // 路径终点回弹分支数量。
        public final float noiseIndex; // 顶点传给 shader 的噪声索引，0/1 分别选择两张噪声图。
        public final float noiseStrength; // 顶点传给 shader 的噪声扰动强度。
        public final float segmentScale; // 路径闪电分段随机倍率，让同长度闪电拥有不同折线密度。
        public final LightningStyle style; // 闪电颜色样式。

        public LightningData(int mode, Vec3 start, Vec3 end, Vec3 center, Vec3 normal, long seed,
                             float startDelay, float growTime, float holdTime, float fadeTime, float width, float jitterScale,
                             float startRadius, float endRadius, int branchCount, int terminalBounceCount,
                             float noiseIndex, float noiseStrength, float segmentScale, LightningStyle style) {
            this.mode = mode;
            this.start = start;
            this.end = end;
            this.center = center;
            this.normal = normal;
            this.seed = seed;
            this.spawnTime = MathUtil.getClientTime(0.0F);
            this.startDelay = Math.max(0.0F, startDelay);
            this.growTime = Math.max(MIN_TIME, growTime);
            this.holdTime = Math.max(0.0F, holdTime);
            this.fadeTime = Math.max(MIN_TIME, fadeTime);
            this.width = Math.max(MIN_WIDTH, width);
            this.jitterScale = Math.max(0.0F, jitterScale);
            this.startRadius = Math.max(0.0F, startRadius);
            this.endRadius = Math.max(this.startRadius, endRadius);
            this.branchCount = Math.max(1, branchCount);
            this.terminalBounceCount = Mth.clamp(terminalBounceCount, 0, PATH_TERMINAL_BOUNCE_MAX_BRANCHES);
            this.noiseIndex = noiseIndex > 0.5F ? NOISE_INDEX_ALT : NOISE_INDEX_PRIMARY;
            this.noiseStrength = Mth.clamp(noiseStrength, 0.0F, MAX_VERTEX_NOISE_STRENGTH);
            this.segmentScale = Mth.clamp(segmentScale, PATH_SEGMENT_SCALE_MIN, PATH_SEGMENT_SCALE_MIN + PATH_SEGMENT_SCALE_RANDOM);
            this.style = style;
        }

        public static LightningData path(Vec3 start, Vec3 end, long seed, float growTime, float holdTime, float fadeTime, float width, LightningStyle style) {
            return path(start, end, seed, growTime, holdTime, fadeTime, width, style, DEFAULT_PATH_JITTER_SCALE, 0);
        }

        public static LightningData path(Vec3 start, Vec3 end, long seed, float growTime, float holdTime, float fadeTime, float width, LightningStyle style, float jitterScale) {
            return path(start, end, seed, growTime, holdTime, fadeTime, width, style, jitterScale, 0);
        }

        public static LightningData path(Vec3 start, Vec3 end, long seed, float growTime, float holdTime, float fadeTime, float width, LightningStyle style, float jitterScale, int terminalBounceCount) {
            float noiseIndex = noiseIndexFromSeed(seed);
            return path(start, end, seed, growTime, holdTime, fadeTime, width, style, jitterScale, terminalBounceCount, noiseIndex, DEFAULT_NOISE_STRENGTH);
        }

        public static LightningData path(Vec3 start, Vec3 end, long seed, float growTime, float holdTime, float fadeTime, float width, LightningStyle style, float jitterScale, int terminalBounceCount, float noiseIndex, float noiseStrength) {
            return path(start, end, seed, growTime, holdTime, fadeTime, width, style, jitterScale,
                    terminalBounceCount, noiseIndex, noiseStrength, 0.0F);
        }

        // 创建带开始延迟的路径闪电，growTime 继续作为真实下落速度变量。
        public static LightningData path(Vec3 start, Vec3 end, long seed, float growTime, float holdTime, float fadeTime, float width,
                                         LightningStyle style, float jitterScale, int terminalBounceCount,
                                         float noiseIndex, float noiseStrength, float startDelay) {
            float segmentScale = segmentScaleFromSeed(seed);
            return new LightningData(MODE_PATH, start, end, null, null, seed, startDelay, growTime, holdTime, fadeTime, width,
                    jitterScale, 0.0F, 0.0F, 1, terminalBounceCount, noiseIndex, noiseStrength, segmentScale, style);
        }

        public static LightningData burst(Vec3 start, Vec3 end, long seed, float growTime, float holdTime, float fadeTime, float width, int branchCount, LightningStyle style) {
            float noiseIndex = noiseIndexFromSeed(seed);
            return new LightningData(MODE_BURST, start, end, null, null, seed, 0.0F, growTime, holdTime, fadeTime, width,
                    DEFAULT_PATH_JITTER_SCALE, 0.0F, 0.0F, branchCount, 0, noiseIndex, noiseStrengthFromIndex(noiseIndex), 1.0F, style);
        }

        public static LightningData ring(Vec3 center, Vec3 normal, long seed, float startRadius, float endRadius, float growTime, float holdTime, float fadeTime, float width, LightningStyle style) {
            return new LightningData(MODE_RING, null, null, center, normal == null ? WORLD_UP : normal, seed, 0.0F, growTime, holdTime, fadeTime, width,
                    DEFAULT_PATH_JITTER_SCALE, startRadius, endRadius, 1, 0, NOISE_INDEX_ALT, RING_NOISE_STRENGTH, 1.0F, style);
        }

        // 根据 seed 生成单条 PATH 闪电的分段倍率，避免同长度闪电拥有完全相同顶点数量。
        public static float segmentScaleFromSeed(long seed) {
            Random random = new Random(seed ^ PATH_SEGMENT_SCALE_RANDOM_SALT);
            return PATH_SEGMENT_SCALE_MIN + random.nextFloat() * PATH_SEGMENT_SCALE_RANDOM;
        }

        public float totalTime() {
            return startDelay + growTime + holdTime + fadeTime;
        }

        public float reveal(float age) {
            float effectiveAge = age - startDelay;
            if (effectiveAge <= 0.0F) return 0.0F;
            return Mth.clamp(effectiveAge / growTime, 0.0F, 1.0F);
        }

        public float alpha(float age) {
            float effectiveAge = age - startDelay;
            if (effectiveAge <= 0.0F) return 0.0F;
            if (effectiveAge <= growTime + holdTime) return 1.0F;
            return 1.0F - Mth.clamp((effectiveAge - growTime - holdTime) / fadeTime, 0.0F, 1.0F);
        }
    }

    // LightningStyle 记录每条闪电的可见颜色和 bloom 颜色。
    public static class LightningStyle {
        public final float coreR; // 可见层红色。
        public final float coreG; // 可见层绿色。
        public final float coreB; // 可见层蓝色。
        public final float bloomR; // bloom 层红色。
        public final float bloomG; // bloom 层绿色。
        public final float bloomB; // bloom 层蓝色。

        public LightningStyle(float coreR, float coreG, float coreB, float bloomR, float bloomG, float bloomB) {
            this.coreR = Mth.clamp(coreR, 0.0F, 1.0F);
            this.coreG = Mth.clamp(coreG, 0.0F, 1.0F);
            this.coreB = Mth.clamp(coreB, 0.0F, 1.0F);
            this.bloomR = Mth.clamp(bloomR, 0.0F, 1.0F);
            this.bloomG = Mth.clamp(bloomG, 0.0F, 1.0F);
            this.bloomB = Mth.clamp(bloomB, 0.0F, 1.0F);
        }

        // 创建蓝色闪电样式。
        public static LightningStyle blueLightning() {
            return new LightningStyle(0.3F, 0.7F, 0.8F, 0.01F, 0.1F, 1.0F);
        }

        // 创建红色闪电样式。
        public static LightningStyle redLightning() {
            return new LightningStyle(1.0F, 0.58F, 0.9F, 1.0F, 0.01F, 0.0F);
        }

        // 创建紫色闪电样式。
        public static LightningStyle purpleLightning() {
            return new LightningStyle(0.82F, 0.55F, 1.0F, 0.62F, 0.18F, 1.0F);
        }

        // 创建粉色闪电样式。
        public static LightningStyle pinkLightning() {
            return new LightningStyle(1.0F, 0.62F, 0.9F, 1.0F, 0.30F, 0.82F);
        }
    }
}
