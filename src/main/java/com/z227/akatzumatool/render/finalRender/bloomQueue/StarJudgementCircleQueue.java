package com.z227.akatzumatool.render.finalRender.bloomQueue;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.z227.akatzumatool.common.MathUtil;
import com.z227.akatzumatool.common.VertexUtil;
import com.z227.akatzumatool.common.render.BeamRender;
import com.z227.akatzumatool.entity.bow.MagicBowParticleEffectEntity;
import com.z227.akatzumatool.render.finalRender.queue.EntityQueue;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinBeamRenderType;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinBeamShader;
import com.z227.akatzumatool.render.renderType.StarJudgementCircleType.StarJudgementCircleRenderType;
import com.z227.akatzumatool.render.renderType.StarJudgementCircleType.StarJudgementCircleShader;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

// StarJudgementCircleQueue 渲染 MagicBowParticleEffectEntity 驱动的星辰裁决光束和天空法阵。
public class StarJudgementCircleQueue extends EntityQueue<MagicBowParticleEffectEntity> {
    private static final float CIRCLE_HEIGHT = 24.0f; // 法阵悬浮在目标上方的高度。
    private static final float MIN_CIRCLE_RADIUS = 12.0f; // 固定法阵半径的最小保护值。
    private static final float MAX_CIRCLE_RADIUS = 25.0f; // 固定法阵半径的最大保护值。
    private static final float CORE_LAYER = 0.25f; // 核心八芒星层编号。
    private static final float MAIN_LAYER = 0.0f; // 主环层编号。
    private static final float OUTER_LAYER = 0.5f; // 外符文层编号。
    private static final float SIDE_LAYER = 0.85f; // 侧面辉光层编号。

    public StarJudgementCircleQueue() {
        super();
    }

    @Override
    public void render(MultiBufferSource.BufferSource fboBuffer, Camera camera, float parTick, Matrix4f viewMatrix) {
        float time = MathUtil.getClientTime(parTick);
        for (MagicBowParticleEffectEntity entity : entities) {
            if (!entity.isStarJudgementVisual()) continue;
            renderBeam(fboBuffer, camera, parTick, viewMatrix, time, entity);
            renderCircle(fboBuffer, camera, viewMatrix, time, entity);
        }
    }

    // 渲染从目标中心冲向法阵中心的蓝白冲天光束。
    public void renderBeam(MultiBufferSource.BufferSource fboBuffer, Camera camera, float parTick,
                           Matrix4f viewMatrix, float time, MagicBowParticleEffectEntity entity) {
        if (!CoinBeamShader.isLoaded()) return;

        BeamRender.BeamStyle style = BeamRender.STAR_JUDGEMENT;
        applyBeamShaderStyle(viewMatrix, time, style);

        Vec3 center = entity.getStarJudgementVisualCenter();
        Vec3 beamStart = center.add(0.0D, -entity.getStarJudgementTargetHeight() * 0.45D, 0.0D);
        Vec3 beamEnd = center.add(0.0D, CIRCLE_HEIGHT, 0.0D);
        VertexConsumer consumer = fboBuffer.getBuffer(CoinBeamRenderType.getRenderType());
        BeamRender.writeBeamSegment(consumer, beamStart, beamEnd, entity.tickCount, 42, camera, parTick, style);
        fboBuffer.endBatch(CoinBeamRenderType.getRenderType());

        renderFinalStrikeBeam(fboBuffer, camera, parTick, viewMatrix, time, entity);
    }

    // 写入光束 shader uniform，不同阶段可以切换不同颜色和 bloom 强度。
    public void applyBeamShaderStyle(Matrix4f viewMatrix, float time, BeamRender.BeamStyle style) {
        CoinBeamShader.setEffectParams(time, style.bloomStrength, style.noiseStrength, 0.0f);
        CoinBeamShader.setRenderFlags(0, 1, 0, 0);
        CoinBeamShader.setBeamColors(
                style.coreR, style.coreG, style.coreB,
                style.innerR, style.innerG, style.innerB,
                style.outerR, style.outerG, style.outerB
        );
        CoinBeamShader.setView(viewMatrix);
    }

    // 渲染星辰裁决最终从法阵落下的粗光束。
    public void renderFinalStrikeBeam(MultiBufferSource.BufferSource fboBuffer, Camera camera, float parTick,
                                      Matrix4f viewMatrix, float time, MagicBowParticleEffectEntity entity) {
        int strikeDelay = Math.max(entity.getStarJudgementStrikeDelayTicks(), 1);
        int beamDuration = MagicBowParticleEffectEntity.STAR_JUDGEMENT_FINAL_BEAM_DURATION_TICKS;
        int beamStartTick = Math.max(1, strikeDelay - beamDuration);
        int beamAge = entity.tickCount - beamStartTick;
        if (beamAge < 0 || beamAge > beamDuration + 10) return;

        BeamRender.BeamStyle style = BeamRender.STAR_JUDGEMENT_FINAL;
        applyBeamShaderStyle(viewMatrix, time, style);
        Vec3 center = entity.getStarJudgementVisualCenter();
        Vec3 beamStart = center.add(0.0D, CIRCLE_HEIGHT, 0.0D);
        Vec3 beamEnd = center.add(0.0D, -entity.getStarJudgementTargetHeight() * 0.45D, 0.0D);
        VertexConsumer consumer = fboBuffer.getBuffer(CoinBeamRenderType.getRenderType());
        BeamRender.writeBeamSegment(consumer, beamStart, beamEnd, beamAge + 1, beamDuration + 10, camera, parTick, style);
        fboBuffer.endBatch(CoinBeamRenderType.getRenderType());
    }

