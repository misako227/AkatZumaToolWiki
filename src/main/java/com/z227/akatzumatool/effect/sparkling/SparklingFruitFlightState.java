package com.z227.akatzumatool.effect.sparkling;

import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.event.EffectRegister;
import com.z227.akatzumatool.network.NetworkRegister;
import com.z227.akatzumatool.network.SparklingFlightStateS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// SparklingFruitFlightState 管理闪闪果实临时飞行能力、Ctrl 加速会话和最大速度横飞姿态。
public class SparklingFruitFlightState {
    public static final String SNAPSHOT_PRESENT_TAG = "akatzumatoolSparklingFlightSnapshot"; // 玩家存档中是否存在原始飞行能力快照。
    public static final String SNAPSHOT_MAYFLY_TAG = "akatzumatoolSparklingMayfly"; // 玩家存档中的原始 mayfly。
    public static final String SNAPSHOT_FLYING_TAG = "akatzumatoolSparklingFlying"; // 玩家存档中的原始 flying。
    public static final String SNAPSHOT_SPEED_TAG = "akatzumatoolSparklingFlyingSpeed"; // 玩家存档中的原始 flyingSpeed。
    public static final Map<UUID, FlightSnapshot> SNAPSHOTS = new HashMap<>(); // 玩家 UUID 到进入 Buff 前飞行能力的映射。
    public static final Map<UUID, BoostSession> BOOST_SESSIONS = new HashMap<>(); // 玩家 UUID 到当前 Ctrl 加速会话的映射。

