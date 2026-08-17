package com.z227.akatzumatool.render.finalRender.miaoOutline;

import java.util.HashMap;
import java.util.Map;

// MiaoOutlineTargetMaskStore 按实体 ID 保存 Miao 管线捕获到的实体 mask 顶点。
public class MiaoOutlineTargetMaskStore {
    public static final Map<Integer, MiaoOutlineCapturedMaskBuffer> BUFFERS = new HashMap<>(); // 多目标捕获缓存，key 为实体 ID。

    public static MiaoOutlineCapturedMaskBuffer beginCapture(int entityId) {
        MiaoOutlineCapturedMaskBuffer buffer = BUFFERS.computeIfAbsent(entityId, id -> new MiaoOutlineCapturedMaskBuffer());
        buffer.clear();
        return buffer;
    }

    public static MiaoOutlineCapturedMaskBuffer get(int entityId) {
        return BUFFERS.get(entityId);
    }

    public static void clear(int entityId) {
        MiaoOutlineCapturedMaskBuffer buffer = BUFFERS.get(entityId);
        if (buffer != null) {
            buffer.clear();
        }
    }

    public static void clearAll() {
        BUFFERS.clear();
    }
}
