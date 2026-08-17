package com.z227.akatzumatool.common.GLBuffers;

import java.util.ArrayList;
import java.util.List;

/**
 * VAO 顶点布局描述器（Builder 模式）
 *
 * 用于描述 VAO 中需要绑定哪些顶点属性，替代硬编码的多个 loadToVAO 重载。
 * 通过链式调用自由组合任意属性，扩展新 VBO 属性无需修改 Loader 类。
 *
 * ── 使用示例 ──
 * <pre>
 *   // 标准模型：position(vec3) + texCoord(vec2) + normal(vec3) + EBO
 *   RawModel model = loader.loadToVAO(
 *       loader.newLayout()
 *           .addAttribute(0, 3, positions)
 *           .addAttribute(1, 2, texCoords)
 *           .addAttribute(2, 3, normals)
 *           .indices(indices)
 *   );
 *
 *   // 法线贴图模型：额外加 tangent(vec3)
 *   RawModel nmModel = loader.loadToVAO(
 *       loader.newLayout()
 *           .addAttribute(0, 3, positions)
 *           .addAttribute(1, 2, texCoords)
 *           .addAttribute(2, 3, normals)
 *           .addAttribute(3, 3, tangents)
 *           .indices(indices)
 *   );
 *
 *   // 简单 2D 四边形：仅 position(vec2)，无 EBO
 *   RawModel quad = loader.loadToVAO(
 *       loader.newLayout()
 *           .addAttribute(0, 2, positions)
 *           .vertexCount(positions.length / 2)
 *   );
 *
 *   // 自定义扩展：骨骼动画（额外加 boneIndex + boneWeight）
 *   RawModel skinned = loader.loadToVAO(
 *       loader.newLayout()
 *           .addAttribute(0, 3, positions)
 *           .addAttribute(1, 2, texCoords)
 *           .addAttribute(2, 3, normals)
 *           .addAttribute(3, 4, boneIndices)    // 新增：骨骼索引 vec4
 *           .addAttribute(4, 4, boneWeights)    // 新增：骨骼权重 vec4
 *           .indices(indices)
 *   );
 * </pre>
 *
 * ── 与 GLSL attribute location 对应 ──
 * {@code addAttribute(i, size, data)} 的 i 就是 GLSL 中
 * {@code layout(location = i) in vecX attribName} 的 location。
 */
public class VAOLayout {

    /** 顶点属性列表（按添加顺序存储） */
    final List<AttributeEntry> attributes = new ArrayList<>();

    /** EBO 索引数据（null 表示无索引缓冲区，使用 glDrawArrays） */
    int[] indices;

    /**
     * 顶点数量。
     * - 有 EBO 时自动设为 {@code indices.length}
     * - 无 EBO 时由调用者通过 {@link #vertexCount(int)} 指定
     */
    int vertexCount;

    /**
     * 添加一个顶点属性
     *
     * @param index GLSL 中 layout(location = index) 的属性位置编号
     * @param size  每个顶点的分量数（2→vec2, 3→vec3, 4→vec4）
     * @param data  顶点数据 float 数组
     * @return this（链式调用）
     */
    public VAOLayout addAttribute(int index, int size, float[] data) {
        attributes.add(new AttributeEntry(index, size, data));
        return this;
    }

    /**
     * 设置 EBO 索引数据（顶点索引数组）
     * 调用后 vertexCount 自动设为 {@code indices.length}
     *
     * @return this（链式调用）
     */
    public VAOLayout indices(int[] indices) {
        this.indices = indices;
        this.vertexCount = indices.length;
        return this;
    }

    /**
     * 手动指定顶点数量（用于无 EBO 的 glDrawArrays 模式）
     * 如果有 EBO，此值会被 {@link #indices(int[])} 覆盖
     *
     * @return this（链式调用）
     */
    public VAOLayout vertexCount(int count) {
        this.vertexCount = count;
        return this;
    }

    /** @return 当前已有的属性数量 */
    public int attributeCount() {
        return attributes.size();
    }

    // ── 内部数据结构 ──

    /** 单个顶点属性条目：location + size + data */
    static class AttributeEntry {
        final int index;
        final int size;
        final float[] data;

        AttributeEntry(int index, int size, float[] data) {
            this.index = index;
            this.size = size;
            this.data = data;
        }
    }
}
