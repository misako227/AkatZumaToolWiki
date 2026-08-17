package com.z227.akatzumatool.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.z227.akatzumatool.event.client.AutoTrackingClientHandler;
import com.z227.akatzumatool.event.client.SparklingFruitClientHandler;
import com.z227.akatzumatool.render.finalRender.miaoOutline.MiaoOutlineCaptureOnlyBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// EntityRenderDispatcherMixin 在目标实体的原版渲染阶段额外捕获模型顶点，供屏幕空间描边后处理使用。
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Shadow
    public abstract <E extends Entity> EntityRenderer<? super E> getRenderer(E entity);

    // 在原版实体 renderer 调用前额外执行一次只捕获顶点的渲染，不替换原调用点，降低和其他模组的冲突概率。
    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    )
    public void akatzumatool$captureOutlineVertices(
            Entity entity,
            double x,
            double y,
            double z,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        if (entity.getId() != AutoTrackingClientHandler.getLockedTargetId()
                && !SparklingFruitClientHandler.shouldCaptureSparklingFruitOutline(entity)) return;

        // 捕获渲染只写入 Miao 深度 mask 缓存，不向原始 bufferSource 输出，避免实体被画第二遍。
        MiaoOutlineCaptureOnlyBufferSource captureBufferSource = new MiaoOutlineCaptureOnlyBufferSource(entity.getId());
        EntityRenderer<? super Entity> renderer = getRenderer(entity);
        renderer.render(entity, entityYaw, partialTick, poseStack, captureBufferSource, packedLight);
    }
}
