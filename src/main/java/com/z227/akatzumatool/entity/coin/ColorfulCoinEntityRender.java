package com.z227.akatzumatool.entity.coin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

// ColorfulCoin 强光束空实体渲染器，实际光束交给后处理队列绘制。
public class ColorfulCoinEntityRender extends EntityRenderer<ColorfulCoinEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkatZumaTool.MODID, "textures/item/coin.png");

    public ColorfulCoinEntityRender(EntityRendererProvider.Context context) {
        super(context);
    }

    // 把强光束实体提交到 bloom 队列，避免 vanilla 实体 pass 重复绘制。
    @Override
    public void render(ColorfulCoinEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (AkatZumaTool.POST == null) return;
        CoinBeamClientEffects.triggerOnce(entity, partialTick);
        AkatZumaTool.POST.addBloomTask(entity, poseStack);
    }

    @Override
    public ResourceLocation getTextureLocation(ColorfulCoinEntity entity) {
        return TEXTURE;
    }
}
