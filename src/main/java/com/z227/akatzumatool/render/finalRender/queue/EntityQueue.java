package com.z227.akatzumatool.render.finalRender.queue;

import com.z227.akatzumatool.render.finalRender.PostRenderPhase;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

// EntityQueue 是后处理实体队列基类，统一描述队列渲染入口和所属后处理阶段。
public abstract class EntityQueue<T extends Entity> {
    public List<T> entities; // 当前帧待渲染实体列表。
    public boolean activeInFrame; // 当前帧是否已经进入 active 队列。

    public EntityQueue() {
        this.entities = new ArrayList<>();
        this.activeInFrame = false;
    }

    // 添加一个实体到当前队列。
    public void add(T entity) {
        entities.add(entity);
    }

    public abstract void render(MultiBufferSource.BufferSource fboBuffer, Camera camera, float parTick, Matrix4f viewMatrix);

    // 渲染带帧时间的队列，默认兼容旧队列。
    public void render(MultiBufferSource.BufferSource fboBuffer, Camera camera, float parTick, Matrix4f viewMatrix, float frameDeltaSeconds) {
        render(fboBuffer, camera, parTick, viewMatrix);
    }

    // 返回当前队列所属后处理阶段，默认按需要深度测试的世界空间效果处理。
    public PostRenderPhase getPhase() {
        return PostRenderPhase.DEPTH_TESTED_WORLD;
    }
}
