package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.ServerKeyChargeTracker;
import com.z227.akatzumatool.common.ServerExcaliburChargeTracker;
import com.z227.akatzumatool.common.ServerSkillCooldowns;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// HeavenlyThunderChargeServerEvent 清理服务端天雷蓄力并为新追踪客户端补发状态。
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeavenlyThunderChargeServerEvent {
    // 服务端 tick 结束时清理超时或失效的蓄力状态。
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerKeyChargeTracker.tick(event.getServer());
        ServerExcaliburChargeTracker.tick(event.getServer());
    }

    // 玩家开始追踪另一名玩家时补发其蓄力动作。
    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer receiver)) return;
        if (!(event.getTarget() instanceof ServerPlayer chargingPlayer)) return;
        ServerKeyChargeTracker.sendStateToPlayer(chargingPlayer, receiver);
        ServerExcaliburChargeTracker.sendStateToPlayer(chargingPlayer, receiver);
    }

    // 玩家离线时清理并广播其蓄力动作结束。
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerKeyChargeTracker.stopHeavenlyThunder(player);
        ServerExcaliburChargeTracker.stop(player);
        ServerSkillCooldowns.clearPlayer(player);
    }

    // 玩家切换维度时清理旧维度中的蓄力动作。
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerKeyChargeTracker.stopHeavenlyThunder(player);
        ServerExcaliburChargeTracker.stop(player);
    }

    // 服务器关闭时清理静态技能冷却，避免单人世界重进残留旧时间轴。
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ServerSkillCooldowns.clearAll();
        ServerExcaliburChargeTracker.clearAll();
    }
}
