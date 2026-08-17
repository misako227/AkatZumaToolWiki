package com.z227.akatzumatool.entity.coin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 电磁炮光束的空实体渲染器。
 * 实际视觉效果在 PostProcessing 的 bloom 队列里绘制，这里只负责让客户端实体渲染系统有合法 renderer。
 */
public class RailgunBeamEntityRender extends EntityRenderer<RailgunBeamEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkatZumaTool.MODID, "textures/item/coin.png");

    public RailgunBeamEntityRender(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * 主渲染留空，避免 vanilla entity pass 重复绘制光束。
     */
    @Override
    public void render(RailgunBeamEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (AkatZumaTool.POST == null) return;
        CoinBeamClientEffects.triggerOnce(entity, partialTick);
        AkatZumaTool.POST.addBloomTask(entity, poseStack);
    }

    /**
     * EntityRenderer 要求提供纹理位置，即使本 renderer 不实际采样纹理。
     */
    @Override
    public ResourceLocation getTextureLocation(RailgunBeamEntity entity) {
        return TEXTURE;
    }
}
