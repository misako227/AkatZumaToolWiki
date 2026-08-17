package com.z227.akatzumatool.render.finalRender.miaoOutline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

// MiaoOutlineCaptureOnlyBufferSource 只复制实体原版渲染顶点，不向真实画面输出。
public class MiaoOutlineCaptureOnlyBufferSource implements MultiBufferSource {
    private final MiaoOutlineCapturedMaskBuffer maskBuffer; // 当前实体本帧捕获缓存。

    public MiaoOutlineCaptureOnlyBufferSource(int entityId) {
        this.maskBuffer = MiaoOutlineTargetMaskStore.beginCapture(entityId);
    }

    // 按原 RenderType mode 和主纹理创建捕获批次。
    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        Optional<ResourceLocation> texture = MiaoOutlineRenderTypeTextureResolver.resolve(renderType);
        if (!MiaoOutlineRenderTypeFilter.shouldCapture(renderType, texture)) {
            return MiaoOutlineDiscardVertexConsumer.INSTANCE;
        }
        MiaoOutlineCapturedBatch batch = maskBuffer.beginBatch(renderType.mode(), texture.orElse(null));
        return new MiaoOutlineCaptureOnlyVertexConsumer(batch);
    }
}
