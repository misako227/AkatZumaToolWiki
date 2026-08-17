package com.z227.akatzumatool.entity.bow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

// MagicBowParticleEffectRenderer 负责提交星辰裁决 bloom 任务，其它粒子效果仍由实体 tick 驱动。
public class MagicBowParticleEffectRenderer extends EntityRenderer<MagicBowParticleEffectEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(AkatZumaTool.MODID, "textures/item/coin.png"); // 空模型渲染器仍需返回一个合法纹理。

    public MagicBowParticleEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MagicBowParticleEffectEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (AkatZumaTool.POST != null
                && entity.isStarJudgementVisual()
                && entity.isStarJudgementVisualInRange(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())) {
            AkatZumaTool.POST.addBloomTask(entity, poseStack);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(MagicBowParticleEffectEntity entity) {
        return TEXTURE;
    }
}
