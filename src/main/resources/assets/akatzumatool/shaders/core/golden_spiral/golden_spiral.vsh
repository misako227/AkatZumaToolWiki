#version 330 core

// 金色螺旋光效顶点 shader。
// Java 侧直接生成世界空间螺旋 ribbon 顶点。
// Position 是世界坐标，UV0.x 是 ribbon 宽度方向，UV0.y 是高度方向。
// Color.a 保存生命周期淡入淡出透明度。
in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ProjMat;
uniform mat4 uView;

out vec2 vUV;
out vec4 vColor;

void main() {
    // 世界空间顶点直接进入当前相机 view/projection。
    gl_Position = ProjMat * uView * vec4(Position, 1.0);
    // 传递局部 UV，片元阶段用它做三噪声采样和圆形 mask。
    vUV = UV0;
    // 顶点颜色当前只使用 alpha，RGB 由片元 shader 的金黄色 uniform 决定。
    vColor = Color;
}
