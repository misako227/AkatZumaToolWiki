package com.z227.akatzumatool.render.gpu;

import com.z227.akatzumatool.common.MathUtil;
import com.z227.akatzumatool.render.gpu.material.ParticleMaterialRegistry;
import com.z227.akatzumatool.render.gpu.material.ParticleRenderPipeline;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

public class GPUParticleSystem {

    public static final int MAX_PARTICLES = 150000;
    private static final int LOCAL_SIZE_X = 256;
    private static final int QUAD_VERTEX_COUNT = 4;
    private static final int RISING_SHOCKWAVE_SEGMENTS = 32; // 上升冲击波圆台圆周分段数。
    private static final int RISING_SHOCKWAVE_VERTEX_COUNT = RISING_SHOCKWAVE_SEGMENTS * 6; // 每段两个三角形。
    private static final int ACTIVE_INDEX_BUFFER_BINDING = 3; // active index SSBO 绑定点。
    private static final int ACTIVE_COUNT_BUFFER_BINDING = 4; // active count SSBO 绑定点。
    private static final int INDIRECT_COMMAND_UINTS = 4; // DrawArraysIndirectCommand 包含 count/instanceCount/first/baseInstance。
    private static final int INDIRECT_COMMAND_STRIDE_BYTES = INDIRECT_COMMAND_UINTS * Integer.BYTES; // 单个 indirect command 字节跨度。
    private static final int INDIRECT_INSTANCE_COUNT_OFFSET_BYTES = Integer.BYTES; // instanceCount 位于 command 的第二个 uint。

    // 每个粒子 13 个 vec4，额外保存生命周期中间/结束尺寸和尺寸时间点。
    private static final int FLOATS_PER_PARTICLE = 52;
    // 每个发射任务 13 个 vec4，只在本帧被 compute shader 读取。
    private static final int FLOATS_PER_EMIT_JOB = 52;
    private static final int MAX_EMIT_JOBS = 768;   // 发射任务数量上限

    private int particleSsbo;
    private int emitJobSsbo;
    private int activeIndexSsbo; // 按 pipeline 压缩后的活跃粒子下标表。
    private int activeCountSsbo; // 每个 pipeline 本帧实际活跃粒子数量。
    private int indirectCommandBuffer; // GPU indirect draw 命令缓冲，避免 CPU 读回 active count。
    private int vao;
    private int vbo;

    private int nextEmitIndex;
    private int emitJobCount;
    private float totalTime;

    private final FloatBuffer particleBuffer = BufferUtils.createFloatBuffer(MAX_PARTICLES * FLOATS_PER_PARTICLE);
    private final FloatBuffer emitJobBuffer = BufferUtils.createFloatBuffer(MAX_EMIT_JOBS * FLOATS_PER_EMIT_JOB);
    private final IntBuffer activeCountResetBuffer = BufferUtils.createIntBuffer(ParticleRenderPipeline.COUNT);
    private final IntBuffer indirectCommandInitBuffer = BufferUtils.createIntBuffer(ParticleRenderPipeline.COUNT * INDIRECT_COMMAND_UINTS);
    private final float[] activePipelineTimeLeft = new float[ParticleRenderPipeline.COUNT];

    public GPUShader gpushader;
    public GPUParticleRenderShader lightEffectShader; // 三噪声光效粒子渲染 Shader。
    public GPUParticleRenderShader directedLightEffectShader; // 世界空间定向三噪声光效粒子渲染 Shader。
    public GPUParticleRenderShader magicCircleEnergyShader; // 水平法阵能量粒子渲染 Shader。
    public GPUParticleRenderShader exSwordWaveShader; // 世界竖直平面的 EX 剑气粒子渲染 Shader。
    public GPUParticleRenderShader starTextureShader; // 始终朝向相机的星星贴图粒子渲染 Shader。
    public GPUParticleRenderShader risingShockwaveShader; // 程序化圆台上升冲击波渲染 Shader。

