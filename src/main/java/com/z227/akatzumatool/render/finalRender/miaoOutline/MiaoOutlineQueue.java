package com.z227.akatzumatool.render.finalRender.miaoOutline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.z227.akatzumatool.render.renderType.MiaoOutlineType.MiaoOutlineDepthMaskRenderType;
import com.z227.akatzumatool.render.renderType.MiaoOutlineType.MiaoOutlineDepthMaskShader;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// MiaoOutlineQueue 保存本帧 UE5 风格描边目标，并负责把捕获实体写入 CA2 深度 mask。
public class MiaoOutlineQueue {
    private final List<MiaoOutlineTask> tasks = new ArrayList<>(); // 当前帧 Miao 描边任务。

    public void add(Entity entity, MiaoOutlineStyle style) {
        if (entity == null) return;
        tasks.add(new MiaoOutlineTask(entity, style == null ? MiaoOutlineStyle.AUTO_TRACKING_RED : style));
    }

    public boolean hasTasks() {
        return !tasks.isEmpty();
    }

    public void clear() {
        for (MiaoOutlineTask task : tasks) {
            if (task != null && task.entity != null) {
                MiaoOutlineTargetMaskStore.clear(task.entity.getId());
            }
        }
        tasks.clear();
    }

    // 按 Miao 类型分组，保证每个类型使用自己的后处理参数。
    public Map<MiaoOutlineStyle.Kind, List<MiaoOutlineTask>> groupTasksByKind() {
        Map<MiaoOutlineStyle.Kind, List<MiaoOutlineTask>> groupedTasks = new EnumMap<>(MiaoOutlineStyle.Kind.class);
        for (MiaoOutlineTask task : tasks) {
            if (task == null || task.entity == null || task.style == null) continue;
            groupedTasks.computeIfAbsent(task.style.kind, kind -> new ArrayList<>()).add(task);
        }
        return groupedTasks;
    }

    // 把指定类型目标写入 mainFBO.CA2：R 为归一化 view depth，G 为目标 mask。
    public void renderDepthMask(MultiBufferSource.BufferSource fboBuffer, List<MiaoOutlineTask> renderTasks,
                                Camera camera, float partialTick, Matrix4f viewMatrix, MiaoOutlineStyle style) {
        if (renderTasks == null || renderTasks.isEmpty() || !MiaoOutlineDepthMaskShader.isLoaded()) return;

        MiaoOutlineStyle safeStyle = style == null ? MiaoOutlineStyle.AUTO_TRACKING_RED : style;
        MiaoOutlineDepthMaskShader.setView(viewMatrix);
        MiaoOutlineDepthMaskShader.setDepthParams(0.0f, safeStyle.depthRange, 1.0f, 0.01f);
        Set<ResourceLocation> renderTypes = new HashSet<>();
        VertexConsumer fallbackConsumer = null;

        for (MiaoOutlineTask task : renderTasks) {
            if (task.entity == null || !task.entity.isAlive()) continue;

            if (writeCapturedEntityMask(fboBuffer, task.entity, renderTypes)) {
                continue;
            }

            ResourceLocation fallbackTexture = MiaoOutlineDepthMaskRenderType.WHITE_TEXTURE;
            if (fallbackConsumer == null) {
                fallbackConsumer = fboBuffer.getBuffer(MiaoOutlineDepthMaskRenderType.getRenderType(fallbackTexture));
            }
            writeEntityBox(fallbackConsumer, task.entity, partialTick);
            renderTypes.add(fallbackTexture);
        }

        for (ResourceLocation texture : renderTypes) {
            fboBuffer.endBatch(MiaoOutlineDepthMaskRenderType.getRenderType(texture));
        }
    }

    // 重放原版实体渲染阶段捕获到的顶点批次，优先保留原纹理 alpha 剔除。
    public static boolean writeCapturedEntityMask(MultiBufferSource.BufferSource fboBuffer, Entity entity, Set<ResourceLocation> renderTypes) {
        if (fboBuffer == null || entity == null) return false;

        MiaoOutlineCapturedMaskBuffer maskBuffer = MiaoOutlineTargetMaskStore.get(entity.getId());
        if (maskBuffer == null || maskBuffer.isEmpty()) return false;

        boolean wroteAny = false;
        for (MiaoOutlineCapturedBatch batch : maskBuffer.getBatches()) {
            ResourceLocation texture = batch.hasTexturedMask() ? batch.texture : MiaoOutlineDepthMaskRenderType.WHITE_TEXTURE;
            VertexConsumer consumer = fboBuffer.getBuffer(MiaoOutlineDepthMaskRenderType.getRenderType(texture));
            if (writeCapturedBatchAsTriangles(consumer, batch)) {
                renderTypes.add(texture);
                wroteAny = true;
            }
        }

        return wroteAny;
    }

    // 根据原始 primitive mode 把捕获批次展开为 triangles。
    public static boolean writeCapturedBatchAsTriangles(VertexConsumer consumer, MiaoOutlineCapturedBatch batch) {
        if (consumer == null || batch == null || batch.isEmpty()) return false;
        List<MiaoOutlineCapturedVertex> vertices = batch.getVertices();

        if (batch.mode == VertexFormat.Mode.QUADS) {
            return writeQuadBatchAsTriangles(consumer, vertices);
        }
        if (batch.mode == VertexFormat.Mode.TRIANGLES) {
            return writeTriangleBatch(consumer, vertices);
        }
        if (batch.mode == VertexFormat.Mode.TRIANGLE_STRIP) {
            return writeTriangleStripBatch(consumer, vertices);
        }
        if (batch.mode == VertexFormat.Mode.TRIANGLE_FAN) {
            return writeTriangleFanBatch(consumer, vertices);
        }
        return false;
    }

