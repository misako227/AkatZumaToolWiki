package com.z227.akatzumatool.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.List;

// ConfigFile 管理模组通用配置项，并向业务代码提供静态读取入口。
public class ConfigFile {
    // Forge 配置规格。
    public static final ForgeConfigSpec CONFIG_SPEC;

    // 通用配置项。
    private static final ForgeConfigSpec.BooleanValue CAN_BREAK_BLOCK;
    private static final ForgeConfigSpec.BooleanValue DAMAGE_PLAYERS;

    // 飞剑配置项。
    private static final ForgeConfigSpec.IntValue FLY_SWORD_ATTACK_DAMAGE;
    private static final ForgeConfigSpec.IntValue FLY_SWORD_SEARCH_RANGE;
    private static final ForgeConfigSpec.BooleanValue FLY_SWORD_ATTACK_ALL_ENTITIES; // 飞剑是否攻击所有非白名单生物配置。
    private static final ForgeConfigSpec.DoubleValue FLY_SWORD_AURA_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue FLY_SWORD_AURA_SPEED;
    private static final ForgeConfigSpec.IntValue FLY_SWORD_AURA_LIFE_TICKS;
    private static final ForgeConfigSpec.DoubleValue FLY_SWORD_AURA_HIT_RADIUS;
    private static final ForgeConfigSpec.IntValue FLY_SWORD_DIMENSION_SLASH_COOLDOWN;
    private static final ForgeConfigSpec.DoubleValue FLY_SWORD_DIMENSION_SLASH_SMALL_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue FLY_SWORD_DIMENSION_SLASH_FINAL_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue FLY_SWORD_BATTO_SLASH_DAMAGE; // 拔刀斩范围伤害配置。
    private static final ForgeConfigSpec.DoubleValue FLY_SWORD_BATTO_SLASH_RADIUS; // 拔刀斩水平范围配置。
    private static final ForgeConfigSpec.IntValue FLY_SWORD_BATTO_SLASH_CHARGE_TICKS; // 拔刀斩蓄力 tick 配置。

    // 普通硬币配置项。
    private static final ForgeConfigSpec.DoubleValue COIN_BEAM_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue COIN_MAX_RANGE;

    // 彩色硬币配置项。
    private static final ForgeConfigSpec.DoubleValue COLORFUL_COIN_BEAM_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue COLORFUL_COIN_MAX_RANGE;
    private static final ForgeConfigSpec.IntValue COLORFUL_COIN_FULL_CHARGE_TIME;
    private static final ForgeConfigSpec.BooleanValue COLORFUL_COIN_QUICK_CHARGE_ENABLED;
    private static final ForgeConfigSpec.DoubleValue COLORFUL_COIN_QUICK_CHARGE_REDUCTION;

    // 闪闪果实配置项。
    private static final ForgeConfigSpec.IntValue SPARKLING_FRUIT_BUFF_DURATION_TICKS; // 闪闪果实 Buff 持续 tick 配置。
    private static final ForgeConfigSpec.IntValue SPARKLING_FRUIT_SPEED_AMPLIFIER; // 闪闪果实速度效果等级配置。
    private static final ForgeConfigSpec.IntValue SPARKLING_FRUIT_JUMP_AMPLIFIER; // 闪闪果实跳跃效果等级配置。
    private static final ForgeConfigSpec.DoubleValue SPARKLING_FRUIT_TELEPORT_DISTANCE; // 闪闪果实瞬移距离配置。
    private static final ForgeConfigSpec.IntValue SPARKLING_FRUIT_TELEPORT_COOLDOWN_TICKS; // 闪闪果实瞬移冷却配置。
    private static final ForgeConfigSpec.DoubleValue SPARKLING_FRUIT_FLIGHT_BOOST_MAX_SPEED; // 闪闪果实 Ctrl 飞行最大速度配置。
    private static final ForgeConfigSpec.IntValue SPARKLING_FRUIT_FLIGHT_BOOST_ACCELERATION_TICKS; // 闪闪果实 Ctrl 飞行加速时间配置。

    // 实体伤害白名单配置项。
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_DAMAGE_WHITELIST;

    // 服务端实体伤害白名单缓存。
    private static volatile HashMap<String, Boolean> entityDamageWhitelistMap = new HashMap<>();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("AkatZumaTool settings").comment("AkatZumaTool 配置").push("setting");

