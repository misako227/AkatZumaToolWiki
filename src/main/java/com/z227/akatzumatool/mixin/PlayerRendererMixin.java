//package com.z227.akatzumatool.mixin;
//
//import net.minecraft.client.renderer.entity.EntityRendererProvider;
//import net.minecraft.client.renderer.entity.player.PlayerRenderer;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(PlayerRenderer.class)
//public abstract class PlayerRendererMixin {
//
//
//
//    @Inject(method = "<init>", at = @At("RETURN"))
//    private void init(EntityRendererProvider.Context pContext, boolean pUseSlimModel, CallbackInfo ci) {
//        System.out.println("PlayerRendererMixin init++++++++++++++++++++++++++++++++");
//        PlayerRenderer playerRenderer = (PlayerRenderer) (Object) this;
//        // 添加你的自定义渲染逻辑
////        playerRenderer.addLayer((RenderLayer)new RibbonTrailLayerPlayer((RenderLayerParent)playerRenderer, pContext.getEntityRenderDispatcher()) );
//    }
//
//}