    // Buff 有效期间保存原始能力并持续维持飞行状态。
    public static void maintainFlight(Player player) {
        if (player == null || player.level().isClientSide()) return;
        Abilities abilities = player.getAbilities();
        if (!SNAPSHOTS.containsKey(player.getUUID())) {
            FlightSnapshot snapshot = readPersistedSnapshot(player);
            if (snapshot == null) {
                snapshot = new FlightSnapshot(abilities.mayfly, abilities.flying, abilities.getFlyingSpeed());
                writePersistedSnapshot(player, snapshot);
            }
            SNAPSHOTS.put(player.getUUID(), snapshot);
        }

        // 只在能力实际变化时同步，避免每 tick 重复发送能力包。
        if (!abilities.mayfly || !abilities.flying) {
            abilities.mayfly = true;
            abilities.flying = true;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
    }

    // 服务端接受一次新的 Ctrl 按下，并从当前前向速度重新开始计算加速。
    public static boolean startBoost(ServerPlayer player) {
        if (!canBoost(player)) {
            sendBoostStateToPlayer(player, false);
            return false;
        }
        if (BOOST_SESSIONS.containsKey(player.getUUID())) return true;

        Vec3 look = getSafeLookDirection(player);
        double maxSpeed = ConfigFile.sparklingFruitFlightBoostMaxSpeed();
        double startSpeed = Math.min(maxSpeed, Math.max(0.0D, player.getDeltaMovement().dot(look)));
        BOOST_SESSIONS.put(player.getUUID(), new BoostSession(player.level().getGameTime(), startSpeed));
        syncBoostState(player, true, false);
        return true;
    }

    // 服务端结束 Ctrl 加速并关闭仅由本功能开启的强制横飞姿态。
    public static void stopBoost(Player player) {
        if (player == null) return;
        BoostSession session = BOOST_SESSIONS.remove(player.getUUID());
        if (session == null) return;

        clearHorizontalPose(player, session);
        if (!player.level().isClientSide()) {
            syncBoostState(player, false, false);
        }
    }

    // 服务端每 tick 推进当前玩家的加速速度和最大速度姿态。
    public static void tickBoost(Player player) {
        if (player == null || player.level().isClientSide()) return;
        BoostSession session = BOOST_SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (!canBoost(player)) {
            stopBoost(player);
            return;
        }

        long elapsedTicks = Math.max(0L, player.level().getGameTime() - session.startTick);
        int accelerationTicks = Math.max(1, ConfigFile.sparklingFruitFlightBoostAccelerationTicks());
        double progress = Math.min(1.0D, (double) elapsedTicks / (double) accelerationTicks);
        double maxSpeed = ConfigFile.sparklingFruitFlightBoostMaxSpeed();
        double speed = Math.min(maxSpeed, session.startSpeed + (maxSpeed - session.startSpeed) * progress);
        Vec3 velocity = getSafeLookDirection(player).scale(speed);

        // 服务端每 tick 覆盖成受上限约束的视线方向速度，禁止增量叠加突破最大速度。
        player.setDeltaMovement(velocity);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        updateMaxSpeedPose(player, session, progress >= 1.0D);
    }

    // 达到最大速度时设置 Forge 强制横飞姿态，避免无鞘翅时被原版校验清除。
    public static void updateMaxSpeedPose(Player player, BoostSession session, boolean atMaxSpeed) {
        if (session.maxSpeedReached == atMaxSpeed) return;
        session.maxSpeedReached = atMaxSpeed;
        if (atMaxSpeed) {
            Pose forcedPose = player.getForcedPose();
            if (forcedPose == null) {
                player.setForcedPose(Pose.FALL_FLYING);
                session.sparklingPoseApplied = true;
                session.horizontalPoseActive = true;
            } else if (forcedPose == Pose.FALL_FLYING) {
                session.horizontalPoseActive = true;
            }
            syncBoostState(player, true, session.horizontalPoseActive);
            return;
        }

        clearHorizontalPose(player, session);
        syncBoostState(player, true, false);
    }

    // 只清理由闪闪果实实际设置的强制姿态，不覆盖其他模组的姿态状态。
    public static void clearHorizontalPose(Player player, BoostSession session) {
        if (player == null || session == null) return;
        if (session.sparklingPoseApplied && player.getForcedPose() == Pose.FALL_FLYING) {
            player.setForcedPose(null);
        }
        session.horizontalPoseActive = false;
        session.sparklingPoseApplied = false;
    }

    // Buff 失效时恢复进入 Buff 前的能力，并清理加速会话。
    public static void restorePlayer(Player player) {
        if (player == null || player.level().isClientSide()) return;
        stopBoost(player);
        FlightSnapshot snapshot = SNAPSHOTS.remove(player.getUUID());
        if (snapshot == null) {
            snapshot = readPersistedSnapshot(player);
        }
        if (snapshot == null) return;

        Abilities abilities = player.getAbilities();
        // 当前已经切换到创造或旁观模式时，保留游戏模式授予的飞行权限。
        if (!player.isCreative() && !player.isSpectator()) {
            abilities.mayfly = snapshot.mayfly;
            abilities.flying = snapshot.mayfly && snapshot.flying;
            abilities.setFlyingSpeed(snapshot.flyingSpeed);
        }
        clearPersistedSnapshot(player);
        player.fallDistance = 0.0F;
        player.onUpdateAbilities();
    }

    // 玩家死亡克隆时把旧玩家对应的原始能力恢复到新玩家对象。
    public static void restoreClonedPlayer(Player original, Player replacement) {
        if (original == null || replacement == null || replacement.level().isClientSide()) return;
        stopBoost(original);
        FlightSnapshot snapshot = SNAPSHOTS.remove(original.getUUID());
        if (snapshot == null) {
            snapshot = readPersistedSnapshot(original);
        }
        if (snapshot == null) return;

        Abilities abilities = replacement.getAbilities();
        if (!replacement.isCreative() && !replacement.isSpectator()) {
            abilities.mayfly = snapshot.mayfly;
            abilities.flying = snapshot.mayfly && snapshot.flying;
            abilities.setFlyingSpeed(snapshot.flyingSpeed);
        }
        clearPersistedSnapshot(original);
        clearPersistedSnapshot(replacement);
        replacement.fallDistance = 0.0F;
        replacement.onUpdateAbilities();
    }

    // 切换维度时保留原始能力快照，但结束旧维度中的加速会话。
    public static void stopBoostForDimensionChange(Player player) {
        stopBoost(player);
    }

    // 判断玩家当前是否满足服务端 Ctrl 加速条件。
    public static boolean canBoost(Player player) {
        return player != null
                && player.isAlive()
                && !player.isInWaterOrBubble()
                && player.hasEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get());
    }

    // 判断玩家服务端是否存在有效的 Ctrl 加速会话。
    public static boolean isBoosting(Player player) {
        return player != null && BOOST_SESSIONS.containsKey(player.getUUID());
    }

    // 向当前追踪玩家和玩家自己同步加速开关及横向姿态。
    public static void syncBoostState(Player player, boolean active, boolean horizontalPose) {
        if (player == null || player.level().isClientSide()) return;
        NetworkRegister.sendToTrackingEntityAndSelf(new SparklingFlightStateS2CPacket(
                player.getId(), active, horizontalPose, ConfigFile.sparklingFruitFlightBoostMaxSpeed()), player);
    }

