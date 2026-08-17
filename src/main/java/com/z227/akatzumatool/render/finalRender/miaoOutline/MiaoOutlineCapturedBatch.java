package com.z227.akatzumatool.render.finalRender.miaoOutline;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// MiaoOutlineCapturedBatch 保存同一个 RenderType mode 和主纹理下捕获到的实体顶点。
public class MiaoOutlineCapturedBatch {
    public final VertexFormat.Mode mode; // 原始 RenderType 的 primitive mode。
    public final ResourceLocation texture; // 原始 RenderType 的主纹理，解析失败时为 null。
    public final List<MiaoOutlineCapturedVertex> vertices = new ArrayList<>(); // 当前批次捕获到的顶点列表。
    public boolean hasUv; // 当前批次是否捕获过 UV。

    public MiaoOutlineCapturedBatch(VertexFormat.Mode mode, ResourceLocation texture) {
        this.mode = mode;
        this.texture = texture;
        this.hasUv = false;
    }

    // 追加一个捕获顶点，重放阶段会按 primitive mode 展开成 triangles。
    public void addVertex(double x, double y, double z, float u, float v, boolean hasUv,
                          float normalX, float normalY, float normalZ, boolean hasNormal) {
        vertices.add(new MiaoOutlineCapturedVertex(x, y, z, u, v, hasUv, normalX, normalY, normalZ, hasNormal));
        this.hasUv |= hasUv;
    }

    public boolean hasTexturedMask() {
        return texture != null && hasUv;
    }

    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    public List<MiaoOutlineCapturedVertex> getVertices() {
        return Collections.unmodifiableList(vertices);
    }
}
