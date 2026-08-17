package com.z227.akatzumatool.network;

import com.z227.akatzumatool.common.ServerSkillCooldowns;
import com.z227.akatzumatool.common.ServerKeyChargeTracker;
import com.z227.akatzumatool.common.SkillCooldownType;
import com.z227.akatzumatool.item.TridentPlusItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// HeavenlyThunderCastC2SPacket 用于客户端按 V 请求服务端释放天雷法阵技能。
public class HeavenlyThunderCastC2SPacket {
    public HeavenlyThunderCastC2SPacket() {
    }

    public HeavenlyThunderCastC2SPacket(FriendlyByteBuf buffer) {
    }

    // 空包编码，释放条件全部交给服务端校验。
    public void encode(FriendlyByteBuf buffer) {
    }

    // 服务端处理天雷战戟天雷技能释放请求。
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            int remainingCooldownTicks = ServerSkillCooldowns.getRemainingTicks(player, SkillCooldownType.HEAVENLY_THUNDER);
            if (remainingCooldownTicks > 0) {
                ServerKeyChargeTracker.stopHeavenlyThunder(player);
                NetworkRegister.sendToPlayer(new HeavenlyThunderCastResultS2CPacket(false, remainingCooldownTicks, false), player);
                return;
            }
            if (!TridentPlusItem.hasEnoughHeavenlyThunderFood(player)) {
                ServerKeyChargeTracker.stopHeavenlyThunder(player);
                NetworkRegister.sendToPlayer(new HeavenlyThunderCastResultS2CPacket(false, 0, true), player);
                return;
            }
            if (!ServerKeyChargeTracker.canReleaseHeavenlyThunder(player)) {
                ServerKeyChargeTracker.stopHeavenlyThunder(player);
                NetworkRegister.sendToPlayer(new HeavenlyThunderCastResultS2CPacket(false, 0, false), player);
                return;
            }
            if (TridentPlusItem.trySpawnHeavenlyThunder(player)) {
                ServerKeyChargeTracker.stopHeavenlyThunder(player);
                ServerSkillCooldowns.setCooldown(player, SkillCooldownType.HEAVENLY_THUNDER);
                NetworkRegister.sendToPlayer(new HeavenlyThunderCastResultS2CPacket(true, SkillCooldownType.HEAVENLY_THUNDER.cooldownTicks(), false), player);
            } else {
                ServerKeyChargeTracker.stopHeavenlyThunder(player);
                NetworkRegister.sendToPlayer(new HeavenlyThunderCastResultS2CPacket(false, 0, !TridentPlusItem.hasEnoughHeavenlyThunderFood(player)), player);
            }
        });
        context.setPacketHandled(true);
    }
}