    public GPUParticleSystem() {
        initParticleSSBO();
        initEmitJobSSBO();
        initActiveIndexSSBO();
        initActiveCountSSBO();
        initIndirectCommandBuffer();
        initQuadVAO();
        gpushader = new GPUShader();
        lightEffectShader = new GPUParticleRenderShader("shaders/gpu/particle_light_effect.vsh", "shaders/gpu/particle_light_effect.fsh");
        directedLightEffectShader = new GPUParticleRenderShader("shaders/gpu/particle_directed_light_effect.vsh", "shaders/gpu/particle_directed_light_effect.fsh");
        magicCircleEnergyShader = new GPUParticleRenderShader("shaders/gpu/particle_magic_circle_energy.vsh", "shaders/gpu/particle_magic_circle_energy.fsh");
        exSwordWaveShader = new GPUParticleRenderShader("shaders/gpu/particle_ex_sword_wave.vsh", "shaders/gpu/particle_ex_sword_wave.fsh");
        starTextureShader = new GPUParticleRenderShader("shaders/gpu/particle_star_texture.vsh", "shaders/gpu/particle_star_texture.fsh");
        risingShockwaveShader = new GPUParticleRenderShader("shaders/gpu/particle_rising_shockwave.vsh", "shaders/gpu/particle_rising_shockwave.fsh");
    }

    private void initParticleSSBO() {
        particleSsbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, particleSsbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER,
                (long) MAX_PARTICLES * FLOATS_PER_PARTICLE * Float.BYTES,
                GL_DYNAMIC_COPY);