    // 把 QUADS 批次转换为两个三角形一组。
    public static boolean writeQuadBatchAsTriangles(VertexConsumer consumer, List<MiaoOutlineCapturedVertex> vertices) {
        int quadVertexCount = vertices.size() - vertices.size() % 4;
        if (quadVertexCount < 4) return false;
        for (int i = 0; i < quadVertexCount; i += 4) {
            MiaoOutlineCapturedVertex v0 = vertices.get(i);
            MiaoOutlineCapturedVertex v1 = vertices.get(i + 1);
            MiaoOutlineCapturedVertex v2 = vertices.get(i + 2);
            MiaoOutlineCapturedVertex v3 = vertices.get(i + 3);
            writeTriangle(consumer, v0, v1, v2);
            writeTriangle(consumer, v2, v3, v0);
        }
        return true;
    }

    // 把 TRIANGLES 批次按每三个顶点直接写入。
    public static boolean writeTriangleBatch(VertexConsumer consumer, List<MiaoOutlineCapturedVertex> vertices) {
        int triangleVertexCount = vertices.size() - vertices.size() % 3;
        if (triangleVertexCount < 3) return false;
        for (int i = 0; i < triangleVertexCount; i += 3) {
            writeTriangle(consumer, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2));
        }
        return true;
    }

    // 把 TRIANGLE_STRIP 批次按奇偶顺序展开为三角形。
    public static boolean writeTriangleStripBatch(VertexConsumer consumer, List<MiaoOutlineCapturedVertex> vertices) {
        if (vertices.size() < 3) return false;
        for (int i = 2; i < vertices.size(); i++) {
            if ((i & 1) == 0) {
                writeTriangle(consumer, vertices.get(i - 2), vertices.get(i - 1), vertices.get(i));
            } else {
                writeTriangle(consumer, vertices.get(i - 1), vertices.get(i - 2), vertices.get(i));
            }
        }
        return true;
    }

    // 把 TRIANGLE_FAN 批次以第一个顶点为中心展开为三角形。
    public static boolean writeTriangleFanBatch(VertexConsumer consumer, List<MiaoOutlineCapturedVertex> vertices) {
        if (vertices.size() < 3) return false;
        MiaoOutlineCapturedVertex center = vertices.get(0);
        for (int i = 2; i < vertices.size(); i++) {
            writeTriangle(consumer, center, vertices.get(i - 1), vertices.get(i));
        }
        return true;
    }

    // 写入一个捕获三角形，颜色固定为白色，实际 mask 强度由 shader uniform 写入。
    public static void writeTriangle(VertexConsumer consumer, MiaoOutlineCapturedVertex v0, MiaoOutlineCapturedVertex v1, MiaoOutlineCapturedVertex v2) {
        writeVertex(consumer, v0);
        writeVertex(consumer, v1);
        writeVertex(consumer, v2);
    }

    // 按 POSITION_TEX_COLOR_NORMAL 写入捕获顶点。
    public static void writeVertex(VertexConsumer consumer, MiaoOutlineCapturedVertex vertex) {
        consumer.vertex(vertex.x, vertex.y, vertex.z)
                .uv(vertex.hasUv ? vertex.u : 0.0f, vertex.hasUv ? vertex.v : 0.0f)
                .color(1.0f, 1.0f, 0.0f, 1.0f)
                .normal(vertex.hasNormal ? vertex.normalX : 0.0f, vertex.hasNormal ? vertex.normalY : 1.0f, vertex.hasNormal ? vertex.normalZ : 0.0f)
                .endVertex();
    }

    // 捕获失败时把实体 AABB 写入 CA2，保证调试阶段至少能看到目标轮廓。
    public static void writeEntityBox(VertexConsumer consumer, Entity entity, float partialTick) {
        if (consumer == null || entity == null) return;

        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY());
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        AABB box = entity.getBoundingBox().move(x - entity.getX(), y - entity.getY(), z - entity.getZ()).inflate(0.08D);
        writeAxisAlignedBox(consumer, box);
    }

    // 把 AABB 的六个面写成三角形。
    public static void writeAxisAlignedBox(VertexConsumer consumer, AABB box) {
        writeQuad(consumer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ);
        writeQuad(consumer, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ);
        writeQuad(consumer, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ);
        writeQuad(consumer, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ);
        writeQuad(consumer, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ);
        writeQuad(consumer, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.minY, box.minZ, box.minX, box.minY, box.minZ);
    }

    // 把一个四边形拆成两个三角形写入 fallback mask。
    public static void writeQuad(VertexConsumer consumer,
                                 double x0, double y0, double z0,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 double x3, double y3, double z3) {
        writeFallbackVertex(consumer, x0, y0, z0);
        writeFallbackVertex(consumer, x1, y1, z1);
        writeFallbackVertex(consumer, x2, y2, z2);
        writeFallbackVertex(consumer, x2, y2, z2);
        writeFallbackVertex(consumer, x3, y3, z3);
        writeFallbackVertex(consumer, x0, y0, z0);
    }

    // fallback 顶点使用白图和默认法线，由 shader 的 uView 转成 view-space。
    public static void writeFallbackVertex(VertexConsumer consumer, double x, double y, double z) {
        consumer.vertex(x, y, z)
                .uv(0.0f, 0.0f)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .normal(0.0f, 1.0f, 0.0f)
                .endVertex();
    }
}