        CAN_BREAK_BLOCK = builder
                .comment("can break terrain blocks, It will not damage FTB Chunks.")
                .comment("是否破坏地形方块，不会破坏FTB Chunks。")
                .define("canBreakBlock", true);
        DAMAGE_PLAYERS = builder
                .comment("can damage player entities.")
                .comment("是否对玩家实体造成伤害。")
                .define("damagePlayers", false);
        ENTITY_DAMAGE_WHITELIST = builder
                .comment("Entity damage whitelist settings. It will not cause damage to creatures on the whitelist, You can use the CraftTweaker mod to view the entity registry names.")
                .comment("实体伤害白名单配置，不会对白名单列表中的生物造成伤害，可使用CraftTweaker模组查看实体注册名。")
                .defineList("whitelist", List.of("touhou_little_maid:maid", "minecraft:cat"), value -> value instanceof String);

        builder.comment("Fly sword settings.")
                .comment("飞剑配置。")
                .push("flySword");
        FLY_SWORD_ATTACK_DAMAGE = builder
                .comment("Damage dealt by each fly sword hit.")
                .comment("飞剑每次命中的伤害值。")
                .defineInRange("attackDamage", 2, 0, Integer.MAX_VALUE);
        FLY_SWORD_SEARCH_RANGE = builder
                .comment("Target search range of fly sword.")
                .comment("飞剑搜索目标的范围。")
                .defineInRange("searchRange", 16, 6, 160);
        FLY_SWORD_ATTACK_ALL_ENTITIES = builder
                .comment("Whether fly swords attack all living entities except the damage whitelist. Disabled only attacks monsters.")
                .comment("飞剑是否攻击除伤害白名单外的所有生物，关闭时只攻击怪物。")
                .define("attackAllEntities", false);
        FLY_SWORD_AURA_DAMAGE = builder
                .comment("Damage dealt by each sword aura hit tick.")
                .comment("飞剑剑气每次命中 tick 造成的伤害值。")
                .defineInRange("swordAuraDamage", 2.0D, 0.0D, Double.MAX_VALUE);
        FLY_SWORD_AURA_SPEED = builder
                .comment("Sword aura movement speed in blocks per tick.")
                .comment("飞剑剑气每 tick 前进的方块距离。")
                .defineInRange("swordAuraSpeed", 2.0D, 0.0D, 16.0D);
        FLY_SWORD_AURA_LIFE_TICKS = builder
                .comment("Sword aura lifetime in ticks.")
                .comment("飞剑剑气存在的 tick 数。")
                .defineInRange("swordAuraLifeTicks", 18, 1, 200);
        FLY_SWORD_AURA_HIT_RADIUS = builder
                .comment("Sword aura hit radius around its swept movement segment.")
                .comment("飞剑剑气移动线段周围的命中半径。")
                .defineInRange("swordAuraHitRadius", 2.0D, 0.0D, 16.0D);
        FLY_SWORD_DIMENSION_SLASH_COOLDOWN = builder
                .comment("Dimension slash cooldown in ticks. Current input cooldown is client-side 200 ticks.")
                .comment("次元斩冷却 tick。当前按键冷却按需求固定为客户端 200 tick。")
                .defineInRange("dimensionSlashCooldown", 200, 1, Integer.MAX_VALUE);
        FLY_SWORD_DIMENSION_SLASH_SMALL_DAMAGE = builder
                .comment("Damage dealt by each dimension slash small strike.")
                .comment("次元斩连续斩击每次小伤害。")
                .defineInRange("dimensionSlashSmallDamage", 2.0D, 0.0D, Double.MAX_VALUE);
        FLY_SWORD_DIMENSION_SLASH_FINAL_DAMAGE = builder
                .comment("Damage dealt by dimension slash final shatter.")
                .comment("次元斩终结破碎伤害。")
                .defineInRange("dimensionSlashFinalDamage", 200.0D, 0.0D, Double.MAX_VALUE);
        FLY_SWORD_BATTO_SLASH_DAMAGE = builder
                .comment("Damage dealt by batto slash.")
                .comment("拔刀斩范围伤害。")
                .defineInRange("battoSlashDamage", 30.0D, 0.0D, Double.MAX_VALUE);
        FLY_SWORD_BATTO_SLASH_RADIUS = builder
                .comment("Horizontal radius of batto slash damage.")
                .comment("拔刀斩水平伤害范围。")
                .defineInRange("battoSlashRadius", 25.0D, 0.0D, 2560.0D);
        FLY_SWORD_BATTO_SLASH_CHARGE_TICKS = builder
                .comment("Ticks required before batto slash is released.")
                .comment("拔刀斩释放前需要蓄力的 tick 数。")
                .defineInRange("battoSlashChargeTicks", 20, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Coin item settings.")
                .comment("普通硬币配置。")
                .push("coin");
        COIN_BEAM_DAMAGE = builder
                .comment("Base damage of coin beam.")
                .comment("普通硬币光束基础伤害。")
                .defineInRange("beamDamage", 20.0D, 0.0D, Double.MAX_VALUE);
        COIN_MAX_RANGE = builder
                .comment("Maximum range of coin beam.")
                .comment("普通硬币光束最大射程。")
                .defineInRange("maxRange", 50.0D, 0.0D, 10000.0D);
        builder.pop();

        builder.comment("Colorful coin item settings.")
                .comment("彩色硬币配置。")
                .push("colorfulCoin");
        COLORFUL_COIN_BEAM_DAMAGE = builder
                .comment("Base damage of colorful coin beam.")
                .comment("彩色硬币光束基础伤害。")
                .defineInRange("beamDamage", 40.0D, 0.0D, Double.MAX_VALUE);
        COLORFUL_COIN_MAX_RANGE = builder
                .comment("Maximum range of colorful coin beam.")
                .comment("彩色硬币光束最大射程。")
                .defineInRange("maxRange", 60.0D, 0.0D, 10000.0D);

        builder.comment("Colorful coin charge settings.")
                .comment("彩色硬币蓄力配置。")
                .push("charge");
        COLORFUL_COIN_FULL_CHARGE_TIME = builder
                .comment("Ticks required for a full colorful coin charge.")
                .comment("彩色硬币满蓄力所需 tick 数。")
                .defineInRange("fullChargeTime", 50, 1, Integer.MAX_VALUE);
        COLORFUL_COIN_QUICK_CHARGE_ENABLED = builder
                .comment("Whether quick charge enchantment reduces colorful coin charge time.")
                .comment("是否启用快速装填附魔缩短彩色硬币蓄力时间。")
                .define("quickChargeEnabled", true);
        COLORFUL_COIN_QUICK_CHARGE_REDUCTION = builder
                .comment("Charge time reduction ratio per quick charge level.")
                .comment("快速装填每级减少的蓄力时间比例。")
                .defineInRange("quickChargeReduction", 0.20D, 0.0D, 1.0D);
        builder.pop();
        builder.pop();


        builder.comment("Sparkling fruit settings.")
                .comment("闪闪果实配置。")
                .push("sparklingFruit");
        SPARKLING_FRUIT_BUFF_DURATION_TICKS = builder
                .comment("Duration of Sparkling Fruit buff in ticks.")
                .comment("闪闪果实 Buff 持续 tick 数。")
                .defineInRange("buffDurationTicks", 600, 1, Integer.MAX_VALUE);
        SPARKLING_FRUIT_SPEED_AMPLIFIER = builder
                .comment("Movement speed amplifier while Sparkling Fruit buff is active. Vanilla amplifier starts at 0.")
                .comment("闪闪果实 Buff 期间速度效果等级，原版 amplifier 从 0 开始。")
                .defineInRange("speedAmplifier", 4, 4, 255);
        SPARKLING_FRUIT_JUMP_AMPLIFIER = builder
                .comment("Jump boost amplifier while Sparkling Fruit buff is active. Vanilla amplifier starts at 0.")
                .comment("闪闪果实 Buff 期间跳跃效果等级，原版 amplifier 从 0 开始。")
                .defineInRange("jumpAmplifier", 4, 4, 255);
        SPARKLING_FRUIT_TELEPORT_DISTANCE = builder
                .comment("Teleport distance when pressing Alt during Sparkling Fruit buff.")
                .comment("闪闪果实 Buff 期间按 Alt 的瞬移距离。")
                .defineInRange("teleportDistance", 12.0D, 0.0D, 2560.0D);
        SPARKLING_FRUIT_TELEPORT_COOLDOWN_TICKS = builder
                .comment("Cooldown ticks for Alt teleport during Sparkling Fruit buff.")
                .comment("闪闪果实 Buff 期间 Alt 瞬移冷却 tick 数。")
                .defineInRange("teleportCooldownTicks", 6, 1, Integer.MAX_VALUE);
        SPARKLING_FRUIT_FLIGHT_BOOST_MAX_SPEED = builder
                .comment("Maximum flight speed in blocks per tick while holding the sprint key during Sparkling Fruit buff.")
                .comment("闪闪果实 Buff 期间按住疾跑键飞行的最大速度，单位为方块/tick。")
                .defineInRange("flightBoostMaxSpeed", 10.0D, 0.1D, Double.MAX_VALUE);
        SPARKLING_FRUIT_FLIGHT_BOOST_ACCELERATION_TICKS = builder
                .comment("Ticks required to accelerate to maximum Sparkling Fruit flight speed.")
                .comment("闪闪果实疾跑飞行加速到最大速度所需 tick 数。")
                .defineInRange("flightBoostAccelerationTicks", 40, 1, Integer.MAX_VALUE);
        builder.pop();

        TridentPlusConfig.register(builder);
        MagicBowConfig.register(builder);
        ExExcaliburConfig.register(builder);

        builder.pop();
        CONFIG_SPEC = builder.build();
    }

