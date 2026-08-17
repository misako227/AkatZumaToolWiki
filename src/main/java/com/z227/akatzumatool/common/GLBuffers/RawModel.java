package com.z227.akatzumatool.common.GLBuffers;


/**
 * 模型类 —— VAO 数据容器
 * 封装一个 VAO 的 ID 和顶点数量，供渲染器使用
 */
// 模型类,只包含模型
public class RawModel {
    int vaoID;
    int vertexCount;

    public RawModel(int vaoID, int vertexCount) {
        this.vaoID = vaoID;
        this.vertexCount = vertexCount;
    }

    public int getVaoID() {
        return vaoID;
    }

    public int getVertexCount() {
        return vertexCount;
    }
}
