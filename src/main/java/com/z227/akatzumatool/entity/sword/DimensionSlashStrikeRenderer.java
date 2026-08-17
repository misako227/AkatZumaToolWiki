package com.z227.akatzumatool.entity.sword;

import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

// DimensionSlashStrikeRenderer 是次元斩连续斩击空渲染器，实际光刃交给 bloom 队列绘制。
public class DimensionSlashStrikeRenderer extends EntityRenderer<DimensionSlashStrikeEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(AkatZumaTool.MODID, "textures/item/fly_sword.png"); // 空渲染器占位纹理。

    public DimensionSlashStrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    // 把连续斩击实体提交到 bloom 队列。
    @Override
    public void render(DimensionSlashStrikeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (AkatZumaTool.POST == null) return;
        AkatZumaTool.POST.addBloomTask(entity, poseStack);
    }

    @Override
    public ResourceLocation getTextureLocation(DimensionSlashStrikeEntity entity) {
        return TEXTURE;
    }
}
