#version 330 core

// miao_outline 顶点 shader。
// 标准全屏 quad：position 从 [-1, 1] 转成屏幕 UV。
in vec2 position;

out vec2 textureCoords;

void main() {
    gl_Position = vec4(position, 0.0, 1.0);
    textureCoords = position * 0.5 + vec2(0.5);
}
