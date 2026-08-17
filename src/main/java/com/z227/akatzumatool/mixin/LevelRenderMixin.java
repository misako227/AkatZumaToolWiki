package com.z227.akatzumatool.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//@Mixin(LevelRenderer.class)
public abstract class LevelRenderMixin {
//    @Shadow
//    @Final
//    private Minecraft minecraft;
//    @Inject(
//            method = "renderLevel",
//            at = @At("RETURN")
//    )
//    private void renderLevelMixin(PoseStack pPoseStack, float pPartialTick,
//                                  long pFinishNanoTime, boolean pRenderBlockOutline,
//                                  Camera pCamera, GameRenderer pGameRenderer,
//                                  LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci){
//        AkatZumaTool.POST.doPostProcessing();
//
//
//    }
}