    public static boolean canBreakBlock() {
        return CAN_BREAK_BLOCK.get();
    }

    public static boolean damagePlayers() {
        return DAMAGE_PLAYERS.get();
    }

    public static int flySwordAttackDamage() {
        return FLY_SWORD_ATTACK_DAMAGE.get();
    }

    public static int flySwordSearchRange() {
        return FLY_SWORD_SEARCH_RANGE.get();
    }

    public static boolean flySwordAttackAllEntities() {
        return FLY_SWORD_ATTACK_ALL_ENTITIES.get();
    }

    public static float flySwordAuraDamage() {
        return FLY_SWORD_AURA_DAMAGE.get().floatValue();
    }

    public static double flySwordAuraSpeed() {
        return FLY_SWORD_AURA_SPEED.get();
    }

    public static int flySwordAuraLifeTicks() {
        return FLY_SWORD_AURA_LIFE_TICKS.get();
    }

    public static double flySwordAuraHitRadius() {
        return FLY_SWORD_AURA_HIT_RADIUS.get();
    }

    public static int flySwordDimensionSlashCooldown() {
        return FLY_SWORD_DIMENSION_SLASH_COOLDOWN.get();
    }

    public static float flySwordDimensionSlashSmallDamage() {
        return FLY_SWORD_DIMENSION_SLASH_SMALL_DAMAGE.get().floatValue();
    }

