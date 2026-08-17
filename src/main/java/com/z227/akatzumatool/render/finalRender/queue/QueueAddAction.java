package com.z227.akatzumatool.render.finalRender.queue;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

// QueueAddAction 是实体队列的入队动作接口，用于承载普通 add 和特殊矩阵更新逻辑。
@FunctionalInterface
public interface QueueAddAction<T extends Entity> {
    // 执行实体入队逻辑，必要时可以使用当前 pose 和模型视图矩阵。
    public void add(EntityQueue<T> queue, T entity, PoseStack pose, Matrix4f modelViewMatrix);
}
