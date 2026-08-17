package com.z227.akatzumatool.linkMod.touhouLittleMaid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// MaidSkillCooldowns 是女仆技能冷却管理器，按女仆 UUID 和技能 key 记录到期 tick。
public class MaidSkillCooldowns {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>(); // 女仆技能冷却表。

    // 判断指定女仆技能是否仍在冷却中。
    public static boolean isCoolingDown(EntityMaid maid, String key) {
        Map<String, Long> cooldowns = COOLDOWNS.get(maid.getUUID());
        if (cooldowns == null) return false;
        Long endTick = cooldowns.get(key);
        return endTick != null && maid.level().getGameTime() < endTick;
    }

    // 设置指定女仆技能冷却。
    public static void setCooldown(EntityMaid maid, String key, int ticks) {
        COOLDOWNS.computeIfAbsent(maid.getUUID(), uuid -> new HashMap<>())
                .put(key, maid.level().getGameTime() + ticks);
    }

    // 清理指定女仆的技能冷却记录。
    public static void clear(EntityMaid maid) {
        COOLDOWNS.remove(maid.getUUID());
    }
}
