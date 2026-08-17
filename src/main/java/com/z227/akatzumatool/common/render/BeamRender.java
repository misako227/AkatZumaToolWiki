package com.z227.akatzumatool.common.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.z227.akatzumatool.common.VertexUtil;
import com.z227.akatzumatool.entity.coin.ColorfulCoinEntity;
import com.z227.akatzumatool.entity.coin.RailgunBeamEntity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// BeamRender 负责把世界空间线段转换成面向相机的光束 billboard 顶点。
public class BeamRender {
    private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0); // 生成法线失败时的优先备用方向。
    private static final Vec3 WORLD_RIGHT = new Vec3(1.0, 0.0, 0.0); // 垂直向上光束的最终备用方向。

    public static final BeamStyle NORMAL = new BeamStyle(
            0.32f, 0.78f, 1.15f, 1.0f, 0.62f, 0.74f,
            0.96f, 0.98f, 1.0f,
            0.36f, 0.82f, 1.0f,
            0.56f, 0.92f, 1.0f
    ); // 普通电磁炮蓝白光束样式。

    public static final BeamStyle COLORFUL = new BeamStyle(
            1.22f, 0.92f, 3.0f, 1.0f, 1.55f, 1.12f,
            1.0f, 0.92f, 0.35f,
            1.0f, 0.55f, 0.12f,
            1.0f, 0.08f, 0.02f
    ); // 彩色硬币黄白核心和红色外辉光束样式。

    public static final BeamStyle STAR_JUDGEMENT = new BeamStyle(
            0.86f, 0.9f, 2.15f, 1.15f, 2.15f, 0.92f,
            0.82f, 0.96f, 1.0f,
            0.36f, 0.78f, 1.0f,
            0.06f, 0.34f, 1.0f
    ); // 星辰裁决冲天光束样式，白蓝核心加深蓝外辉。

    public static final BeamStyle STAR_JUDGEMENT_FINAL = new BeamStyle(
            1.85f, 0.96f, 3.2f, 1.35f, 2.45f, 0.95f,
            0.92f, 0.95f, 1.0f,
            0.55f, 0.58f, 1.0f,
            0.34f, 0.08f, 0.95f
    ); // 星辰裁决最终落下粗光束样式，白蓝核心加紫蓝外辉。

    // BeamStyle 保存同一套光束几何下的宽度、透明度、bloom 和颜色参数。
    public static class BeamStyle {
        public final float width; // 基础半宽。
        public final float alpha; // 基础透明度。
        public final float bloomStrength; // bloom 输出强度。
        public final float noiseStrength; // shader 噪声强度。
        public final float outerWidthScale; // 第二层外辉宽度倍率。
        public final float outerAlphaScale; // 第二层外辉透明度倍率。
        public final float coreR; // 核心色 R。
        public final float coreG; // 核心色 G。
        public final float coreB; // 核心色 B。
        public final float innerR; // 内辉色 R。
        public final float innerG; // 内辉色 G。
        public final float innerB; // 内辉色 B。
        public final float outerR; // 外辉色 R。
        public final float outerG; // 外辉色 G。
        public final float outerB; // 外辉色 B。

        public BeamStyle(float width, float alpha, float bloomStrength, float noiseStrength,
                         float outerWidthScale, float outerAlphaScale,
                         float coreR, float coreG, float coreB,
                         float innerR, float innerG, float innerB,
                         float outerR, float outerG, float outerB) {
            this.width = width;
            this.alpha = alpha;
            this.bloomStrength = bloomStrength;
            this.noiseStrength = noiseStrength;
            this.outerWidthScale = outerWidthScale;
            this.outerAlphaScale = outerAlphaScale;
            this.coreR = coreR;
            this.coreG = coreG;
            this.coreB = coreB;
            this.innerR = innerR;
            this.innerG = innerG;
            this.innerB = innerB;
            this.outerR = outerR;
            this.outerG = outerG;
            this.outerB = outerB;
        }
    }

    // 写入默认电磁炮光束顶点。
    public static void writeBeam(VertexConsumer consumer, RailgunBeamEntity beam, Camera camera, float partialTick) {
        writeBeam(consumer, beam, camera, partialTick, NORMAL);
    }

    // 按指定样式写入电磁炮光束顶点。
    public static void writeBeam(VertexConsumer consumer, RailgunBeamEntity beam, Camera camera, float partialTick, BeamStyle style) {
        writeBeamData(consumer, beam.getOrigin(partialTick), beam.getEndpoint(partialTick),
                beam.getAge(), beam.getLifetime(), camera, partialTick, style);
    }

    // 按指定样式写入彩色硬币光束顶点。
    public static void writeBeam(VertexConsumer consumer, ColorfulCoinEntity beam, Camera camera, float partialTick, BeamStyle style) {
        writeBeamData(consumer, beam.getOrigin(partialTick), beam.getEndpoint(partialTick),
                beam.getAge(), beam.getLifetime(), camera, partialTick, style);
    }

    // 按任意起点和终点写入光束，供星辰裁决这类非光束实体复用。
    public static void writeBeamSegment(VertexConsumer consumer, Vec3 origin, Vec3 endpoint, int entityAge,
                                        int entityLifetime, Camera camera, float partialTick, BeamStyle style) {
        writeBeamData(consumer, origin, endpoint, entityAge, entityLifetime, camera, partialTick, style);
    }

    // 根据线段、相机和样式生成两层 billboard 光束几何。
    public static void writeBeamData(VertexConsumer consumer, Vec3 origin, Vec3 endpoint, int entityAge,
                                     int entityLifetime, Camera camera, float partialTick, BeamStyle style) {
        Vec3 delta = endpoint.subtract(origin);
        double length = delta.length();
        if (length < 0.05) return;

        Vec3 direction = delta.scale(1.0 / length);

        // 光束前段随生命周期展开，后段在生命周期末尾淡出。
        float age = entityAge + partialTick;
        float lifetime = Math.max(entityLifetime, 1);
        float expand = Mth.clamp(age / Math.max(lifetime * 0.28f, 1.0f), 0.1f, 1.0f);
        float fade = 1.0f - Mth.clamp((age - lifetime * 0.62f) / Math.max(lifetime * 0.38f, 1.0f), 0.0f, 1.0f);
        if (fade <= 0.01f || expand <= 0.01f) return;

        Vec3 visibleEnd = origin.add(direction.scale(length * expand));
        Vec3 mid = origin.add(visibleEnd).scale(0.5);
        Vec3 cameraVector = camera.getPosition().subtract(mid);
        Vec3 side = normalFrom(direction, cameraVector);
        Vec3 side2 = safeNormalize(direction.cross(side), WORLD_UP);

        float alpha = style.alpha * fade;
        float width = style.width * (0.65f + 0.35f * fade);
        writeBeamQuad(consumer, origin, visibleEnd, side.scale(width), alpha, style.innerR, style.innerG, style.innerB);
        writeBeamQuad(consumer, origin, visibleEnd, side2.scale(width * style.outerWidthScale),
                alpha * style.outerAlphaScale, style.outerR, style.outerG, style.outerB);
    }

    // 写入单个光束四边形，UV.x 表示线段方向，UV.y 表示横截面方向。
    public static void writeBeamQuad(VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 halfWidth,
                                     float alpha, float r, float g, float b) {
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);
        Vec3 s0 = start.add(halfWidth);
        Vec3 s1 = start.subtract(halfWidth);
        Vec3 e1 = end.subtract(halfWidth);
        Vec3 e0 = end.add(halfWidth);

        VertexUtil.putVertex(consumer, s0, 0.0f, 1.0f, r, g, b, alpha);
        VertexUtil.putVertex(consumer, s1, 0.0f, 0.0f, r, g, b, alpha);
        VertexUtil.putVertex(consumer, e1, 1.0f, 0.0f, r, g, b, alpha);
        VertexUtil.putVertex(consumer, e0, 1.0f, 1.0f, r, g, b, alpha);
    }

    // 根据线段方向和相机方向计算稳定横向法线。
    public static Vec3 normalFrom(Vec3 direction, Vec3 reference) {
        Vec3 side = direction.cross(reference);
        if (side.lengthSqr() < 1.0E-6) {
            side = direction.cross(WORLD_UP);
        }
        if (side.lengthSqr() < 1.0E-6) {
            side = direction.cross(WORLD_RIGHT);
        }
        return safeNormalize(side, WORLD_RIGHT);
    }

    // 向量长度过小时使用备用方向。
    public static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        if (vector.lengthSqr() < 1.0E-8) {
            return fallback;
        }
        return vector.normalize();
    }
}
