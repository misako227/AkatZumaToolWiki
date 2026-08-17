package com.z227.akatzumatool.render.finalRender.bloomQueue;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.z227.akatzumatool.common.GLBuffers.instancing.InstanceLayout;
import com.z227.akatzumatool.common.GLBuffers.instancing.InstanceVBO;
import com.z227.akatzumatool.entity.sword.SwordAuraEntity;
import com.z227.akatzumatool.entity.sword.SwordAuraVisualConfig;
import com.z227.akatzumatool.render.renderType.SwordAuraType.SwordAuraShader;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;
import java.util.List;

// SwordAuraInstancedRenderer 使用一份静态 OBJ 网格和一份实例 VBO 批量绘制剑气。
public class SwordAuraInstancedRenderer {
    public static final int POSITION_LOCATION = 0; // shader 中局部位置 attribute 的 location。
    public static final int UV_LOCATION = 1; // shader 中局部 UV attribute 的 location。
    public static final int MODEL_COL0_LOCATION = 2; // 实例 model 矩阵第 1 列 attribute 的 location。
    public static final int MODEL_COL1_LOCATION = 3; // 实例 model 矩阵第 2 列 attribute 的 location。
    public static final int MODEL_COL2_LOCATION = 4; // 实例 model 矩阵第 3 列 attribute 的 location。
    public static final int MODEL_COL3_LOCATION = 5; // 实例 model 矩阵第 4 列 attribute 的 location。
    public static final int VISUAL_LOCATION = 6; // 实例可视参数 attribute 的 location。
    public static final int INSTANCE_STRIDE_FLOATS = 20; // 每条实例数据包含 mat4 和 vec4 visual。
    public static final int DEFAULT_MAX_INSTANCES = 256; // 剑气单帧默认实例容量。
    public static final int TRIANGLE_VERTEX_COUNT_PER_QUAD = 6; // 一个 baked quad 转成两个三角形。
    public static final int POSITION_X_OFFSET = 0; // BakedQuad 顶点 X 偏移。
    public static final int POSITION_Y_OFFSET = 1; // BakedQuad 顶点 Y 偏移。
    public static final int POSITION_Z_OFFSET = 2; // BakedQuad 顶点 Z 偏移。
    public static final int UV_U_OFFSET = 4; // BakedQuad 顶点 U 偏移。
    public static final int UV_V_OFFSET = 5; // BakedQuad 顶点 V 偏移。
    public static final int[] QUAD_TO_TRIANGLE_ORDER = {0, 1, 2, 2, 3, 0}; // quad 顶点转三角形顺序。

    public final InstanceLayout instanceLayout; // 剑气实例数据布局。
    public final InstanceVBO instanceVBO; // 每帧上传剑气实例参数的 VBO。
    public int vaoID; // 静态 OBJ mesh VAO。
    public int positionVboID; // 静态 OBJ 局部位置 VBO。
    public int uvVboID; // 静态 OBJ 局部 UV VBO。
    public int vertexCount; // 静态 mesh 三角形顶点数量。
    public BakedModel sourceModel; // 当前已经转换成 mesh 的 baked model。

    public SwordAuraInstancedRenderer() {
        instanceLayout = InstanceLayout.create(INSTANCE_STRIDE_FLOATS)
                .attr(MODEL_COL0_LOCATION, 4, 0)
                .attr(MODEL_COL1_LOCATION, 4, 4)
                .attr(MODEL_COL2_LOCATION, 4, 8)
                .attr(MODEL_COL3_LOCATION, 4, 12)
                .attr(VISUAL_LOCATION, 4, 16);
        instanceVBO = new InstanceVBO(instanceLayout, DEFAULT_MAX_INSTANCES);
    }

    // 判断当前静态 mesh 是否已经可用于绘制。
    public boolean isReady(BakedModel model) {
        return vaoID != 0 && vertexCount > 0 && sourceModel == model;
    }

    // 确保 Forge baked OBJ 已经被转换成自管 VAO。
    public boolean ensureMesh(BakedModel model, TextureAtlasSprite targetSprite) {
        if (model == null || targetSprite == null) {
            return false;
        }
        if (isReady(model)) {
            return true;
        }
        cleanupMesh();
        buildMesh(model, targetSprite);
        if (vaoID != 0) {
            instanceVBO.attachTo(vaoID);
        }
        sourceModel = model;
        return vaoID != 0 && vertexCount > 0;
    }

