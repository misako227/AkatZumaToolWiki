package com.z227.akatzumatool.network;

import com.z227.akatzumatool.effect.sparkling.SparklingFruitFlightState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// SparklingFlightInputC2SPacket 同步本地玩家闪闪果实疾跑飞行键的按下和松开边沿。
public class SparklingFlightInputC2SPacket {
    public final boolean boosting; // 客户端当前是否请求持续加速。

    // 创建空的停止加速包。
    public SparklingFlightInputC2SPacket() {
        this(false);
    }

    // 根据按键状态创建飞行输入包。
    public SparklingFlightInputC2SPacket(boolean boosting) {
        this.boosting = boosting;
    }

    // 从网络缓冲区读取按键状态。
    public SparklingFlightInputC2SPacket(FriendlyByteBuf buffer) {
        this.boosting = buffer.readBoolean();
    }

    // 把按键状态写入网络缓冲区。
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(boosting);
    }

    // 服务端只接受开关意图，速度、计时和视线方向全部由服务端重新计算。
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        boolean requestedBoosting = boosting;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (requestedBoosting) {
                SparklingFruitFlightState.startBoost(player);
                return;
            }
            SparklingFruitFlightState.stopBoost(player);
        });
        context.setPacketHandled(true);
    }
}
