package com.z227.akatzumatool.event.client;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.AutoTrackingTargetValidator;
import com.z227.akatzumatool.config.MagicBowConfig;
import com.z227.akatzumatool.item.MagicBowItem;
import com.z227.akatzumatool.network.AutoTrackingShootC2SPacket;
import com.z227.akatzumatool.network.NetworkRegister;
import com.z227.akatzumatool.render.finalRender.miaoOutline.MiaoOutlineStyle;
import com.z227.akatzumatool.render.finalRender.miaoOutline.MiaoOutlineTargetMaskStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;

// AutoTrackingClientHandler 处理自动追踪客户端锁定、screen outline 提交和射击请求。
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AutoTrackingClientHandler {
    private static int lockedTargetId = -1; // 当前客户端锁定目标实体 ID。
    private static int autoShootRequestCooldown = 0; // 自动射击 C2S 请求冷却，避免客户端连续发包。

    // 客户端每 tick 更新自动追踪锁定目标。
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            setLockedTarget(-1);
            return;
        }

        if (autoShootRequestCooldown > 0) {
            autoShootRequestCooldown--;
        }
        if (!isDrawingMagicBowWithAutoTracking(player)) {
            autoShootRequestCooldown = 0;
            setLockedTarget(-1);
            return;
        }

        setLockedTarget(findBestTarget(level, player));
    }

    // 返回当前锁定目标 ID。
    public static int getLockedTargetId() {
        return lockedTargetId;
    }

    // 客户端松开右键时发送一次射击请求。
    public static void requestShoot(boolean restartUsing) {
        NetworkRegister.sendToServer(new AutoTrackingShootC2SPacket(lockedTargetId, restartUsing));
        if (!restartUsing) {
            setLockedTarget(-1);
        }
    }

    // 客户端检测自动射击满蓄并发送一次射击请求。
    public static void requestAutoShootIfReady(Player player, ItemStack stack, int remainingUseDuration) {
        if (!(stack.getItem() instanceof MagicBowItem magicBowItem)) return;
        if (autoShootRequestCooldown > 0) return;

        int useTicks = magicBowItem.getChargeUseTicks(stack, player, remainingUseDuration);
        int fullTicks = magicBowItem.getFullChargeTicks(stack, magicBowItem.getChargeType(stack));
        if (useTicks < fullTicks) return;

        autoShootRequestCooldown = 5;
        requestShoot(true);
    }

    // 判断玩家是否正在拉有自动追踪附魔的魔法弓。
    public static boolean isDrawingMagicBowWithAutoTracking(LocalPlayer player) {
        if (!player.isUsingItem()) return false;
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof MagicBowItem magicBowItem)) return false;
        return magicBowItem.hasAutoTracking(stack);
    }

    // 扫描客户端视野内最靠近准心的合法目标。
    public static int findBestTarget(ClientLevel level, LocalPlayer player) {
        double maxRange = MagicBowConfig.autoTrackingMaxLockRange();
        AABB searchBox = player.getBoundingBox().inflate(maxRange);
        return level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> AutoTrackingTargetValidator.isValidClientTarget(player, entity))
                .stream()
                .min(Comparator
                        .comparingDouble((LivingEntity entity) -> AutoTrackingTargetValidator.aimScore(player, entity))
                        .thenComparingDouble(entity -> entity.distanceToSqr(player)))
                .map(Entity::getId)
                .orElse(-1);
    }

    // 设置客户端锁定目标 ID，不再维护 glowing 或 scoreboard 状态。
    public static void setLockedTarget(int targetId) {
        if (lockedTargetId != targetId && lockedTargetId >= 0) {
            MiaoOutlineTargetMaskStore.clear(lockedTargetId);
        }
        lockedTargetId = targetId;
    }

    // 把当前锁定目标提交到本地 screen outline 队列，后续由 PostProcessing 写入 CA2。
    public static void submitLockedTargetOutline(ClientLevel level) {
        if (level == null || lockedTargetId < 0) return;
        if (AkatZumaTool.POST == null) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isDrawingMagicBowWithAutoTracking(player)) return;

        Entity entity = level.getEntity(lockedTargetId);
        if (entity == null || !entity.isAlive()) return;
        AkatZumaTool.POST.addMiaoOutline(entity, MiaoOutlineStyle.AUTO_TRACKING_RED);
    }

    // 获取当前锁定目标实体。
    public static Entity getLockedTarget(ClientLevel level) {
        if (level == null || lockedTargetId < 0) return null;
        Entity entity = level.getEntity(lockedTargetId);
        if (entity == null || !entity.isAlive()) return null;
        return entity;
    }

    // 尝试从当前锁定目标快速收回一个纯实体引用，供其他逻辑临时复用。
    public static Entity getLockedTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        return getLockedTarget(minecraft.level);
    }

    // 判断当前是否存在有效锁定目标。
    public static boolean hasLockedTarget() {
        return lockedTargetId >= 0;
    }

    // 兼容旧调用：把旧目标 ID 直接清空，不再恢复 glowing/team 状态。
    public static void clearLockedTarget() {
        lockedTargetId = -1;
    }

    // 兼容旧调用：当前实现不再维护客户端 glowing 状态。
    public static void restoreGlowingTarget() {
    }

    // 兼容旧调用：当前实现不再向客户端 scoreboard 队伍写入状态。
    public static void addEntityToAutoTrackingTeam(Entity entity) {
    }

    // 兼容旧调用：当前实现不再从客户端 scoreboard 队伍移除实体。
    public static void removeEntityFromAutoTrackingTeam(Entity entity) {
    }

    // 兼容旧调用：当前实现不再恢复旧队伍。
    public static void restorePreviousTeam(Entity entity) {
    }
}
