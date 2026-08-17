package com.z227.akatzumatool.event.render;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinBeamShader;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinLightningShader;
import com.z227.akatzumatool.render.renderType.BattoSlashType.BattoSlashShader;
import com.z227.akatzumatool.render.renderType.CircleShockwaveType.CircleShockwaveShader;
import com.z227.akatzumatool.render.renderType.DimensionSlashType.DimensionSlashStrikeShader;
import com.z227.akatzumatool.render.renderType.FlySwordType.FlySwordHeldShader;
import com.z227.akatzumatool.render.renderType.GoldenSpiralType.GoldenSpiralShader;
import com.z227.akatzumatool.render.renderType.MiaoOutlineType.MiaoOutlineDepthMaskShader;
import com.z227.akatzumatool.render.renderType.ShockwaveType.ShockwaveShader;
import com.z227.akatzumatool.render.renderType.SmokeParticleType.SmokeParticleShader;
import com.z227.akatzumatool.render.renderType.StarJudgementCircleType.StarJudgementCircleShader;
import com.z227.akatzumatool.render.renderType.SwordAuraType.SwordAuraShader;
import com.z227.akatzumatool.render.renderType.TrailRibbonType.TrailRibbonShader;
import com.z227.akatzumatool.render.renderType.TridentPlusType.TridentPlusGlowShader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@OnlyIn(value = Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RenderTypeEvent {

    /**
     * 注册所有 Minecraft ShaderInstance shader。
     */
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(TrailRibbonShader.reloadShaders(event.getResourceProvider()), TrailRibbonShader::onLoad);
            event.registerShader(CoinBeamShader.reloadShaders(event.getResourceProvider()), CoinBeamShader::onLoad);
            event.registerShader(CoinLightningShader.reloadShaders(event.getResourceProvider()), CoinLightningShader::onLoad);
            event.registerShader(ShockwaveShader.reloadShaders(event.getResourceProvider()), ShockwaveShader::onLoad);
            event.registerShader(CircleShockwaveShader.reloadShaders(event.getResourceProvider()), CircleShockwaveShader::onLoad);
            event.registerShader(StarJudgementCircleShader.reloadShaders(event.getResourceProvider()), StarJudgementCircleShader::onLoad);
            event.registerShader(SwordAuraShader.reloadShaders(event.getResourceProvider()), SwordAuraShader::onLoad);
            event.registerShader(DimensionSlashStrikeShader.reloadShaders(event.getResourceProvider()), DimensionSlashStrikeShader::onLoad);
            event.registerShader(BattoSlashShader.reloadShaders(event.getResourceProvider()), BattoSlashShader::onLoad);
            event.registerShader(MiaoOutlineDepthMaskShader.reloadShaders(event.getResourceProvider()), MiaoOutlineDepthMaskShader::onLoad);
            event.registerShader(TridentPlusGlowShader.reloadShaders(event.getResourceProvider()), TridentPlusGlowShader::onLoad);
            event.registerShader(FlySwordHeldShader.reloadShaders(event.getResourceProvider()), FlySwordHeldShader::onLoad);
            event.registerShader(SmokeParticleShader.reloadShaders(event.getResourceProvider()), SmokeParticleShader::onLoad);
            event.registerShader(GoldenSpiralShader.reloadShaders(event.getResourceProvider()), GoldenSpiralShader::onLoad);
        } catch (IOException e) {
            throw new RuntimeException("Failed to register custom shader by: " + AkatZumaTool.MODID, e);
        }
    }
}
