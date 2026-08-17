package com.z227.akatzumatool.common.GLBuffers.instancing;

import org.joml.Matrix4f;

// InstanceDataWriter 提供常用实例数据写入辅助方法。
public class InstanceDataWriter {

    // 将 Matrix4f 按 GLSL mat4 列主序写入 float 数组。
    public static void mat4(Matrix4f mat, float[] data, int offset) {
        data[offset     ] = mat.m00();
        data[offset + 1 ] = mat.m01();
        data[offset + 2 ] = mat.m02();
        data[offset + 3 ] = mat.m03();
        data[offset + 4 ] = mat.m10();
        data[offset + 5 ] = mat.m11();
        data[offset + 6 ] = mat.m12();
        data[offset + 7 ] = mat.m13();
        data[offset + 8 ] = mat.m20();
        data[offset + 9 ] = mat.m21();
        data[offset + 10] = mat.m22();
        data[offset + 11] = mat.m23();
        data[offset + 12] = mat.m30();
        data[offset + 13] = mat.m31();
        data[offset + 14] = mat.m32();
        data[offset + 15] = mat.m33();
    }

    // 将 vec3 写入 float 数组。
    public static void vec3(float x, float y, float z, float[] data, int offset) {
        data[offset    ] = x;
        data[offset + 1] = y;
        data[offset + 2] = z;
    }

    // 将 vec4 写入 float 数组。
    public static void vec4(float x, float y, float z, float w, float[] data, int offset) {
        data[offset    ] = x;
        data[offset + 1] = y;
        data[offset + 2] = z;
        data[offset + 3] = w;
    }

    // 将单个 float 写入 float 数组。
    public static void float1(float v, float[] data, int offset) {
        data[offset] = v;
    }
}
