package com.z227.akatzumatool.event.render;


import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.GLBuffers.Loader;
import com.z227.akatzumatool.render.finalRender.PostProcessing;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import com.z227.akatzumatool.render.texture.AtlasReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(value = Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TextureEvent{

    @OnlyIn(value = Dist.CLIENT)
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Loader loader = new Loader();
            AkatZumaTool.POST = new PostProcessing(loader);
        });
    }

    @OnlyIn(value = Dist.CLIENT)
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        AkatZumaTextureAtlas.init();
        event.registerReloadListener(new AtlasReloadListener());
    }


}
