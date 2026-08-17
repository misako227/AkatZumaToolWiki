package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.ModEnchantmentUtil;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

// VillagerTradeEvent 给图书管理员追加本模组自定义附魔书交易。
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillagerTradeEvent {
    // 图书管理员专家等级出售随机魔法弓自定义附魔书。
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.LIBRARIAN) return;

        event.getTrades().computeIfAbsent(5, key -> new ArrayList<>()).add((trader, random) -> {
            ItemStack book = ModEnchantmentUtil.createRandomMagicBowEnchantedBook(random);
            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, 64),
                    new ItemStack(Items.BOOK),
                    book,
                    12,
                    30,
                    0.2F
            );
        });
    }
}
