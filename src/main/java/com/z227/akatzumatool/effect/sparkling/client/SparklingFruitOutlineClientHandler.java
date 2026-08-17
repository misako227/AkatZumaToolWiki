package com.z227.akatzumatool.effect.sparkling.client;

// SparklingFruitOutlineClientHandler 在客户端网络线程回调中更新闪闪果实火焰描边缓存。
public class SparklingFruitOutlineClientHandler {
    // 应用服务端同步来的火焰描边状态。
    public static void apply(int entityId, boolean active, int durationTicks) {
        if (active) {
            SparklingFruitOutlineClientState.activate(entityId, durationTicks);
            return;
        }
        SparklingFruitOutlineClientState.deactivate(entityId);
    }
}
