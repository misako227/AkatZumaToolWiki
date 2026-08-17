package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.effect.sparkling.SparklingFruitFlightState;
import com.z227.akatzumatool.effect.sparkling.SparklingFruitOutlineSync;
import com.z227.akatzumatool.network.SparklingBoostC2SPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// SparklingFruitEventHandler 处理闪闪果实 Buff 的服务端能力和抗干扰效果。
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SparklingFruitEventHandler {
    public static final Set<UUID> SYNCED_EFFECT_ENTITIES = new HashSet<>(); // 服务端上一 tick 仍拥有闪闪果实 Buff 的实体 UUID。

    // 闪闪果实无敌：只放行 /kill，其余攻击全部取消且不播放受伤音效。
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (!hasSparklingFruitEffect(entity)) return;

        DamageSource source = event.getSource();
        if (source.is(DamageTypes.GENERIC_KILL)) {
            return;
        }

        event.setCanceled(true);
    }

    // 闪闪果实期间取消受伤击退。
    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (hasSparklingFruitEffect(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // 每 tick 处理飞行维持、持续加速、清火、抗黑暗和入水取消。
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!hasSparklingFruitEffect(entity)) {
            if (entity instanceof Player player) {
                SparklingFruitFlightState.restorePlayer(player);
            }
            syncOutlineRemoval(entity);
            return;
        }

        // 入水立即移除闪闪果实状态，并恢复飞行能力及配套效果。
        if (entity.isInWaterOrBubble()) {
            entity.removeEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get());
            SparklingFruitOutlineSync.sendInactive(entity);
            SYNCED_EFFECT_ENTITIES.remove(entity.getUUID());
            if (entity instanceof Player player) {
                SparklingFruitFlightState.restorePlayer(player);
                entity.removeEffect(MobEffects.MOVEMENT_SPEED);
                entity.removeEffect(MobEffects.JUMP);
            }
            return;
        }

        syncOutlineState(entity);

        if (entity.isOnFire()) {
            entity.clearFire();
        }
        if (entity.hasEffect(MobEffects.DARKNESS)) {
            entity.removeEffect(MobEffects.DARKNESS);
        }
        if (entity instanceof Player player) {
            SparklingFruitFlightState.maintainFlight(player);
            SparklingFruitFlightState.tickBoost(player);
        }
    }

    // 玩家退出时先恢复临时飞行能力，再清理服务端状态记录。
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SparklingFruitFlightState.restorePlayer(event.getEntity());
        SparklingBoostC2SPacket.clearCooldown(event.getEntity());
        SYNCED_EFFECT_ENTITIES.remove(event.getEntity().getUUID());
    }

    // 玩家开始追踪闪闪果实实体时，补发当前描边和 Ctrl 加速状态。
    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        Entity target = event.getTarget();
        if (target instanceof LivingEntity livingEntity && hasSparklingFruitEffect(livingEntity)) {
            SparklingFruitOutlineSync.sendActiveToPlayer(livingEntity, serverPlayer);
        }
        if (target instanceof Player targetPlayer && SparklingFruitFlightState.isBoosting(targetPlayer)) {
            SparklingFruitFlightState.sendBoostStateToPlayer(targetPlayer, serverPlayer);
        }
    }

    // 玩家切换维度时结束旧维度加速，保留原始能力快照供 Buff 继续使用。
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        SparklingFruitFlightState.stopBoostForDimensionChange(event.getEntity());
    }

    // 玩家死亡克隆时把进入 Buff 前的能力恢复到新玩家对象。
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        SparklingFruitFlightState.restoreClonedPlayer(event.getOriginal(), event.getEntity());
    }

    // 同步服务端闪闪果实火焰描边关闭边沿。
    public static void syncOutlineRemoval(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        if (SYNCED_EFFECT_ENTITIES.remove(entity.getUUID())) {
            SparklingFruitOutlineSync.sendInactive(entity);
        }
    }

    // 强制刷新服务端闪闪果实火焰描边剩余时间，重复食用时也会发送 active 包。
    public static void syncOutlineRefresh(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        SYNCED_EFFECT_ENTITIES.add(entity.getUUID());
        SparklingFruitOutlineSync.sendActive(entity);
    }

    // 同步服务端闪闪果实火焰描边开启边沿。
    public static void syncOutlineState(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        if (SYNCED_EFFECT_ENTITIES.add(entity.getUUID())) {
            SparklingFruitOutlineSync.sendActive(entity);
        }
    }

    // 判断实体是否拥有闪闪果实 Buff。
    public static boolean hasSparklingFruitEffect(LivingEntity entity) {
        return entity != null && entity.hasEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get());
    }
}
