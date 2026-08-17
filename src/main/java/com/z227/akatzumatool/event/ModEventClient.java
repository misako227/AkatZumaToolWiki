package com.z227.akatzumatool.event;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.entity.FlySwordEntityRender;
import com.z227.akatzumatool.entity.bow.MagicArrowRenderer;
import com.z227.akatzumatool.entity.bow.MagicBowParticleEffectRenderer;
import com.z227.akatzumatool.entity.coin.ColorfulCoinEntityRender;
import com.z227.akatzumatool.entity.coin.RailgunBeamEntityRender;
import com.z227.akatzumatool.entity.sword.BattoSlashRenderer;
import com.z227.akatzumatool.entity.sword.DimensionSlashDomainRenderer;
import com.z227.akatzumatool.entity.sword.DimensionSlashStrikeRenderer;
import com.z227.akatzumatool.entity.sword.ExcaliburChargeRenderer;
import com.z227.akatzumatool.entity.sword.ExcaliburSwordWaveRenderer;
import com.z227.akatzumatool.entity.sword.SwordAuraRenderer;
import com.z227.akatzumatool.entity.trident.HeavenlyThunderRenderer;
import com.z227.akatzumatool.entity.trident.TridentLightningStrikeRenderer;
import com.z227.akatzumatool.entity.trident.TridentPlusEntityRenderer;
import com.z227.akatzumatool.item.FlySwordBakedModel;
import com.z227.akatzumatool.item.TridentPlusBakedModel;
import com.z227.akatzumatool.render.finalRender.bloomQueue.SwordAuraObjModel;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

