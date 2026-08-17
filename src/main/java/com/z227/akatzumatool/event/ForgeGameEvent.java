package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.item.MagicBowItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

// ForgeGameEvent 处理运行期游戏事件。
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeGameEvent {
    private static final int MAGIC_BOW_MAX_QUICK_CHARGE_LEVEL = 5; // 魔法弓快速装填铁砧合成上限。

    // 彩色硬币铁砧修复：1 个金锭直接修满耐久。
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (tryApplyMagicBowQuickCharge(event, left, right)) return;

        if (!left.is(AkatZumaTool.COLORFUL_COIN.get())) return;
        if (!right.is(Items.GOLD_INGOT)) return;
        if (!left.isDamaged()) return;

        ItemStack output = left.copy();
        output.setDamageValue(0);
        event.setOutput(output);
        event.setCost(1);
        event.setMaterialCost(1);
    }

    // 魔法弓支持通过铁砧附加快速装填附魔。
    public static boolean tryApplyMagicBowQuickCharge(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!left.is(AkatZumaTool.MAGIC_BOW.get())) return false;

        int incomingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, right);
        if (incomingLevel <= 0) return false;

        int currentLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, left);
        int targetLevel = Math.max(currentLevel, incomingLevel);
        if (currentLevel == incomingLevel && currentLevel < MAGIC_BOW_MAX_QUICK_CHARGE_LEVEL) {
            targetLevel = currentLevel + 1;
        }
        if (targetLevel <= currentLevel) return false;

        ItemStack output = left.copy();
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(output);
        enchantments.put(Enchantments.QUICK_CHARGE, Math.min(targetLevel, MAGIC_BOW_MAX_QUICK_CHARGE_LEVEL));
        EnchantmentHelper.setEnchantments(enchantments, output);

        event.setOutput(output);
        event.setCost(targetLevel);
        event.setMaterialCost(1);
        return true;
    }

    // 彩色硬币支持通过铁砧附加快速装填附魔。
//    @SubscribeEvent
//    public static void onColorfulCoinQuickChargeEnchant(AnvilUpdateEvent event) {
//        ItemStack left = event.getLeft();
//        ItemStack right = event.getRight();
//        if (!left.is(AkatZumaTool.COLORFUL_COIN.get())) return;
//
//        int incomingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, right);
////        int incomingLevel = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.QUICK_CHARGE, right);
//        if (incomingLevel <= 0) return;
//
//        int currentLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, left);
//        int targetLevel = Math.max(currentLevel, incomingLevel);
//        if (currentLevel == incomingLevel && currentLevel < Enchantments.QUICK_CHARGE.getMaxLevel()) {
//            targetLevel = currentLevel + 1;
//        }
//        if (targetLevel <= currentLevel) return;
//
//        ItemStack output = left.copy();
//        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(output);
//        enchantments.put(Enchantments.QUICK_CHARGE, Math.min(targetLevel, Enchantments.QUICK_CHARGE.getMaxLevel()));
//        EnchantmentHelper.setEnchantments(enchantments, output);
//
//        event.setOutput(output);
//        event.setCost(targetLevel);
//        event.setMaterialCost(1);
//    }

    // 服务端启动后构建实体伤害白名单。
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        rebuildEntityDamageWhitelistMap();
    }

    // 玩家退出时清理自动追踪防重复标记。
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MagicBowItem.clearSuppressNextAutoTrackingVanillaShot(event.getEntity());
    }

    // 重建服务端生效后的实体伤害白名单。
    public static HashMap<String, Boolean> rebuildEntityDamageWhitelistMap() {
        HashMap<String, Boolean> whitelistMap = new HashMap<>();
        for (String id : ConfigFile.entityDamageWhitelist()) {
            ResourceLocation location = ResourceLocation.tryParse(id);
            if (location == null || !ForgeRegistries.ENTITY_TYPES.containsKey(location)) {
                AkatZumaTool.LOGGER.warn("Invalid entity damage whitelist entry: {}", id);
                continue;
            }
            whitelistMap.put(location.toString(), true);
        }
        if (!ConfigFile.damagePlayers()) {
            whitelistMap.put("minecraft:player", true);
        }
        ConfigFile.setEntityDamageWhitelistMap(whitelistMap);
        return whitelistMap;
    }
}
