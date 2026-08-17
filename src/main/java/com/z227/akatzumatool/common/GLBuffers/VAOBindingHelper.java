package com.z227.akatzumatool.common.GLBuffers;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * VAO 绑定 / 解绑辅助工具
 *
 * 封装项目中渲染器常用的 VAO 绑定和解绑模式。
 * 使用带资源的 try-finally 或显式配对调用，确保状态一致。
 *
 * 使用示例（标准模型 3 属性）：
 * <pre>
 *   VAOBindingHelper.bindStandard(rawModel);
 *   try {
 *       glDrawElements(GL_TRIANGLES, rawModel.getVertexCount(), GL_UNSIGNED_INT, 0);
 *   } finally {
 *       VAOBindingHelper.unbindStandard();
 *   }
 * </pre>
 *
 * 使用示例（法线贴图模型 4 属性）：
 * <pre>
 *   VAOBindingHelper.bindNormalMapped(rawModel);
 *   try {
 *       glDrawElements(GL_TRIANGLES, rawModel.getVertexCount(), GL_UNSIGNED_INT, 0);
 *   } finally {
 *       VAOBindingHelper.unbindNormalMapped();
 *   }
 * </pre>
 */
public class VAOBindingHelper {

    /**
     * 绑定标准模型的 VAO —— 启用 3 个顶点属性
     * 对应 Loader 中 loadToVAO(positions, texCoords, normals, indices)
     * attribute 0 = position (vec3)
     * attribute 1 = textureCoords (vec2)
     * attribute 2 = normal (vec3)
     */
    public static void bindStandard(RawModel model) {
        // 绑定VAO（切换到VAO）
        glBindVertexArray(model.getVaoID());
        // 启用顶点属性数组，前面绑定到VAO的0中
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);//启用纹理坐标
        glEnableVertexAttribArray(2);//启用法向量
    }

    /**
     * 解绑标准模型的 VAO
     */
    public static void unbindStandard() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    /**
     * 绑定额外带切线的模型 VAO —— 启用 4 个顶点属性
     * 对应 Loader 中 loadToVAO(positions, texCoords, normals, tangents, indices)
     * attribute 0 = position (vec3)
     * attribute 1 = textureCoords (vec2)
     * attribute 2 = normal (vec3)
     * attribute 3 = tangent (vec3)
     */
    public static void bindNormalMapped(RawModel model) {
        glBindVertexArray(model.getVaoID());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
    }

    /**
     * 解绑法线贴图模型的 VAO
     */
    public static void unbindNormalMapped() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        glBindVertexArray(0);
    }

    /**
     * 绑定 GUI/后处理 模型的 VAO —— 启用 1 个顶点属性
     * 对应 Loader 中 loadToVAO(positions) 或 loadToVAO(positions, 2)
     * attribute 0 = position (vec2)
     */
    public static void bindSimple(RawModel model) {
        glBindVertexArray(model.getVaoID());
        glEnableVertexAttribArray(0);
    }

    /**
     * 解绑简单模型的 VAO
     */
    public static void unbindSimple() {
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    /**
     * 绑定字体文本的 VAO —— 启用 2 个顶点属性
     * 对应 Loader 中 loadToVAO(positions, textureCoords)
     * attribute 0 = position (vec2)
     * attribute 1 = textureCoords (vec2)
     */
    public static void bindFont(int vaoID) {
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
    }

    /**
     * 解绑字体文本的 VAO
     */
    public static void unbindFont() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
    }
}
