package com.z227.akatzumatool.render.texture;

import com.z227.akatzumatool.render.gpu.material.ParticleMaterialRegistry;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AtlasReloadListener implements PreparableReloadListener {
    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier barrier,
            ResourceManager manager,
            ProfilerFiller prepProfiler,
            ProfilerFiller applyProfiler,
            Executor prepExecutor,
            Executor applyExecutor) {

        // SpriteLoader.create 会读取 MY_ENTITY_ATLAS_LOCATION 对应的 atlas JSON
        // 即 assets/yourmod/atlases/my_entities.png.json
        SpriteLoader spriteLoader = SpriteLoader.create(AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS);

        return spriteLoader
                // 第三个参数 mipLevel: 一般实体纹理不需要 mipmap 传 0
                .loadAndStitch(manager, AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS_LOCATION, 4, prepExecutor)
                .thenCompose(SpriteLoader.Preparations::waitForUpload)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(preparations -> {
                    // 这步在主线程（apply 阶段）执行，上传纹理到 GPU
                    AkatZumaTextureAtlas.AKATZUMA_TOOL_ATLAS.upload(preparations);
                    // 每次资源重载后重新设置整个 atlas 的线性、mipmap 和各向异性过滤。
                    AkatZumaTextureAtlas.applyLinearFilter(true);
                    ParticleMaterialRegistry.markDirty();
                }, applyExecutor);
    }
}
