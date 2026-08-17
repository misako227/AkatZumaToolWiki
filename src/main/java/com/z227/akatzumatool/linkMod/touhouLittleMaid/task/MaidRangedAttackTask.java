package com.z227.akatzumatool.linkMod.touhouLittleMaid.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidAttackStrafingAnyItemTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidShootTargetAnyItemTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.config.MagicBowConfig;
import com.z227.akatzumatool.entity.bow.MagicArrowEntity;
import com.z227.akatzumatool.entity.bow.MagicBowParticleEffectEntity;
import com.z227.akatzumatool.entity.coin.ColorfulCoinEntity;
import com.z227.akatzumatool.entity.coin.RailgunBeamEntity;
import com.z227.akatzumatool.entity.trident.TridentPlusEntity;
import com.z227.akatzumatool.event.EnchantmentRegister;
import com.z227.akatzumatool.event.EntityTypeRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

// 女仆远程攻击任务，同时支持普通硬币、彩色硬币、魔法弓和天雷战戟。
// 战斗开始时自动从背包装备可用远程武器到主手。
public class MaidRangedAttackTask implements IRangedAttackTask {

    // 任务 UID 和行为参数。
    public static final ResourceLocation UID = new ResourceLocation(AkatZumaTool.MODID, "ranged_attack");
    private static final float ATTACK_RADIUS = 50.0f;
    private static final float WALK_SPEED = 0.6f;
    private static final int SHOOT_COOLDOWN = 2;
    private static final int CHARGE_DURATION = 40;
    private static final float PROJECTILE_RANGE = 50.0f;

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return AkatZumaTool.COIN_ITEM.get().getDefaultInstance();
    }

    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }

    // 行为链：装备远程武器 → 寻敌 → 停止检测 → 走向目标 → 横向走位 → 射击。
    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> equipTask = new EquipRangedWeaponBehavior();
        BehaviorControl<EntityMaid> supplementedTask = StartAttacking.create(
                MaidRangedAttackTask::hasRangedWeapon,
                IRangedAttackTask::findFirstValidAttackTarget
        );
        BehaviorControl<EntityMaid> findTargetTask = StopAttackingIfTargetInvalid.create(
                target -> !hasRangedWeapon(maid) || distanceTooFar(target, maid)
        );
        BehaviorControl<EntityMaid> moveToTargetTask = MaidRangedWalkToTarget.create(WALK_SPEED);
        BehaviorControl<EntityMaid> strafingTask = new MaidAttackStrafingAnyItemTask(
                stack -> isWeapon(maid, stack), PROJECTILE_RANGE, WALK_SPEED);
        BehaviorControl<EntityMaid> shootTask = new MaidShootTargetAnyItemTask(SHOOT_COOLDOWN, CHARGE_DURATION,
                stack -> isWeapon(maid, stack));

        return Lists.newArrayList(
                Pair.of(4, equipTask),
                Pair.of(5, supplementedTask),
                Pair.of(5, findTargetTask),
                Pair.of(5, moveToTargetTask),
                Pair.of(5, strafingTask),
                Pair.of(5, shootTask)
        );
    }

    // 战斗时禁用慌乱 AI。
    @Override
    public boolean enablePanic(EntityMaid maid) {
        return false;
    }

    // 任务描述文本 key（对应语言文件 task.akatzumatool.ranged_attack.desc）。
    @Override
    public List<String> getDescription(EntityMaid maid) {
        return List.of("task." + AkatZumaTool.MODID + ".ranged_attack.desc");
    }

    // 条件描述：显示硬币数量要求及当前是否满足。
    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return List.of(Pair.of("0", this::isEnable));
    }

    // 检查女仆背包或主手是否有可用远程武器。
    @Override
    public boolean isEnable(EntityMaid maid) {
        return hasRangedWeapon(maid);
    }

    // 硬币、彩币和魔法弓都被视为本任务武器。
    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return isRangedWeaponStack(stack);
    }

    // 执行远程射击：天雷战戟走无限投掷分支，魔法弓走魔法箭分支，硬币和彩币保留原光束分支。
    @Override
    public void performRangedAttack(EntityMaid shooter, LivingEntity target, float distanceFactor) {
        ItemStack hand = shooter.getMainHandItem();
        if (isTridentPlusStack(hand)) {
            shootTridentPlus(shooter, target, hand);
            return;
        }
        if (isMagicBowStack(hand)) {
            shootMagicBow(shooter, target, hand);
            return;
        }
        if (!isCoinStack(hand)) return;

        boolean colorful = hand.getItem() == AkatZumaTool.COLORFUL_COIN.get();
        double maxRange = colorful ? ConfigFile.colorfulCoinMaxRange() : ConfigFile.coinMaxRange();
        float damage = colorful ? ConfigFile.colorfulCoinBeamDamage() : ConfigFile.coinBeamDamage();

        // 计算光束参数。
        Vec3 origin = shooter.getEyePosition();
        Vec3 direction = target.position().subtract(origin);
        Vec3 endpoint = origin.add(direction.normalize().scale(Math.max(direction.length(), maxRange)));

        // 消费弹药。
        consumeAmmo(shooter, hand, colorful);

        if (colorful) {
            ColorfulCoinEntity beam = new ColorfulCoinEntity(EntityTypeRegister.COLORFUL_COIN_ENTITY.get(), shooter.level());
            beam.setBeamData(origin, endpoint, shooter, damage);
            beam.setBreakBlocksEnabled(false);
            beam.setUseOwnerEyeHitOrigin(false);
            UUID ownerId = shooter.getOwnerUUID();
            if (ownerId != null) {
                beam.setOwnerUUID(ownerId);
            }
            if (shooter.level() instanceof ServerLevel serverLevel) {
                serverLevel.addFreshEntity(beam);
            }
        } else {
            RailgunBeamEntity beam = new RailgunBeamEntity(EntityTypeRegister.RAILGUN_BEAM_ENTITY.get(), shooter.level());
            UUID ownerId = shooter.getOwnerUUID();
            beam.setBeamData(origin, endpoint, ownerId, damage);
            if (shooter.level() instanceof ServerLevel serverLevel) {
                serverLevel.addFreshEntity(beam);
            }
        }

        // 播放硬币发射音效。
        shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                AkatZumaTool.COIN_CHARGE_COMPLETE.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.5f, 1.0f);
    }

    // 女仆无限投掷天雷战戟，不消耗、不扣耐久、不清空主手，落雷逻辑继续由 TridentPlusEntity 处理。
    public static void shootTridentPlus(EntityMaid shooter, LivingEntity target, ItemStack stack) {
        if (!(shooter.level() instanceof ServerLevel serverLevel)) return;
        if (stack.isEmpty()) return;

        ItemStack thrownStack = stack.copy();
        TridentPlusEntity trident = new TridentPlusEntity(serverLevel, shooter, thrownStack);
        trident.setMaidInfiniteThrow(true);
        trident.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());

        Vec3 direction = target.getEyePosition().subtract(shooter.getEyePosition());
        if (direction.lengthSqr() < 1.0E-6D) return;

        trident.shoot(direction.x, direction.y, direction.z, 2.5F, 1.0F);
        serverLevel.addFreshEntity(trident);

        shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    // 女仆使用魔法弓时生成满蓄魔法箭。
    public static void shootMagicBow(EntityMaid shooter, LivingEntity target, ItemStack bow) {
        if (!(shooter.level() instanceof ServerLevel serverLevel)) return;

        int chargeType = rollMagicBowChargeType(shooter, bow);
        Vec3 origin = shooter.getEyePosition();
        Vec3 targetPos = target.getEyePosition();
        Vec3 direction = targetPos.subtract(origin);
        if (direction.lengthSqr() < 1.0E-6D) return;

        // 女仆射击按满蓄处理，保证暴击、强蓄力和星辰裁决表现稳定。
        MagicArrowEntity arrow = new MagicArrowEntity(EntityTypeRegister.MAGIC_ARROW_ENTITY.get(), serverLevel);
        arrow.setOwner(shooter);
        arrow.setPos(origin.x, origin.y - 0.1D, origin.z);
        arrow.setChargeType(chargeType);
        arrow.setBaseDamage(MagicBowConfig.arrowDamage(chargeType));
        arrow.shoot(direction.x, direction.y, direction.z, 3.0F, 1.0F);
        arrow.setCritArrow(true);
        applyVanillaBowEnchantments(bow, arrow);
        serverLevel.addFreshEntity(arrow);

        shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F,
                1.0F / (shooter.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
    }

    // 女仆魔法弓复用玩家魔法弓的蓄力类型概率。
    public static int rollMagicBowChargeType(EntityMaid shooter, ItemStack bow) {
        double roll = shooter.getRandom().nextDouble();
        boolean hasStarJudgement = EnchantmentHelper.getItemEnchantmentLevel(EnchantmentRegister.STAR_JUDGEMENT.get(), bow) > 0;
        double superChance = hasStarJudgement ? MagicBowConfig.superChargeChance() : 0.0D;
        if (roll < superChance) {
            return MagicBowParticleEffectEntity.CHARGE_SUPER;
        }
        if (roll < superChance + MagicBowConfig.strongChargeChance()) {
            return MagicBowParticleEffectEntity.CHARGE_STRONG;
        }
        return MagicBowParticleEffectEntity.CHARGE_NORMAL;
    }

    // 按原版弓发射期规则写入魔法箭附魔效果。
    public static void applyVanillaBowEnchantments(ItemStack bow, MagicArrowEntity arrow) {
        int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
        if (power > 0) {
            arrow.setBaseDamage(arrow.getBaseDamage() + power * 0.5D + 0.5D);
        }
        int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
        if (punch > 0) {
            arrow.setKnockback(punch);
        }
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0) {
            arrow.setSecondsOnFire(100);
        }
    }

    // 检查女仆是否有可用远程武器（主手或背包）。
    public static boolean hasRangedWeapon(EntityMaid maid) {
        if (isRangedWeaponStack(maid.getMainHandItem())) return true;
        IItemHandler inv = maid.getMaidInv();
        if (inv == null) return false;
        for (int i = 0; i < inv.getSlots(); i++) {
            if (isRangedWeaponStack(inv.getStackInSlot(i))) return true;
        }
        return false;
    }

    // 判断单个 ItemStack 是否为硬币。
    public static boolean isCoinStack(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == AkatZumaTool.COIN_ITEM.get()
                || stack.getItem() == AkatZumaTool.COLORFUL_COIN.get());
    }

    // 判断单个 ItemStack 是否为魔法弓。
    public static boolean isMagicBowStack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == AkatZumaTool.MAGIC_BOW.get();
    }

    // 判断单个 ItemStack 是否为天雷战戟。
    public static boolean isTridentPlusStack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == AkatZumaTool.TRIDENT_PLUS.get();
    }

    // 判断单个 ItemStack 是否为本任务可装备的远程武器。
    public static boolean isRangedWeaponStack(ItemStack stack) {
        return isCoinStack(stack) || isTridentPlusStack(stack) || isMagicBowStack(stack);
    }

    // 消耗弹药：普通硬币 shrink，彩色硬币扣耐久。
    public static void consumeAmmo(EntityMaid maid, ItemStack stack, boolean colorful) {
        if (colorful) {
            stack.hurtAndBreak(1, maid, m -> {});
        } else {
            stack.shrink(1);
        }
    }

    // 目标距离是否超出搜索范围。
    public static boolean distanceTooFar(LivingEntity target, EntityMaid maid) {
        return target.distanceTo(maid) > ATTACK_RADIUS;
    }

    // 自动装备 Behavior：主手没有远程武器但背包有时，从背包取出一个放到主手。
    public static class EquipRangedWeaponBehavior extends Behavior<EntityMaid> {

        public EquipRangedWeaponBehavior() {
            super(Map.of());
        }

        @Override
        public boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
            // 主手已有远程武器则跳过。
            if (isRangedWeaponStack(maid.getMainHandItem())) return false;
            // 背包中没有远程武器则跳过。
            IItemHandler inv = maid.getMaidInv();
            if (inv == null) return false;
            return findRangedWeaponSlot(inv) >= 0;
        }

        @Override
        public void start(ServerLevel level, EntityMaid maid, long gameTime) {
            IItemHandler inv = maid.getMaidInv();
            if (inv == null) return;
            int slot = findRangedWeaponSlot(inv);
            if (slot < 0) return;

            // 从背包取出 1 个远程武器。
            ItemStack extracted = inv.extractItem(slot, 1, false);
            if (extracted.isEmpty()) return;

            // 把当前主手物品放回背包，远程武器放到主手。
            ItemStack oldHand = maid.getMainHandItem().copy();
            if (!oldHand.isEmpty()) {
                ItemStack leftover = inv.insertItem(slot, oldHand, false);
                // 如果放不回去则丢弃。
                if (!leftover.isEmpty()) {
                    maid.spawnAtLocation(leftover);
                }
            }
            maid.setItemInHand(InteractionHand.MAIN_HAND, extracted);
        }

        @Override
        public boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
            return false;
        }

        // 彩色硬币优先，其次天雷战戟、魔法弓，最后普通硬币。
        public static int findRangedWeaponSlot(IItemHandler inv) {
            int tridentPlusSlot = -1;
            int magicBowSlot = -1;
            int coinSlot = -1;
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() == AkatZumaTool.COLORFUL_COIN.get()) return i;
                if (tridentPlusSlot < 0 && stack.getItem() == AkatZumaTool.TRIDENT_PLUS.get()) {
                    tridentPlusSlot = i;
                }
                if (magicBowSlot < 0 && stack.getItem() == AkatZumaTool.MAGIC_BOW.get()) {
                    magicBowSlot = i;
                }
                if (coinSlot < 0 && stack.getItem() == AkatZumaTool.COIN_ITEM.get()) {
                    coinSlot = i;
                }
            }
            if (tridentPlusSlot >= 0) return tridentPlusSlot;
            if (magicBowSlot >= 0) return magicBowSlot;
            return coinSlot;
        }
    }
}
