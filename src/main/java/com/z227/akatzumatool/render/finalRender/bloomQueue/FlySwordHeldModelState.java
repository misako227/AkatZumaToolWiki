package com.z227.akatzumatool.render.finalRender.bloomQueue;

import net.minecraft.client.resources.model.BakedModel;
import com.z227.akatzumatool.item.FlySwordHeldItemRenderer;
import org.joml.Matrix4f;

// FlySwordHeldModelState 保存一次玩家手中飞剑 item 渲染阶段提交的模型矩阵和材质状态。
public class FlySwordHeldModelState {
    public final BakedModel model; // 当前手持飞剑使用的 3D baked model。
    public final Matrix4f modelViewMatrix; // item renderer 阶段真实模型视图矩阵。
    public final boolean plusSword; // 是否是真·飞剑，用于后续调整颜色或强度。
    public final long submitGameTime; // 提交时客户端世界 tick，用于清理过期状态。
    public final FlySwordHeldItemRenderer.FlySwordFlowParams flowParams; // 当前物品栈稳定的双噪声流动参数。

    public FlySwordHeldModelState(BakedModel model, Matrix4f modelViewMatrix,
                                  boolean plusSword, long submitGameTime, FlySwordHeldItemRenderer.FlySwordFlowParams flowParams) {
        this.model = model;
        this.modelViewMatrix = new Matrix4f(modelViewMatrix);
        this.plusSword = plusSword;
        this.submitGameTime = submitGameTime;
        this.flowParams = flowParams;
    }

    // 判断该手持模型提交是否已经过期。
    public boolean isExpired(long gameTime, long maxAgeTicks) {
        return gameTime - submitGameTime > maxAgeTicks;
    }
}
