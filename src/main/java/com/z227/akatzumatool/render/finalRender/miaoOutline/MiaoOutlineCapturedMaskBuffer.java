package com.z227.akatzumatool.render.finalRender.miaoOutline;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// MiaoOutlineCapturedMaskBuffer 保存单个实体本帧捕获出的多个 RenderType 顶点批次。
public class MiaoOutlineCapturedMaskBuffer {
    public final List<MiaoOutlineCapturedBatch> batches = new ArrayList<>(); // 按原始 RenderType 拆分的捕获批次。

    public void clear() {
        batches.clear();
    }

    // 创建新的捕获批次，保留 primitive mode 与主纹理供 mask 重放使用。
    public MiaoOutlineCapturedBatch beginBatch(VertexFormat.Mode mode, ResourceLocation texture) {
        MiaoOutlineCapturedBatch batch = new MiaoOutlineCapturedBatch(mode, texture);
        batches.add(batch);
        return batch;
    }

    public boolean isEmpty() {
        for (MiaoOutlineCapturedBatch batch : batches) {
            if (!batch.isEmpty()) return false;
        }
        return true;
    }

    public List<MiaoOutlineCapturedBatch> getBatches() {
        return Collections.unmodifiableList(batches);
    }
}
