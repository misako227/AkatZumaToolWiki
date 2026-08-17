#version 330 core

// 次元斩斩击顶点 shader：顶点已经是世界坐标，使用 uView 和 ProjMat 转到裁剪空间。
in vec3 Position;
in vec4 Color;
in vec2 UV0;

// Minecraft ShaderInstance 会自动写入 ProjMat，队列每批写入 uView。
uniform mat4 uView;
uniform mat4 ProjMat;

// UV 控制斩击截面，Color.a 控制生命周期淡入淡出。
out vec2 vUV;
out vec4 vColor;

void main() {
    gl_Position = ProjMat * uView * vec4(Position, 1.0);
    vUV = UV0;
    vColor = Color;
}
