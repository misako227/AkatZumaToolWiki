package com.z227.akatzumatool.render.renderType.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class RibbonTrailLayerPlayer extends RenderLayer<LivingEntity, EntityModel<LivingEntity>> {

    static ResourceLocation TRAIL_TEXTURE = new ResourceLocation(AkatZumaTool.MODID, "textures/entity/tail_light.png");
    private static final RenderType TRAIL_RENDER_TYPE = RenderType.beaconBeam(TRAIL_TEXTURE, true);

    public RibbonTrailLayerPlayer(RenderLayerParent<LivingEntity, EntityModel<LivingEntity>> pRenderer, EntityRenderDispatcher entityRenderDispatcher) {
        super(pRenderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       LivingEntity player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
//        if (player != Minecraft.getInstance().player) return;


        poseStack.popPose();
    }

    private static Vec3 rotateYWorldToLocal(Vec3 vec, float cos, float sin) {
        return new Vec3(
                cos * vec.x + sin * vec.z,
                -vec.y,
                -sin * vec.x + cos * vec.z
        );
    }
}