    public static float flySwordDimensionSlashFinalDamage() {
        return FLY_SWORD_DIMENSION_SLASH_FINAL_DAMAGE.get().floatValue();
    }

    public static float flySwordBattoSlashDamage() {
        return FLY_SWORD_BATTO_SLASH_DAMAGE.get().floatValue();
    }

    public static double flySwordBattoSlashRadius() {
        return FLY_SWORD_BATTO_SLASH_RADIUS.get();
    }

    public static int flySwordBattoSlashChargeTicks() {
        return FLY_SWORD_BATTO_SLASH_CHARGE_TICKS.get();
    }

    public static float coinBeamDamage() {
        return COIN_BEAM_DAMAGE.get().floatValue();
    }

    public static double coinMaxRange() {
        return COIN_MAX_RANGE.get();
    }

    public static float colorfulCoinBeamDamage() {
        return COLORFUL_COIN_BEAM_DAMAGE.get().floatValue();
    }

    public static double colorfulCoinMaxRange() {
        return COLORFUL_COIN_MAX_RANGE.get();
    }

    public static int colorfulCoinFullChargeTime() {
        return COLORFUL_COIN_FULL_CHARGE_TIME.get();
    }

    public static boolean colorfulCoinQuickChargeEnabled() {
        return COLORFUL_COIN_QUICK_CHARGE_ENABLED.get();
    }

    public static double colorfulCoinQuickChargeReduction() {
        return COLORFUL_COIN_QUICK_CHARGE_REDUCTION.get();
    }


    public static int sparklingFruitBuffDurationTicks() {
        return SPARKLING_FRUIT_BUFF_DURATION_TICKS.get();
    }

    public static int sparklingFruitSpeedAmplifier() {
        return SPARKLING_FRUIT_SPEED_AMPLIFIER.get();
    }

    public static int sparklingFruitJumpAmplifier() {
        return SPARKLING_FRUIT_JUMP_AMPLIFIER.get();
    }

    public static double sparklingFruitTeleportDistance() {
        return SPARKLING_FRUIT_TELEPORT_DISTANCE.get();
    }

    public static int sparklingFruitTeleportCooldownTicks() {
        return SPARKLING_FRUIT_TELEPORT_COOLDOWN_TICKS.get();
    }

    public static double sparklingFruitFlightBoostMaxSpeed() {
        return SPARKLING_FRUIT_FLIGHT_BOOST_MAX_SPEED.get();
    }

    public static int sparklingFruitFlightBoostAccelerationTicks() {
        return SPARKLING_FRUIT_FLIGHT_BOOST_ACCELERATION_TICKS.get();
    }

    public static List<? extends String> entityDamageWhitelist() {
        return ENTITY_DAMAGE_WHITELIST.get();
    }

    public static HashMap<String, Boolean> entityDamageWhitelistMap() {
        return entityDamageWhitelistMap;
    }

    public static void setEntityDamageWhitelistMap(HashMap<String, Boolean> whitelistMap) {
        entityDamageWhitelistMap = whitelistMap;
    }
}
