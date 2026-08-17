package com.z227.akatzumatool.common;

import com.z227.akatzumatool.event.EnchantmentRegister;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.List;

// ModEnchantmentUtil 统一维护本模组魔法弓相关自定义附魔书列表。
public class ModEnchantmentUtil {
    // 返回当前可通过额外途径获取的魔法弓自定义附魔。
    public static List<Enchantment> magicBowEnchantments() {
        return List.of(
                EnchantmentRegister.STAR_JUDGEMENT.get(),
                EnchantmentRegister.AUTO_SHOOT.get(),
                EnchantmentRegister.AUTO_TRACKING.get()
        );
    }

    // 随机创建一本魔法弓自定义附魔书。
    public static ItemStack createRandomMagicBowEnchantedBook(RandomSource random) {
        List<Enchantment> enchantments = magicBowEnchantments();
        Enchantment enchantment = enchantments.get(random.nextInt(enchantments.size()));
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, 1));
    }
}
