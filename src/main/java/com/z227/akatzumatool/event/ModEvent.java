package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.item.CoinItem;
import com.z227.akatzumatool.item.ColorfulCoinItem;
import com.z227.akatzumatool.item.MagicBowItem;
import com.z227.akatzumatool.network.NetworkRegister;
import com.z227.akatzumatool.network.WhitelistSyncS2CPacket;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;

// ModEvent 负责模组配置变更后的缓存刷新。
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvent {
    // 配置加载或热更新后刷新物品缓存字段。
    @SubscribeEvent
    public static void onConfigLoaded(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (config.getSpec() != com.z227.akatzumatool.config.ConfigFile.CONFIG_SPEC) {
            return;
        }

        if (AkatZumaTool.COIN_ITEM.isPresent() && AkatZumaTool.COIN_ITEM.get() instanceof CoinItem coinItem) {
            coinItem.loadConfigValues();
        }
        if (AkatZumaTool.COLORFUL_COIN.isPresent() && AkatZumaTool.COLORFUL_COIN.get() instanceof ColorfulCoinItem colorfulCoinItem) {
            colorfulCoinItem.loadConfigValues();
        }
        if (AkatZumaTool.MAGIC_BOW.isPresent() && AkatZumaTool.MAGIC_BOW.get() instanceof MagicBowItem magicBowItem) {
            magicBowItem.loadConfigValues();
        }
        HashMap<String, Boolean> whitelistMap = ForgeGameEvent.rebuildEntityDamageWhitelistMap();
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            NetworkRegister.sendToAll(new WhitelistSyncS2CPacket(new ArrayList<>(whitelistMap.keySet())));
        }
    }
}
