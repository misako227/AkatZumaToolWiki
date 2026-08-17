package com.z227.akatzumatool.render.finalRender.miaoOutline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// MiaoOutlineCaptureOnlyVertexConsumer 捕获实体模型的 view-space 顶点、UV 和法线。
public class MiaoOutlineCaptureOnlyVertexConsumer implements VertexConsumer {
    private final MiaoOutlineCapturedBatch batch; // 当前 RenderType 对应的捕获批次。
    private double x; // 当前顶点 X。
    private double y; // 当前顶点 Y。
    private double z; // 当前顶点 Z。
    private float u; // 当前顶点纹理 U。
    private float v; // 当前顶点纹理 V。
    private float normalX; // 当前顶点法线 X。
    private float normalY; // 当前顶点法线 Y。
    private float normalZ; // 当前顶点法线 Z。
    private boolean hasPosition; // 是否已经收到 position。
    private boolean hasUv; // 是否已经收到 UV。
    private boolean hasNormal; // 是否已经收到 normal。

    public MiaoOutlineCaptureOnlyVertexConsumer(MiaoOutlineCapturedBatch batch) {
        this.batch = batch;
    }

    // 记录已经被实体 PoseStack 转换后的顶点坐标。
    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasPosition = true;
        this.hasUv = false;
        this.hasNormal = false;
        return this;
    }

    // 保持与原版 VertexConsumer 矩阵路径一致，避免捕获坐标和正常实体渲染不一致。
    @Override
    public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        Vector3f transformed = matrix.transformPosition(x, y, z, new Vector3f());
        return vertex(transformed.x(), transformed.y(), transformed.z());
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        this.u = u;
        this.v = v;
        this.hasUv = true;
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer normal(float normalX, float normalY, float normalZ) {
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.hasNormal = true;
        return this;
    }

    @Override
    public void endVertex() {
        if (hasPosition) {
            batch.addVertex(x, y, z, u, v, hasUv, normalX, normalY, normalZ, hasNormal);
        }
        hasPosition = false;
        hasUv = false;
        hasNormal = false;
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
    }

    @Override
    public void unsetDefaultColor() {
    }
}
