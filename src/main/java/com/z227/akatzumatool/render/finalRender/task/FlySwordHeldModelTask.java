package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.item.FlySwordHeldItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import org.joml.Matrix4f;

// FlySwordHeldModelTask 保存一次手持飞剑透明模型后处理重放任务。
public class FlySwordHeldModelTask implements PostRenderTask {
    public final BakedModel model; // 当前手持飞剑使用的 3D baked model。
    public final Matrix4f modelViewMatrix; // item renderer 阶段真实模型视图矩阵。
    public final boolean plusSword; // 是否是真·飞剑。
    public final long gameTime; // 提交时客户端世界 tick。
    public final FlySwordHeldItemRenderer.FlySwordFlowParams flowParams; // 当前物品栈稳定的双噪声流动参数。

    public FlySwordHeldModelTask(BakedModel model, Matrix4f modelViewMatrix,
                                 boolean plusSword, long gameTime, FlySwordHeldItemRenderer.FlySwordFlowParams flowParams) {
        this.model = model;
        this.modelViewMatrix = modelViewMatrix;
        this.plusSword = plusSword;
        this.gameTime = gameTime;
        this.flowParams = flowParams;
    }

    public PostRenderQueueType queueType() {
        return PostRenderQueueType.FLY_SWORD_HELD_MODEL;
    }
}
