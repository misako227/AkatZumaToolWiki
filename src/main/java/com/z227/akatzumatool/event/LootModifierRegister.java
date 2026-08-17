package com.z227.akatzumatool.event;

import com.mojang.serialization.Codec;
import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.loot.AddMagicBowEnchantedBookModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// LootModifierRegister 注册模组全局战利品修饰器。
public class LootModifierRegister {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, AkatZumaTool.MODID); // 全局战利品修饰器 Codec 注册器。
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ADD_MAGIC_BOW_ENCHANTED_BOOK = LOOT_MODIFIERS.register("add_magic_bow_enchanted_book", () -> AddMagicBowEnchantedBookModifier.CODEC); // 钓鱼追加魔法弓附魔书修饰器。

    // 将全局战利品修饰器注册器挂到模组事件总线。
    public static void register(IEventBus eventBus) {
        LOOT_MODIFIERS.register(eventBus);
    }
}
