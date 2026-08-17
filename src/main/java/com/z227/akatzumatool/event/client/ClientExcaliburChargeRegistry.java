package com.z227.akatzumatool.event.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// ClientExcaliburChargeRegistry 保存本地和远端玩家咖喱棒蓄力动作状态。
public class ClientExcaliburChargeRegistry {
    public static final int EXPIRE_GRACE_TICKS = 20; // 最大蓄力后的同步兜底清理 tick。
    public static final Map<Integer, VisualChargeState> STATES = new HashMap<>(); // 按玩家实体 ID 保存视觉蓄力状态。

    // 应用服务端同步的咖喱棒蓄力开始或结束状态。
    public static void apply(int entityId, boolean active, InteractionHand hand, int fullChargeTicks, int elapsedTicks, int maxChargeTicks) {
        if (!active) {
            STATES.remove(entityId);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.player.getId() == entityId) {
                KeyChargeHandler.cancel(DimensionSlashKeyInputHandler.EXCALIBUR_CHARGE_KEY, null);
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        STATES.put(entityId, new VisualChargeState(hand, gameTime - Math.max(0, elapsedTicks), fullChargeTicks, maxChargeTicks));
    }

    // 本地按下 C 键后立即登记视觉状态，避免等待网络往返。
    public static void startLocal(Player player, InteractionHand hand, int fullChargeTicks, int maxChargeTicks) {
        if (player == null) return;
        apply(player.getId(), true, hand, fullChargeTicks, 0, maxChargeTicks);
    }

    public static void stop(Player player) {
        if (player == null) return;
        STATES.remove(player.getId());
    }

    // 每客户端 tick 清理超时或已经离开当前世界的玩家状态。
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            STATES.clear();
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        Iterator<Map.Entry<Integer, VisualChargeState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, VisualChargeState> entry = iterator.next();
            VisualChargeState state = entry.getValue();
            if (minecraft.level.getEntity(entry.getKey()) == null
                    || gameTime - state.startGameTime > state.maxChargeTicks + EXPIRE_GRACE_TICKS) {
                iterator.remove();
            }
        }
    }

    public static boolean isCharging(Player player) {
        return player != null && STATES.containsKey(player.getId());
    }

    public static InteractionHand getHand(Player player) {
        VisualChargeState state = player == null ? null : STATES.get(player.getId());
        return state == null ? InteractionHand.MAIN_HAND : state.hand;
    }

    public static float getProgress(Player player, float partialTick) {
        if (player == null) return 0.0F;
        VisualChargeState state = STATES.get(player.getId());
        Minecraft minecraft = Minecraft.getInstance();
        if (state == null || minecraft.level == null) return 0.0F;
        float elapsed = minecraft.level.getGameTime() - state.startGameTime + partialTick;
        return Mth.clamp(elapsed / state.fullChargeTicks, 0.0F, 1.0F);
    }

    public static void clear() {
        STATES.clear();
    }

    // VisualChargeState 保存一名玩家的咖喱棒蓄力视觉时间和手部。
    public static class VisualChargeState {
        public final InteractionHand hand; // 玩家蓄力使用的手。
        public final long startGameTime; // 换算到客户端时间轴的开始 tick。
        public final int fullChargeTicks; // 满蓄力所需 tick。
        public final int maxChargeTicks; // 最大蓄力 tick。

        public VisualChargeState(InteractionHand hand, long startGameTime, int fullChargeTicks, int maxChargeTicks) {
            this.hand = hand == null ? InteractionHand.MAIN_HAND : hand;
            this.startGameTime = startGameTime;
            this.fullChargeTicks = Math.max(1, fullChargeTicks);
            this.maxChargeTicks = Math.max(this.fullChargeTicks, maxChargeTicks);
        }
    }
}
