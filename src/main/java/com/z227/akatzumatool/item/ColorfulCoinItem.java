package com.z227.akatzumatool.item;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.entity.coin.CoinChargeTracker;
import com.z227.akatzumatool.entity.coin.ColorfulCoinEntity;
import com.z227.akatzumatool.event.EntityTypeRegister;
import com.z227.akatzumatool.event.client.ChargeLightningClientRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// ColorfulCoin 物品，沿用硬币蓄力方式并释放更强的黄红光束。
public class ColorfulCoinItem extends CoinItem {
    // Tooltip 文本。
    private static final Component TOOLTIP_USE = Component.translatable("item.akatzumatool.colorful_coin.tooltip.1");
    private static final int ENCHANTMENT_VALUE = 10;
    private static final float POWER_DAMAGE_BONUS_PER_LEVEL = 0.25f;

    // 彩色硬币配置字段。
    private int fullChargeTime = 50;
    private boolean quickChargeEnabled = true;
    private double quickChargeReduction = 0.20D;

    public ColorfulCoinItem(Properties properties) {
        super(properties);
    }

    // 在配置加载完成后刷新彩色硬币字段。
    @Override
    public void loadConfigValues() {
        this.beamDamage = ConfigFile.colorfulCoinBeamDamage();
        this.maxRange = ConfigFile.colorfulCoinMaxRange();
        this.fullChargeTime = ConfigFile.colorfulCoinFullChargeTime();
        this.quickChargeEnabled = ConfigFile.colorfulCoinQuickChargeEnabled();
        this.quickChargeReduction = ConfigFile.colorfulCoinQuickChargeReduction();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
//        tooltip.add(TOOLTIP_USE);
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            CoinChargeTracker.startCharge(player, player.tickCount, getChargeTime(stack));
            ChargeLightningClientRegistry.startCoin(player, true);
            player.startUsingItem(context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    // 右键按下开始蓄力，复用 CoinChargeTracker。
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CoinChargeTracker.startCharge(player, player.tickCount, getChargeTime(stack));
        ChargeLightningClientRegistry.startCoin(player, true);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // 每 tick 更新蓄力进度。
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (livingEntity instanceof Player player) {
            CoinChargeTracker.updateCharge(player, player.tickCount);
            // 蓄力完成时复用普通硬币的手部汇聚粒子。
            if (level.isClientSide() && CoinChargeTracker.isFullyCharged(player)) {
                CoinItem.emitFullChargeParticles(player, true);
            }
        }
    }

    // 松开右键后在服务端生成 ColorfulCoinEntity。
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (livingEntity instanceof Player player) {
            float chargeProgress = CoinChargeTracker.stopCharge(player);
            ChargeLightningClientRegistry.stop(player);
            if (!level.isClientSide() && chargeProgress >= CoinChargeTracker.MIN_LAUNCH_THRESHOLD) {
                launchBeam(level, player, stack, chargeProgress);
                // 发射后扣除1点耐久。
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            }
        }
    }

    // 计算彩色硬币本次蓄力所需时间。
    private int getChargeTime(ItemStack stack) {
        int chargeTime = Math.max(1, fullChargeTime);
        if (!quickChargeEnabled) {
            return chargeTime;
        }

        int level = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        double multiplier = 1.0D - level * quickChargeReduction;
        return Math.max(1, (int) Math.ceil(chargeTime * multiplier));
    }

    // 计算力量附魔加成后的光束伤害。
    private float getBeamDamage(ItemStack stack, float chargeProgress) {
        int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
        float multiplier = 1.0f + powerLevel * POWER_DAMAGE_BONUS_PER_LEVEL;
        return beamDamage * chargeProgress * multiplier;
    }

    // 创建强光束实体，并写入手部起点、视线终点和高伤害。
    private void launchBeam(Level level, Player player, ItemStack stack, float chargeProgress) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 origin = CoinItem.getBeamHandOrigin(player, eyePos, lookVec);
        Vec3 endPos = eyePos.add(lookVec.scale(maxRange * chargeProgress));

        ColorfulCoinEntity beamEntity = new ColorfulCoinEntity(EntityTypeRegister.COLORFUL_COIN_ENTITY.get(), level);
        beamEntity.setBeamData(origin, endPos, player, getBeamDamage(stack, chargeProgress));

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(beamEntity);
        }

        // 蓄力发射音效。
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                AkatZumaTool.COIN_CHARGE_COMPLETE.get(), SoundSource.PLAYERS, 1.8f, 1.0f);

        player.getCooldowns().addCooldown(this, 10);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.BOW;
    }

    // 允许彩色硬币进入附魔台。
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    // 附魔台使用的附魔价值。
    @Override
    public int getEnchantmentValue() {
        return ENCHANTMENT_VALUE;
    }

    // 附魔台使用的附魔价值。
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    // 附魔台允许适合彩色硬币的常用附魔。
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.QUICK_CHARGE
                || enchantment == Enchantments.UNBREAKING
                || enchantment == Enchantments.MENDING
                || enchantment == Enchantments.POWER_ARROWS;
    }

    // 铁砧修复材料：允许使用金锭修复彩色硬币。
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.GOLD_INGOT) || super.isValidRepairItem(stack, repairCandidate);
    }
}
