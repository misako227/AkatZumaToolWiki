#version 150

// miao_outline_depth_mask 片元 shader。
// 本 pass 只写 mainFBO.CA2：
// R = 归一化 view depth，给后处理做径向深度跳变检测。
// G = 目标 mask，限制边缘只出现在描边目标附近。
// B/A 当前预留，第一版保持 0/1。
uniform sampler2D Sampler0;

// x=NearDepth，y=DepthRange，z=MaskValue，w=AlphaCutoff。
uniform vec4 DepthParams;

in vec2 texCoord0;
in vec3 viewPosition;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // 采样实体原纹理 alpha，透明像素不写入目标深度 mask。
    vec4 entityColor = texture(Sampler0, texCoord0);
    if (entityColor.a <= DepthParams.w) {
        discard;
    }

    // 捕获坐标处于 view-space，摄像机前方的实体 z 为负数，所以使用 -z 作为距离。
    float viewDepth = max(-viewPosition.z - DepthParams.x, 0.0);
    float normalizedDepth = clamp(viewDepth / max(DepthParams.y, 1.0), 0.0, 1.0);

    // 输出纯净的深度和目标 mask，不把颜色或噪声写入 CA2，避免后处理误判内部纹理。
    fragColor = vec4(normalizedDepth, DepthParams.z, 0.0, 1.0);
}
