package com.z227.akatzumatool.event.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.effect.sparkling.client.SparklingFruitFlightClientState;
import com.z227.akatzumatool.effect.sparkling.client.SparklingFruitOutlineClientState;
import com.z227.akatzumatool.event.EffectRegister;
import com.z227.akatzumatool.network.NetworkRegister;
import com.z227.akatzumatool.network.SparklingBoostC2SPacket;
import com.z227.akatzumatool.network.SparklingFlightInputC2SPacket;
import com.z227.akatzumatool.render.finalRender.miaoOutline.MiaoOutlineStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

// SparklingFruitClientHandler 处理闪闪果实 Ctrl 飞行、Alt 瞬移输入和火焰描边目标提交。
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SparklingFruitClientHandler {
    public static int teleportCooldown = 0; // 当前客户端剩余瞬移发包冷却。
    public static boolean lastTeleportKeyDown; // 上一客户端 tick 的瞬移键按下状态。
    public static boolean lastFlightBoostKeyDown; // 上一客户端 tick 是否正在请求 Ctrl 加速飞行。

    // 客户端每 tick 更新描边、飞行轨迹、Ctrl 持续输入和 Alt 瞬移边沿。
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        SparklingFruitOutlineClientState.tick(level);
        SparklingFruitFlightClientState.tick(level);
        if (player == null) {
            lastFlightBoostKeyDown = false;
            lastTeleportKeyDown = false;
            return;
        }

        if (teleportCooldown > 0) {
            teleportCooldown--;
        }
        handleFlightBoostInput(minecraft, player);
        handleTeleportInput(minecraft, player);
    }

    // 使用原版疾跑键映射发送 Ctrl 加速开始和停止边沿。
    public static void handleFlightBoostInput(Minecraft minecraft, LocalPlayer player) {
        boolean hasBuff = player.hasEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get());
        boolean boostKeyDown = minecraft.screen == null && hasBuff && minecraft.options.keySprint.isDown();
        if (boostKeyDown == lastFlightBoostKeyDown) return;

        lastFlightBoostKeyDown = boostKeyDown;
        // 本地先应用状态减少拖尾延迟，服务端拒绝时会回包纠正。
        SparklingFruitFlightClientState.apply(player.getId(), boostKeyDown, false,
                ConfigFile.sparklingFruitFlightBoostMaxSpeed());
        NetworkRegister.sendToServer(new SparklingFlightInputC2SPacket(boostKeyDown));
    }

    // 检测 Alt 按下边沿，瞬移只发 C2S 请求，实际落点由服务端校验。
    public static void handleTeleportInput(Minecraft minecraft, LocalPlayer player) {
        boolean teleportKeyDown = isTeleportKeyDown(minecraft);
        boolean pressedThisTick = teleportKeyDown && !lastTeleportKeyDown;
        lastTeleportKeyDown = teleportKeyDown;

        if (!pressedThisTick) return;
        if (teleportCooldown > 0) return;
        if (!player.hasEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get())) return;

        NetworkRegister.sendToServer(new SparklingBoostC2SPacket());
        teleportCooldown = ConfigFile.sparklingFruitTeleportCooldownTicks();
    }

    // 把客户端可见的闪闪果实 Buff 实体提交到火焰描边通道。
    public static void submitSparklingFruitFireOutlines(ClientLevel level, Frustum frustum) {
        if (level == null || AkatZumaTool.POST == null) return;

        SparklingFruitOutlineClientState.forEachActive(level, entity -> {
            if (!shouldRenderSparklingOutline(entity, frustum)) return;
            AkatZumaTool.POST.addMiaoOutline(entity, MiaoOutlineStyle.SPARKLING_FRUIT_FIRE);
        });
    }

    // 判断实体是否需要在原版渲染阶段捕获火焰描边顶点。
    public static boolean shouldCaptureSparklingFruitOutline(Entity entity) {
        if (shouldSkipFirstPersonLocalPlayerOutline(entity)) return false;
        if (!(entity instanceof LivingEntity)) return false;
        return isSparklingOutlineActive(entity);
    }

    // 判断实体是否需要提交到本帧火焰描边队列。
    public static boolean shouldRenderSparklingOutline(Entity entity, Frustum frustum) {
        if (entity == null || !entity.isAlive()) return false;
        if (shouldSkipFirstPersonLocalPlayerOutline(entity)) return false;
        if (!(entity instanceof LivingEntity)) return false;
        if (!isSparklingOutlineActive(entity)) return false;
        return frustum == null || frustum.isVisible(entity.getBoundingBox().inflate(0.5D));
    }

    // 判断火焰描边状态，远端实体以 S2C 缓存为准，本地玩家保留 MobEffect 兜底。
    public static boolean isSparklingOutlineActive(Entity entity) {
        if (entity == null) return false;
        if (SparklingFruitOutlineClientState.isActive(entity.getId())) return true;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return player != null && entity.getId() == player.getId() && player.hasEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get());
    }

    // 判断是否需要跳过第一人称本地玩家自己的火焰描边。
    public static boolean shouldSkipFirstPersonLocalPlayerOutline(Entity entity) {
        if (entity == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return false;

        // 只在第一人称隐藏自己，第三人称仍保留自身火焰描边。
        if (!minecraft.options.getCameraType().isFirstPerson()) return false;

        // 捕获阶段有时拿到的对象引用不稳定，实体 ID 相同也视为本地玩家。
        return entity == player || entity.getId() == player.getId();
    }

    // 判断左右 Alt 是否按下。
    public static boolean isTeleportKeyDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }
}
