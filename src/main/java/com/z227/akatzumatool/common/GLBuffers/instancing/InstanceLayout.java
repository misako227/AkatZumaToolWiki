package com.z227.akatzumatool.common.GLBuffers.instancing;

import java.util.ArrayList;
import java.util.List;

// InstanceLayout 描述每条实例数据的 float 跨度和 attribute 分布。
public class InstanceLayout {

    public final int strideFloats; // 每个实例的总 float 数。

    public final List<AttrEntry> attributes = new ArrayList<>(); // 实例 attribute 列表。

    public InstanceLayout(int strideFloats) {
        if (strideFloats <= 0) {
            throw new IllegalArgumentException("实例布局 strideFloats 必须大于 0");
        }
        this.strideFloats = strideFloats;
    }

    // 创建一个实例布局 Builder。
    public static InstanceLayout create(int strideFloats) {
        return new InstanceLayout(strideFloats);
    }

    // 添加一个实例 attribute，offset 使用 float 偏移而不是字节偏移。
    public InstanceLayout attr(int location, int size, int offset) {
        if (location < 0) {
            throw new IllegalArgumentException("实例 attribute location 不能小于 0");
        }
        if (size < 1 || size > 4) {
            throw new IllegalArgumentException("实例 attribute size 必须在 1 到 4 之间");
        }
        if (offset < 0 || offset + size > strideFloats) {
            throw new IllegalArgumentException("实例 attribute 超出 strideFloats 范围");
        }
        attributes.add(new AttrEntry(location, size, offset));
        return this;
    }

    // 返回实例 attribute 数量。
    public int count() {
        return attributes.size();
    }

    // AttrEntry 保存单个实例 attribute 的 location、size 和 float 偏移。
    public static class AttrEntry {
        public final int location; // GLSL attribute location。
        public final int size; // attribute 分量数。
        public final int offset; // attribute 在实例结构内的 float 偏移。

        public AttrEntry(int location, int size, int offset) {
            this.location = location;
            this.size = size;
            this.offset = offset;
        }
    }
}
