package com.z227.akatzumatool.render.finalRender.bloomQueue;

import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;

import java.util.Map;

// SwordAuraObjModel 缓存 Forge OBJ loader 烘焙后的飞剑剑气模型。
@OnlyIn(Dist.CLIENT)
public class SwordAuraObjModel {
    public static final ResourceLocation MODEL_LOCATION = new ResourceLocation(AkatZumaTool.MODID, "sword_aura_obj"); // 剑气 OBJ 模型 JSON 位置。
    public static final ModelResourceLocation STANDALONE_MODEL_LOCATION = new ModelResourceLocation(MODEL_LOCATION, "standalone"); // 额外模型可能使用的 standalone key。
    public static BakedModel model; // Forge bake 后的 OBJ 模型。

    public SwordAuraObjModel() {
    }

    // 注册额外模型，让 Forge 在模型烘焙阶段加载 OBJ。
    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(MODEL_LOCATION);
    }

    // 从模型 bake 结果中缓存剑气 OBJ 模型。
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        model = models.get(MODEL_LOCATION);
        if (model == null) {
            model = models.get(STANDALONE_MODEL_LOCATION);
        }
    }

    public static BakedModel getModel() {
        return model;
    }

    public static boolean isLoaded() {
        return model != null;
    }
}
