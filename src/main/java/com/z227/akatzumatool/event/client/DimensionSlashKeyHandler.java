package com.z227.akatzumatool.event.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DimensionSlashKeyHandler {
    public static final KeyMapping DIMENSION_SLASH_KEY = new KeyMapping(
            "key.akatzumatool.dimension_slash",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.akatzumatool"
    );

    public static final KeyMapping SUMMON_FLY_SWORD_KEY = new KeyMapping(
            "key.akatzumatool.summon_fly_sword",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.akatzumatool"
    );

    public static final KeyMapping EXCALIBUR_KEY = new KeyMapping(
            "key.akatzumatool.excalibur",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.akatzumatool"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DIMENSION_SLASH_KEY);
        event.register(SUMMON_FLY_SWORD_KEY);
        event.register(EXCALIBUR_KEY);
    }
}
