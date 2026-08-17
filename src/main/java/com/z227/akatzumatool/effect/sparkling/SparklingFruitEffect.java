package com.z227.akatzumatool.effect.sparkling;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

// SparklingFruitEffect 是闪闪果实的有益 Buff 标记，具体能力由事件和网络包实现。
public class SparklingFruitEffect extends MobEffect {
    public static final int COLOR = 0xFFFF44; // Buff 图标显示用亮金色。

    // 创建闪闪果实 Buff。
    public SparklingFruitEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }
}