package com.z227.akatzumatool.render.finalRender.bloomQueue;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.z227.akatzumatool.common.MathUtil;
import com.z227.akatzumatool.entity.sword.DimensionSlashConfig;
import com.z227.akatzumatool.entity.sword.DimensionSlashStrikeEntity;
import com.z227.akatzumatool.render.finalRender.queue.EntityQueue;
import com.z227.akatzumatool.render.renderType.DimensionSlashType.DimensionSlashStrikeRenderType;
import com.z227.akatzumatool.render.renderType.DimensionSlashType.DimensionSlashStrikeShader;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

// DimensionSlashStrikeQueue 使用 RenderType 和 VertexConsumer 批量写入次元斩白蓝光刃。
public class DimensionSlashStrikeQueue extends EntityQueue<DimensionSlashStrikeEntity> {
    public DimensionSlashStrikeQueue() {
        super();
    }

    @Override
    public void render(MultiBufferSource.BufferSource fboBuffer, Camera camera, float partialTick, Matrix4f viewMatrix) {
        if (!DimensionSlashStrikeShader.isLoaded()) return;
        float time = MathUtil.getClientTime(partialTick);
        DimensionSlashStrikeShader.setEffectParams(time, DimensionSlashConfig.BLOOM_STRENGTH);
        DimensionSlashStrikeShader.setView(viewMatrix);
        VertexConsumer consumer = fboBuffer.getBuffer(DimensionSlashStrikeRenderType.getRenderType());
        for (DimensionSlashStrikeEntity entity : entities) {
            renderStrikes(consumer, entity, camera, partialTick);
        }
        fboBuffer.endBatch(DimensionSlashStrikeRenderType.getRenderType());
    }

    // 按实体 age 依次生成多道斩击。
    public void renderStrikes(VertexConsumer consumer, DimensionSlashStrikeEntity entity, Camera camera, float partialTick) {
        float age = entity.getAge() + partialTick;
        int total = DimensionSlashConfig.STRIKE_COUNT;
        for (int i = 0; i < total; i++) {
            float spawnAge = i * (DimensionSlashConfig.STRIKE_SPAWN_TICKS / (float) total);
            float localAge = age - spawnAge;
            if (localAge < 0.0F) continue;
            float alpha = getStrikeAlpha(age, localAge);
            if (alpha <= 0.001F) continue;
            writeStrikeQuad(consumer, entity, camera, i, alpha);
        }
    }

    // 计算单道斩击透明度，出现后保留，终结前统一淡出。
    public float getStrikeAlpha(float age, float localAge) {
        float fadeIn = Mth.clamp(localAge / 0.55F, 0.0F, 1.0F);
        float globalFade = 1.0F - Mth.clamp((age - DimensionSlashConfig.STRIKE_HOLD_END_TICK) / Math.max(1.0F, (float) DimensionSlashConfig.STRIKE_FADE_OUT_TICKS), 0.0F, 1.0F);
        return fadeIn * globalFade;
    }

    // 写入一道面向摄像机的白蓝斩击四边形。
    public void writeStrikeQuad(VertexConsumer consumer, DimensionSlashStrikeEntity entity, Camera camera, int index, float alpha) {
        Random random = new Random(entity.getVisualSeed() * 734287L + index * 9127L);
        double radius = DimensionSlashConfig.RADIUS * (0.06D + random.nextDouble() * 0.86D);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double height = 0.35D + random.nextDouble() * 4.7D;
        Vec3 center = entity.position().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
        Vec3 toCamera = camera.getPosition().subtract(center);
        Vec3 cameraRight = getSafeNormalize(new Vec3(toCamera.z, 0.0D, -toCamera.x), new Vec3(1.0D, 0.0D, 0.0D));
        Vec3 cameraUp = getSafeNormalize(cameraRight.cross(toCamera).normalize(), new Vec3(0.0D, 1.0D, 0.0D));
        float roll = (float) (random.nextDouble() * Math.PI);
        double cos = Math.cos(roll);
        double sin = Math.sin(roll);
        Vec3 axisLong = getSafeNormalize(cameraRight.scale(cos).add(cameraUp.scale(sin)), cameraRight);
        Vec3 axisWide = getSafeNormalize(axisLong.cross(toCamera).normalize(), cameraUp);
        double length = 4.8D + random.nextDouble() * 8.4D;
        double width = 0.045D + random.nextDouble() * 0.105D;
        Vec3 p0 = center.subtract(axisLong.scale(length)).subtract(axisWide.scale(width));
        Vec3 p1 = center.add(axisLong.scale(length)).subtract(axisWide.scale(width));
        Vec3 p2 = center.add(axisLong.scale(length)).add(axisWide.scale(width));
        Vec3 p3 = center.subtract(axisLong.scale(length)).add(axisWide.scale(width));
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        writeVertex(consumer, p0, 0.0F, 0.0F, a);
        writeVertex(consumer, p1, 1.0F, 0.0F, a);
        writeVertex(consumer, p2, 1.0F, 1.0F, a);
        writeVertex(consumer, p3, 0.0F, 1.0F, a);
    }

    // 写入 POSITION_COLOR_TEX 顶点。
    public void writeVertex(VertexConsumer consumer, Vec3 pos, float u, float v, int alpha) {
        consumer.vertex(pos.x, pos.y, pos.z)
                .color(205, 240, 255, alpha)
                .uv(u, v)
                .endVertex();
    }

    // 安全归一化，避免摄像机和斩击中心重合时出现 NaN。
    public Vec3 getSafeNormalize(Vec3 value, Vec3 fallback) {
        if (value == null || value.lengthSqr() < 1.0E-8D) return fallback;
        return value.normalize();
    }
}
