package com.z227.akatzumatool.common.GLBuffers;


import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL30.*;

/**
 * VAO / VBO / EBO 管理器（纯顶点缓冲区操作，不含纹理加载）
 *
 * 职责：
 * 1. 创建并管理所有 VAO、VBO、EBO 的生命周期
 * 2. 提供统一的 {@link #loadToVAO(VAOLayout)} 方法 + 便捷重载
 * 3. 支持实例化渲染所需的 VBO 创建与属性绑定
 * 4. 统一清理所有缓冲区资源
 *
 * ── VBO 扩展方式 ──
 * 新增顶点属性（如骨骼权重、顶点颜色等）无需修改 Loader 类，
 * 通过 {@link VAOLayout} Builder 链式添加即可：
 * <pre>
 *   RawModel model = loader.loadToVAO(
 *       loader.newLayout()
 *           .addAttribute(0, 3, positions)
 *           .addAttribute(1, 2, texCoords)
 *           .addAttribute(2, 3, normals)
 *           .addAttribute(3, 4, boneWeights)   // 新属性：GLSL location=3，vec4
 *           .indices(indices)
 *   );
 * </pre>
 *
 * 使用示例（便捷方法）：
 * <pre>
 *   Loader loader = new Loader();
 *   RawModel model = loader.loadToVAO(positions, texCoords, normals, indices);
 *   loader.cleanUp();
 * </pre>
 */
public class Loader {

    List<Integer> vaos = new ArrayList<>();
    List<Integer> vbos = new ArrayList<>();

    // ================================================================
    // 核心方法：VAOLayout Builder 驱动（可任意扩展 VBO 属性）
    // ================================================================

    /**
     * 创建一个新的 {@link VAOLayout} Builder 实例
     * @return 空布局描述器，通过链式调用添加属性和索引
     */
    public VAOLayout newLayout() {
        return new VAOLayout();
    }

    /**
     * 【核心方法】使用 VAOLayout 描述器加载 VAO
     *
     * 根据 layout 中声明的属性列表依次创建 VBO 并绑定到当前 VAO。
     * 如果有 EBO 索引数据则一并创建 EBO。
     *
     * @param layout 顶点布局描述器，由 .addAttribute() / .indices() 构建
     * @return 包含 vaoID 和 vertexCount 的 RawModel
     */
    public RawModel loadToVAO(VAOLayout layout) {
        int vaoID = createVAO();

        // 如果有索引数据，先创建 EBO（EBO 绑定在当前 VAO 上）
        if (layout.indices != null) {
            bindIndicesBuffer(layout.indices);
        }

        // 遍历所有属性，依次创建 VBO 并绑定到 VAO
        for (VAOLayout.AttributeEntry attr : layout.attributes) {
            storeDataInAttributeList(attr.index, attr.size, attr.data);
        }

        unbindVAO(); // 顶点属性会一直保持绑定状态，直到被显式禁用或绑定到另一个VAO。
        return new RawModel(vaoID, layout.vertexCount);
    }

    // ────────────────────────────────────────────────────────────────
    // 便捷方法（内部委托给 loadToVAO(VAOLayout)，保持向后兼容）
    // ────────────────────────────────────────────────────────────────

    /**
     * 标准模型加载（位置 + 纹理坐标 + 法线 + 索引）
     * VAO 布局：
     *   attribute 0 → vec3 position
     *   attribute 1 → vec2 textureCoords
     *   attribute 2 → vec3 normal
     */
    public RawModel loadToVAO(float[] positions, float[] textureCoords, float[] normals, int[] indices) {
        return loadToVAO(newLayout()
                .addAttribute(0, 3, positions)
                .addAttribute(1, 2, textureCoords)
                .addAttribute(2, 3, normals)
                .indices(indices));
    }

    /**
     * 法线贴图模型加载（位置 + 纹理坐标 + 法线 + 切线 + 索引）
     * VAO 布局：
     *   attribute 0 → vec3 position
     *   attribute 1 → vec2 textureCoords
     *   attribute 2 → vec3 normal
     *   attribute 3 → vec3 tangent
     */
    // 31.法线贴图
    public RawModel loadToVAO(float[] positions, float[] textureCoords, float[] normals, float[] tangents, int[] indices) {
        return loadToVAO(newLayout()
                .addAttribute(0, 3, positions)
                .addAttribute(1, 2, textureCoords)
                .addAttribute(2, 3, normals)
                .addAttribute(3, 3, tangents)
                .indices(indices));
    }


    // 36. 实例化渲染

