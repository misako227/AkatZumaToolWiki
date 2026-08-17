package com.z227.akatzumatool.render.finalRender.bloomQueue;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.z227.akatzumatool.common.MathUtil;
import com.z227.akatzumatool.common.render.BeamRender;
import com.z227.akatzumatool.entity.coin.RailgunBeamEntity;
import com.z227.akatzumatool.render.finalRender.queue.EntityQueue;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinBeamRenderType;
import com.z227.akatzumatool.render.renderType.CoinRenderType.CoinBeamShader;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;


public class RailgunBeamQueue extends EntityQueue<RailgunBeamEntity> {

    public RailgunBeamQueue() {
        super();
    }



    @Override
    public void render(MultiBufferSource.BufferSource fboBuffer, Camera camera, float parTick, Matrix4f viewMatrix) {
        if(!CoinBeamShader.isLoaded()) return;
        BeamRender.BeamStyle style = BeamRender.NORMAL;
        float time = MathUtil.getClientTime(parTick);
        CoinBeamShader.setEffectParams(time, style.bloomStrength, style.noiseStrength, 0.0f);
        CoinBeamShader.setRenderFlags(0, 1, 0, 0);
        CoinBeamShader.setBeamColors(
                style.coreR, style.coreG, style.coreB,
                style.innerR, style.innerG, style.innerB,
                style.outerR, style.outerG, style.outerB
        );
        CoinBeamShader.setView(viewMatrix);
        VertexConsumer renderType = fboBuffer.getBuffer(CoinBeamRenderType.getRenderType());

        for (RailgunBeamEntity beam : entities) {
            BeamRender.writeBeam(renderType, beam, camera, parTick, style);
        }
        fboBuffer.endBatch(CoinBeamRenderType.getRenderType());
    }


}
