package com.z227.akatzumatool;

import com.mojang.logging.LogUtils;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.config.TridentPlusConfig;
import com.z227.akatzumatool.event.EnchantmentRegister;
import com.z227.akatzumatool.event.EffectRegister;
import com.z227.akatzumatool.event.EntityTypeRegister;
import com.z227.akatzumatool.event.LootModifierRegister;
import com.z227.akatzumatool.item.AkatZumaCreativeTab;
import com.z227.akatzumatool.item.BeamCrossTestItem;
import com.z227.akatzumatool.item.CoinItem;
import com.z227.akatzumatool.item.ColorfulCoinItem;
import com.z227.akatzumatool.item.FlySwordItem;
import com.z227.akatzumatool.item.FlySwordPlusItem;
import com.z227.akatzumatool.item.MagicBowItem;
import com.z227.akatzumatool.item.SparklingFruitItem;
import com.z227.akatzumatool.item.TridentPlusItem;
import com.z227.akatzumatool.item.testitem.testitem;
import com.z227.akatzumatool.network.NetworkRegister;
import com.z227.akatzumatool.render.finalRender.PostProcessing;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(AkatZumaTool.MODID)
public class AkatZumaTool {
    public static final String MODID = "akatzumatool";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS;
    public static final DeferredRegister<SoundEvent> SOUNDS;

    public static final RegistryObject<Item> FLY_SWORD;
    public static final RegistryObject<Item> FLY_SWORD_PLUS;
    public static final RegistryObject<Item> COIN_ITEM;
    public static final RegistryObject<Item> COLORFUL_COIN;
    public static final RegistryObject<Item> BEAM_CROSS_TEST_ITEM;
    public static final RegistryObject<Item> MAGIC_BOW;
    public static final RegistryObject<Item> SPARKLING_FRUIT; // 闪闪果实强化食物。
    public static final RegistryObject<Item> TRIDENT_PLUS; // 天雷战戟升级三叉戟。
    public static final RegistryObject<Item> TEST_ITEM;

    public static final RegistryObject<SoundEvent> COIN_CHARGE_COMPLETE;
    public static final RegistryObject<SoundEvent> STAR_JUDGEMENT_SUMMON;
    public static final RegistryObject<SoundEvent> DIMENSION_SLASH;
    public static final RegistryObject<SoundEvent> DIMENSION_SLASH_END;
    public static final RegistryObject<SoundEvent> SWORD_AURA;
    public static final RegistryObject<SoundEvent> BATTO_SLASH;
    public static final RegistryObject<SoundEvent> SPARKLING_1; // 闪闪果实 Buff 添加音效。
    public static final RegistryObject<SoundEvent> SPARKLING_2; // 闪闪果实 Alt 瞬移音效。
    public static final RegistryObject<SoundEvent> CHARGING_1; // 咖喱棒剑气终点爆闪音效。
    public static final RegistryObject<SoundEvent> EX_BOOM_1; // 咖喱棒终点冲击波二段爆发音效。
    public static final RegistryObject<SoundEvent> EX; // 咖喱棒蓄力增强阶段音效。
    public static final RegistryObject<SoundEvent> CALIBUR; // 咖喱棒成功发射剑气音效。

    public static PostProcessing POST;
    public static ExecutorService AkatPool = Executors.newCachedThreadPool();

    static {
        ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
        SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

        FLY_SWORD = ITEMS.register("fly_sword", () -> new FlySwordItem(Tiers.DIAMOND, 4, 2.4F, new Item.Properties().fireResistant()));
        FLY_SWORD_PLUS = ITEMS.register("fly_sword_plus", () -> new FlySwordPlusItem(Tiers.DIAMOND, 6, 2.4F, new Item.Properties().fireResistant().rarity(Rarity.RARE)));
        COIN_ITEM = ITEMS.register("coin", () -> new CoinItem(new Item.Properties()));
        COLORFUL_COIN = ITEMS.register("colorful_coin", () -> new ColorfulCoinItem(new Item.Properties().durability(256).rarity(Rarity.RARE)));
        BEAM_CROSS_TEST_ITEM = ITEMS.register("beam_cross_test", () -> new BeamCrossTestItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
        MAGIC_BOW = ITEMS.register("magic_bow", () -> new MagicBowItem(new Item.Properties().stacksTo(1).durability(384).rarity(Rarity.RARE)));
        SPARKLING_FRUIT = ITEMS.register("sparkling_fruit", () -> new SparklingFruitItem(new Item.Properties().rarity(Rarity.EPIC)));
        TRIDENT_PLUS = ITEMS.register("trident_plus", () -> new TridentPlusItem(new Item.Properties().stacksTo(1).durability(TridentPlusConfig.TRIDENT_PLUS_DURABILITY).rarity(Rarity.EPIC)));
        TEST_ITEM = ITEMS.register("test_item", () -> new testitem(new Item.Properties()));

        COIN_CHARGE_COMPLETE = SOUNDS.register("coin_charge_complete",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "coin_charge_complete")));
        STAR_JUDGEMENT_SUMMON = SOUNDS.register("star_judgement_summon",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "star_judgement_summon")));
        DIMENSION_SLASH = SOUNDS.register("slash",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "slash")));
        DIMENSION_SLASH_END = SOUNDS.register("slash_end",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "slash_end")));
        SWORD_AURA = SOUNDS.register("sword_aura",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "sword_aura")));
        BATTO_SLASH = SOUNDS.register("batto_skash",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "batto_skash")));
        SPARKLING_1 = SOUNDS.register("sparkling_1",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "sparkling_1")));
        SPARKLING_2 = SOUNDS.register("sparkling_2",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "sparkling_2")));
        CHARGING_1 = SOUNDS.register("charging_1",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "charging_1")));
        EX_BOOM_1 = SOUNDS.register("ex_boom_1",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "ex_boom_1")));
        EX = SOUNDS.register("ex",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "ex")));
        CALIBUR = SOUNDS.register("calibur",
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "calibur")));
    }

    public AkatZumaTool() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        SOUNDS.register(bus);
        AkatZumaCreativeTab.TABS.register(bus);
        EffectRegister.register(bus);
        EntityTypeRegister.register(bus);
        EnchantmentRegister.register(bus);
        LootModifierRegister.register(bus);
        NetworkRegister.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigFile.CONFIG_SPEC, "Akatzumatool/akatzumatool.toml");
    }

    public static void submitAkatTask(Runnable runnable) {
        AkatPool.submit(runnable);
    }
}
