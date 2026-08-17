package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.CameraShakeUtil;
import com.z227.akatzumatool.entity.coin.CoinChargeTracker;
import com.z227.akatzumatool.event.client.AutoTrackingClientHandler;
import com.z227.akatzumatool.event.client.ChargeLightningClientRegistry;
import com.z227.akatzumatool.event.client.SparklingFruitClientHandler;
import com.z227.akatzumatool.item.BeamCrossTestItem;
import com.z227.akatzumatool.item.CoinItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


// RenderLevelEvent 负责把本帧需要的自定义效果提交到后处理队列。
@OnlyIn(value = Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RenderLevelEvent {

    // 渲染阶段入口：实体后收集效果，世界后提交锁定描边并执行后处理。
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (AkatZumaTool.POST == null || Minecraft.getInstance().level == null) return;

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            AkatZumaTool.POST.flushFlySwordTrailPose(event.getPoseStack());
            queueChargingLightning(event);
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            AkatZumaTool.POST.setPartialTick(event.getPartialTick(), event.getCamera());
            AutoTrackingClientHandler.submitLockedTargetOutline(Minecraft.getInstance().level);
            SparklingFruitClientHandler.submitSparklingFruitFireOutlines(Minecraft.getInstance().level, event.getFrustum());
            AkatZumaTool.POST.doPostProcessing();
        }
    }

    // 应用通用相机抖动，次元斩终结阶段会向工具类提交抖动任务。
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraShakeUtil.apply(event);
    }


    // 收集蓄力阶段的手部闪电，使用模式一短路径闪电。
    public static void queueChargingLightning(RenderLevelStageEvent event) {
        float partialTick = event.getPartialTick();
        Level level = Minecraft.getInstance().level;

        ChargeLightningClientRegistry.cleanup(level);
        for (ChargeLightningClientRegistry.ChargeVisualState state : ChargeLightningClientRegistry.activeCharges()) {
            Player player = level.getPlayerByUUID(state.playerId());
            if (player == null || !player.isAlive()) {
                ChargeLightningClientRegistry.stop(state.playerId());
                continue;
            }

            // 玩家停止右键后立即移除视觉状态，避免继续提交手部闪电。
            if (!player.isUsingItem()) {
                if (state.type() != ChargeLightningClientRegistry.ChargeVisualType.BEAM_CROSS) {
                    CoinChargeTracker.stopCharge(player);
                }
                ChargeLightningClientRegistry.stop(player);
                continue;
            }

            if (state.type() == ChargeLightningClientRegistry.ChargeVisualType.BEAM_CROSS) {
                if (!(player.getUseItem().getItem() instanceof BeamCrossTestItem)) {
                    ChargeLightningClientRegistry.stop(player);
                    continue;
                }
                BeamCrossTestItem.renderChargeEffects(player, partialTick);
                continue;
            }

            if (!CoinChargeTracker.isCharging(player)) {
                ChargeLightningClientRegistry.stop(player);
                continue;
            }
            if (!(player.getUseItem().getItem() instanceof CoinItem)) {
                CoinChargeTracker.stopCharge(player);
                ChargeLightningClientRegistry.stop(player);
                continue;
            }

            float progress = Mth.clamp(CoinChargeTracker.getProgress(player), 0.0f, 1.0f);
            AkatZumaTool.POST.effects().addChargingLightning(player, progress, partialTick, state.colorful());
        }
    }
}
