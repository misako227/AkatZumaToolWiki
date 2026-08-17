package com.z227.akatzumatool.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.z227.akatzumatool.common.ModEnchantmentUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

// AddMagicBowEnchantedBookModifier 给指定战利品表额外添加随机魔法弓自定义附魔书。
public class AddMagicBowEnchantedBookModifier extends LootModifier {
    public static final Codec<AddMagicBowEnchantedBookModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, AddMagicBowEnchantedBookModifier::new)); // 全局战利品修饰器 Codec。

    public AddMagicBowEnchantedBookModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.add(ModEnchantmentUtil.createRandomMagicBowEnchantedBook(context.getRandom()));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
