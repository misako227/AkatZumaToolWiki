package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.config.ConfigFile;
import com.z227.akatzumatool.network.NetworkRegister;
import com.z227.akatzumatool.network.WhitelistSyncS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

// PlayerJoinHandler 处理玩家加入时的客户端白名单同步。
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerJoinHandler {
    // 玩家登录后同步服务端生效的实体伤害白名单。
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        if (ConfigFile.entityDamageWhitelistMap().isEmpty()) {
            ForgeGameEvent.rebuildEntityDamageWhitelistMap();
        }
        NetworkRegister.sendToPlayer(new WhitelistSyncS2CPacket(new ArrayList<>(ConfigFile.entityDamageWhitelistMap().keySet())), serverPlayer);
    }
}
