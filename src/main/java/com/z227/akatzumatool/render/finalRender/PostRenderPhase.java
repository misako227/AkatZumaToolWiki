package com.z227.akatzumatool.render.finalRender;

// PostRenderPhase 描述后处理队列需要的统一渲染阶段，PostProcessing 按阶段设置 GL 状态。
public enum PostRenderPhase {
    DEPTH_TESTED_WORLD, // 需要使用主场景深度测试的世界空间效果。
    ALWAYS_VISIBLE_WORLD, // 不受场景深度遮挡的世界空间常显效果。
    SCREEN_MASK, // 写入屏幕空间 mask 的阶段。
    SCREEN_SPACE // 不需要深度测试的全屏后处理阶段。
}
