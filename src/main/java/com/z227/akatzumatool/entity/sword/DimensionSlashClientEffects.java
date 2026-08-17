package com.z227.akatzumatool.entity.sword;

import com.z227.akatzumatool.common.CameraShakeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

// DimensionSlashClientEffects 负责次元斩客户端非粒子类效果。
public class DimensionSlashClientEffects {
    // 破碎阶段触发一次本地相机抖动。
    public static void tryPlayShake(DimensionSlashDomainEntity entity) {
        if (entity.clientShakePlayed) return;
        if (entity.getAge() < DimensionSlashConfig.FINAL_HIT_TICK) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) return;
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        double distanceSqr = cameraPos.distanceToSqr(entity.position());
        double radiusSqr = DimensionSlashConfig.RADIUS * DimensionSlashConfig.RADIUS;
        if (distanceSqr > radiusSqr) return;
        CameraShakeUtil.addShake(entity.position(), (float) DimensionSlashConfig.RADIUS, DimensionSlashConfig.SHAKE_TICKS, DimensionSlashConfig.SHAKE_STRENGTH);
        entity.clientShakePlayed = true;
    }
}
