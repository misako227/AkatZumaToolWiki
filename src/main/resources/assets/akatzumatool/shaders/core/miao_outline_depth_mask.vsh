#version 150

// miao_outline_depth_mask 顶点 shader。
// 捕获实体顶点已经处于 view-space，fallback AABB 顶点是 world-space。
// 为了让两种输入共用同一个 RenderType，fallback 顶点通过 Color.r 标记为 1 时再乘 uView。
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

// Minecraft core shader 自动提供当前投影矩阵。
uniform mat4 ProjMat;
// uView 只给 fallback AABB 使用，捕获实体顶点不能再次乘 view，否则会随视角漂移。
uniform mat4 uView;

// 原实体纹理 UV，片元阶段用于 alpha discard。
out vec2 texCoord0;
// view-space 位置，片元阶段从 -z 计算归一化深度。
out vec3 viewPosition;
// 顶点颜色保留调试通道，当前只作为 fallback 标记使用。
out vec4 vertexColor;

void main() {
    // Color.b 大于 0.5 代表 fallback AABB 顶点；捕获实体顶点已经是 view-space。
    // 第一版 fallback 不作为主要路径，保留 uView 是为了捕获失败时至少能看到 AABB 调试轮廓。
    vec4 viewPos = vec4(Position, 1.0);
    if (Color.b > 0.5) {
        viewPos = uView * vec4(Position, 1.0);
    }

    gl_Position = ProjMat * viewPos;
    texCoord0 = UV0;
    viewPosition = viewPos.xyz;
    vertexColor = Color;
}
