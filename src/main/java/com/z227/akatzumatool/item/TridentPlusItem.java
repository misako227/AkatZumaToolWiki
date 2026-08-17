package com.z227.akatzumatool.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.PlayerUtil;
import com.z227.akatzumatool.config.TridentPlusConfig;
import com.z227.akatzumatool.entity.trident.HeavenlyThunderEntity;
import com.z227.akatzumatool.entity.trident.TridentPlusEntity;
import com.z227.akatzumatool.event.EnchantmentRegister;
import com.z227.akatzumatool.event.EntityTypeRegister;
import com.z227.akatzumatool.event.client.ClientKeyChargeRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

// TridentPlusItem 是天雷战戟物品，复用三叉戟使用逻辑并投掷自定义实体。
public class TridentPlusItem extends TridentItem {
    private static final Component TOOLTIP_ENCHANT = Component.translatable("item.akatzumatool.trident_plus.tooltip.1");
    public static final int HEAVENLY_THUNDER_FOOD_COST = 10; // 天雷技能饱食度消耗。
    private final Multimap<Attribute, AttributeModifier> tridentPlusModifiers; // 天雷战戟近战属性修饰器。

    public TridentPlusItem(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", TridentPlusConfig.attackDamage(), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", -2.9D, AttributeModifier.Operation.ADDITION));
        this.tridentPlusModifiers = builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(TOOLTIP_ENCHANT);
        super.appendHoverText(stack, level, tooltip, flag);
    }

    // 返回天雷战戟近战属性，伤害读取独立配置类。
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.tridentPlusModifiers : super.getDefaultAttributeModifiers(slot);
    }

    // 客户端使用原版三叉戟实体模型渲染手中战戟，避免 vanilla trident JSON 空模型不显示。
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final TridentPlusItemRenderer renderer = new TridentPlusItemRenderer(); // 天雷战戟物品自定义渲染器。

            @Override
            public TridentPlusItemRenderer getCustomRenderer() {
                return this.renderer;
            }

            // 第三人称按服务端同步状态显示拉弓动作，不改变右键投掷的 UseAnim.SPEAR。
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                if (!(entity instanceof Player player)) return null;
                if (!ClientKeyChargeRegistry.isCharging(player)) return null;
                return ClientKeyChargeRegistry.getHand(player) == hand ? HumanoidModel.ArmPose.BOW_AND_ARROW : null;
            }

            // 第一人称为正在天雷蓄力的手应用拉弓式物品变换。
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                                   ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
                if (!ClientKeyChargeRegistry.isCharging(player)) return false;
                InteractionHand hand = ClientKeyChargeRegistry.getHand(player);
                HumanoidArm chargeArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
                if (arm != chargeArm) return false;
                renderer.applyBowChargeTransform(poseStack, player, arm, partialTick, equipProcess);
                return true;
            }
        });
    }

    // 显式使用三叉戟举矛蓄力动作，避免后续模型包装影响姿态判断。
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    // 保持原版三叉戟的长按使用时长。
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // 复制原版三叉戟投掷流程，但服务端生成 TridentPlusEntity。
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof Player player)) return;
        int useTicks = this.getUseDuration(stack) - timeLeft;
        if (useTicks < THROW_THRESHOLD_TIME) return;

        int riptideLevel = EnchantmentHelper.getRiptide(stack);
        if (riptideLevel > 0 && !player.isInWaterOrRain()) return;

        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(living.getUsedItemHand()));
            if (riptideLevel == 0) {
                TridentPlusEntity trident = new TridentPlusEntity(level, player, stack);
                trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, SHOOT_POWER + riptideLevel * 0.5F, 1.0F);
                if (player.getAbilities().instabuild) {
                    trident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }

                level.addFreshEntity(trident);
                level.playSound(null, trident, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    player.getInventory().removeItem(stack);
                }
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (riptideLevel > 0) {
            applyRiptideMovement(level, player, riptideLevel);
        }
    }

    // 保留原版激流附魔的玩家冲刺逻辑。
    public void applyRiptideMovement(Level level, Player player, int riptideLevel) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        float motionX = -Mth.sin(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));
        float motionY = -Mth.sin(pitch * ((float) Math.PI / 180F));
        float motionZ = Mth.cos(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));
        float motionLength = Mth.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        float power = 3.0F * ((1.0F + riptideLevel) / 4.0F);
        motionX *= power / motionLength;
        motionY *= power / motionLength;
        motionZ *= power / motionLength;
        player.push(motionX, motionY, motionZ);
        player.startAutoSpinAttack(20);
        if (player.onGround()) {
            player.move(MoverType.SELF, new Vec3(0.0D, 1.1999999D, 0.0D));
        }

        SoundEvent sound = riptideLevel >= 3 ? SoundEvents.TRIDENT_RIPTIDE_3 : riptideLevel == 2 ? SoundEvents.TRIDENT_RIPTIDE_2 : SoundEvents.TRIDENT_RIPTIDE_1;
        level.playSound(null, player, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // 判断玩家是否手持天雷战戟。
    public static boolean isHoldingTridentPlus(Player player) {
        if (player == null) return false;
        return player.getMainHandItem().is(AkatZumaTool.TRIDENT_PLUS.get())
                || player.getOffhandItem().is(AkatZumaTool.TRIDENT_PLUS.get());
    }

    // 取得玩家手中的天雷战戟，优先主手。
    public static ItemStack getHeldTridentPlusStack(Player player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(AkatZumaTool.TRIDENT_PLUS.get())) return mainHand;
        ItemStack offHand = player.getOffhandItem();
        return offHand.is(AkatZumaTool.TRIDENT_PLUS.get()) ? offHand : ItemStack.EMPTY;
    }

    // 判断物品栈是否带有天雷附魔。
    public static boolean hasHeavenlyThunder(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return EnchantmentHelper.getItemEnchantmentLevel(EnchantmentRegister.HEAVENLY_THUNDER.get(), stack) > 0;
    }

    // 判断玩家是否手持带天雷附魔的天雷战戟。
    public static boolean isHoldingHeavenlyThunderTrident(Player player) {
        return hasHeavenlyThunder(getHeldTridentPlusStack(player));
    }

    // 取得玩家手持带天雷附魔战戟的手，优先主手。
    public static InteractionHand getHeldHeavenlyThunderHand(Player player) {
        if (player == null) return null;
        if (hasHeavenlyThunder(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (hasHeavenlyThunder(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }

    // 判断玩家是否拥有释放天雷技能所需的饱食度。
    public static boolean hasEnoughHeavenlyThunderFood(Player player) {
        if (player == null) return false;
        if (player.isCreative() || player.isSpectator()) return true;
        return player.getFoodData().getFoodLevel() >= HEAVENLY_THUNDER_FOOD_COST;
    }

    // 服务端释放天雷技能，生成持续法阵实体并扣除饱食度。
    public static boolean trySpawnHeavenlyThunder(Player player) {
        if (player == null) return false;
        if (player.level().isClientSide()) return false;
        if (player.isSpectator()) return false;
        if (!isHoldingHeavenlyThunderTrident(player)) return false;
        if (!hasEnoughHeavenlyThunderFood(player)) return false;

        HeavenlyThunderEntity thunder = new HeavenlyThunderEntity(EntityTypeRegister.HEAVENLY_THUNDER.get(), player.level());
        thunder.setThunderData(player.position(), player, player.getRandom().nextInt());
        player.level().addFreshEntity(thunder);
        PlayerUtil.deductFood(player, HEAVENLY_THUNDER_FOOD_COST);
        return true;
    }
}
