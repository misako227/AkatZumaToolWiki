package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.effect.sparkling.SparklingFruitEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// EffectRegister 注册 AkatZumaTool 自定义 MobEffect。
public class EffectRegister {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, AkatZumaTool.MODID); // MobEffect 延迟注册器。
    public static final RegistryObject<MobEffect> SPARKLING_FRUIT_EFFECT = EFFECTS.register("sparkling_fruit_effect", SparklingFruitEffect::new); // 闪闪果实 Buff。

    // 把 MobEffect 注册器挂到 MOD 事件总线。
    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
