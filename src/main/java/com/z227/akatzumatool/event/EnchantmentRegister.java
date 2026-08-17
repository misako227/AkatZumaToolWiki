package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.enchantment.AutoTrackingEnchantment;
import com.z227.akatzumatool.enchantment.AutoShootEnchantment;
import com.z227.akatzumatool.enchantment.HeavenlyThunderEnchantment;
import com.z227.akatzumatool.enchantment.StarJudgementEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// EnchantmentRegister 负责注册本模组自定义附魔。
public class EnchantmentRegister {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, AkatZumaTool.MODID); // 自定义附魔注册器。
    public static final RegistryObject<Enchantment> STAR_JUDGEMENT = ENCHANTMENTS.register("star_judgement", StarJudgementEnchantment::new); // 星辰裁决附魔。
    public static final RegistryObject<Enchantment> AUTO_SHOOT = ENCHANTMENTS.register("auto_shoot", AutoShootEnchantment::new); // 自动射击附魔。
    public static final RegistryObject<Enchantment> AUTO_TRACKING = ENCHANTMENTS.register("auto_tracking", AutoTrackingEnchantment::new); // 自动追踪附魔。
    public static final RegistryObject<Enchantment> HEAVENLY_THUNDER = ENCHANTMENTS.register("heavenly_thunder", HeavenlyThunderEnchantment::new); // 天雷战戟天雷附魔。

    // 将附魔注册器挂到模组事件总线。
    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
