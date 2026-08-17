package com.z227.akatzumatool.render.gpu;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;

// GPUParticleRenderShader 管理单个 GPU 粒子渲染 Shader，用于按 Render Pipeline 分批绘制粒子。
public class GPUParticleRenderShader extends ShaderProgram {
    public int render_uProjection; // 投影矩阵 uniform。
    public int render_uView; // 视图矩阵 uniform。
    public int render_uTime; // 客户端累计时间 uniform。
    public int render_uRenderPipelineId; // 当前绘制的 Render Pipeline ID。
    public int render_uMaxParticles; // 单个 pipeline 在 active index 表中的容量。
    public int render_uSampler0; // 自定义 atlas 采样器 uniform。
    public int render_uCameraPos; // 相机世界坐标 uniform，供需要世界空间视线的粒子 shader 使用。

    public GPUParticleRenderShader(String vertexFile, String fragmentFile) {
        super(vertexFile, fragmentFile);
    }

    @Override
    protected void getAllUniformLocations() {
        render_uProjection = glGetUniformLocation(programID, "uProjection");
        render_uView = glGetUniformLocation(programID, "uView");
        render_uTime = glGetUniformLocation(programID, "uTime");
        render_uRenderPipelineId = glGetUniformLocation(programID, "uRenderPipelineId");
        render_uMaxParticles = glGetUniformLocation(programID, "uMaxParticles");
        render_uSampler0 = glGetUniformLocation(programID, "Sampler0");
        render_uCameraPos = glGetUniformLocation(programID, "uCameraPos");
    }

    // 写入当前渲染批次的通用 uniform。
    public void updateRenderUniforms(Matrix4f projectionMatrix, Matrix4f viewMatrix, float totalTime, int pipelineId, int maxParticles) {
        loadMatrix(render_uProjection, projectionMatrix);
        loadMatrix(render_uView, viewMatrix);
        loadFloat(render_uTime, totalTime);
        loadInt(render_uRenderPipelineId, pipelineId);
        loadInt(render_uMaxParticles, maxParticles);
        if (render_uSampler0 >= 0) {
            loadInt(render_uSampler0, 0);
        }
    }

    // 写入相机世界坐标，未声明该 uniform 的 shader 会自动跳过。
    public void loadCameraPosition(Vec3 cameraPosition) {
        if (render_uCameraPos < 0 || cameraPosition == null) return;
        loadVector(render_uCameraPos, new Vector3f((float) cameraPosition.x, (float) cameraPosition.y, (float) cameraPosition.z));
    }
}
