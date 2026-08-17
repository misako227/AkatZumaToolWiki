package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.EntityUtil;
import com.z227.akatzumatool.item.FlySwordItem;
import com.z227.akatzumatool.network.NetworkRegister;
import com.z227.akatzumatool.network.SwordAuraCastC2SPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvent {
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!(event.getEntity().getMainHandItem().getItem() instanceof FlySwordItem)) return;
        playSwordAuraSound(event.getEntity());
        NetworkRegister.sendToServer(new SwordAuraCastC2SPacket());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity().getMainHandItem().getItem() instanceof FlySwordItem)) return;
        if (event.getLevel().isClientSide()) {
            playSwordAuraSound(event.getEntity());
            return;
        }
        FlySwordItem.trySpawnSwordAura(event.getEntity());
    }

    public static void playSwordAuraSound(Player player) {
        if (player == null || !player.level().isClientSide()) return;
        player.level().playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                AkatZumaTool.SWORD_AURA.get(), SoundSource.PLAYERS, 1.0F, 1.0F, false
        );
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        EntityUtil.tickMovementLock(event.getEntity());
    }
}
