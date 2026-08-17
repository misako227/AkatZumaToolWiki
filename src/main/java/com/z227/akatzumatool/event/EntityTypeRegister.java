package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.entity.FlySwordEntity;
import com.z227.akatzumatool.entity.bow.MagicArrowEntity;
import com.z227.akatzumatool.entity.bow.MagicBowParticleEffectEntity;
import com.z227.akatzumatool.entity.coin.ColorfulCoinEntity;
import com.z227.akatzumatool.entity.coin.RailgunBeamEntity;
import com.z227.akatzumatool.entity.sword.BattoSlashEntity;
import com.z227.akatzumatool.entity.sword.DimensionSlashDomainEntity;
import com.z227.akatzumatool.entity.sword.DimensionSlashStrikeEntity;
import com.z227.akatzumatool.entity.sword.ExcaliburChargeEntity;
import com.z227.akatzumatool.entity.sword.ExcaliburSwordWaveEntity;
import com.z227.akatzumatool.entity.sword.SwordAuraEntity;
import com.z227.akatzumatool.entity.trident.HeavenlyThunderEntity;
import com.z227.akatzumatool.entity.trident.TridentLightningStrikeEntity;
import com.z227.akatzumatool.entity.trident.TridentPlusEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// EntityTypeRegister 负责集中注册模组实体类型。
public class EntityTypeRegister {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AkatZumaTool.MODID); // 实体注册表。
    public static final RegistryObject<EntityType<FlySwordEntity>> FLY_SWORD_ENTITY = ENTITY_TYPES.register("fly_sword_entity", () -> EntityType.Builder.<FlySwordEntity>of(FlySwordEntity::new, MobCategory.MISC).sized(0.3f, 1.5F).clientTrackingRange(16).updateInterval(1).build("fly_sword_entity")); // 飞剑实体。
    public static final RegistryObject<EntityType<RailgunBeamEntity>> RAILGUN_BEAM_ENTITY = ENTITY_TYPES.register("railgun_beam_entity", () -> EntityType.Builder.of(RailgunBeamEntity::new, MobCategory.MISC).sized(0.1f, 0.1f).build("railgun_beam_entity")); // 电磁炮光束实体。
    public static final RegistryObject<EntityType<ColorfulCoinEntity>> COLORFUL_COIN_ENTITY = ENTITY_TYPES.register("colorful_coin_entity", () -> EntityType.Builder.of(ColorfulCoinEntity::new, MobCategory.MISC).sized(0.1f, 0.1f).build("colorful_coin_entity")); // 彩色硬币光束实体。
    public static final RegistryObject<EntityType<MagicArrowEntity>> MAGIC_ARROW_ENTITY = ENTITY_TYPES.register("magic_arrow", () -> EntityType.Builder.<MagicArrowEntity>of(MagicArrowEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(32).updateInterval(1).build("magic_arrow")); // 魔法箭实体。
    public static final RegistryObject<EntityType<MagicBowParticleEffectEntity>> MAGIC_BOW_PARTICLE_EFFECT_ENTITY = ENTITY_TYPES.register("magic_bow_particle_effect", () -> EntityType.Builder.<MagicBowParticleEffectEntity>of(MagicBowParticleEffectEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(8).updateInterval(2).build("magic_bow_particle_effect")); // 魔法弓粒子效果实体。
    public static final RegistryObject<EntityType<SwordAuraEntity>> SWORD_AURA_ENTITY = ENTITY_TYPES.register("sword_aura", () -> EntityType.Builder.<SwordAuraEntity>of(SwordAuraEntity::new, MobCategory.MISC).sized(0.8F, 0.4F).clientTrackingRange(8).updateInterval(1).build("sword_aura")); // 飞剑剑气实体。
    public static final RegistryObject<EntityType<DimensionSlashDomainEntity>> DIMENSION_SLASH_DOMAIN = ENTITY_TYPES.register("dimension_slash_domain", () -> EntityType.Builder.<DimensionSlashDomainEntity>of(DimensionSlashDomainEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(32).updateInterval(1).build("dimension_slash_domain")); // 次元斩领域实体。
    public static final RegistryObject<EntityType<DimensionSlashStrikeEntity>> DIMENSION_SLASH_STRIKE = ENTITY_TYPES.register("dimension_slash_strike", () -> EntityType.Builder.<DimensionSlashStrikeEntity>of(DimensionSlashStrikeEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(32).updateInterval(1).build("dimension_slash_strike")); // 次元斩连击实体。
    public static final RegistryObject<EntityType<BattoSlashEntity>> BATTO_SLASH = ENTITY_TYPES.register("batto_slash", () -> EntityType.Builder.<BattoSlashEntity>of(BattoSlashEntity::new, MobCategory.MISC).sized(40.0F, 6.0F).clientTrackingRange(48).updateInterval(1).build("batto_slash")); // 拔刀斩实体。
    public static final RegistryObject<EntityType<TridentPlusEntity>> TRIDENT_PLUS_ENTITY = ENTITY_TYPES.register("trident_plus", () -> EntityType.Builder.<TridentPlusEntity>of(TridentPlusEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1).build("trident_plus")); // 天雷战戟投掷实体。
    public static final RegistryObject<EntityType<TridentLightningStrikeEntity>> TRIDENT_LIGHTNING_STRIKE = ENTITY_TYPES.register("trident_lightning_strike", () -> EntityType.Builder.<TridentLightningStrikeEntity>of(TridentLightningStrikeEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(32).updateInterval(1).build("trident_lightning_strike")); // 天雷战戟落点雷电实体。
    public static final RegistryObject<EntityType<HeavenlyThunderEntity>> HEAVENLY_THUNDER = ENTITY_TYPES.register("heavenly_thunder", () -> EntityType.Builder.<HeavenlyThunderEntity>of(HeavenlyThunderEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(96).updateInterval(1).build("heavenly_thunder")); // 天雷附魔持续法阵实体。
    public static final RegistryObject<EntityType<ExcaliburChargeEntity>> EXCALIBUR_CHARGE = ENTITY_TYPES.register("excalibur_charge", () -> EntityType.Builder.<ExcaliburChargeEntity>of(ExcaliburChargeEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(96).updateInterval(1).build("excalibur_charge")); // 咖喱棒蓄力同步实体。
    public static final RegistryObject<EntityType<ExcaliburSwordWaveEntity>> EXCALIBUR_SWORD_WAVE = ENTITY_TYPES.register("excalibur_sword_wave", () -> EntityType.Builder.<ExcaliburSwordWaveEntity>of(ExcaliburSwordWaveEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(96).updateInterval(1).build("excalibur_sword_wave")); // EX 咖喱棒剑气控制实体。

    // 注册实体类型表。
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
