package com.z227.akatzumatool.common.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

// TrailRibbonRenderer 负责飞剑拖尾顶点写入，条带几何计算复用 RibbonGeometry。
public class TrailRibbonRenderer {

    public static final float HALF_WIDTH = 0.15f; // 飞剑拖尾半宽。

    // 渲染飞剑拖尾 ribbon，对外接口保持不变，内部只替换连续条带几何计算。
    public static void render(List<Vec3> points, VertexConsumer buffer,
                              Matrix4f matrix, Vec3 cameraPos, Vec3 offset) {
        int size = points.size();
        if (size < 2) return;

        List<RibbonGeometry.RibbonCenter> centers = buildCenters(points, offset);
        List<RibbonGeometry.RibbonPoint> ribbonPoints = RibbonGeometry.buildBillboardRibbon(centers, cameraPos, HALF_WIDTH);
        if (ribbonPoints.size() < 2) return;

        // RibbonGeometry 返回世界坐标，飞剑拖尾原流程写入相对相机坐标，因此这里保持减 cameraPos。
        for (int i = 0; i < ribbonPoints.size() - 1; i++) {
            RibbonGeometry.RibbonPoint start = ribbonPoints.get(i);
            RibbonGeometry.RibbonPoint end = ribbonPoints.get(i + 1);
            putVertex(buffer, matrix, start.left.subtract(cameraPos), 0.0f, start.t);
            putVertex(buffer, matrix, start.right.subtract(cameraPos), 1.0f, start.t);
            putVertex(buffer, matrix, end.right.subtract(cameraPos), 1.0f, end.t);
            putVertex(buffer, matrix, end.left.subtract(cameraPos), 0.0f, end.t);
        }
    }

    // 将飞剑拖尾路径点转换为通用 ribbon 中心线，offset 保持旧实现语义。
    public static List<RibbonGeometry.RibbonCenter> buildCenters(List<Vec3> points, Vec3 offset) {
        List<RibbonGeometry.RibbonCenter> centers = new ArrayList<>();
        int size = points.size();
        if (size == 0) return centers;

        // t 沿拖尾长度从 0 到 1，继续用于原 shader 的 UV 与颜色参数。
        for (int i = 0; i < size; i++) {
            float t = size == 1 ? 0.0F : (float) i / (float) (size - 1);
            centers.add(new RibbonGeometry.RibbonCenter(points.get(i).add(offset), t));
        }
        return centers;
    }

    public static void putVertex(VertexConsumer buffer, Matrix4f matrix, Vec3 point, float u, float t) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(u, t, 0.5f, t)
                .uv(u, t)
                .endVertex();
    }
}