    // 把 Forge baked quads 转成局部坐标三角形 mesh。
    public void buildMesh(BakedModel model, TextureAtlasSprite targetSprite) {
        List<BakedQuad> quads = model.getQuads(null, null, RandomSource.create(0L));
        vertexCount = quads.size() * TRIANGLE_VERTEX_COUNT_PER_QUAD;
        if (vertexCount <= 0) {
            return;
        }

        FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);
        FloatBuffer uvBuffer = BufferUtils.createFloatBuffer(vertexCount * 2);
        for (BakedQuad quad : quads) {
            writeQuadToMeshBuffers(quad, targetSprite, positionBuffer, uvBuffer);
        }
        positionBuffer.flip();
        uvBuffer.flip();
        createMeshBuffers(positionBuffer, uvBuffer);
    }

    // 把一个 baked quad 按两个三角形写入静态 mesh buffer。
    public void writeQuadToMeshBuffers(BakedQuad quad, TextureAtlasSprite targetSprite,
                                       FloatBuffer positionBuffer, FloatBuffer uvBuffer) {
        int[] vertices = quad.getVertices();
        int stride = com.mojang.blaze3d.vertex.DefaultVertexFormat.BLOCK.getIntegerSize();
        TextureAtlasSprite sourceSprite = quad.getSprite();

        for (int index : QUAD_TO_TRIANGLE_ORDER) {
            int base = index * stride;
            float localX = Float.intBitsToFloat(vertices[base + POSITION_X_OFFSET]);
            float localY = Float.intBitsToFloat(vertices[base + POSITION_Y_OFFSET]);
            float localZ = Float.intBitsToFloat(vertices[base + POSITION_Z_OFFSET]);
            float sourceU = Float.intBitsToFloat(vertices[base + UV_U_OFFSET]);
            float sourceV = Float.intBitsToFloat(vertices[base + UV_V_OFFSET]);
            Vec3 local = buildCorrectedLocal(localX, localY, localZ);

            positionBuffer.put((float) local.x).put((float) local.y).put((float) local.z);
            uvBuffer.put(remapToLocalU(sourceSprite, targetSprite, sourceU));
            uvBuffer.put(remapToLocalV(sourceSprite, targetSprite, sourceV));
        }
    }

    // 创建静态 mesh 的 VAO 和两个基础 VBO。
    public void createMeshBuffers(FloatBuffer positionBuffer, FloatBuffer uvBuffer) {
        vaoID = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoID);

        positionVboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVboID);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, positionBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(POSITION_LOCATION, 3, GL11.GL_FLOAT, false, 0, 0L);

        uvVboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvVboID);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(UV_LOCATION, 2, GL11.GL_FLOAT, false, 0, 0L);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    // 渲染本帧所有可见剑气实例。
    public boolean render(List<SwordAuraEntity> entities, SwordAuraQueue queue, BakedModel model,
                          TextureAtlasSprite targetSprite, float partialTick) {
        if (!ensureMesh(model, targetSprite)) {
            return false;
        }

        FloatBuffer buffer = instanceVBO.getBuffer();
        buffer.clear();
        int instanceCount = 0;
        for (SwordAuraEntity aura : entities) {
            if (instanceCount >= instanceVBO.maxInstances) {
                break;
            }
            instanceCount += writeAuraInstance(buffer, aura, queue, partialTick) ? 1 : 0;
        }
        if (instanceCount <= 0) {
            return true;
        }

        instanceVBO.update(buffer, instanceCount);
        drawInstanced(instanceCount);
        return true;
    }

    // 写入单个剑气实例数据，返回该剑气是否需要绘制。
    public boolean writeAuraInstance(FloatBuffer buffer, SwordAuraEntity aura, SwordAuraQueue queue, float partialTick) {
        float visualProgress = aura.isPreviewStatic() ? 1.0F : aura.getAgeProgress(partialTick);
        float fade = queue.getFade(aura, partialTick);
        if (fade <= 0.01F) {
            return false;
        }

        SwordAuraQueue.AuraBasis basis = queue.buildAuraBasis(aura, partialTick, visualProgress);
        float alpha = Mth.clamp(fade * (float) SwordAuraVisualConfig.OBJ_ALPHA, 0.0F, 1.0F);
        float gradientSelect = queue.getGradientSelect(aura) > 0 ? 1.0F : 0.0F;
        float reveal = queue.getRevealProgress(aura, partialTick);
        writeBasisColumns(buffer, basis);
        buffer.put(gradientSelect).put(reveal).put((float) SwordAuraVisualConfig.BLOOM_STRENGTH_SCALE).put(alpha);
        return true;
    }

    // 写入局部坐标到世界坐标的 model 矩阵四列。
    public void writeBasisColumns(FloatBuffer buffer, SwordAuraQueue.AuraBasis basis) {
        buffer.put((float) (basis.side.x * basis.scale)).put((float) (basis.side.y * basis.scale)).put((float) (basis.side.z * basis.scale)).put(0.0F);
        buffer.put((float) (basis.up.x * basis.scale)).put((float) (basis.up.y * basis.scale)).put((float) (basis.up.z * basis.scale)).put(0.0F);
        buffer.put((float) (basis.forward.x * basis.scale)).put((float) (basis.forward.y * basis.scale)).put((float) (basis.forward.z * basis.scale)).put(0.0F);
        buffer.put((float) basis.center.x).put((float) basis.center.y).put((float) basis.center.z).put(1.0F);
    }

    // 执行实例化 draw call，并把剑气直接写入当前 MRT FBO。
    public void drawInstanced(int instanceCount) {
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int previousVao = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousProgram = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
            RenderSystem.setShaderTexture(0, AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS_LOCATION);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            int atlasTextureId = AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.getId();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlasTextureId);

            // 实例化绘制必须显式绑定自管 VAO，避免后续 RenderType 批处理拿到空 VAO。
            GL30.glBindVertexArray(vaoID);

            // 直接调用 ShaderInstance.apply 时不会自动写入 Minecraft 默认投影矩阵。
            SwordAuraShader.setProjection(RenderSystem.getProjectionMatrix());
            SwordAuraShader.getShader().setSampler("Sampler0", atlasTextureId);
            SwordAuraShader.getShader().apply();
            instanceVBO.enable(POSITION_LOCATION, UV_LOCATION);
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, vertexCount, instanceCount);
            instanceVBO.disable(POSITION_LOCATION, UV_LOCATION);
        } finally {
            SwordAuraShader.getShader().clear();
            GL20.glUseProgram(previousProgram);
            GL13.glActiveTexture(previousActiveTexture);
            GL30.glBindVertexArray(previousVao);
            if (blendEnabled) {
                GL11.glEnable(GL11.GL_BLEND);
            } else {
                GL11.glDisable(GL11.GL_BLEND);
            }
            if (cullEnabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
            GL11.glDepthFunc(depthFunc);
            GL11.glDepthMask(depthMask);
        }
    }

    // 构建已经应用 OBJ 坐标修正的局部坐标。
    public Vec3 buildCorrectedLocal(float localX, float localY, float localZ) {
        Vec3 local = rotateLocal(new Vec3(localX, localY, localZ * SwordAuraVisualConfig.OBJ_FORWARD_SIGN));
        return local.add(SwordAuraVisualConfig.OBJ_SIDE_OFFSET,
                SwordAuraVisualConfig.OBJ_UP_OFFSET,
                SwordAuraVisualConfig.OBJ_FORWARD_OFFSET);
    }

    // 应用 OBJ 导出坐标系修正。
    public Vec3 rotateLocal(Vec3 local) {
        Vec3 afterYaw = rotateAroundY(local, Math.toRadians(SwordAuraVisualConfig.OBJ_YAW_DEGREES));
        Vec3 afterPitch = rotateAroundX(afterYaw, Math.toRadians(SwordAuraVisualConfig.OBJ_PITCH_DEGREES));
        return rotateAroundZ(afterPitch, Math.toRadians(SwordAuraVisualConfig.OBJ_ROLL_DEGREES));
    }

    public Vec3 rotateAroundX(Vec3 value, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(value.x, value.y * cos - value.z * sin, value.y * sin + value.z * cos);
    }

    public Vec3 rotateAroundY(Vec3 value, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(value.x * cos + value.z * sin, value.y, -value.x * sin + value.z * cos);
    }

    public Vec3 rotateAroundZ(Vec3 value, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(value.x * cos - value.y * sin, value.x * sin + value.y * cos, value.z);
    }

    // 把 baked quad 源 UV 转成 sword1 sprite 的 0 到 1 局部 U。
    public float remapToLocalU(TextureAtlasSprite sourceSprite, TextureAtlasSprite targetSprite, float sourceU) {
        if (sourceSprite == null) {
            return Mth.clamp(targetSprite.getUOffset(sourceU) / 16.0F, 0.0F, 1.0F);
        }
        return Mth.clamp(sourceSprite.getUOffset(sourceU) / 16.0F, 0.0F, 1.0F);
    }

    // 把 baked quad 源 UV 转成 sword1 sprite 的 0 到 1 局部 V。
    public float remapToLocalV(TextureAtlasSprite sourceSprite, TextureAtlasSprite targetSprite, float sourceV) {
        if (sourceSprite == null) {
            return Mth.clamp(targetSprite.getVOffset(sourceV) / 16.0F, 0.0F, 1.0F);
        }
        return Mth.clamp(sourceSprite.getVOffset(sourceV) / 16.0F, 0.0F, 1.0F);
    }

    // 清理全部 GL 资源。
    public void cleanup() {
        cleanupMesh();
        instanceVBO.cleanup();
    }

    // 清理静态 mesh 相关 GL 资源。
    public void cleanupMesh() {
        if (positionVboID != 0) {
            GL15.glDeleteBuffers(positionVboID);
            positionVboID = 0;
        }
        if (uvVboID != 0) {
            GL15.glDeleteBuffers(uvVboID);
            uvVboID = 0;
        }
        if (vaoID != 0) {
            GL30.glDeleteVertexArrays(vaoID);
            vaoID = 0;
        }
        vertexCount = 0;
        sourceModel = null;
    }
}
