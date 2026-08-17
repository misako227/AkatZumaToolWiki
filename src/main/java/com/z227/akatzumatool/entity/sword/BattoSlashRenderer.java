package com.z227.akatzumatool.entity.sword;

import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

// BattoSlashRenderer 是拔刀斩的空渲染器，真实刀光交给 bloom 队列绘制。
public class BattoSlashRenderer extends EntityRenderer<BattoSlashEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(AkatZumaTool.MODID, "textures/item/fly_sword.png"); // 空渲染器占位纹理。

    public BattoSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    // 把拔刀斩实体提交到后处理 bloom 队列。
    @Override
    public void render(BattoSlashEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (AkatZumaTool.POST == null) return;
        AkatZumaTool.POST.addBloomTask(entity, poseStack);
        BattoSlashParticleEffects.emitAppearanceParticles(entity, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(BattoSlashEntity entity) {
        return TEXTURE;
    }
}
