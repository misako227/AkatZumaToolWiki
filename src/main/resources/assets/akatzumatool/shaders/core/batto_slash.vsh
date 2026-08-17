#version 330 core

// 拔刀斩顶点 shader：
// Java 侧已经把 OBJ 顶点直接写成世界坐标。
// 这里继续沿用现有 bloom 队列的矩阵路径，用 uView 和 ProjMat 转到裁剪空间。
in vec3 Position;
in vec4 Color;
in vec2 UV0;

// Minecraft ShaderInstance 会自动写入 ProjMat。
// bloom 队列在每批绘制前写入当前视图矩阵。
uniform mat4 uView;
uniform mat4 ProjMat;

// 把局部 UV 和按实体编码过的顶点颜色传给片段阶段。
out vec2 vUV;
out vec4 vColor;

void main() {
    gl_Position = ProjMat * uView * vec4(Position, 1.0);
    vUV = UV0;
    vColor = Color;
}
