package com.z227.akatzumatool.render.finalRender.task;

// PostRenderQueueType 标识无实体后处理任务应该进入的具体队列。
public enum PostRenderQueueType {
    LIGHTNING, // 无实体闪电队列。
    SHOCKWAVE, // 独立冲击波队列。
    CIRCLE_SHOCKWAVE, // 法阵冲击波队列。
    SMOKE_PARTICLE, // 无实体烟雾粒子队列。
    FLY_SWORD_HELD_MODEL, // 手持飞剑透明模型重放队列。
    GOLDEN_SPIRAL_EFFECT, // 金色三噪声螺旋光效队列。
    EXCALIBUR_SPIRAL // 咖喱棒玩家中心向上螺旋光效队列。
}
