package com.z227.akatzumatool.render.finalRender.bloomQueue;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.common.MathUtil;
import com.z227.akatzumatool.item.FlySwordHeldItemRenderer;
import com.z227.akatzumatool.render.renderType.FlySwordType.FlySwordHeldRenderType;
import com.z227.akatzumatool.render.renderType.FlySwordType.FlySwordHeldShader;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

// FlySwordHeldModelQueue 缓存玩家手中飞剑模型矩阵，并在后处理阶段统一重放透明模型到 bloom MRT。
public class FlySwordHeldModelQueue {
    public static final long MAX_STATE_AGE_TICKS = 2L; // 手持矩阵最多保留 2 tick，避免切换物品后残留。
    public static final float NORMAL_BLOOM_STRENGTH = 0.3F; // 普通飞剑 bloom 强度。
    public static final float PLUS_BLOOM_STRENGTH = 0.5F; // 真·飞剑 bloom 强度。
    public static final float FRESNEL_POWER = 3.0F; // 菲尼尔边缘收束指数。
    public static final float FRESNEL_EDGE_START = 0.16F; // 菲尼尔边缘带开始阈值。
    public static final float FRESNEL_EDGE_END = 0.52F; // 菲尼尔边缘带结束阈值。
    public static final float EMISSIVE_STRENGTH = 3.45F; // CA1 双噪声菲尼尔自发光基础强度。
    public static final float MODEL_ALPHA = 0.92F; // 飞剑 baked model 顶点透明度，不依赖原始纹理 alpha。
    public static final ResourceLocation FLY_SWORD_TEXTURE = new ResourceLocation(AkatZumaTool.MODID, "item/fly_sword_tex"); // 飞剑主纹理在 item/block atlas 中的位置。
    private static final int POSITION_X_OFFSET = 0; // BakedQuad 单顶点 X 坐标 int 偏移。
    private static final int POSITION_Y_OFFSET = 1; // BakedQuad 单顶点 Y 坐标 int 偏移。
    private static final int POSITION_Z_OFFSET = 2; // BakedQuad 单顶点 Z 坐标 int 偏移。
    private static final int UV_U_OFFSET = 4; // BakedQuad 单顶点 U 坐标 int 偏移。
    private static final int UV_V_OFFSET = 5; // BakedQuad 单顶点 V 坐标 int 偏移。
    private final List<FlySwordHeldModelState> states = new ArrayList<>(); // 本帧或上一帧提交的手持飞剑模型状态。
    private final Map<BakedModel, CachedFlySwordModel> cachedModels = new WeakHashMap<>(); // 按 baked model 缓存的静态顶点、UV 和法线数据。

    // 提交当前手持飞剑模型矩阵，真正绘制延后到后处理阶段。
    public void submit(BakedModel model, Matrix4f modelViewMatrix, boolean plusSword,
                       long gameTime, FlySwordHeldItemRenderer.FlySwordFlowParams flowParams) {
        if (model == null || modelViewMatrix == null) return;
        states.add(new FlySwordHeldModelState(model, modelViewMatrix, plusSword, gameTime, flowParams));
    }

    // 判断当前是否存在待重放的手持飞剑模型。
    public boolean hasActive() {
        return !states.isEmpty();
    }

    // 清理超过允许缓存 tick 的手持飞剑状态。
    public void clearExpired(long gameTime) {
        Iterator<FlySwordHeldModelState> iterator = states.iterator();
        while (iterator.hasNext()) {
            FlySwordHeldModelState state = iterator.next();
            if (state.isExpired(gameTime, MAX_STATE_AGE_TICKS)) {
                iterator.remove();
            }
        }
    }

    // 清空全部手持飞剑状态。
    public void clear() {
        states.clear();
    }

    // 清空 baked model 静态数据缓存，供资源重载或客户端世界退出后调用。
    public void clearCachedModels() {
        cachedModels.clear();
    }

