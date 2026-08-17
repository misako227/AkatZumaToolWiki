package com.z227.akatzumatool.render.finalRender.miaoOutline;

import net.minecraft.world.entity.Entity;

// MiaoOutlineTask 保存单个实体本帧的 Miao 描边任务。
public class MiaoOutlineTask {
    public final Entity entity; // 需要写入目标深度 mask 的实体。
    public final MiaoOutlineStyle style; // 当前实体使用的 Miao 描边样式。

    public MiaoOutlineTask(Entity entity, MiaoOutlineStyle style) {
        this.entity = entity;
        this.style = style;
    }
}
