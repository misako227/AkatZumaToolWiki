package com.z227.akatzumatool.network;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.effect.sparkling.SparklingFruitFlightState;
import com.z227.akatzumatool.event.EffectRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

// SparklingBoostC2SPacket 表示客户端请求闪闪果实 Alt 朝向安全瞬移。
public class SparklingBoostC2SPacket {
    public static final double SEARCH_STEP = 0.5D; // 安全目标点回退搜索步长。
    public static final double MIN_TELEPORT_DISTANCE = 1.0D; // 允许尝试的最短瞬移距离。
    public static final Map<UUID, Long> NEXT_ALLOWED_TICK = new HashMap<>(); // 玩家下一次允许瞬移的游戏 tick。

    public SparklingBoostC2SPacket() {
    }

    public SparklingBoostC2SPacket(FriendlyByteBuf buffer) {
    }

    // 编码空包体。
    public void encode(FriendlyByteBuf buffer) {
    }

    // 服务端校验玩家仍有 Buff 后，按玩家朝向查找安全落点并瞬移。
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!player.hasEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get())) return;
            if (!canBoostNow(player)) return;

            tryTeleport(player);
        });
        context.setPacketHandled(true);
    }

    // 尝试执行闪闪果实安全瞬移，成功时播放瞬移音效并同步客户端 GPU 粒子。
    public static boolean tryTeleport(ServerPlayer player) {
        Vec3 origin = player.position();
        float height = player.getBbHeight();
        float width = player.getBbWidth();
        Vec3 target = findSafeTeleportTarget(player, ConfigFile.sparklingFruitTeleportDistance());
        if (target == null) return false;

        player.connection.teleport(target.x, target.y, target.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        player.level().playSound(null, target.x, target.y, target.z,
                AkatZumaTool.SPARKLING_2.get(), player.getSoundSource(), 1.0F, 1.0F);
        // Ctrl 加速会话继续计时，但刷新追踪客户端的历史起点，避免连接瞬移前后位置。
        SparklingFruitFlightState.resetBoostTrails(player);
        sendTeleportParticles(player, origin, target, height, width);
        return true;
    }

    // 向客户端同步瞬移残影 GPU 粒子。
    public static void sendTeleportParticles(ServerPlayer player, Vec3 origin, Vec3 target, float height, float width) {
        if (player == null || origin == null || target == null) return;
        NetworkRegister.sendToPlayer(new SparklingTeleportParticlesS2CPacket(origin, target, height, width), player);
    }

    // 从配置距离向近处回退搜索第一个安全瞬移目标。
    public static Vec3 findSafeTeleportTarget(ServerPlayer player, double distance) {
        if (player == null || distance < MIN_TELEPORT_DISTANCE) return null;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position();
        for (double currentDistance = distance; currentDistance >= MIN_TELEPORT_DISTANCE; currentDistance -= SEARCH_STEP) {
            Vec3 position = origin.add(look.scale(currentDistance));
            if (isSafeTeleportPosition(player, position)) {
                return position;
            }
        }
        return null;
    }

    // 判断目标点是否不会卡方块、进液体或进入危险方块。
    public static boolean isSafeTeleportPosition(ServerPlayer player, Vec3 position) {
        ServerLevel level = player.serverLevel();
        AABB targetBox = player.getBoundingBox().move(position.subtract(player.position()));
        if (!level.noCollision(player, targetBox)) return false;

        BlockPos feet = BlockPos.containing(position);
        BlockPos head = feet.above();
        return isSafeBodyBlock(level, feet) && isSafeBodyBlock(level, head);
    }

    // 判断玩家身体所在方块是否适合作为瞬移目标空间。
    public static boolean isSafeBodyBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!level.getFluidState(pos).isEmpty()) return false;
        return !isHazardBlock(state);
    }

    // 判断方块是否属于火焰、岩浆或接触伤害类危险方块。
    public static boolean isHazardBlock(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.POWDER_SNOW);
    }

    // 服务端短冷却，防止客户端绕过本地冷却刷包。
    public static boolean canBoostNow(ServerPlayer player) {
        long now = player.level().getGameTime();
        long nextAllowedTick = NEXT_ALLOWED_TICK.getOrDefault(player.getUUID(), 0L);
        if (now < nextAllowedTick) return false;
        int serverCooldown = Math.max(1, ConfigFile.sparklingFruitTeleportCooldownTicks() - 1);
        NEXT_ALLOWED_TICK.put(player.getUUID(), now + serverCooldown);
        return true;
    }

    // 玩家退出时清理服务端瞬移冷却记录。
    public static void clearCooldown(Player player) {
        if (player == null) return;
        NEXT_ALLOWED_TICK.remove(player.getUUID());
    }
}
