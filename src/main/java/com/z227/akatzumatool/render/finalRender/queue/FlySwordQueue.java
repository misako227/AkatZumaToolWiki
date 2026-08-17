package com.z227.akatzumatool.render.finalRender.queue;

import com.z227.akatzumatool.common.render.TrailRibbonRenderer;
import com.z227.akatzumatool.entity.FlySwordEntity;
import com.z227.akatzumatool.entity.FlySwordEntityRender;
import com.z227.akatzumatool.render.renderType.TrailRibbonType.TrailRibbonRenderType;
import com.z227.akatzumatool.render.renderType.TrailRibbonType.TrailRibbonShader;
import com.z227.akatzumatool.render.texture.AkatZumaTextureAtlas;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class FlySwordQueue extends EntityQueue<FlySwordEntity>{
    public Matrix4f modelMatrix;
    static TextureAtlasSprite sprite2;

    public FlySwordQueue() {
        super();
        sprite2 = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.multi_gradient);
    }

    public void updateModelMatrix(Matrix4f model){
        this.modelMatrix = new Matrix4f(model);
    }


    public void render(MultiBufferSource.BufferSource fboBuffer, Camera camera, float parTick, Matrix4f viewMatrix){
        if(sprite2 == null){
            sprite2 = AkatZumaTextureAtlas.getTextureLocation(AkatZumaTextureAtlas.multi_gradient);
        }
        if (TrailRibbonShader.spriteUV0 != null) {
            TrailRibbonShader.spriteUV0.set(sprite2.getU0(), sprite2.getV0(), sprite2.getU1(), sprite2.getV1());
        }


        for(FlySwordEntity entity : entities){
            FlySwordEntityRender.renderTrail(entity, parTick, modelMatrix, fboBuffer, camera.getPosition());
        }
        fboBuffer.endBatch(TrailRibbonRenderType.getRenderType());
    }

}