        /* 初始全部为 0，life=0 表示死亡粒子。 */
        particleBuffer.clear();
        for (int i = 0; i < MAX_PARTICLES * FLOATS_PER_PARTICLE; i++) {
            particleBuffer.put(0f);
        }
        particleBuffer.flip();
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, particleBuffer);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    private void initEmitJobSSBO() {
        emitJobSsbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, emitJobSsbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER,
                (long) MAX_EMIT_JOBS * FLOATS_PER_EMIT_JOB * Float.BYTES,
                GL_STREAM_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    // 初始化 DrawArraysIndirectCommand 缓冲，count/first/baseInstance 固定，instanceCount 每帧由 GPU 填入。
    public void initIndirectCommandBuffer() {
        indirectCommandBuffer = glGenBuffers();
        indirectCommandInitBuffer.clear();
        for (int i = 0; i < ParticleRenderPipeline.COUNT; i++) {
            indirectCommandInitBuffer.put(vertexCountOfPipeline(i));
            indirectCommandInitBuffer.put(0);
            indirectCommandInitBuffer.put(0);
            indirectCommandInitBuffer.put(0);
        }
        indirectCommandInitBuffer.flip();

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectCommandBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, indirectCommandInitBuffer, GL_DYNAMIC_COPY);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
    }

    // 初始化 active index SSBO，每个 pipeline 预留 MAX_PARTICLES 个 uint 下标。
    public void initActiveIndexSSBO() {
        activeIndexSsbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, activeIndexSsbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER,
                (long) MAX_PARTICLES * ParticleRenderPipeline.COUNT * Integer.BYTES,
                GL_DYNAMIC_COPY);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    // 初始化 active count SSBO，compute 每帧写入各 pipeline 的活跃数量。
    public void initActiveCountSSBO() {
        activeCountSsbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, activeCountSsbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER,
                (long) ParticleRenderPipeline.COUNT * Integer.BYTES,
                GL_DYNAMIC_COPY);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    private void initQuadVAO() {
        float[] vertices = {
                -0.5f,  0.5f,
                -0.5f, -0.5f,
                 0.5f,  0.5f,
                 0.5f, -0.5f
        };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices);
        buf.flip();
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void beginEmitJobs() {
        emitJobCount = 0;
        emitJobBuffer.clear();
    }

    public void addEmitJob(ParticleEmitTask task, int emitCount) {
        if (task == null || emitCount <= 0 || emitJobCount >= MAX_EMIT_JOBS) {
            return;
        }

        int count = Math.min(emitCount, MAX_PARTICLES);
        int startIndex = nextEmitIndex;
        nextEmitIndex = (nextEmitIndex + count) % MAX_PARTICLES;

        // EmitJob 布局必须与 gpushader.comp 中的 struct EmitJob 保持一致，每个 helper 固定写入一个 vec4 槽位。
        writeEmitJobPositionVec4(task, startIndex);
        writeEmitJobDirectionVec4(task, count, startIndex);
        writeEmitJobColorVec4s(task);
        writeEmitJobPhysicsVec4(task);
        writeEmitJobRenderVec4(task);
        writeEmitJobMotionVec4(task);
        writeEmitJobRandomVec4(task);
        writeEmitJobSpeedParamsVec4(task);
        writeEmitJobRenderParamsVec4(task);
        writeEmitJobSizeParamsVec4(task);
        writeEmitJobSizeControlVec4(task);

        int pipelineId = ParticleMaterialRegistry.pipelineIdOf(task.materialId());
        if (pipelineId >= 0 && pipelineId < activePipelineTimeLeft.length) {
            activePipelineTimeLeft[pipelineId] = Math.max(activePipelineTimeLeft[pipelineId], task.life);
        }

        emitJobCount++;
    }

    // 写入 EmitJob.position：xyz 为发射位置，w 为环形粒子缓冲写入起点。
    public void writeEmitJobPositionVec4(ParticleEmitTask task, int startIndex) {
        emitJobBuffer.put(task.posX).put(task.posY).put(task.posZ).put((float) startIndex);
    }

    // 写入 EmitJob.direction：普通模式保存方向，圆形和径向扩散模式保存轨道平面欧拉角。
    public void writeEmitJobDirectionVec4(ParticleEmitTask task, int count, int startIndex) {
        if (task.motionType == ParticleEmitTask.MOTION_CIRCULAR
                || task.motionType == ParticleEmitTask.MOTION_RADIAL_DIFFUSION) {
            float planeSeed = totalTime * 17.0f + emitJobCount * 31.0f + startIndex * 0.013f;
            float planePitch = task.orbitPlanePitch + signedRandom(planeSeed + 1.0f) * task.orbitPlanePitchRange;
            float planeYaw = task.orbitPlaneYaw + signedRandom(planeSeed + 2.0f) * task.orbitPlaneYawRange;
            float planeRoll = task.orbitPlaneRoll + signedRandom(planeSeed + 3.0f) * task.orbitPlaneRollRange;
            emitJobBuffer.put(planePitch).put(planeYaw).put(planeRoll).put((float) count);
            return;
        }
        emitJobBuffer.put(task.dirX).put(task.dirY).put(task.dirZ).put((float) count);
    }

    // 连续写入 EmitJob.startColor/midColor/endColor 三个颜色槽位。
    public void writeEmitJobColorVec4s(ParticleEmitTask task) {
        emitJobBuffer.put(task.startR).put(task.startG).put(task.startB).put(task.startA);
        emitJobBuffer.put(task.midR).put(task.midG).put(task.midB).put(task.midA);
        emitJobBuffer.put(task.endR).put(task.endG).put(task.endB).put(task.endA);
    }

    // 写入 EmitJob.physics：不同运动模式复用 xyzw，但仍保持固定槽位顺序。
    public void writeEmitJobPhysicsVec4(ParticleEmitTask task) {
        switch (task.motionType) {
            case ParticleEmitTask.MOTION_CIRCULAR:
                emitJobBuffer.put(task.orbitPhase).put(task.spread).put(task.life).put(task.orbitPhaseRange);
                break;
            case ParticleEmitTask.MOTION_TURBULENT_RISE:
                emitJobBuffer.put(task.startSpeed).put(task.turbulentSpawnRadius).put(task.life).put(task.turbulentRadialExpansion);
                break;
            default:
                emitJobBuffer.put(task.startSpeed).put(task.spread).put(task.life).put(task.gravity);
                break;
        }
    }

    // 写入 EmitJob.render：xy 为出生尺寸，z 为基础旋转，w 为 SDF 形状类型。
    public void writeEmitJobRenderVec4(ParticleEmitTask task) {
        emitJobBuffer.put(task.sizeX).put(task.sizeY).put(task.rotation).put((float) task.shapeType);
    }

    // 写入 EmitJob.motion：按运动模式集中解释 yzw 的复用语义。
    public void writeEmitJobMotionVec4(ParticleEmitTask task) {
        if (task.materialId() == ParticleMaterialRegistry.RISING_SHOCKWAVE_ID) {
            emitJobBuffer.put((float) task.motionType).put(task.risingShockwaveDissolvePower)
                    .put(0.0F).put(0.0F);
            return;
        }
        switch (task.motionType) {
            case ParticleEmitTask.MOTION_RADIAL_DIFFUSION:
                emitJobBuffer.put((float) task.motionType).put(task.radialSpawnRadiusJitter)
                        .put(task.radialVerticalSpeed).put(task.radialVerticalSpeedJitter);
                break;
            case ParticleEmitTask.MOTION_TURBULENT_RISE:
                emitJobBuffer.put((float) task.motionType).put(task.turbulentCurlStrength)
                        .put(task.turbulentNoiseScale).put(task.turbulentNoiseSpeed);
                break;
            default:
                emitJobBuffer.put((float) task.motionType).put(task.orbitRadius)
                        .put(task.angularSpeed).put(task.verticalSpeed);
                break;
        }
    }

    // 写入 EmitJob.random：时间种子、任务序号、圆形出生模式和材质 ID。
    public void writeEmitJobRandomVec4(ParticleEmitTask task) {
        emitJobBuffer.put(totalTime).put((float) emitJobCount)
                .put((float) task.orbitSpawnMode).put((float) task.materialId());
    }

    // 写入 EmitJob.speedParams：速度曲线和方向符号，弧面方向模式会复用这些槽位。
    public void writeEmitJobSpeedParamsVec4(ParticleEmitTask task) {
        emitJobBuffer.put(task.startSpeed).put(task.endSpeed).put(task.speedCurvePower).put(task.directionSign);
    }

    // 写入 EmitJob.renderParams：z 对普通 billboard 是自旋速度，对噪声上升是出生高度下限，对上升冲击波是 UV 流速。
    public void writeEmitJobRenderParamsVec4(ParticleEmitTask task) {
        float renderParamZ;
        float renderParamW;
        if (task.materialId() == ParticleMaterialRegistry.RISING_SHOCKWAVE_ID) {
            renderParamZ = task.risingShockwaveUvFlowSpeed;
            renderParamW = task.risingShockwavePower;
        } else if (task.motionType == ParticleEmitTask.MOTION_TURBULENT_RISE) {
            renderParamZ = task.turbulentSpawnHeightMin;
            renderParamW = task.turbulentSpawnHeightMax;
        } else {
            renderParamZ = task.rotationSpeed;
            renderParamW = task.turbulentSpawnHeightMax;
        }
        emitJobBuffer.put(task.midColorTime).put(task.randomRotation ? 1.0F : 0.0F)
                .put(renderParamZ).put(renderParamW);
    }

    // 写入 EmitJob.sizeParams：xy 为中间尺寸，zw 为结束尺寸。
    public void writeEmitJobSizeParamsVec4(ParticleEmitTask task) {
        emitJobBuffer.put(task.midSizeX).put(task.midSizeY).put(task.endSizeX).put(task.endSizeY);
    }

    // 写入 EmitJob.sizeControl：x 为中间尺寸时间，y 为固定尺寸开关，zw 为 LIGHT_EFFECT 遮罩或上升冲击波 UV 平铺参数。
    public void writeEmitJobSizeControlVec4(ParticleEmitTask task) {
        if (task.materialId() == ParticleMaterialRegistry.RISING_SHOCKWAVE_ID) {
            emitJobBuffer.put(task.midSizeTime).put(task.fixedSizeScale ? 1.0F : 0.0F)
                    .put(task.risingShockwaveUvTileX).put(task.risingShockwaveUvTileY);
            return;
        }
        emitJobBuffer.put(task.midSizeTime).put(task.fixedSizeScale ? 1.0F : 0.0F)
                .put(task.lightEffectMaskRadius).put(task.lightEffectMaskSoftness);
    }

    // 返回每个 pipeline 的间接绘制顶点数，普通粒子为四顶点 quad，上升冲击波为程序化圆台三角形。
    public int vertexCountOfPipeline(int pipelineId) {
        return pipelineId == ParticleRenderPipeline.RISING_SHOCKWAVE
                ? RISING_SHOCKWAVE_VERTEX_COUNT
                : QUAD_VERTEX_COUNT;
    }

    private static float signedRandom(float seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return (float) ((value - Math.floor(value)) * 2.0 - 1.0);
    }

    private void uploadEmitJobs() {
        emitJobBuffer.flip();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, emitJobSsbo);
        if (emitJobBuffer.hasRemaining()) {
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, emitJobBuffer);
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    // 每帧 compute 前清空 pipeline 计数器，避免上一帧活跃数量残留。
    public void resetActiveCounts() {
        activeCountResetBuffer.clear();
        for (int i = 0; i < ParticleRenderPipeline.COUNT; i++) {
            activeCountResetBuffer.put(0);
        }
        activeCountResetBuffer.flip();

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, activeCountSsbo);
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, activeCountResetBuffer);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    /**
     * 兼容旧接口：把单发射器参数转换成一个本帧发射任务。
     */
    public void setEmitter(int emitPerFrame,
                           float x, float y, float z,
                           float dx, float dy, float dz,
                           float speed, float life, float size,
                           float spread, float gravity) {
        beginEmitJobs();
        addEmitJob(new ParticleEmitTask()
                .position(x, y, z)
                .direction(dx, dy, dz)
                .speed(speed)
                .life(life)
                .size(size, size, 0f)
                .spread(spread)
                .gravity(gravity)
                .color(1f, 1f, 1f, 1f)
                .endColor(1f, 1f, 1f, 0f)
                .shape(ParticleEmitTask.SHAPE_CIRCLE), emitPerFrame);
    }


    public void updateAndRender(float dt, Matrix4f projMatrix, Camera camera) {
        totalTime += dt;
        uploadEmitJobs();
        resetActiveCounts();

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, particleSsbo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, emitJobSsbo);
        ParticleMaterialRegistry.bindMaterialBuffer();
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, ACTIVE_INDEX_BUFFER_BINDING, activeIndexSsbo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, ACTIVE_COUNT_BUFFER_BINDING, activeCountSsbo);

        glUseProgram(gpushader.getComputeProgram());
        gpushader.updateComputeUniforms(dt, MAX_PARTICLES, emitJobCount, totalTime, ParticleRenderPipeline.COUNT);

        int numGroups = (MAX_PARTICLES + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;
        glDispatchCompute(numGroups, 1, 1);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_BUFFER_UPDATE_BARRIER_BIT);
        updateIndirectCommandsFromActiveCounts();
        glMemoryBarrier(GL_COMMAND_BARRIER_BIT);
        Matrix4f viewMatrix = MathUtil.createViewMatrix(camera);

