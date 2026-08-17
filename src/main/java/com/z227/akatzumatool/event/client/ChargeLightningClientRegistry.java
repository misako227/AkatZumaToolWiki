package com.z227.akatzumatool.event.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// ChargeLightningClientRegistry 记录客户端正在右键蓄力的玩家，避免渲染帧扫描所有玩家。
public class ChargeLightningClientRegistry {
    private static final Map<UUID, ChargeVisualState> ACTIVE_CHARGES = new ConcurrentHashMap<>(); // 客户端活跃蓄力视觉状态。

    // 注册普通硬币蓄力视觉状态。
    public static void startCoin(Player player, boolean colorful) {
        if (player == null || !player.level().isClientSide()) return;

        ChargeVisualType type = colorful ? ChargeVisualType.COLORFUL_COIN : ChargeVisualType.COIN;
        ACTIVE_CHARGES.put(player.getUUID(), new ChargeVisualState(player.getUUID(), type, colorful));
    }

    // 注册 BeamCross 测试物品蓄力视觉状态。
    public static void startBeamCross(Player player) {
        if (player == null || !player.level().isClientSide()) return;

        ACTIVE_CHARGES.put(player.getUUID(), new ChargeVisualState(player.getUUID(), ChargeVisualType.BEAM_CROSS, false));
    }

    // 移除指定玩家的蓄力视觉状态。
    public static void stop(Player player) {
        if (player == null) return;

        stop(player.getUUID());
    }

    // 移除指定 UUID 的蓄力视觉状态。
    public static void stop(UUID playerId) {
        if (playerId == null) return;

        ACTIVE_CHARGES.remove(playerId);
    }

    // 返回当前活跃蓄力视觉状态集合。
    public static Collection<ChargeVisualState> activeCharges() {
        return ACTIVE_CHARGES.values();
    }

    // 清理已离开客户端世界或死亡的玩家状态。
    public static void cleanup(Level level) {
        if (level == null) {
            clearAll();
            return;
        }

        ACTIVE_CHARGES.keySet().removeIf(playerId -> {
            Player player = level.getPlayerByUUID(playerId);
            return player == null || !player.isAlive();
        });
    }

    // 清理所有客户端蓄力视觉状态。
    public static void clearAll() {
        ACTIVE_CHARGES.clear();
    }

    // 蓄力视觉类型。
    public enum ChargeVisualType {
        COIN, // 普通硬币蓄力闪电。
        COLORFUL_COIN, // 彩色硬币蓄力闪电。
        BEAM_CROSS // BeamCross 测试物品蓄力特效。
    }

    // 单个玩家的蓄力视觉状态。
    public record ChargeVisualState(UUID playerId, ChargeVisualType type, boolean colorful) {
    }
}
