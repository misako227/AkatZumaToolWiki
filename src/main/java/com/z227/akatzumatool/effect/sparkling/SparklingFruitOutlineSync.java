package com.z227.akatzumatool.effect.sparkling;

import com.z227.akatzumatool.event.EffectRegister;
import com.z227.akatzumatool.network.NetworkRegister;
import com.z227.akatzumatool.network.SparklingFruitOutlineS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// SparklingFruitOutlineSync 负责把服务端闪闪果实火焰描边状态同步给追踪客户端。
public class SparklingFruitOutlineSync {
    public static final int CLOSE_DURATION_TICKS = 0; // 关闭描边包不需要持续时间。

    // 向追踪实体的客户端和实体自身同步开启火焰描边。
    public static void sendActive(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        int durationTicks = getRemainingDuration(entity);
        if (durationTicks <= 0) return;
        NetworkRegister.sendToTrackingEntityAndSelf(new SparklingFruitOutlineS2CPacket(entity.getId(), true, durationTicks), entity);
    }

    // 向追踪实体的客户端和实体自身同步关闭火焰描边。
    public static void sendInactive(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        NetworkRegister.sendToTrackingEntityAndSelf(new SparklingFruitOutlineS2CPacket(entity.getId(), false, CLOSE_DURATION_TICKS), entity);
    }

    // 向单个玩家补发某个实体当前的火焰描边状态。
    public static void sendActiveToPlayer(LivingEntity entity, ServerPlayer player) {
        if (entity == null || player == null || entity.level().isClientSide()) return;
        int durationTicks = getRemainingDuration(entity);
        if (durationTicks <= 0) return;
        NetworkRegister.sendToPlayer(new SparklingFruitOutlineS2CPacket(entity.getId(), true, durationTicks), player);
    }

    // 读取闪闪果实 Buff 剩余 tick，服务端补发和开启同步共用。
    public static int getRemainingDuration(LivingEntity entity) {
        if (entity == null) return 0;
        MobEffectInstance instance = entity.getEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get());
        return instance == null ? 0 : instance.getDuration();
    }

    // 判断实体是否适合参与火焰描边状态同步。
    public static boolean canSync(Entity entity) {
        return entity instanceof LivingEntity livingEntity && livingEntity.hasEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get());
    }
}
