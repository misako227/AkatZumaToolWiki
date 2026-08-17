package com.z227.akatzumatool.common.GLBuffers;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * 公告板四边形 VAO 工具类
 *
 * 用于 GPU 粒子系统、后处理、天空盒等只需 2D 四边形的场景。
 * 完全自包含 —— 无需依赖 Loader，直接使用原始 OpenGL 调用。
 *
 * 使用示例：
 * <pre>
 *   QuadVAOUtil.QuadData quad = QuadVAOUtil.create();
 *   // 渲染时：
 *   glBindVertexArray(quad.vaoID);
 *   glEnableVertexAttribArray(0);
 *   glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, instanceCount);
 *   glDisableVertexAttribArray(0);
 *   glBindVertexArray(0);
 *   // 清理时：
 *   QuadVAOUtil.cleanup(quad);
 * </pre>
 *
 * 四边形形状：
 *    (-0.5, 0.5) ──── (0.5, 0.5)
 *        |  \              |
 *        |    \            |
 *    (-0.5,-0.5) ──── (0.5,-0.5)
 *
 * 使用 GL_TRIANGLE_STRIP 绘制，4 个顶点即可构成两个三角形。
 */
public class QuadVAOUtil {

    /** 四边形顶点数据（GL_TRIANGLE_STRIP 顺序） */
    private static final float[] QUAD_VERTICES = {
        -0.5f,  0.5f,   // 左上
        -0.5f, -0.5f,   // 左下
         0.5f,  0.5f,   // 右上
         0.5f, -0.5f    // 右下
    };

    /**
     * 公告板四边形 VAO + VBO 数据封装
     */
    public static class QuadData {
        public final int vaoID;
        public final int vboID;
        public final int vertexCount;

        QuadData(int vaoID, int vboID, int vertexCount) {
            this.vaoID = vaoID;
            this.vboID = vboID;
            this.vertexCount = vertexCount;
        }
    }

    /**
     * 创建一个公告板四边形的 VAO + VBO。
     *
     * 四边形使用 GL_TRIANGLE_STRIP 绘制，只需要 4 个顶点：
     *
     *   (-0.5, 0.5) ──── (0.5, 0.5)
     *       |  \              |
     *       |    \            |
     *   (-0.5,-0.5) ──── (0.5,-0.5)
     *
     * 顶点顺序：(-0.5, 0.5) → (-0.5, -0.5) → (0.5, 0.5) → (0.5, -0.5)
     * 这样用 GL_TRIANGLE_STRIP 即可绘制出两个三角形组成四边形。
     */
    public static QuadData create() {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = BufferUtils.createFloatBuffer(QUAD_VERTICES.length);
        buf.put(QUAD_VERTICES);
        buf.flip();
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        return new QuadData(vao, vbo, 4);
    }

    /**
     * 创建一个不自动启用顶点属性的公告板四边形 VAO + VBO
     * 适用于需要在外部手动控制属性启用的场景
     */
    public static QuadData createWithoutEnable() {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = BufferUtils.createFloatBuffer(QUAD_VERTICES.length);
        buf.put(QUAD_VERTICES);
        buf.flip();
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        // 不调用 glEnableVertexAttribArray(0)，由外部控制

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        return new QuadData(vao, vbo, 4);
    }

    /**
     * 释放公告板四边形的 VAO 和 VBO 资源。
     * 在窗口销毁前必须调用。
     */
    public static void cleanup(QuadData quad) {
        if (quad != null) {
            glDeleteBuffers(quad.vboID);
            glDeleteVertexArrays(quad.vaoID);
        }
    }
}
