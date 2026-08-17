package com.z227.akatzumatool.event.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import java.util.HashMap;
import java.util.Map;

// KeyChargeHandler 管理客户端通用按键蓄力生命周期、可选移动减速和完全禁用移动状态。
public class KeyChargeHandler {
    public static final Map<String, ChargeState> CHARGES = new HashMap<>(); // 按技能 key 保存当前客户端蓄力状态。

    // 在按键首次按下时创建一轮蓄力并触发开始回调。
    public static boolean start(Minecraft minecraft, KeyMapping keyMapping, String skillKey, int requiredTicks,
                                boolean slowWhileCharging, Runnable onStart) {
        if (minecraft == null || minecraft.level == null || keyMapping == null || skillKey == null) return false;
        if (CHARGES.containsKey(skillKey)) return false;
        ChargeState state = new ChargeState(keyMapping, minecraft.level.getGameTime(), requiredTicks, requiredTicks, slowWhileCharging, false);
        CHARGES.put(skillKey, state);
        if (onStart != null) onStart.run();
        return true;
    }

    // 在按键首次按下时创建一轮手动松键释放蓄力并触发开始回调。
    public static boolean startManualRelease(Minecraft minecraft, KeyMapping keyMapping, String skillKey, int requiredTicks,
                                             int maxChargeTicks, boolean blockMovement, Runnable onStart) {
        if (minecraft == null || minecraft.level == null || keyMapping == null || skillKey == null) return false;
        if (CHARGES.containsKey(skillKey)) return false;
        ChargeState state = new ChargeState(keyMapping, minecraft.level.getGameTime(), requiredTicks, maxChargeTicks, false, blockMovement);
        CHARGES.put(skillKey, state);
        if (onStart != null) onStart.run();
        return true;
    }

    // 更新指定技能蓄力，松键时取消，满蓄力时只触发一次完成回调。
    public static void tick(Minecraft minecraft, String skillKey, Runnable onFullCharge, Runnable onCancel) {
        ChargeState state = CHARGES.get(skillKey);
        if (state == null || minecraft == null || minecraft.level == null) return;

        // 自动释放后保留状态到松键，阻止持续按住时立即开始下一轮。
        if (state.completed) {
            if (!state.keyMapping.isDown()) CHARGES.remove(skillKey);
            return;
        }
        if (!state.keyMapping.isDown()) {
            CHARGES.remove(skillKey);
            if (onCancel != null) onCancel.run();
            return;
        }

        state.chargeTicks = Math.max(0L, minecraft.level.getGameTime() - state.startGameTime);
        if (state.chargeTicks < state.requiredChargeTicks) return;
        state.completed = true;
        if (onFullCharge != null) onFullCharge.run();
    }

    // 更新手动松键释放蓄力，满蓄后不自动释放，只在松键时按进度决定释放或取消。
    public static void tickHoldToRelease(Minecraft minecraft, String skillKey, Runnable onRelease, Runnable onCancel, Runnable onExpire) {
        ChargeState state = CHARGES.get(skillKey);
        if (state == null || minecraft == null || minecraft.level == null) return;

        state.chargeTicks = Math.max(0L, minecraft.level.getGameTime() - state.startGameTime);
        if (state.chargeTicks > state.maxChargeTicks) {
            CHARGES.remove(skillKey);
            if (onExpire != null) onExpire.run();
            return;
        }

        if (state.keyMapping.isDown()) return;
        CHARGES.remove(skillKey);
        if (state.chargeTicks >= state.requiredChargeTicks) {
            if (onRelease != null) onRelease.run();
            return;
        }
        if (onCancel != null) onCancel.run();
    }

    // 强制取消指定技能蓄力并执行取消回调。
    public static void cancel(String skillKey, Runnable onCancel) {
        ChargeState state = CHARGES.remove(skillKey);
        if (state == null || state.completed) return;
        if (onCancel != null) onCancel.run();
    }

    // 清理全部客户端蓄力状态，切换世界或断线时使用。
    public static void clear() {
        CHARGES.clear();
    }

    public static boolean isCharging(String skillKey) {
        ChargeState state = CHARGES.get(skillKey);
        return state != null && !state.completed;
    }

    public static boolean isCompleted(String skillKey) {
        ChargeState state = CHARGES.get(skillKey);
        return state != null && state.completed;
    }

    public static float getProgress(String skillKey) {
        ChargeState state = CHARGES.get(skillKey);
        if (state == null) return 0.0F;
        return Math.min(1.0F, (float) state.chargeTicks / (float) state.requiredChargeTicks);
    }

    public static long getChargeTicks(String skillKey) {
        ChargeState state = CHARGES.get(skillKey);
        return state == null ? 0L : state.chargeTicks;
    }

    // 服务端开始同步返回后用权威配置修正本轮满蓄力 tick。
    public static void updateRequiredTicks(String skillKey, int requiredTicks) {
        ChargeState state = CHARGES.get(skillKey);
        if (state == null || state.completed) return;
        state.requiredChargeTicks = Math.max(1, requiredTicks);
    }

    // 判断当前是否存在要求降低移动速度的按键蓄力。
    public static boolean shouldSlowMovement() {
        for (ChargeState state : CHARGES.values()) {
            if (!state.completed && state.slowWhileCharging) return true;
        }
        return false;
    }

    // 判断当前是否存在要求完全禁止移动输入的按键蓄力。
    public static boolean shouldBlockMovement() {
        for (ChargeState state : CHARGES.values()) {
            if (!state.completed && state.blockMovement) return true;
        }
        return false;
    }

    // ChargeState 保存单轮客户端按键蓄力数据。
    public static class ChargeState {
        public final KeyMapping keyMapping; // 本轮蓄力绑定的按键。
        public final long startGameTime; // 客户端开始蓄力 gameTime。
        public int requiredChargeTicks; // 满蓄力所需 tick，可由服务端同步修正。
        public final int maxChargeTicks; // 本轮最大允许蓄力 tick。
        public final boolean slowWhileCharging; // 蓄力期间是否减速。
        public final boolean blockMovement; // 蓄力期间是否完全禁用移动。
        public long chargeTicks; // 当前已蓄力 tick。
        public boolean completed; // 本轮是否已经自动释放。

        public ChargeState(KeyMapping keyMapping, long startGameTime, int requiredChargeTicks, int maxChargeTicks,
                           boolean slowWhileCharging, boolean blockMovement) {
            this.keyMapping = keyMapping;
            this.startGameTime = startGameTime;
            this.requiredChargeTicks = Math.max(1, requiredChargeTicks);
            this.maxChargeTicks = Math.max(this.requiredChargeTicks, maxChargeTicks);
            this.slowWhileCharging = slowWhileCharging;
            this.blockMovement = blockMovement;
        }
    }
}
