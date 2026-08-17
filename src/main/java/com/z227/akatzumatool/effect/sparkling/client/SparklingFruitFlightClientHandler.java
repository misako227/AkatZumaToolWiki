package com.z227.akatzumatool.effect.sparkling.client;

// SparklingFruitFlightClientHandler 在客户端网络回调中应用闪闪果实加速飞行状态。
public class SparklingFruitFlightClientHandler {
    // 应用服务端同步的开始或停止状态。
    public static void apply(int entityId, boolean boosting, boolean horizontalPose, double maxSpeed) {
        SparklingFruitFlightClientState.apply(entityId, boosting, horizontalPose, maxSpeed);
    }
}
