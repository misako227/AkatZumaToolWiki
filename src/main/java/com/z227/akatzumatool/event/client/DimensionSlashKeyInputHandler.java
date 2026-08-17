package com.z227.akatzumatool.event.client;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.SkillCooldownType;
import com.z227.akatzumatool.config.TridentPlusConfig;
import com.z227.akatzumatool.item.FlySwordItem;
import com.z227.akatzumatool.item.FlySwordPlusItem;
import com.z227.akatzumatool.item.TridentPlusItem;
import com.z227.akatzumatool.network.DimensionSlashCastC2SPacket;
import com.z227.akatzumatool.network.HeavenlyThunderCastC2SPacket;
import com.z227.akatzumatool.network.HeavenlyThunderChargeStartC2SPacket;
import com.z227.akatzumatool.network.HeavenlyThunderChargeStopC2SPacket;
import com.z227.akatzumatool.network.NetworkRegister;
import com.z227.akatzumatool.network.ExcaliburCastC2SPacket;
import com.z227.akatzumatool.network.ExcaliburChargeStartC2SPacket;
import com.z227.akatzumatool.network.ExcaliburChargeStopC2SPacket;
import com.z227.akatzumatool.network.SummonFlySwordC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
// DimensionSlashKeyInputHandler 负责飞剑按键输入与客户端冷却提示。
public class DimensionSlashKeyInputHandler {
    public static final String HEAVENLY_THUNDER_CHARGE_KEY = SkillCooldownType.HEAVENLY_THUNDER.key(); // 天雷通用按键蓄力 key。
    public static final String EXCALIBUR_CHARGE_KEY = "excalibur"; // 咖喱棒 C 键蓄力 key。
    public static final int SUMMON_COOLDOWN_TICKS = 200; // 召唤飞剑客户端冷却 10 秒。
    public static final int BATTO_SLASH_COOLDOWN_TICKS = 100; // 拔刀斩客户端冷却 5 秒。
    public static final Map<SkillCooldownType, Map<UUID, Long>> COOLDOWNS = new HashMap<>(); // 按技能类型和玩家 UUID 保存本地冷却结束时间。
    public static final String COOLDOWN_MESSAGE_KEY = "message.akatzumatool.client_cooldown_remaining"; // 冷却提示翻译键。
    public static final String NAMED_COOLDOWN_MESSAGE_KEY = "message.akatzumatool.client_cooldown_remaining_named"; // 带技能名的冷却提示翻译键。
    public static final String NOT_ENOUGH_FOOD_MESSAGE_KEY = "message.akatzumatool.not_enough_food"; // 饱食度不足提示翻译键。
    public static final Component DIMENSION_SLASH_NAME = Component.translatable("skill.akatzumatool.dimension_slash"); // 次元斩技能名。
    public static final Component SUMMON_FLY_SWORD_NAME = Component.translatable("skill.akatzumatool.summon_fly_sword"); // 召唤飞剑技能名。
    public static final Component BATTO_SLASH_NAME = Component.translatable("skill.akatzumatool.batto_slash"); // 拔刀斩技能名。
    public static final Component HEAVENLY_THUNDER_NAME = Component.translatable("skill.akatzumatool.heavenly_thunder"); // 天雷技能名。
    public static Object lastLevel = null; // 上一次客户端世界引用，用于切换世界时清理本地冷却。

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) return;

        while (DimensionSlashKeyHandler.DIMENSION_SLASH_KEY.consumeClick()) {
            if (TridentPlusItem.isHoldingHeavenlyThunderTrident(minecraft.player)) {
                startHeavenlyThunderCharge(minecraft);
                continue;
            }
            if (!FlySwordItem.isHoldingFlySwordPlus(minecraft.player)) continue;
            int remainingTicks = getRemainingCooldownTicks(minecraft, SkillCooldownType.DIMENSION_SLASH);
            if (remainingTicks > 0) {
                sendCooldownMessage(minecraft, SkillCooldownType.DIMENSION_SLASH, remainingTicks);
                continue;
            }
            setCooldown(minecraft, SkillCooldownType.DIMENSION_SLASH);
            NetworkRegister.sendToServer(new DimensionSlashCastC2SPacket());
        }

        while (DimensionSlashKeyHandler.SUMMON_FLY_SWORD_KEY.consumeClick()) {
            if (!FlySwordItem.isHoldingAnyFlySword(minecraft.player)) continue;
            int remainingTicks = getRemainingCooldownTicks(minecraft, SkillCooldownType.SUMMON_FLY_SWORD);
            if (remainingTicks > 0) {
                sendCooldownMessage(minecraft, SkillCooldownType.SUMMON_FLY_SWORD, remainingTicks);
                continue;
            }
            setCooldown(minecraft, SkillCooldownType.SUMMON_FLY_SWORD);
            NetworkRegister.sendToServer(new SummonFlySwordC2SPacket());
        }

        while (DimensionSlashKeyHandler.EXCALIBUR_KEY.consumeClick()) {
            if (!FlySwordItem.isHoldingFlySwordPlus(minecraft.player)) continue;
            startExcaliburCharge(minecraft);
        }
    }

    // 客户端每 tick 维护天雷 V 键蓄力、咖喱棒 C 键蓄力和异常取消。
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clearClientSkillState();
            lastLevel = null;
            return;
        }
        if (lastLevel != minecraft.level) {
            clearClientSkillState();
            lastLevel = minecraft.level;
        }
        ClientKeyChargeRegistry.tick();
        ClientExcaliburChargeRegistry.tick();

        // 打开界面时立刻取消全部按键蓄力，避免服务端残留动作。
        if (minecraft.screen != null) {
            cancelHeavenlyThunderCharge(minecraft);
            cancelExcaliburCharge(minecraft);
            return;
        }

        if (!TridentPlusItem.isHoldingHeavenlyThunderTrident(minecraft.player)) {
            cancelHeavenlyThunderCharge(minecraft);
        }
        if (!FlySwordItem.isHoldingFlySwordPlus(minecraft.player)) {
            cancelExcaliburCharge(minecraft);
        }

        KeyChargeHandler.tick(minecraft, HEAVENLY_THUNDER_CHARGE_KEY,
                () -> releaseHeavenlyThunderCharge(minecraft),
                () -> stopHeavenlyThunderCharge(minecraft));
        KeyChargeHandler.tickHoldToRelease(minecraft, EXCALIBUR_CHARGE_KEY,
                () -> releaseExcaliburCharge(minecraft),
                () -> stopExcaliburCharge(minecraft),
                () -> stopExcaliburCharge(minecraft));
    }

    // 客户端断开连接时清理静态本地状态，避免旧世界 gameTime 冷却残留。
    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientSkillState();
        lastLevel = null;
    }

    // 校验客户端条件后开始天雷蓄力并立即通知服务端同步动作。
    public static void startHeavenlyThunderCharge(Minecraft minecraft) {
        int remainingTicks = getRemainingCooldownTicks(minecraft, SkillCooldownType.HEAVENLY_THUNDER);
        if (remainingTicks > 0) {
            sendCooldownMessage(minecraft, SkillCooldownType.HEAVENLY_THUNDER, remainingTicks);
            return;
        }
        if (!TridentPlusItem.hasEnoughHeavenlyThunderFood(minecraft.player)) {
            sendActionbarMessage(minecraft, Component.translatable(NOT_ENOUGH_FOOD_MESSAGE_KEY, SkillCooldownType.HEAVENLY_THUNDER.displayName()));
            return;
        }

        InteractionHand hand = TridentPlusItem.getHeldHeavenlyThunderHand(minecraft.player);
        if (hand == null) return;
        int chargeTicks = TridentPlusConfig.heavenlyThunderChargeTicks();
        boolean started = KeyChargeHandler.start(minecraft, DimensionSlashKeyHandler.DIMENSION_SLASH_KEY,
                HEAVENLY_THUNDER_CHARGE_KEY, chargeTicks, TridentPlusConfig.heavenlyThunderSlowWhileCharging(),
                () -> NetworkRegister.sendToServer(new HeavenlyThunderChargeStartC2SPacket()));
        if (started) ClientKeyChargeRegistry.startLocal(minecraft.player, hand, chargeTicks);
    }

    // 满蓄力后只发送一次释放包并从此刻开始客户端冷却。
    public static void releaseHeavenlyThunderCharge(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) return;
        ClientKeyChargeRegistry.stop(minecraft.player);
        NetworkRegister.sendToServer(new HeavenlyThunderCastC2SPacket());
    }

    // 松键或状态失效时同步停止服务端蓄力动作。
    public static void stopHeavenlyThunderCharge(Minecraft minecraft) {
        if (minecraft != null && minecraft.player != null) ClientKeyChargeRegistry.stop(minecraft.player);
        NetworkRegister.sendToServer(new HeavenlyThunderChargeStopC2SPacket());
    }

    // 外部条件失效时取消尚未完成的天雷蓄力。
    public static void cancelHeavenlyThunderCharge(Minecraft minecraft) {
        KeyChargeHandler.cancel(HEAVENLY_THUNDER_CHARGE_KEY, () -> stopHeavenlyThunderCharge(minecraft));
    }

    // 校验客户端条件后开始咖喱棒蓄力并立即通知服务端同步动作。
    public static void startExcaliburCharge(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) return;
        InteractionHand hand = FlySwordItem.getHeldFlySwordPlusHand(minecraft.player);
        if (hand == null) return;
        int fullChargeTicks = FlySwordPlusItem.getExcaliburFullChargeTicks();
        int maxChargeTicks = FlySwordPlusItem.EXCALIBUR_MAX_CHARGE_TICKS;
        boolean started = KeyChargeHandler.startManualRelease(minecraft, DimensionSlashKeyHandler.EXCALIBUR_KEY,
                EXCALIBUR_CHARGE_KEY, fullChargeTicks, maxChargeTicks, true,
                () -> NetworkRegister.sendToServer(new ExcaliburChargeStartC2SPacket()));
        if (started) ClientExcaliburChargeRegistry.startLocal(minecraft.player, hand, fullChargeTicks, maxChargeTicks);
    }

    // 满蓄力后松开 C 键，请求服务端释放咖喱棒首版视觉。
    public static void releaseExcaliburCharge(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) return;
        ClientExcaliburChargeRegistry.stop(minecraft.player);
        NetworkRegister.sendToServer(new ExcaliburCastC2SPacket());
    }

    // 未满蓄力、超时或状态失效时同步停止服务端咖喱棒蓄力动作。
    public static void stopExcaliburCharge(Minecraft minecraft) {
        if (minecraft != null && minecraft.player != null) ClientExcaliburChargeRegistry.stop(minecraft.player);
        NetworkRegister.sendToServer(new ExcaliburChargeStopC2SPacket());
    }

    // 外部条件失效时取消尚未完成的咖喱棒蓄力。
    public static void cancelExcaliburCharge(Minecraft minecraft) {
        KeyChargeHandler.cancel(EXCALIBUR_CHARGE_KEY, () -> stopExcaliburCharge(minecraft));
    }

    // 应用服务端天雷释放结果，成功后才写入完整冷却。
    public static void applyHeavenlyThunderCastResult(boolean success, int cooldownTicks, boolean notEnoughFood) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) return;
        ClientKeyChargeRegistry.stop(minecraft.player);
        if (success) {
            setCooldown(minecraft, SkillCooldownType.HEAVENLY_THUNDER, cooldownTicks);
            return;
        }
        if (notEnoughFood) {
            sendActionbarMessage(minecraft, Component.translatable(NOT_ENOUGH_FOOD_MESSAGE_KEY, SkillCooldownType.HEAVENLY_THUNDER.displayName()));
            return;
        }
        if (cooldownTicks > 0) {
            setCooldown(minecraft, SkillCooldownType.HEAVENLY_THUNDER, cooldownTicks);
            sendCooldownMessage(minecraft, SkillCooldownType.HEAVENLY_THUNDER, cooldownTicks);
        }
    }

    // 清理客户端技能冷却和蓄力视觉状态。
    public static void clearClientSkillState() {
        COOLDOWNS.clear();
        KeyChargeHandler.clear();
        ClientKeyChargeRegistry.clear();
        ClientExcaliburChargeRegistry.clear();
    }

    // 计算当前玩家指定技能的剩余本地冷却 tick。
    public static int getRemainingCooldownTicks(Minecraft minecraft, SkillCooldownType skillType) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || skillType == null) return 0;
        Map<UUID, Long> cooldowns = COOLDOWNS.computeIfAbsent(skillType, key -> new HashMap<>());
        UUID uuid = minecraft.player.getUUID();
        long gameTime = minecraft.level.getGameTime();
        long expiry = cooldowns.getOrDefault(uuid, 0L);
        return expiry > gameTime ? (int) (expiry - gameTime) : 0;
    }

    // 发送纯客户端的剩余冷却提示。
    public static void sendCooldownMessage(Minecraft minecraft, int remainingTicks) {
        sendCooldownMessage(minecraft, Component.translatable("skill.akatzumatool.generic"), remainingTicks);
    }

    // 发送指定技能的纯客户端剩余冷却提示。
    public static void sendCooldownMessage(Minecraft minecraft, SkillCooldownType skillType, int remainingTicks) {
        sendCooldownMessage(minecraft, skillType == null ? Component.translatable("skill.akatzumatool.generic") : skillType.displayName(), remainingTicks);
    }

    // 发送带技能名的纯客户端剩余冷却提示，显示在血条上方。
    public static void sendCooldownMessage(Minecraft minecraft, Component skillName, int remainingTicks) {
        int remainingSeconds = (remainingTicks + 19) / 20;
        sendActionbarMessage(minecraft, Component.translatable(NAMED_COOLDOWN_MESSAGE_KEY, skillName, remainingSeconds));
    }

    // 在血条上方显示纯客户端提示。
    public static void sendActionbarMessage(Minecraft minecraft, Component message) {
        if (minecraft == null || minecraft.player == null || message == null) return;
        minecraft.player.displayClientMessage(message, true);
    }

    // 写入指定技能的本地冷却结束时间。
    public static void setCooldown(Minecraft minecraft, SkillCooldownType skillType) {
        if (skillType == null) return;
        setCooldown(minecraft, skillType, skillType.cooldownTicks());
    }

    // 按指定 tick 写入本地冷却结束时间。
    public static void setCooldown(Minecraft minecraft, SkillCooldownType skillType, int cooldownTicks) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || skillType == null) return;
        int safeCooldownTicks = Math.max(1, cooldownTicks);
        COOLDOWNS.computeIfAbsent(skillType, key -> new HashMap<>())
                .put(minecraft.player.getUUID(), minecraft.level.getGameTime() + safeCooldownTicks);
    }
}
