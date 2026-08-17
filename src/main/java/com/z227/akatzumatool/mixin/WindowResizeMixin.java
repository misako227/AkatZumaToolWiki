package com.z227.akatzumatool.mixin;

import com.mojang.blaze3d.platform.Window;
import com.z227.akatzumatool.AkatZumaTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowResizeMixin {

    /**
     * 在 Window.onFramebufferResize 方法结束时注入，
     * 触发 PostProcessing 重建所有 FBO。
     */
    @Inject(method = "onFramebufferResize", at = @At("RETURN"))
    private void akatzumatool$onFramebufferResize(long window, int width, int height, CallbackInfo ci) {
        if (AkatZumaTool.POST != null) {
            AkatZumaTool.POST.onFramebufferResize(width, height);
        }
    }
}
