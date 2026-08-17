package com.z227.akatzumatool.common.GLBuffers.instancing;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

// InstanceVBO 管理实例化渲染使用的动态 VBO。
public class InstanceVBO {

    public final InstanceLayout layout; // 每条实例数据的布局。
    public final int maxInstances; // 本 VBO 支持的最大实例数。

    public int vboID = 0; // OpenGL 实例 VBO ID。

    public int vaoID = 0; // 已绑定实例属性的 VAO ID。

    public final FloatBuffer buffer; // 复用的实例数据缓冲，避免每帧分配。

    public InstanceVBO(InstanceLayout layout, int maxInstances) {
        if (layout == null) {
            throw new IllegalArgumentException("实例布局不能为空");
        }
        if (maxInstances <= 0) {
            throw new IllegalArgumentException("maxInstances 必须大于 0");
        }
        this.layout = layout;
        this.maxInstances = maxInstances;
        this.buffer = BufferUtils.createFloatBuffer(maxInstances * layout.strideFloats);
        createVBO();
    }

    // 创建固定容量的实例 VBO。
    public void createVBO() {
        vboID = glGenBuffers();
        long bytes = (long) maxInstances * layout.strideFloats * 4;
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, bytes, GL_STREAM_DRAW);  // 每帧更新用 STREAM_DRAW
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    // 将实例 VBO 的 attribute 附加到指定 VAO 上。
    public void attachTo(int vaoID) {
        if (vaoID == 0) {
            throw new IllegalArgumentException("目标 VAO 不能为 0");
        }
        this.vaoID = vaoID;
        // 每个实例数据的字节总跨度
        int strideBytes = layout.strideFloats * 4;

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBindVertexArray(vaoID);

        for (InstanceLayout.AttrEntry attr : layout.attributes) {
            int offsetBytes = attr.offset * 4;
            glVertexAttribPointer(attr.location, attr.size, GL_FLOAT, false, strideBytes, offsetBytes);
            glVertexAttribDivisor(attr.location, 1);   // 每渲染一个实例，数据更新一次
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    // 使用 float 数组更新实例 VBO，只上传实际实例数量对应的数据。
    public void update(float[] instanceData, int instanceCount) {
        validateInstanceCount(instanceCount);
        int floatCount = instanceCount * layout.strideFloats;
        if (instanceData == null || instanceData.length < floatCount) {
            throw new IllegalArgumentException("实例数组长度不足");
        }
        buffer.clear();
        buffer.put(instanceData, 0, floatCount);
        buffer.flip();

        uploadFlippedBuffer(buffer, floatCount);
    }

    // 使用已写入但未 flip 的 FloatBuffer 更新实例 VBO，方法内部负责 flip。
    public void update(FloatBuffer dataBuffer, int instanceCount) {
        validateInstanceCount(instanceCount);
        int floatCount = instanceCount * layout.strideFloats;
        if (dataBuffer == null || dataBuffer.position() < floatCount) {
            throw new IllegalArgumentException("实例 FloatBuffer 写入数量不足");
        }
        dataBuffer.flip();
        uploadFlippedBuffer(dataBuffer, floatCount);
    }

    // 上传已经 flip 的缓冲区，空实例时只刷新 VBO 存储不写入数据。
    public void uploadFlippedBuffer(FloatBuffer dataBuffer, int floatCount) {
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) maxInstances * layout.strideFloats * 4, GL_STREAM_DRAW);
        if (floatCount > 0) {
            dataBuffer.limit(floatCount);
            glBufferSubData(GL_ARRAY_BUFFER, 0, dataBuffer);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    // 检查本帧实例数量是否落在 VBO 容量范围内。
    public void validateInstanceCount(int instanceCount) {
        if (instanceCount < 0) {
            throw new IllegalArgumentException("instanceCount 不能小于 0");
        }
        if (instanceCount > maxInstances) {
            throw new IllegalArgumentException("instanceCount 不能超过 maxInstances");
        }
    }

    // 返回复用的 FloatBuffer，调用方写入后直接传给 update。
    public FloatBuffer getBuffer() {
        return buffer;
    }

    // 绑定 VAO 并启用默认基础 attribute 0 和所有实例 attribute。
    public void enable() {
        glBindVertexArray(vaoID);
        // 启用基础顶点属性（通常 location=0）
        glEnableVertexAttribArray(0);
        // 启用所有实例属性
        for (InstanceLayout.AttrEntry attr : layout.attributes) {
            glEnableVertexAttribArray(attr.location);
        }
    }

    // 绑定 VAO 并启用指定基础 attribute 和所有实例 attribute。
    public void enable(int... baseAttribs) {
        glBindVertexArray(vaoID);
        for (int loc : baseAttribs) {
            glEnableVertexAttribArray(loc);
        }
        for (InstanceLayout.AttrEntry attr : layout.attributes) {
            glEnableVertexAttribArray(attr.location);
        }
    }

    // 禁用默认基础 attribute 0 和所有实例 attribute。
    public void disable() {
        // 禁用实例属性
        for (InstanceLayout.AttrEntry attr : layout.attributes) {
            glDisableVertexAttribArray(attr.location);
        }
        // 禁用基础顶点属性
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    // 禁用指定基础 attribute 和所有实例 attribute。
    public void disable(int... baseAttribs) {
        for (InstanceLayout.AttrEntry attr : layout.attributes) {
            glDisableVertexAttribArray(attr.location);
        }
        for (int loc : baseAttribs) {
            glDisableVertexAttribArray(loc);
        }
        glBindVertexArray(0);
    }

    // 删除实例 VBO。
    public void cleanup() {
        if (vboID != 0) {
            glDeleteBuffers(vboID);
            vboID = 0;
        }
    }
}