//        BufferUploader.reset();
        glBindVertexArray(vao);
        glEnableVertexAttribArray(0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        boolean depthWriteEnabled = glGetBoolean(GL_DEPTH_WRITEMASK);
        glDepthMask(false);

        renderSdfPipeline(projMatrix, viewMatrix);
        renderLightEffectPipeline(projMatrix, viewMatrix);
        renderDirectedLightEffectPipeline(projMatrix, viewMatrix);
        renderMagicCircleEnergyPipeline(projMatrix, viewMatrix);
        renderExSwordWavePipeline(projMatrix, viewMatrix);
        renderStarTexturePipeline(projMatrix, viewMatrix);
        renderRisingShockwavePipeline(projMatrix, viewMatrix, camera);
        ParticleMaterialRegistry.unbindMaterialBuffer();

        glDepthMask(depthWriteEnabled);
        glDisable(GL_BLEND);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
        glUseProgram(0);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, ACTIVE_COUNT_BUFFER_BINDING, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, ACTIVE_INDEX_BUFFER_BINDING, 0);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);

        for (int i = 0; i < activePipelineTimeLeft.length; i++) {
            activePipelineTimeLeft[i] = Math.max(0.0F, activePipelineTimeLeft[i] - dt);
        }
    }

    // 渲染旧版 SDF 粒子批次，shapeType 继续决定圆形、心形、星形等几何。
    public void renderSdfPipeline(Matrix4f projMatrix, Matrix4f viewMatrix) {
        if (!isPipelineActive(ParticleRenderPipeline.SDF_BASIC)) return;
        gpushader.start();
        gpushader.loadMatrix(gpushader.render_uProjection, projMatrix);
        gpushader.loadMatrix(gpushader.render_uView, viewMatrix);
        gpushader.updateRenderUniforms(totalTime, ParticleRenderPipeline.SDF_BASIC, MAX_PARTICLES);
        drawPipelineIndirect(ParticleRenderPipeline.SDF_BASIC);
    }

    // 渲染三噪声光效粒子批次，CA0 输出核心，CA1 输出 shader 内配置的光晕。
    public void renderLightEffectPipeline(Matrix4f projMatrix, Matrix4f viewMatrix) {
        if (!isPipelineActive(ParticleRenderPipeline.LIGHT_EFFECT)) return;
        if (AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS == null) return;
        int atlasTextureId = AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.getId();
        if (atlasTextureId <= 0) return;

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, atlasTextureId);
        lightEffectShader.start();
        lightEffectShader.updateRenderUniforms(projMatrix, viewMatrix, totalTime, ParticleRenderPipeline.LIGHT_EFFECT, MAX_PARTICLES);
        drawPipelineIndirect(ParticleRenderPipeline.LIGHT_EFFECT);
    }

    // 渲染世界空间定向三噪声光效粒子批次，矩形不再始终朝向相机。
    public void renderDirectedLightEffectPipeline(Matrix4f projMatrix, Matrix4f viewMatrix) {
        if (!isPipelineActive(ParticleRenderPipeline.DIRECTED_LIGHT_EFFECT)) return;
        if (AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS == null) return;
        int atlasTextureId = AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.getId();
        if (atlasTextureId <= 0) return;

        // directed light 复用 LIGHT_EFFECT 的三噪声 atlas 纹理，只改顶点空间基底。
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, atlasTextureId);
        directedLightEffectShader.start();
        directedLightEffectShader.updateRenderUniforms(projMatrix, viewMatrix, totalTime,
                ParticleRenderPipeline.DIRECTED_LIGHT_EFFECT, MAX_PARTICLES);

        // 光柱平面需要正反两侧都能看见，仅在本批次临时关闭背面剔除。
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            GL11.glDisable(GL11.GL_CULL_FACE);
            drawPipelineIndirect(ParticleRenderPipeline.DIRECTED_LIGHT_EFFECT);
        } finally {
            if (cullEnabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
        }
    }

    // 渲染水平法阵能量粒子批次，使用独立顶点 Shader 固定在世界 XZ 平面。
    public void renderMagicCircleEnergyPipeline(Matrix4f projMatrix, Matrix4f viewMatrix) {
        if (!isPipelineActive(ParticleRenderPipeline.MAGIC_CIRCLE_ENERGY)) return;
        if (AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS == null) return;
        int atlasTextureId = AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.getId();
        if (atlasTextureId <= 0) return;

        // 两张法阵纹理都位于同一个自定义 atlas，通过材质 SSBO 中的 sprite UV 分区采样。
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, atlasTextureId);
        magicCircleEnergyShader.start();
        magicCircleEnergyShader.updateRenderUniforms(projMatrix, viewMatrix, totalTime,
                ParticleRenderPipeline.MAGIC_CIRCLE_ENERGY, MAX_PARTICLES);

        // 水平法阵需要从上下两面可见，只在本批次关闭背面剔除。
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            GL11.glDisable(GL11.GL_CULL_FACE);
            drawPipelineIndirect(ParticleRenderPipeline.MAGIC_CIRCLE_ENERGY);
        } finally {
            // 无论 draw 是否成功都恢复上游背面剔除状态。
            if (cullEnabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
        }
    }

    // 渲染根据粒子方向构造世界竖直平面的 EX 剑气批次，并保持正反两面可见。
    public void renderExSwordWavePipeline(Matrix4f projMatrix, Matrix4f viewMatrix) {
        if (!isPipelineActive(ParticleRenderPipeline.EX_SWORD_WAVE)) return;
        if (AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS == null) return;
        int atlasTextureId = AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.getId();
        if (atlasTextureId <= 0) return;

        // 三张剑气纹理位于同一个自定义 atlas，通过材质 SSBO 中的 sprite UV 采样。
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, atlasTextureId);
        exSwordWaveShader.start();
        exSwordWaveShader.updateRenderUniforms(projMatrix, viewMatrix, totalTime,
                ParticleRenderPipeline.EX_SWORD_WAVE, MAX_PARTICLES);

        // 测试阶段允许从剑气正反两侧观察，绘制结束后恢复上游剔除状态。
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            GL11.glDisable(GL11.GL_CULL_FACE);
            drawPipelineIndirect(ParticleRenderPipeline.EX_SWORD_WAVE);
        } finally {
            if (cullEnabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
        }
    }

    // 渲染星星贴图粒子批次，使用 ai_star R 通道和发射器 alpha 作为最终透明度。
    public void renderStarTexturePipeline(Matrix4f projMatrix, Matrix4f viewMatrix) {
        if (!isPipelineActive(ParticleRenderPipeline.STAR_TEXTURE)) return;
        if (AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS == null) return;
        int atlasTextureId = AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.getId();
        if (atlasTextureId <= 0) return;

        // 星星材质只采样主贴图，仍通过自定义 atlas 和材质 SSBO 取得 sprite UV。
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, atlasTextureId);
        starTextureShader.start();
        starTextureShader.updateRenderUniforms(projMatrix, viewMatrix, totalTime,
                ParticleRenderPipeline.STAR_TEXTURE, MAX_PARTICLES);
        drawPipelineIndirect(ParticleRenderPipeline.STAR_TEXTURE);
    }

    // 渲染程序化上升冲击波圆台批次，使用 t_fx_tile_0016、法线和 1-Fresnel 形成主体光柱。
    public void renderRisingShockwavePipeline(Matrix4f projMatrix, Matrix4f viewMatrix, Camera camera) {
        if (!isPipelineActive(ParticleRenderPipeline.RISING_SHOCKWAVE)) return;
        if (AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS == null) return;
        int atlasTextureId = AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.getId();
        if (atlasTextureId <= 0) return;

        // 上升冲击波主纹理位于自定义 atlas；圆台顶点由 shader 根据 gl_VertexID 程序化生成。
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, atlasTextureId);
        risingShockwaveShader.start();
        risingShockwaveShader.updateRenderUniforms(projMatrix, viewMatrix, totalTime,
                ParticleRenderPipeline.RISING_SHOCKWAVE, MAX_PARTICLES);
        // 水平 Fresnel 需要相机世界坐标，避免完整 3D 视线把圆台显隐错误带到上下方向。
        if (camera != null) {
            risingShockwaveShader.loadCameraPosition(camera.getPosition());
        }

        // 圆台光效在近距离可能从内部观察，临时关闭背面剔除避免半边消失。
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            GL11.glDisable(GL11.GL_CULL_FACE);
            drawPipelineIndirect(ParticleRenderPipeline.RISING_SHOCKWAVE, GL_TRIANGLES);
        } finally {
            if (cullEnabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
        }
    }

    // 把 activeCount SSBO 中的计数复制到 indirect command 的 instanceCount 字段，不经过 CPU 读回。
    public void updateIndirectCommandsFromActiveCounts() {
        glBindBuffer(GL_COPY_READ_BUFFER, activeCountSsbo);
        glBindBuffer(GL_COPY_WRITE_BUFFER, indirectCommandBuffer);
        for (int pipelineId = 0; pipelineId < ParticleRenderPipeline.COUNT; pipelineId++) {
            long readOffset = (long) pipelineId * Integer.BYTES;
            long writeOffset = (long) pipelineId * INDIRECT_COMMAND_STRIDE_BYTES + INDIRECT_INSTANCE_COUNT_OFFSET_BYTES;
            glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, readOffset, writeOffset, Integer.BYTES);
        }
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
        glBindBuffer(GL_COPY_READ_BUFFER, 0);
    }

    // 使用当前 pipeline 对应的 GPU indirect command 提交绘制。
    public void drawPipelineIndirect(int pipelineId) {
        drawPipelineIndirect(pipelineId, GL_TRIANGLE_STRIP);
    }

    // 使用指定 OpenGL 图元模式和当前 pipeline 对应的 GPU indirect command 提交绘制。
    public void drawPipelineIndirect(int pipelineId, int drawMode) {
        if (pipelineId < 0 || pipelineId >= ParticleRenderPipeline.COUNT) return;
        long commandOffset = (long) pipelineId * INDIRECT_COMMAND_STRIDE_BYTES;
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectCommandBuffer);
        glDrawArraysIndirect(drawMode, commandOffset);
    }

    // 判断指定渲染批次是否仍存在可能存活的粒子。
    public boolean isPipelineActive(int pipelineId) {
        return pipelineId >= 0 && pipelineId < activePipelineTimeLeft.length && activePipelineTimeLeft[pipelineId] > 0.0F;
    }

    public void cleanUp() {
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (particleSsbo != 0) {
            glDeleteBuffers(particleSsbo);
        }
        if (emitJobSsbo != 0) {
            glDeleteBuffers(emitJobSsbo);
        }
        if (activeIndexSsbo != 0) {
            glDeleteBuffers(activeIndexSsbo);
        }
        if (activeCountSsbo != 0) {
            glDeleteBuffers(activeCountSsbo);
        }
        if (indirectCommandBuffer != 0) {
            glDeleteBuffers(indirectCommandBuffer);
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (gpushader != null) {
            gpushader.cleanUp();
        }
        if (lightEffectShader != null) {
            lightEffectShader.cleanUp();
        }
        if (directedLightEffectShader != null) {
            directedLightEffectShader.cleanUp();
        }
        if (magicCircleEnergyShader != null) {
            magicCircleEnergyShader.cleanUp();
        }
        if (exSwordWaveShader != null) {
            exSwordWaveShader.cleanUp();
        }
        if (starTextureShader != null) {
            starTextureShader.cleanUp();
        }
        if (risingShockwaveShader != null) {
            risingShockwaveShader.cleanUp();
        }
        ParticleMaterialRegistry.cleanUp();
    }
}
