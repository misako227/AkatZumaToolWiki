package com.z227.akatzumatool.linkMod.touhouLittleMaid.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.entity.sword.BattoSlashEntity;
import com.z227.akatzumatool.entity.sword.DimensionSlashDomainEntity;
import com.z227.akatzumatool.entity.sword.SwordAuraEntity;
import com.z227.akatzumatool.event.EntityTypeRegister;
import com.z227.akatzumatool.item.FlySwordItem;
import com.z227.akatzumatool.item.FlySwordPlusItem;
import com.z227.akatzumatool.linkMod.touhouLittleMaid.MaidSkillCooldowns;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// MaidMeleeAttackTask 是女仆飞剑近战任务，负责装备飞剑并释放剑气、拔刀斩和次元斩。
public class MaidMeleeAttackTask implements IRangedAttackTask {
    public static final ResourceLocation UID = new ResourceLocation(AkatZumaTool.MODID, "melee_attack");
    public static final String SWORD_AURA_KEY = "sword_aura";
    public static final String BATTO_SLASH_KEY = "batto_slash";
    public static final String DIMENSION_SLASH_KEY = "dimension_slash";
    private static final float ATTACK_RADIUS = 16.0F;
    private static final float WALK_SPEED = 0.8F;
    private static final int ATTACK_COOLDOWN = 10;
    private static final int SWORD_AURA_COOLDOWN = 10;
    private static final int BATTO_SLASH_COOLDOWN = 80;
    private static final int BATTO_SLASH_RANGE = 8;
    private static final int DIMENSION_SLASH_RANGE = 10;
    private static final int MELEE_RANGE = 3;

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return AkatZumaTool.FLY_SWORD.get().getDefaultInstance();
    }

    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }

    // 行为链：装备飞剑 → 寻敌 → 停止检测 → 走向目标 → 飞剑近战攻击。
    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> equipTask = new EquipMeleeWeaponBehavior();
        BehaviorControl<EntityMaid> startTask = StartAttacking.create(
                MaidMeleeAttackTask::hasFlySword,
                IRangedAttackTask::findFirstValidAttackTarget
        );
        BehaviorControl<EntityMaid> stopTask = StopAttackingIfTargetInvalid.create(
                target -> !hasFlySword(maid) || distanceTooFar(target, maid)
        );
        BehaviorControl<EntityMaid> walkTask = MaidRangedWalkToTarget.create(WALK_SPEED);
        BehaviorControl<EntityMaid> attackTask = new MaidFlySwordAttackBehavior();

        return Lists.newArrayList(
                Pair.of(4, equipTask),
                Pair.of(5, startTask),
                Pair.of(5, stopTask),
                Pair.of(5, walkTask),
                Pair.of(5, attackTask)
        );
    }

    @Override
    public boolean enablePanic(EntityMaid maid) {
        return false;
    }

    @Override
    public List<String> getDescription(EntityMaid maid) {
        return List.of("task." + AkatZumaTool.MODID + ".melee_attack.desc");
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return List.of(Pair.of("0", this::isEnable));
    }

    @Override
    public boolean isEnable(EntityMaid maid) {
        return hasFlySword(maid);
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return isFlySwordStack(stack);
    }

    // IRangedAttackTask 接口入口保留为空，实际攻击由 MaidFlySwordAttackBehavior 执行。
    @Override
    public void performRangedAttack(EntityMaid shooter, LivingEntity target, float distanceFactor) {
    }

    // 检查女仆主手或背包是否有飞剑。
    public static boolean hasFlySword(EntityMaid maid) {
        if (isFlySwordStack(maid.getMainHandItem())) return true;
        IItemHandler inv = maid.getMaidInv();
        if (inv == null) return false;
        for (int i = 0; i < inv.getSlots(); i++) {
            if (isFlySwordStack(inv.getStackInSlot(i))) return true;
        }
        return false;
    }

    // 判断是否为任意飞剑。
    public static boolean isFlySwordStack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof FlySwordItem;
    }

    // 判断是否为真·飞剑。
    public static boolean isFlySwordPlusStack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof FlySwordPlusItem;
    }

    // 目标距离是否超出近战任务搜索范围。
    public static boolean distanceTooFar(LivingEntity target, EntityMaid maid) {
        return target.distanceTo(maid) > ATTACK_RADIUS;
    }

    // 女仆释放剑气，冷却与玩家左键触发保持一致。
    public static void trySpawnSwordAura(EntityMaid maid) {
        if (maid.level().isClientSide()) return;
        if (MaidSkillCooldowns.isCoolingDown(maid, SWORD_AURA_KEY)) return;

        SwordAuraEntity aura = new SwordAuraEntity(EntityTypeRegister.SWORD_AURA_ENTITY.get(), maid.level());
        Vec3 direction = maid.getLookAngle();
        Vec3 origin = maid.getEyePosition().add(direction.scale(0.35D));
        aura.setAuraData(maid, origin, direction, ConfigFile.flySwordAuraDamage());
        maid.level().addFreshEntity(aura);

        MaidSkillCooldowns.setCooldown(maid, SWORD_AURA_KEY, SWORD_AURA_COOLDOWN);
    }

    // 女仆尝试释放拔刀斩，只判断目标距离。
    public static void tryBattoSlash(EntityMaid maid, LivingEntity target) {
        if (!(maid.level() instanceof ServerLevel serverLevel)) return;
        if (!isFlySwordPlusStack(maid.getMainHandItem())) return;
        if (MaidSkillCooldowns.isCoolingDown(maid, BATTO_SLASH_KEY)) return;
        if (maid.distanceTo(target) > BATTO_SLASH_RANGE) return;

        BattoSlashEntity battoSlash = BattoSlashEntity.create(maid);
        serverLevel.addFreshEntity(battoSlash);
        MaidSkillCooldowns.setCooldown(maid, BATTO_SLASH_KEY, BATTO_SLASH_COOLDOWN);
    }

    // 女仆尝试释放次元斩，只判断目标距离。
    public static void tryDimensionSlash(EntityMaid maid, LivingEntity target) {
        if (!(maid.level() instanceof ServerLevel serverLevel)) return;
        if (!isFlySwordPlusStack(maid.getMainHandItem())) return;
        if (MaidSkillCooldowns.isCoolingDown(maid, DIMENSION_SLASH_KEY)) return;
        if (maid.distanceTo(target) > DIMENSION_SLASH_RANGE) return;

        DimensionSlashDomainEntity domain = DimensionSlashDomainEntity.create(maid);
        serverLevel.addFreshEntity(domain);
        MaidSkillCooldowns.setCooldown(maid, DIMENSION_SLASH_KEY, ConfigFile.flySwordDimensionSlashCooldown() + 60);
    }

    // 自动装备 Behavior：主手没有飞剑但背包有时，从背包取出一把放到主手。
    public static class EquipMeleeWeaponBehavior extends Behavior<EntityMaid> {

        public EquipMeleeWeaponBehavior() {
            super(Map.of());
        }

        @Override
        public boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
            if (isFlySwordStack(maid.getMainHandItem())) return false;
            IItemHandler inv = maid.getMaidInv();
            if (inv == null) return false;
            return findFlySwordSlot(inv) >= 0;
        }

        @Override
        public void start(ServerLevel level, EntityMaid maid, long gameTime) {
            IItemHandler inv = maid.getMaidInv();
            if (inv == null) return;
            int slot = findFlySwordSlot(inv);
            if (slot < 0) return;

            ItemStack extracted = inv.extractItem(slot, 1, false);
            if (extracted.isEmpty()) return;

            ItemStack oldHand = maid.getMainHandItem().copy();
            if (!oldHand.isEmpty()) {
                ItemStack leftover = inv.insertItem(slot, oldHand, false);
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

        // 真·飞剑优先，其次普通飞剑。
        public static int findFlySwordSlot(IItemHandler inv) {
            int flySwordSlot = -1;
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() instanceof FlySwordPlusItem) return i;
                if (flySwordSlot < 0 && stack.getItem() instanceof FlySwordItem) {
                    flySwordSlot = i;
                }
            }
            return flySwordSlot;
        }
    }

    // 飞剑近战行为：距离足够时攻击目标，并按距离释放真·飞剑技能。
    public static class MaidFlySwordAttackBehavior extends Behavior<EntityMaid> {

        public MaidFlySwordAttackBehavior() {
            super(Map.of());
        }

        @Override
        public boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
            LivingEntity target = maid.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).orElse(null);
            return target != null && target.isAlive() && isFlySwordStack(maid.getMainHandItem());
        }

        @Override
        public void start(ServerLevel level, EntityMaid maid, long gameTime) {
            LivingEntity target = maid.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).orElse(null);
            if (target == null || !target.isAlive()) return;

            maid.getLookControl().setLookAt(target, 30.0F, 30.0F);
            tryDimensionSlash(maid, target);
            tryBattoSlash(maid, target);
            if (maid.distanceTo(target) > MELEE_RANGE) return;
            if (MaidSkillCooldowns.isCoolingDown(maid, "melee_attack")) return;

            maid.swing(InteractionHand.MAIN_HAND);
            maid.doHurtTarget(target);
            trySpawnSwordAura(maid);
            MaidSkillCooldowns.setCooldown(maid, "melee_attack", ATTACK_COOLDOWN);
        }
    }
}