    // 玩家开始追踪时补发目标当前的加速状态。
    public static void sendBoostStateToPlayer(Player target, ServerPlayer receiver) {
        if (target == null || receiver == null) return;
        BoostSession session = BOOST_SESSIONS.get(target.getUUID());
        boolean active = session != null;
        boolean horizontalPose = session != null && session.horizontalPoseActive;
        NetworkRegister.sendToPlayer(new SparklingFlightStateS2CPacket(
                target.getId(), active, horizontalPose, ConfigFile.sparklingFruitFlightBoostMaxSpeed()), receiver);
    }

    // 只向玩家本人发送服务端拒绝后的关闭状态。
    public static void sendBoostStateToPlayer(ServerPlayer player, boolean active) {
        if (player == null) return;
        NetworkRegister.sendToPlayer(new SparklingFlightStateS2CPacket(
                player.getId(), active, false, ConfigFile.sparklingFruitFlightBoostMaxSpeed()), player);
    }

    // Alt 瞬移后保持服务端加速会话，但用关闭和开启状态让所有客户端重建轨迹起点。
    public static void resetBoostTrails(Player player) {
        BoostSession session = player == null ? null : BOOST_SESSIONS.get(player.getUUID());
        if (session == null) return;
        syncBoostState(player, false, false);
        syncBoostState(player, true, session.horizontalPoseActive);
    }

    // 返回始终可归一化的玩家当前视线方向。
    public static Vec3 getSafeLookDirection(Player player) {
        Vec3 look = player == null ? Vec3.ZERO : player.getLookAngle();
        return look.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
    }

    // 从玩家 Forge 持久数据读取跨重启保留的原始飞行能力。
    public static FlightSnapshot readPersistedSnapshot(Player player) {
        if (player == null) return null;
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(SNAPSHOT_PRESENT_TAG)) return null;
        return new FlightSnapshot(
                data.getBoolean(SNAPSHOT_MAYFLY_TAG),
                data.getBoolean(SNAPSHOT_FLYING_TAG),
                data.getFloat(SNAPSHOT_SPEED_TAG));
    }

    // 首次获得 Buff 时把原始飞行能力写入玩家存档，避免服务器重启丢失快照。
    public static void writePersistedSnapshot(Player player, FlightSnapshot snapshot) {
        if (player == null || snapshot == null) return;
        CompoundTag data = player.getPersistentData();
        data.putBoolean(SNAPSHOT_PRESENT_TAG, true);
        data.putBoolean(SNAPSHOT_MAYFLY_TAG, snapshot.mayfly);
        data.putBoolean(SNAPSHOT_FLYING_TAG, snapshot.flying);
        data.putFloat(SNAPSHOT_SPEED_TAG, snapshot.flyingSpeed);
    }

    // 能力恢复后删除持久快照，下一次食用重新记录当时状态。
    public static void clearPersistedSnapshot(Player player) {
        if (player == null) return;
        CompoundTag data = player.getPersistentData();
        data.remove(SNAPSHOT_PRESENT_TAG);
        data.remove(SNAPSHOT_MAYFLY_TAG);
        data.remove(SNAPSHOT_FLYING_TAG);
        data.remove(SNAPSHOT_SPEED_TAG);
    }

    // FlightSnapshot 保存进入闪闪果实 Buff 前的原版飞行能力。
    public static class FlightSnapshot {
        public final boolean mayfly; // 进入 Buff 前是否允许飞行。
        public final boolean flying; // 进入 Buff 前是否正在飞行。
        public final float flyingSpeed; // 进入 Buff 前的原版能力飞行速度。

        // 创建不可变飞行能力快照。
        public FlightSnapshot(boolean mayfly, boolean flying, float flyingSpeed) {
            this.mayfly = mayfly;
            this.flying = flying;
            this.flyingSpeed = flyingSpeed;
        }
    }

    // BoostSession 保存一次持续按住 Ctrl 的服务端加速计时。
    public static class BoostSession {
        public final long startTick; // 本次 Ctrl 按下的服务端游戏 tick。
        public final double startSpeed; // 本次按下时的初始前向速度。
        public boolean maxSpeedReached; // 本次会话是否已到达配置最大速度。
        public boolean horizontalPoseActive; // 当前是否对客户端声明最大速度横飞姿态。
        public boolean sparklingPoseApplied; // 服务端强制姿态是否确实由闪闪果实设置。

        // 创建新的服务端加速会话。
        public BoostSession(long startTick, double startSpeed) {
            this.startTick = startTick;
            this.startSpeed = startSpeed;
        }
    }
}
