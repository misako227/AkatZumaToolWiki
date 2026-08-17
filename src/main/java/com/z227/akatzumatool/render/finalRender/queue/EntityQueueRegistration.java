package com.z227.akatzumatool.render.finalRender.queue;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.function.Predicate;

// EntityQueueRegistration 保存实体类型、目标队列、过滤条件和入队动作。
public class EntityQueueRegistration<T extends Entity> {
    public final Class<T> entityClass; // 队列接收的实体类型。
    public final EntityQueue<T> queue; // 实体对应的渲染队列。
    public final Predicate<T> filter; // 入队前过滤条件。
    public final QueueAddAction<T> addAction; // 实际入队动作。

    // 创建一个实体队列注册项。
    public EntityQueueRegistration(Class<T> entityClass, EntityQueue<T> queue, Predicate<T> filter, QueueAddAction<T> addAction) {
        this.entityClass = entityClass;
        this.queue = queue;
        this.filter = filter;
        this.addAction = addAction;
    }

    // 尝试把实体加入队列，成功入队才返回 true。
    public boolean tryAdd(Entity entity, PoseStack pose, Matrix4f modelViewMatrix) {
        if (!entityClass.isInstance(entity)) return false;
        T typedEntity = entityClass.cast(entity);
        if (!filter.test(typedEntity)) return false;
        addAction.add(queue, typedEntity, pose, modelViewMatrix);
        return true;
    }
}
