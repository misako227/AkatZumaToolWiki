package com.z227.akatzumatool.event.client;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.event.ForgeEvent;
import com.z227.akatzumatool.item.FlySwordItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SwordAuraClientEvent {
    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() || event.isCanceled()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        if (!(minecraft.player.getMainHandItem().getItem() instanceof FlySwordItem)) return;
        if (minecraft.hitResult == null || minecraft.hitResult.getType() == HitResult.Type.MISS) return;
        ForgeEvent.playSwordAuraSound(minecraft.player);
    }
}
