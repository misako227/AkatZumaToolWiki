package com.z227.akatzumatool.common.render;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// RibbonGeometry 是通用 billboard 条带几何工具，只负责把中心线转换成连续左右边节点。
public class RibbonGeometry {
    public static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D); // 世界上方向，用作退化宽度轴备用。
    public static final Vec3 WORLD_RIGHT = new Vec3(1.0D, 0.0D, 0.0D); // 世界右方向，用作最终退化宽度轴备用。
    public static final Vec3 WORLD_FORWARD = new Vec3(0.0D, 0.0D, 1.0D); // 世界前方向，用作退化路径方向备用。

    // 根据带 t 的中心线构建连续 billboard 条带节点。
    public static List<RibbonPoint> buildBillboardRibbon(List<RibbonCenter> centers, Vec3 cameraPos, double halfWidth) {
        List<RibbonPoint> points = new ArrayList<>();
        if (centers == null || centers.size() < 2 || cameraPos == null) return points;

        // 每个中心点只计算一次 left/right，相邻四边形复用同一对接缝点。
        for (int i = 0; i < centers.size(); i++) {
            points.add(buildPoint(centers, i, cameraPos, halfWidth));
        }
        return points;
    }

    // 构建单个中心点对应的左右边节点。
    public static RibbonPoint buildPoint(List<RibbonCenter> centers, int index, Vec3 cameraPos, double halfWidth) {
        RibbonCenter center = centers.get(index);
        Vec3 point = center.position;
        Vec3 forward = safeNormalize(forwardAt(centers, index), WORLD_FORWARD);
        Vec3 toCamera = safeNormalize(cameraPos.subtract(point), WORLD_FORWARD);
        Vec3 right = forward.cross(toCamera);

        // 当路径方向几乎朝向摄像机时，叉积会退化，需要用世界轴兜底。
        if (right.lengthSqr() < 1.0E-8D) right = forward.cross(WORLD_UP);
        if (right.lengthSqr() < 1.0E-8D) right = forward.cross(WORLD_RIGHT);
        right = safeNormalize(right, WORLD_RIGHT);

        Vec3 halfRight = right.scale(Math.max(0.0D, halfWidth));
        return new RibbonPoint(point.subtract(halfRight), point.add(halfRight), center.t);
    }

    // 计算某个中心点的平滑路径方向，中间点使用前后方向平均来减少接缝折断。
    public static Vec3 forwardAt(List<RibbonCenter> centers, int index) {
        int size = centers.size();
        Vec3 point = centers.get(index).position;
        if (index == 0) {
            return centers.get(1).position.subtract(point);
        }
        if (index == size - 1) {
            return point.subtract(centers.get(index - 1).position);
        }

        Vec3 fromPrev = safeNormalize(point.subtract(centers.get(index - 1).position), WORLD_FORWARD);
        Vec3 toNext = safeNormalize(centers.get(index + 1).position.subtract(point), WORLD_FORWARD);
        Vec3 forward = fromPrev.add(toNext);
        if (forward.lengthSqr() < 1.0E-8D) return toNext.lengthSqr() > 1.0E-8D ? toNext : fromPrev;
        return forward;
    }

    public static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        if (vector == null || vector.lengthSqr() < 1.0E-8D) return fallback;
        return vector.normalize();
    }

    // RibbonCenter 表示条带中心线上的一个点和对应路径进度。
    public static class RibbonCenter {
        public final Vec3 position; // 世界坐标中心点。
        public final float t; // 路径进度，通常用于 UV 的长度方向。

        public RibbonCenter(Vec3 position, float t) {
            this.position = position;
            this.t = t;
        }
    }

    // RibbonPoint 表示中心线点展开后的左右边顶点。
    public static class RibbonPoint {
        public final Vec3 left; // 条带左侧世界坐标点。
        public final Vec3 right; // 条带右侧世界坐标点。
        public final float t; // 路径进度，通常用于 UV 的长度方向。

        public RibbonPoint(Vec3 left, Vec3 right, float t) {
            this.left = left;
            this.right = right;
            this.t = t;
        }
    }
}