// ModEventClient 负责客户端渲染器、模型和物品属性注册。
@Mod.EventBusSubscriber(modid = AkatZumaTool.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventClient {
    // 注册客户端实体渲染器。
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientEvent(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(EntityTypeRegister.FLY_SWORD_ENTITY.get(), FlySwordEntityRender::new);
            EntityRenderers.register(EntityTypeRegister.RAILGUN_BEAM_ENTITY.get(), RailgunBeamEntityRender::new);
            EntityRenderers.register(EntityTypeRegister.COLORFUL_COIN_ENTITY.get(), ColorfulCoinEntityRender::new);
            EntityRenderers.register(EntityTypeRegister.MAGIC_ARROW_ENTITY.get(), MagicArrowRenderer::new);
            EntityRenderers.register(EntityTypeRegister.MAGIC_BOW_PARTICLE_EFFECT_ENTITY.get(), MagicBowParticleEffectRenderer::new);
            EntityRenderers.register(EntityTypeRegister.SWORD_AURA_ENTITY.get(), SwordAuraRenderer::new);
            EntityRenderers.register(EntityTypeRegister.DIMENSION_SLASH_DOMAIN.get(), DimensionSlashDomainRenderer::new);
            EntityRenderers.register(EntityTypeRegister.DIMENSION_SLASH_STRIKE.get(), DimensionSlashStrikeRenderer::new);
            EntityRenderers.register(EntityTypeRegister.BATTO_SLASH.get(), BattoSlashRenderer::new);
            EntityRenderers.register(EntityTypeRegister.TRIDENT_PLUS_ENTITY.get(), TridentPlusEntityRenderer::new);
            EntityRenderers.register(EntityTypeRegister.TRIDENT_LIGHTNING_STRIKE.get(), TridentLightningStrikeRenderer::new);
            EntityRenderers.register(EntityTypeRegister.HEAVENLY_THUNDER.get(), HeavenlyThunderRenderer::new);
            EntityRenderers.register(EntityTypeRegister.EXCALIBUR_CHARGE.get(), ExcaliburChargeRenderer::new);
            EntityRenderers.register(EntityTypeRegister.EXCALIBUR_SWORD_WAVE.get(), ExcaliburSwordWaveRenderer::new);
            registerMagicBowUsePredicates();
            registerTridentPlusUsePredicates();
        });
    }

    // 注册魔法弓拉弓模型谓词。
    @OnlyIn(Dist.CLIENT)
    public static void registerMagicBowUsePredicates() {
        ItemProperties.register(AkatZumaTool.MAGIC_BOW.get(), new ResourceLocation("pull"), (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            if (entity.getUseItem() != stack) {
                return 0.0F;
            }
            return (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
        });
        ItemProperties.register(AkatZumaTool.MAGIC_BOW.get(), new ResourceLocation("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
        );
    }

    // 注册天雷战戟投掷模型谓词。
    @OnlyIn(Dist.CLIENT)
    public static void registerTridentPlusUsePredicates() {
        ItemProperties.register(AkatZumaTool.TRIDENT_PLUS.get(), new ResourceLocation("throwing"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
        );
    }

    // 注册额外模型资源。
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        event.register(new ModelResourceLocation(new ResourceLocation("akatzumatool", "fly_sword_3d"), "inventory"));
        event.register(new ModelResourceLocation(new ResourceLocation("akatzumatool", "fly_sword_plus_3d"), "inventory"));
        event.register(new ModelResourceLocation(new ResourceLocation("akatzumatool", "trident_plus_in_hand"), "inventory"));
        event.register(new ModelResourceLocation(new ResourceLocation("akatzumatool", "trident_plus_throwing"), "inventory"));
        SwordAuraObjModel.registerAdditional(event);
    }

    // 替换飞剑 GUI/手持双模型。
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        SwordAuraObjModel.onModelBake(event);

        ModelResourceLocation itemLoc = new ModelResourceLocation(new ResourceLocation("akatzumatool", "fly_sword"), "inventory");
        ModelResourceLocation item3dLoc = new ModelResourceLocation(new ResourceLocation("akatzumatool", "fly_sword_3d"), "inventory");
        ModelResourceLocation itemPlusLoc = new ModelResourceLocation(new ResourceLocation("akatzumatool", "fly_sword_plus"), "inventory");
        ModelResourceLocation itemPlus3dLoc = new ModelResourceLocation(new ResourceLocation("akatzumatool", "fly_sword_plus_3d"), "inventory");
        ModelResourceLocation tridentPlusLoc = new ModelResourceLocation(new ResourceLocation("akatzumatool", "trident_plus"), "inventory");
//        ModelResourceLocation tridentPlusInHandLoc = new ModelResourceLocation(new ResourceLocation("akatzumatool", "trident_plus_in_hand"), "inventory");
        ModelResourceLocation tridentPlusInHandLoc = ItemRenderer.TRIDENT_IN_HAND_MODEL;
        //        ModelResourceLocation tridentPlusThrowingLoc = new ModelResourceLocation(new ResourceLocation("akatzumatool", "trident_plus_throwing"), "inventory");

        BakedModel guiModel = models.get(itemLoc);
        BakedModel handModel = models.get(item3dLoc);
        BakedModel plusGuiModel = models.get(itemPlusLoc);
        BakedModel plusHandModel = models.get(itemPlus3dLoc);
        BakedModel tridentPlusGuiModel = models.get(tridentPlusLoc);
        BakedModel tridentPlusInHandModel = models.get(tridentPlusInHandLoc);
//        BakedModel tridentPlusThrowingModel = models.get(tridentPlusThrowingLoc);

        if (guiModel == null || handModel == null) {
            return;
        }

        models.put(itemLoc, new FlySwordBakedModel(guiModel, handModel));
        if (plusGuiModel != null && plusHandModel != null) {
            models.put(itemPlusLoc, new FlySwordBakedModel(plusGuiModel, plusHandModel));
        }

        models.put(tridentPlusLoc, new TridentPlusBakedModel(tridentPlusGuiModel, tridentPlusInHandModel));
//        if (tridentPlusGuiModel != null && tridentPlusInHandModel != null) {
//
//            models.put(tridentPlusThrowingLoc, new TridentPlusBakedModel(tridentPlusGuiModel, tridentPlusThrowingModel));
//        }
    }
}
