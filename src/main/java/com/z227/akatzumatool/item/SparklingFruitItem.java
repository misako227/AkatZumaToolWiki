package com.z227.akatzumatool.item;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.event.EffectRegister;
import com.z227.akatzumatool.event.SparklingFruitEventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

// SparklingFruitItem 是闪闪果实食物，食用后提供强化金苹果效果和闪闪果实 Buff。
public class SparklingFruitItem extends Item {
    private static final Component TOOLTIP_1 = Component.translatable("item.akatzumatool.sparkling_fruit.tooltip.1");
    private static final Component TOOLTIP_2 = Component.translatable("item.akatzumatool.sparkling_fruit.tooltip.2");
    public static final int DEFAULT_BUFF_DURATION = 20 * 30; // 闪闪果实 Buff 默认持续 30 秒。
    public static final FoodProperties SPARKLING_FRUIT_FOOD = new FoodProperties.Builder()
            .nutrition(20)
            .saturationMod(1.2F)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 4), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 4), 1.0F)
            .alwaysEat()
            .build(); // 强化版金苹果食物属性。

    // 创建闪闪果实物品，并把强化食物属性写入物品属性。
    public SparklingFruitItem(Properties properties) {
        super(properties.food(SPARKLING_FRUIT_FOOD));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(TOOLTIP_1);
        tooltip.add(TOOLTIP_2);
        super.appendHoverText(stack, level, tooltip, flag);
    }

    // 食用完成后先保留食物效果，再给任意 LivingEntity 添加闪闪果实 Buff 和启动音效。
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide()) {
            int duration = ConfigFile.sparklingFruitBuffDurationTicks();
            entity.addEffect(new MobEffectInstance(
                    EffectRegister.SPARKLING_FRUIT_EFFECT.get(),
                    duration,
                    0,
                    false,
                    true,
                    true
            ));
            if (entity instanceof Player) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, ConfigFile.sparklingFruitSpeedAmplifier(), false, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, ConfigFile.sparklingFruitJumpAmplifier(), false, true, true));
            }
            SparklingFruitEventHandler.syncOutlineRefresh(entity);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    AkatZumaTool.SPARKLING_1.get(), entity.getSoundSource(), 1.0F, 1.0F);
        }
        return result;
    }
}
