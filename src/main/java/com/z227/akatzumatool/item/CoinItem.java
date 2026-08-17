package com.z227.akatzumatool.item;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.entity.coin.CoinChargeTracker;
import com.z227.akatzumatool.entity.coin.RailgunBeamEntity;
import com.z227.akatzumatool.event.EntityTypeRegister;
import com.z227.akatzumatool.event.client.ChargeLightningClientRegistry;
import com.z227.akatzumatool.render.gpu.ParticleEmitTask;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * 电磁炮硬币物品
 * 右键按住蓄力 2-3 秒，松开发射可见光束
 * 光束穿透所有实体，每个实体仅造成一次伤害
 */
public class CoinItem extends Item {
    // Tooltip 和满蓄力粒子参数。
    private static final Component TOOLTIP_USE = Component.translatable("item.akatzumatool.coin.tooltip.1");
    private static final int[] FULL_CHARGE_PARTICLE_SHAPES = new int[] {
            ParticleEmitTask.SHAPE_CIRCLE,
            ParticleEmitTask.SHAPE_SQUARE,
            ParticleEmitTask.SHAPE_TRIANGLE,
            ParticleEmitTask.SHAPE_HEART,
            ParticleEmitTask.SHAPE_STAR
    };

    // 普通硬币配置字段。
    protected double maxRange = 50.0;
    protected float beamDamage = 20.0f;

    // 光束位置参数。
    private static final double BEAM_HAND_FORWARD_OFFSET = 0.35;
    private static final double BEAM_HAND_SIDE_OFFSET = 0.38;
    private static final double BEAM_HAND_DOWN_OFFSET = 0.32;
    private static final double CHARGE_EFFECT_LEFT_OFFSET = -0.15;

    public CoinItem(Properties pProperties) {
        super(pProperties);
    }

    // 在配置加载完成后刷新普通硬币字段。
    public void loadConfigValues() {
        this.maxRange = ConfigFile.coinMaxRange();
        this.beamDamage = ConfigFile.coinBeamDamage();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(TOOLTIP_USE);
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            CoinChargeTracker.startCharge(player, player.tickCount);
            ChargeLightningClientRegistry.startCoin(player, false);
            player.startUsingItem(context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 右键按下开始蓄力
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 客户端也记录蓄力状态，用于本地渲染蓄力闪电；服务端记录用于发射判定。
        CoinChargeTracker.startCharge(player, player.tickCount);
        ChargeLightningClientRegistry.startCoin(player, false);
        player.startUsingItem(hand);

        // 返回成功并开始使用物品（触发 useTick 和 releaseUsing）
        return InteractionResultHolder.consume(stack);
    }

    /**
     * 每 tick 更新蓄力进度
     */
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (livingEntity instanceof Player player) {
            // 客户端和服务端都更新进度：客户端负责特效，服务端负责最终发射。
            CoinChargeTracker.updateCharge(player, player.tickCount);

            // 蓄力完成时发射往手部位置集中的粒子
            if (level.isClientSide() && CoinChargeTracker.isFullyCharged(player)) {
                emitFullChargeParticles(player, false);
            }
        }
    }