    /**
     * 创建一个空的实例化 VBO
     * 使用 GL_STREAM_DRAW，因为每帧需要更新实例数据
     *
     * @param floatCount 缓冲区容量（float 数量）
     * @return VBO ID
     */
    public int createEmptyVbp(float floatCount) {
        int vboID = glGenBuffers();
        vbos.add(vboID);
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long)(floatCount * 4), GL_STREAM_DRAW);  // floatCount * 4 需要的字节数
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vboID;
    }

    /**
     * 给现有 VAO 添加实例化顶点属性
     * 实例化属性每渲染一个实例更新一次，而非每个顶点
     *
     * @param vaoID              顶点数组对象ID
     * @param vbo                顶点缓冲对象ID
     * @param attribute          顶点属性索引
     * @param dataSize           顶点属性数据大小（分量数，如 vec4 就是 4）
     * @param instanceDataLength 实例数据总长度（float 数量）
     * @param offset             顶点属性的偏移量（float 数量，非字节）
     */
    /*
      @param vaoID 顶点数组对象ID
      @param vbo 顶点缓冲对象ID
      @param attribute 顶点属性索引
      @param dataSize 顶点属性数据大小
      @param instanceDataLength 实例数据长度
      @param offset 顶点属性的偏移量
      @return
    */
    public void addInstanceAttribute(int vaoID, int vbo, int attribute, int dataSize, int instanceDataLength, int  offset) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindVertexArray(vaoID);
        glVertexAttribPointer(attribute, dataSize, GL_FLOAT, false, instanceDataLength * 4, offset * 4);
        GL33.glVertexAttribDivisor(attribute, 1);   //每渲染一个实例，数据更改一次
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /**
     * 更新 VBO 数据（通常用于每帧更新实例数据）
     *
     * @param vboID  要更新的 VBO ID
     * @param data   新数据
     * @param buffer 复用的 FloatBuffer（避免每帧分配）
     */
    public void updateVbo(int vboID, float[] data, FloatBuffer buffer){
        buffer.clear();
        buffer.put(data);
        buffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, buffer.capacity() * 4L, GL_STREAM_DRAW);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);    // 更新缓冲区数据
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /**
     * GUI 四边形加载（仅位置，2D）
     * VAO 布局：
     *   attribute 0 → vec2 position
     */
    // gui
    public RawModel loadToVAO(float[] positions) {
        return loadToVAO(newLayout()
                .addAttribute(0, 2, positions)
                .vertexCount(positions.length / 2));
    }

    /**
     * 通用自定义维度数据加载
     * VAO 布局：
     *   attribute 0 → 自定义 dimensions 维向量
     *
     * @param positions  顶点数据
     * @param dimensions 每个顶点的分量数（2 = 2D, 3 = 3D, ...）
     */
    public RawModel loadToVAO(float[] positions, int dimensions) {
        return loadToVAO(newLayout()
                .addAttribute(0, dimensions, positions)
                .vertexCount(positions.length / dimensions));
    }


    // ── 内部工具方法 ──

    private int createVAO() {
        int vaoID = glGenVertexArrays();
        vaos.add(vaoID);
        glBindVertexArray(vaoID);
        return vaoID;
    }

/**
 * 将数据存储到属性列表中，用于顶点属性数据的处理
 * @param attributeNumber 属性编号，用于标识顶点属性的索引（GLSL 中 layout(location = attributeNumber)）
 * @param coordinateSize  每个顶点的坐标分量数（2→vec2, 3→vec3, 4→vec4）
 * @param data 包含顶点属性数据的浮点数组
 */
    private void storeDataInAttributeList(int attributeNumber, int coordinateSize, float[] data) {
    // 生成一个顶点缓冲对象(VBO)并获取其ID
        int vboID = glGenBuffers();
        vbos.add(vboID);
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        FloatBuffer dataBuffer = storeDataInFloatBuffer(data);
        glBufferData(GL_ARRAY_BUFFER, dataBuffer, GL_STATIC_DRAW);  // 将数据存储到VBO中
        // https://mouse0w0.github.io/lwjglbook-CN-Translation/04-rendering/
        // 1.顶点属性索引 2.顶点属性的大小 3.数据类型 4.是否归一化 5.步长 6.偏移量
        // 5.stride 表示连续顶点属性之间的字节偏移量，这里为0表示连续顶点属性紧密排列
        // 6.pointer 是否从头开始读取
        glVertexAttribPointer(attributeNumber, coordinateSize, GL_FLOAT, false, 0, 0);// 将 VBO 存到 VAO的属性列表0中，attributeNumber为0
        glBindBuffer(GL_ARRAY_BUFFER,0); // 解绑VBO
    }

    private void unbindVAO() {
        glBindVertexArray(0);
    }

    /**
     * 创建索引缓冲区 EBO（Element Buffer Object）
     * 注意：EBO 绑定到当前已激活的 VAO 上，
     * 所以必须在 createVAO() 之后、unbindVAO() 之前调用
     */
    // 创建ebo
    public void bindIndicesBuffer(int[] indices) {
        int vboID = glGenBuffers();
        vbos.add(vboID);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, vboID);
        IntBuffer buffer = storeDataInIntBuffer(indices);
        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);  // 将数据存储到EBO中
    }

    public IntBuffer storeDataInIntBuffer(int[] data) {
        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public FloatBuffer storeDataInFloatBuffer(float[] data) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    /**
     * 清理所有已创建的 OpenGL 缓冲区资源
     * 应在程序退出前调用
     */
    public void cleanUp() {
        for (int vao : vaos) {
            glDeleteVertexArrays(vao);
        }
        for (int vbo : vbos) {
            glDeleteBuffers(vbo);
        }

    }




}
