package com.z227.akatzumatool.entity.coin;

import com.z227.akatzumatool.common.BlockUtil;
import com.z227.akatzumatool.common.EntityUtil;
import com.z227.akatzumatool.config.ConfigFile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

// ColorfulCoin 的强光束实体，负责同步渲染数据、造成高伤害并沿途破坏方块。
public class ColorfulCoinEntity extends Entity {
    // 网络同步字段：光束起点、终点、年龄和伤害。
    private static final EntityDataAccessor<Float> ORIGIN_X = SynchedEntityData.defineId(ColorfulCoinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORIGIN_Y = SynchedEntityData.defineId(ColorfulCoinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORIGIN_Z = SynchedEntityData.defineId(ColorfulCoinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ENDPOINT_X = SynchedEntityData.defineId(ColorfulCoinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ENDPOINT_Y = SynchedEntityData.defineId(ColorfulCoinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ENDPOINT_Z = SynchedEntityData.defineId(ColorfulCoinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(ColorfulCoinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(ColorfulCoinEntity.class, EntityDataSerializers.FLOAT);

    // 强光束生命周期和沿途方块破坏半径。
    public static final int LIFETIME = 20;
    private static final double BLOCK_BREAK_RADIUS = 2.0;

    // 服务端本地字段：记录发射者和防止重复结算。
    @Nullable
    private UUID ownerUUID;
    Entity master = null;
    private boolean effectApplied = false;
    private boolean useOwnerEyeHitOrigin = true;
    private boolean breakBlocksEnabled = true;

    public ColorfulCoinEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ORIGIN_X, 0f);
        this.entityData.define(ORIGIN_Y, 0f);
        this.entityData.define(ORIGIN_Z, 0f);
        this.entityData.define(ENDPOINT_X, 0f);
        this.entityData.define(ENDPOINT_Y, 0f);
        this.entityData.define(ENDPOINT_Z, 0f);
        this.entityData.define(AGE, 0);
        this.entityData.define(DAMAGE, 0f);
    }

    // 设置强光束数据，服务端生成实体时调用。
    public void setBeamData(Vec3 origin, Vec3 endpoint, @Nullable Entity master, float damage ) {

        this.entityData.set(ORIGIN_X, (float) origin.x);
        this.entityData.set(ORIGIN_Y, (float) origin.y);
        this.entityData.set(ORIGIN_Z, (float) origin.z);
        this.entityData.set(ENDPOINT_X, (float) endpoint.x);
        this.entityData.set(ENDPOINT_Y, (float) endpoint.y);
        this.entityData.set(ENDPOINT_Z, (float) endpoint.z);
        this.entityData.set(DAMAGE, damage);
        this.ownerUUID = master.getUUID();
        this.master = master;
        Vec3 spawnPosition = getSpawnPosition(origin, endpoint);
        this.setPos(spawnPosition);
    }

    // 设置伤害检测是否使用玩家眼位修正，环绕光束需要使用真实光束段。
    public void setUseOwnerEyeHitOrigin(boolean useOwnerEyeHitOrigin) {
        this.useOwnerEyeHitOrigin = useOwnerEyeHitOrigin;
    }

    // 允许女仆任务禁用方块破坏。
    public void setBreakBlocksEnabled(boolean enabled) {
        this.breakBlocksEnabled = enabled;
    }

    // 允许覆盖 ownerUUID，用于女仆发射时归属玩家伤害。
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
    }

    // 获取客户端渲染用起点。
    public Vec3 getOrigin(float partialTick) {
        return new Vec3(
                this.entityData.get(ORIGIN_X),
                this.entityData.get(ORIGIN_Y),
                this.entityData.get(ORIGIN_Z)
        );
    }

    // 获取客户端渲染用终点。
    public Vec3 getEndpoint(float partialTick) {
        return new Vec3(
                this.entityData.get(ENDPOINT_X),
                this.entityData.get(ENDPOINT_Y),
                this.entityData.get(ENDPOINT_Z)
        );
    }

    private static Vec3 getSpawnPosition(Vec3 origin, Vec3 endpoint) {
        Vec3 direction = endpoint.subtract(origin);
        if (direction.lengthSqr() < 1.0E-6) {
            return origin;
        }
        return origin.add(direction.normalize().scale(0.5));
    }

    public int getAge() {
        return this.entityData.get(AGE);
    }

    public int getLifetime() {
        return LIFETIME;
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    @Override
    public void tick() {
        this.entityData.set(AGE, this.entityData.get(AGE) + 1);
        int age = this.entityData.get(AGE);

        if (!this.level().isClientSide()) {
            if (age == 1 && !effectApplied) {
                applyBeamEffects();
                effectApplied = true;
            }
            if (age > LIFETIME) {
                this.discard();
            }
        }
    }

    // 服务端执行强光束的实体伤害和方块破坏。
    private void applyBeamEffects() {
        damageEntities();
        if (breakBlocksEnabled && ConfigFile.canBreakBlock() && this.level() instanceof ServerLevel serverLevel) {
            breakBlocks(serverLevel);
        }
    }

    // 对路径附近实体造成更高穿透伤害。
    private void damageEntities() {
        Vec3 origin = getOrigin(0);
        Vec3 endpoint = getEndpoint(0);
        float damage = getDamage();
        Player owner = ownerUUID == null ? null : this.level().getPlayerByUUID(ownerUUID);
        Vec3 hitOrigin = useOwnerEyeHitOrigin ? getHitOrigin(origin, endpoint, owner) : origin;



        AABB pathBox = new AABB(hitOrigin, endpoint).inflate(2.5);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class, pathBox, e -> {
                    if (ownerUUID != null && e.getUUID().equals(ownerUUID)) return false;
                    return e.isAlive();
                }
        );

        for (LivingEntity target : targets) {
            if (rayIntersectsEntity(hitOrigin, endpoint, target)) {
                if (EntityUtil.isInDamageWhitelist(target.getType())) continue;

                if (owner != null) {
                    target.hurt(owner.level().damageSources().playerAttack(owner), damage);
                } else {
                    target.hurt(this.level().damageSources().magic(), damage);
                }
            }
        }
    }

    // 伤害检测使用玩家眼位瞄准线，避免手部光束起点在近距离漏掉正前方实体。
    private Vec3 getHitOrigin(Vec3 origin, Vec3 endpoint, @Nullable Player owner) {
        Vec3 hitOrigin = owner == null ? origin : owner.getEyePosition();
        Vec3 direction = endpoint.subtract(hitOrigin);
        if (direction.lengthSqr() < 1.0E-6) {
            direction = endpoint.subtract(origin);
        }
        if (direction.lengthSqr() < 1.0E-6) {
            return hitOrigin;
        }
        return hitOrigin.subtract(direction.normalize().scale(0.5));
    }

    // 按整条光束线段的胶囊体范围破坏方块，避免起点附近先耗尽破坏数量。
    private void breakBlocks(ServerLevel serverLevel) {
        Vec3 origin = getOrigin(0);
        Vec3 endpoint = getEndpoint(0);
        Vec3 segment = endpoint.subtract(origin);
        origin = origin.add(segment.normalize());

        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 0.0025) return;

        AABB breakBox = new AABB(origin, endpoint).inflate(BLOCK_BREAK_RADIUS);
        BlockPos min = BlockPos.containing(breakBox.minX, breakBox.minY, breakBox.minZ);
        BlockPos max = BlockPos.containing(breakBox.maxX, breakBox.maxY, breakBox.maxZ);
        double radiusSqr = BLOCK_BREAK_RADIUS * BLOCK_BREAK_RADIUS;

        // 破坏方块采样：遍历光束胶囊体外接盒，再按到线段的距离过滤。
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockPos immutablePos = pos.immutable();
            Vec3 center = Vec3.atCenterOf(immutablePos);
            if (distanceToSegmentSqr(center, origin, segment, lengthSqr) > radiusSqr) continue;
            if(master != null && !BlockUtil.isPlaceBlock(serverLevel, master, immutablePos)) continue;
            tryBreakBlock(serverLevel, immutablePos);
        }
    }

    // 计算方块中心到光束线段的最近距离平方，用于得到稳定的沿线破坏范围。
    private static double distanceToSegmentSqr(Vec3 point, Vec3 origin, Vec3 segment, double lengthSqr) {
        double t = point.subtract(origin).dot(segment) / lengthSqr;
        t = Math.max(0.0, Math.min(1.0, t));
        Vec3 closest = origin.add(segment.scale(t));
        return point.distanceToSqr(closest);
    }

    // 过滤空气和不可破坏方块后执行破坏。
    private boolean tryBreakBlock(ServerLevel serverLevel, BlockPos pos) {
        // 方块过滤：跳过空气和不可破坏方块，避免破坏基岩等特殊方块。
        BlockState state = serverLevel.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.getDestroySpeed(serverLevel, pos) < 0.0f) return false;
        return serverLevel.destroyBlock(pos, true, this);
    }

    // 使用膨胀后的实体包围盒检测粗光束命中。
    private boolean rayIntersectsEntity(Vec3 origin, Vec3 endpoint, LivingEntity entity) {
        AABB box = entity.getBoundingBox().inflate(2.0);
        return box.clip(origin, endpoint).isPresent();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
