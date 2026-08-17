package com.z227.akatzumatool.network;

import com.z227.akatzumatool.common.ServerSkillCooldowns;
import com.z227.akatzumatool.item.FlySwordItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SummonFlySwordC2SPacket {
    public SummonFlySwordC2SPacket() {
    }

    public SummonFlySwordC2SPacket(FriendlyByteBuf buffer) {
    }

    public void encode(FriendlyByteBuf buffer) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (ServerSkillCooldowns.isCoolingDown(player, ServerSkillCooldowns.SUMMON_FLY_SWORD)) return;
            if (FlySwordItem.trySummonFlySwords(player)) {
                ServerSkillCooldowns.setCooldown(
                        player,
                        ServerSkillCooldowns.SUMMON_FLY_SWORD,
                        ServerSkillCooldowns.serverCooldownTicks(ServerSkillCooldowns.SUMMON_FLY_SWORD_CLIENT_COOLDOWN_TICKS)
                );
            }
        });
        context.setPacketHandled(true);
    }
}