    // 切换物品或停止使用时清理残留蓄力状态。
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        if (!CoinChargeTracker.isCharging(player)) return;
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof CoinItem) return;

        CoinChargeTracker.stopCharge(player);
        ChargeLightningClientRegistry.stop(player);
    }

    /**
     * 松开右键停止蓄力并发射
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (livingEntity instanceof Player player) {
            // 两端都停止蓄力；只有服务端真正生成光束实体。
            float chargeProgress = CoinChargeTracker.stopCharge(player);
            ChargeLightningClientRegistry.stop(player);

            // 检查是否达到最小发射阈值
            if (!level.isClientSide() && chargeProgress >= CoinChargeTracker.MIN_LAUNCH_THRESHOLD) {
                // 发射电磁炮光束
                launchBeam(level, player, chargeProgress);
                // 创造模式不扣除数量
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }
    }

    /**
     * 发射电磁炮光束
     * @param level 游戏世界
     * @param player 发射的玩家
     * @param chargeProgress 蓄力进度（影响伤害和射程）
     */
    private void launchBeam(Level level, Player player, float chargeProgress) {
        // 计算瞄准点（玩家视线方向）
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 origin = getBeamHandOrigin(player, eyePos, lookVec);

        // 计算光束终点（基于蓄力进度的射程）
        double range = maxRange * chargeProgress;
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        // 创建光束实体
        RailgunBeamEntity beamEntity = new RailgunBeamEntity(EntityTypeRegister.RAILGUN_BEAM_ENTITY.get(), level);
        beamEntity.setBeamData(origin, endPos, player.getUUID(), beamDamage * chargeProgress);

        // 生成实体
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(beamEntity);
        }

        // 蓄力发射音效预留，音频文件后续放入 sounds.json 指定位置即可。

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                AkatZumaTool.COIN_CHARGE_COMPLETE.get(), SoundSource.PLAYERS, 1.8f, 1.0f);


        // 设置物品冷却（防止连续发射）
        player.getCooldowns().addCooldown(this, 10); // 1秒冷却
    }

    // 计算硬币光束的手部发射点，供实际光束和客户端闪电共用。
    public static Vec3 getBeamHandOrigin(Player player, Vec3 eyePos, Vec3 lookVec) {
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = up.cross(lookVec);
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }

        double side = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? -BEAM_HAND_SIDE_OFFSET : BEAM_HAND_SIDE_OFFSET;
        return eyePos
                .add(lookVec.scale(BEAM_HAND_FORWARD_OFFSET))
                .add(right.scale(side))
                .add(0.0, -BEAM_HAND_DOWN_OFFSET, 0.0)
                .add(0,0.2,0);
    }

    // 计算蓄力闪电和满蓄力粒子的手部特效点，比光束起点额外向视角左侧偏移。
    public static Vec3 getChargeEffectHandOrigin(Player player, Vec3 eyePos, Vec3 lookVec) {
        return getBeamHandOrigin(player, eyePos, lookVec)
                .add(getViewLeft(lookVec).scale(CHARGE_EFFECT_LEFT_OFFSET));
    }

    private static Vec3 getViewLeft(Vec3 lookVec) {
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = up.cross(lookVec);
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        return right.scale(-1.0);
    }

    // 生成满蓄力后向手部汇聚的 GPU 粒子。
    public static void emitFullChargeParticles(Player player, boolean colorful) {
        if (AkatZumaTool.POST == null || player.tickCount % 2 != 0) return;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 handPos = getChargeEffectHandOrigin(player, eyePos, lookVec);
        Random random = new Random(player.getUUID().getLeastSignificantBits() ^ (long) player.tickCount * 918271L);

        int rgb = colorful ? 0x864e74 : 0xA5D8FF;
        int endRgb = colorful ? 0xAF5579 : 0xA5D8Fe;
        for (int i = 0; i < 18; i++) {
            Vec3 start = handPos.add(randomSphereOffset(random, 1.5));
            Vec3 direction = handPos.subtract(start);
            if (direction.lengthSqr() < 1.0E-6) continue;

            float size = 0.02f + random.nextFloat() * 0.015f;
            float rotation = random.nextFloat() * 360.0f;
            AkatZumaTool.POST.addParticle(new ParticleEmitTask()
                    .position(start)
                    .direction((float) direction.x, (float) direction.y, (float) direction.z)
                    .speed(0.1f + random.nextFloat() * 1.1f)
                    .spread(0.06f)
                    .life(1.24f + random.nextFloat() * 0.16f)
                    .gravity(0.0f)
                    .size(size, size, rotation)
                    .color(rgb, 0.9f)
                    .endColor(endRgb, 0.8f)
                    .shape(randomFullChargeParticleShape(random))
                    .motion(ParticleEmitTask.MOTION_BALLISTIC)
                    .rate(0)
                    .duration(0.0f)
                    .burst(10));
        }
    }

    private static int randomFullChargeParticleShape(Random random) {
        return FULL_CHARGE_PARTICLE_SHAPES[random.nextInt(FULL_CHARGE_PARTICLE_SHAPES.length)];
    }

    private static Vec3 randomSphereOffset(Random random, double radius) {
        double x = (random.nextDouble() * 2.0 - 1.0) * radius;
        double y = (random.nextDouble() * 2.0 - 1.0) * radius;
        double z = (random.nextDouble() * 2.0 - 1.0) * radius;
        return new Vec3(x, y, z);
    }

    /**
     * 获取物品使用持续时间（tick）
     * 设置为较大的值，允许长时间蓄力
     */
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // 1小时（实际会由 releaseUsing 提前结束）
    }

    /**
     * 返回使用动画（拉弓动画）
     */
    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.BOW;
    }
}
