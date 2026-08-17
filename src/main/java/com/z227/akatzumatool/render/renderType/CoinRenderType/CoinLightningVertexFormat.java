package com.z227.akatzumatool.render.renderType.CoinRenderType;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

// CoinLightningVertexFormat 定义闪电专用顶点格式，把 bloom 颜色随顶点写入 VBO。
public class CoinLightningVertexFormat {
    // 顶点格式字段。
    public static final VertexFormat FORMAT = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
            .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
            .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
            .put("Color", DefaultVertexFormat.ELEMENT_COLOR)
            .put("BloomColor", DefaultVertexFormat.ELEMENT_UV2)
            .build());

    private CoinLightningVertexFormat() {
    }
}
