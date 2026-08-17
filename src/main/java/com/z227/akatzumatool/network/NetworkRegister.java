package com.z227.akatzumatool.network;

import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkDirection;

// NetworkRegister 注册模组自定义网络消息。
public class NetworkRegister {
    private static final String PROTOCOL_VERSION = "1"; // 网络协议版本。
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AkatZumaTool.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0; // 下一个网络消息编号。

    // 注册所有网络消息。
    public static void init() {
        CHANNEL.messageBuilder(SwordAuraCastC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SwordAuraCastC2SPacket::encode)
                .decoder(SwordAuraCastC2SPacket::new)
                .consumerMainThread(SwordAuraCastC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(DimensionSlashCastC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DimensionSlashCastC2SPacket::encode)
                .decoder(DimensionSlashCastC2SPacket::new)
                .consumerMainThread(DimensionSlashCastC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(BattoSlashCastC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BattoSlashCastC2SPacket::encode)
                .decoder(BattoSlashCastC2SPacket::new)
                .consumerMainThread(BattoSlashCastC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SummonFlySwordC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SummonFlySwordC2SPacket::encode)
                .decoder(SummonFlySwordC2SPacket::new)
                .consumerMainThread(SummonFlySwordC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(HeavenlyThunderCastC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(HeavenlyThunderCastC2SPacket::encode)
                .decoder(HeavenlyThunderCastC2SPacket::new)
                .consumerMainThread(HeavenlyThunderCastC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(HeavenlyThunderChargeStartC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(HeavenlyThunderChargeStartC2SPacket::encode)
                .decoder(HeavenlyThunderChargeStartC2SPacket::new)
                .consumerMainThread(HeavenlyThunderChargeStartC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(HeavenlyThunderChargeStopC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(HeavenlyThunderChargeStopC2SPacket::encode)
                .decoder(HeavenlyThunderChargeStopC2SPacket::new)
                .consumerMainThread(HeavenlyThunderChargeStopC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(HeavenlyThunderChargeSyncS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HeavenlyThunderChargeSyncS2CPacket::encode)
                .decoder(HeavenlyThunderChargeSyncS2CPacket::new)
                .consumerMainThread(HeavenlyThunderChargeSyncS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(HeavenlyThunderCastResultS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HeavenlyThunderCastResultS2CPacket::encode)
                .decoder(HeavenlyThunderCastResultS2CPacket::new)
                .consumerMainThread(HeavenlyThunderCastResultS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(ExcaliburChargeStartC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ExcaliburChargeStartC2SPacket::encode)
                .decoder(ExcaliburChargeStartC2SPacket::new)
                .consumerMainThread(ExcaliburChargeStartC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(ExcaliburChargeStopC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ExcaliburChargeStopC2SPacket::encode)
                .decoder(ExcaliburChargeStopC2SPacket::new)
                .consumerMainThread(ExcaliburChargeStopC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(ExcaliburCastC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ExcaliburCastC2SPacket::encode)
                .decoder(ExcaliburCastC2SPacket::new)
                .consumerMainThread(ExcaliburCastC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(ExcaliburChargeSyncS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ExcaliburChargeSyncS2CPacket::encode)
                .decoder(ExcaliburChargeSyncS2CPacket::new)
                .consumerMainThread(ExcaliburChargeSyncS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(AutoTrackingShootC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AutoTrackingShootC2SPacket::encode)
                .decoder(AutoTrackingShootC2SPacket::new)
                .consumerMainThread(AutoTrackingShootC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SparklingBoostC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SparklingBoostC2SPacket::encode)
                .decoder(SparklingBoostC2SPacket::new)
                .consumerMainThread(SparklingBoostC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SparklingFlightInputC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SparklingFlightInputC2SPacket::encode)
                .decoder(SparklingFlightInputC2SPacket::new)
                .consumerMainThread(SparklingFlightInputC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SparklingFlightStateS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SparklingFlightStateS2CPacket::encode)
                .decoder(SparklingFlightStateS2CPacket::new)
                .consumerMainThread(SparklingFlightStateS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SparklingTeleportParticlesS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SparklingTeleportParticlesS2CPacket::encode)
                .decoder(SparklingTeleportParticlesS2CPacket::new)
                .consumerMainThread(SparklingTeleportParticlesS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SparklingFruitOutlineS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SparklingFruitOutlineS2CPacket::encode)
                .decoder(SparklingFruitOutlineS2CPacket::new)
                .consumerMainThread(SparklingFruitOutlineS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(WhitelistSyncS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WhitelistSyncS2CPacket::encode)
                .decoder(WhitelistSyncS2CPacket::new)
                .consumerMainThread(WhitelistSyncS2CPacket::handle)
                .add();
    }

    // 发送消息到服务端。
    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    // 发送消息到指定玩家。
    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    // 发送消息到所有在线玩家。
    public static void sendToAll(Object message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    // 发送消息到追踪实体的玩家和实体自身，适合实体渲染状态同步。
    public static void sendToTrackingEntityAndSelf(Object message, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }
}
