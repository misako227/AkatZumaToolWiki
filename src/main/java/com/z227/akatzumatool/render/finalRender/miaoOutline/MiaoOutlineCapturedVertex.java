package com.z227.akatzumatool.render.finalRender.miaoOutline;

// MiaoOutlineCapturedVertex 保存原版实体渲染阶段复制出的 view-space 顶点、UV 和法线。
public class MiaoOutlineCapturedVertex {
    public final double x; // view-space 顶点 X。
    public final double y; // view-space 顶点 Y。
    public final double z; // view-space 顶点 Z。
    public final float u; // 实体原纹理 U。
    public final float v; // 实体原纹理 V。
    public final boolean hasUv; // 当前顶点是否带有可用 UV。
    public final float normalX; // 捕获到的 view-space 法线 X。
    public final float normalY; // 捕获到的 view-space 法线 Y。
    public final float normalZ; // 捕获到的 view-space 法线 Z。
    public final boolean hasNormal; // 当前顶点是否带有可用法线。

    public MiaoOutlineCapturedVertex(double x, double y, double z, float u, float v, boolean hasUv,
                                     float normalX, float normalY, float normalZ, boolean hasNormal) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
        this.hasUv = hasUv;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.hasNormal = hasNormal;
    }
}
