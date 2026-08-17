package com.z227.akatzumatool.item;

import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// AkatZumaCreativeTab 注册模组独立创造模式物品栏。
public class AkatZumaCreativeTab {
    // 创造模式物品栏注册器。
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AkatZumaTool.MODID);

    // AkatZumaTool 专属物品栏。
    public static final RegistryObject<CreativeModeTab> AKATZUMA_TOOL_TAB = TABS.register("akatzuma_tool",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.akatzumatool"))
                    .icon(() -> new ItemStack(AkatZumaTool.FLY_SWORD.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(AkatZumaTool.FLY_SWORD.get());
                        output.accept(AkatZumaTool.FLY_SWORD_PLUS.get());
                        output.accept(AkatZumaTool.COIN_ITEM.get());
                        output.accept(AkatZumaTool.COLORFUL_COIN.get());
                        output.accept(AkatZumaTool.BEAM_CROSS_TEST_ITEM.get());
                        output.accept(AkatZumaTool.MAGIC_BOW.get());
                        output.accept(AkatZumaTool.SPARKLING_FRUIT.get());
                        output.accept(AkatZumaTool.TRIDENT_PLUS.get());
                    })
                    .build());
}