    // 在后处理 MRT FBO 中重放全部手持飞剑透明模型。
    public void render(MultiBufferSource.BufferSource fboBuffer, float partialTick, int sceneTextureId, int sceneWidth, int sceneHeight) {
        if (!FlySwordHeldShader.isLoaded()) return;
        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        clearExpired(gameTime);
        if (states.isEmpty()) return;

        TextureAtlasSprite mainSprite = Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(FLY_SWORD_TEXTURE);
        TextureAtlasSprite noise1Sprite = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.T_FX_TILE_0012_TEXTURE);
        TextureAtlasSprite noise2Sprite = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.cellnoise_a);
        TextureAtlasSprite noise3Sprite = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.T_FX_TILE_0137_MOON_TEXTURE);

        // RenderType 负责绑定两个图集，这里只写入各 sprite 的 UV 范围和材质参数。
        FlySwordHeldShader.setSpriteUvs(mainSprite, noise1Sprite, noise2Sprite, noise3Sprite);
        FlySwordHeldShader.setFresnelParams(FRESNEL_POWER, FRESNEL_EDGE_START, FRESNEL_EDGE_END);
        FlySwordHeldShader.setEmissiveStrength(EMISSIVE_STRENGTH);
        FlySwordHeldShader.setSceneParams(sceneTextureId, sceneWidth, sceneHeight);
        float time = MathUtil.getClientTime(partialTick);

        for (FlySwordHeldModelState state : states) {
            renderState(fboBuffer, state, time);
        }
        states.clear();
    }

    // 渲染单次手持飞剑模型状态，按 state 写入不同矩阵和材质参数。
    public void renderState(MultiBufferSource.BufferSource fboBuffer, FlySwordHeldModelState state, float time) {
        float bloomStrength = state.plusSword ? PLUS_BLOOM_STRENGTH : NORMAL_BLOOM_STRENGTH;
        FlySwordHeldShader.setModelViewMat(state.modelViewMatrix);
        FlySwordHeldShader.setEffectParams(time, bloomStrength);
        FlySwordHeldShader.setGradientEndColor(state.plusSword ? 1.0F : 0.22F, state.plusSword ? 0.32F : 0.72F,
                state.plusSword ? 0.68F : 1.0F);
        FlySwordHeldShader.setGradientStartColor(0.20F, 0.82F, 1.0F);
        FlySwordHeldShader.setNoiseFlowParams(state.flowParams);

        VertexConsumer consumer = fboBuffer.getBuffer(FlySwordHeldRenderType.getRenderType());
        CachedFlySwordModel cachedModel = cachedModels.computeIfAbsent(state.model, this::buildCachedModel);
        writeModel(consumer, cachedModel);
        // Sampler1 固定读取纹理槽 1 的场景颜色；槽位 0 由 RenderType 绑定自定义噪声图集。
        RenderSystem.setShaderTexture(1, FlySwordHeldShader.getSceneTextureId());
        fboBuffer.endBatch(FlySwordHeldRenderType.getRenderType());
    }

    // 首次读取 baked model 时解析顶点、UV 和面法线，后续帧直接复用缓存数据。
    public CachedFlySwordModel buildCachedModel(BakedModel model) {
        CachedFlySwordModel cachedModel = new CachedFlySwordModel();
        RandomSource random = RandomSource.create(42L);
        List<BakedQuad> quads = model.getQuads(null, null, random);
        if (!quads.isEmpty()) {
            cacheQuads(cachedModel, quads);
            return cachedModel;
        }

        // 少数模型会按方向返回 quad，null 为空时再遍历方向兜底。
        for (Direction direction : Direction.values()) {
            random = RandomSource.create(42L);
            cacheQuads(cachedModel, model.getQuads(null, direction, random));
        }
        return cachedModel;
    }

    // 批量缓存 baked quads 的静态渲染数据。
    public void cacheQuads(CachedFlySwordModel cachedModel, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            CachedFlySwordQuad cachedQuad = cacheQuad(quad);
            if (cachedQuad != null) {
                cachedModel.quads.add(cachedQuad);
            }
        }
    }

    // 解析单个 baked quad 的顶点位置、UV 和稳定面法线。
    public CachedFlySwordQuad cacheQuad(BakedQuad quad) {
        int[] vertices = quad.getVertices();
        int vertexSize = vertices.length / 4;
        if (vertexSize <= UV_V_OFFSET) return null;

        // 从 quad 几何位置计算稳定面法线，避免依赖 BakedQuad packed normal 的内部偏移。
        Vector3f[] positions = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            int offset = i * vertexSize;
            positions[i] = new Vector3f(
                    Float.intBitsToFloat(vertices[offset + POSITION_X_OFFSET]),
                    Float.intBitsToFloat(vertices[offset + POSITION_Y_OFFSET]),
                    Float.intBitsToFloat(vertices[offset + POSITION_Z_OFFSET])
            );
        }
        Vector3f edgeA = new Vector3f(positions[1]).sub(positions[0]);
        Vector3f edgeB = new Vector3f(positions[2]).sub(positions[0]);
        Vector3f normal = edgeA.cross(edgeB);
        if (normal.lengthSquared() <= 0.000001F) {
            normal.set(0.0F, 1.0F, 0.0F);
        } else {
            normal.normalize();
        }

        float[] uvs = new float[8];
        for (int i = 0; i < 4; i++) {
            int offset = i * vertexSize;
            uvs[i * 2] = Float.intBitsToFloat(vertices[offset + UV_U_OFFSET]);
            uvs[i * 2 + 1] = Float.intBitsToFloat(vertices[offset + UV_V_OFFSET]);
        }
        return new CachedFlySwordQuad(positions, uvs, normal);
    }

    // 将缓存模型写入 VertexConsumer，不再解码 packed vertices 或计算法线。
    public void writeModel(VertexConsumer consumer, CachedFlySwordModel cachedModel) {
        for (CachedFlySwordQuad quad : cachedModel.quads) {
            for (int i = 0; i < 4; i++) {
                Vector3f position = quad.positions[i];
                consumer.vertex(position.x, position.y, position.z)
                    .uv(quad.uvs[i * 2], quad.uvs[i * 2 + 1])
                    .color(1.0F, 1.0F, 1.0F, MODEL_ALPHA)
                    .normal(quad.normal.x, quad.normal.y, quad.normal.z)
                    .endVertex();
            }
        }
    }

    // CachedFlySwordModel 保存一个 baked model 的全部静态 quad 渲染数据。
    public static class CachedFlySwordModel {
        public final List<CachedFlySwordQuad> quads = new ArrayList<>(); // 已解析的全部静态 quad。
    }

    // CachedFlySwordQuad 保存一个 quad 的四个顶点、UV 和共享面法线。
    public static class CachedFlySwordQuad {
        public final Vector3f[] positions; // 四个局部空间顶点位置。
        public final float[] uvs; // 四个顶点的主纹理 UV。
        public final Vector3f normal; // 四顶点共用的归一化面法线。

        public CachedFlySwordQuad(Vector3f[] positions, float[] uvs, Vector3f normal) {
            this.positions = positions;
            this.uvs = uvs;
            this.normal = normal;
        }
    }

}