    // 渲染由 shader 自己计算细节的低顶点天空法阵。

    // centerProgress：中心层展开时间。
    // 当前是第 4 tick 开始，持续 12 tick 展开完。
    //
    // outerProgress：外围层展开时间。
    // 当前是第 10 tick 开始，持续 20 tick 展开完。
    //
    // strikeProgress：最终裁决闪光时间。
    // 当前是在最终粗光束落地前约 10 tick 开始，持续到大爆炸后高亮。
    public void renderCircle(MultiBufferSource.BufferSource fboBuffer, Camera camera, Matrix4f viewMatrix,
                             float time, MagicBowParticleEffectEntity entity) {
        if (!StarJudgementCircleShader.isLoaded()) return;

        int duration = Math.max(entity.getStarJudgementDurationTicks(), 1);
        int strikeDelay = Math.max(entity.getStarJudgementStrikeDelayTicks(), 1);
        float age = Mth.clamp(entity.tickCount / (float) duration, 0.0f, 1.0f);
        float centerProgress = smoothProgress((entity.tickCount - 4.0f) / 12.0f);
        float outerProgress = smoothProgress((entity.tickCount - 10.0f) / 20.0f);
        float strikeProgress = smoothProgress((entity.tickCount - strikeDelay + 10.0f) / 16.0f);
        float radius = Mth.clamp(entity.getStarJudgementVisualRadius(), MIN_CIRCLE_RADIUS, MAX_CIRCLE_RADIUS);
        float bloomStrength = getRadiusScaledBloomStrength(radius);

        StarJudgementCircleShader.setEffectParams(time, age, centerProgress, outerProgress);
        StarJudgementCircleShader.setStrikeParams(strikeProgress, radius, bloomStrength, 0.0f);
        StarJudgementCircleShader.setView(viewMatrix);

        Vec3 center = entity.getStarJudgementVisualCenter().add(0.0D, CIRCLE_HEIGHT, 0.0D);
        VertexConsumer consumer = fboBuffer.getBuffer(StarJudgementCircleRenderType.getRenderType());
        writeHorizontalCircleQuad(consumer, center.add(0.0D, 0.45D, 0.0D), radius * 0.58f, CORE_LAYER, 0.78f);
        writeHorizontalCircleQuad(consumer, center, radius * 0.82f, MAIN_LAYER, 0.64f);
        writeHorizontalCircleQuad(consumer, center.add(0.0D, -0.35D, 0.0D), radius, OUTER_LAYER, 0.48f);
        writeSideGlowQuad(consumer, camera, center, radius, SIDE_LAYER, 0.26f);
        fboBuffer.endBatch(StarJudgementCircleRenderType.getRenderType());
    }

    // 写入水平法阵四边形，shader 根据 UV 自己画圆环和符文。
    public void writeHorizontalCircleQuad(VertexConsumer consumer, Vec3 center, float radius, float layer, float alpha) {
        Vec3 x = new Vec3(radius, 0.0D, 0.0D);
        Vec3 z = new Vec3(0.0D, 0.0D, radius);
        VertexUtil.putVertex(consumer, center.subtract(x).subtract(z), 0.0f, 0.0f, layer, 1.0f, 1.0f, alpha);
        VertexUtil.putVertex(consumer, center.add(x).subtract(z), 1.0f, 0.0f, layer, 1.0f, 1.0f, alpha);
        VertexUtil.putVertex(consumer, center.add(x).add(z), 1.0f, 1.0f, layer, 1.0f, 1.0f, alpha);
        VertexUtil.putVertex(consumer, center.subtract(x).add(z), 0.0f, 1.0f, layer, 1.0f, 1.0f, alpha);
    }

    // 写入面向相机的薄侧光，远处侧视时仍能看到法阵厚度。
    public void writeSideGlowQuad(VertexConsumer consumer, Camera camera, Vec3 center, float radius, float layer, float alpha) {
        Vec3 toCamera = camera.getPosition().subtract(center);
        Vec3 side = new Vec3(toCamera.z, 0.0D, -toCamera.x);
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize().scale(radius);
        Vec3 up = new Vec3(0.0D, radius * 0.12D, 0.0D);
        VertexUtil.putVertex(consumer, center.subtract(side).subtract(up), 0.0f, 0.0f, layer, 1.0f, 1.0f, alpha);
        VertexUtil.putVertex(consumer, center.add(side).subtract(up), 1.0f, 0.0f, layer, 1.0f, 1.0f, alpha);
        VertexUtil.putVertex(consumer, center.add(side).add(up), 1.0f, 1.0f, layer, 1.0f, 1.0f, alpha);
        VertexUtil.putVertex(consumer, center.subtract(side).add(up), 0.0f, 1.0f, layer, 1.0f, 1.0f, alpha);
    }

    public float smoothProgress(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    // 法阵半径越大，稳定阶段 bloom 越低，避免范围翻倍后整盘过曝。
    public float getRadiusScaledBloomStrength(float radius) {
        return Mth.clamp(28.0f / Math.max(radius, 1.0f), 0.95f, 1.65f);
    }
}
