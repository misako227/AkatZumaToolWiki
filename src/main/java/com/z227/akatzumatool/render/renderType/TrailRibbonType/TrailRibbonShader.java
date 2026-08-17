package com.z227.akatzumatool.render.renderType.TrailRibbonType;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.z227.akatzumatool.AkatZumaTool;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

import java.io.IOException;

public class TrailRibbonShader {
    public static ShaderInstance TrailRibbonShader;

    public static AbstractUniform spriteUV0;
    public static AbstractUniform gameTime;



    public static ShaderInstance reloadShaders(ResourceProvider manager) throws IOException {
        ShaderInstance shader = new ShaderInstance(
                manager,
                new ResourceLocation(AkatZumaTool.MODID, "trail_ribbon_shader"),
                DefaultVertexFormat.POSITION_COLOR_TEX
        );
        return shader;
    }

    public static void onLoad(ShaderInstance shader) {
        TrailRibbonShader = shader;
        spriteUV0 = shader.safeGetUniform("SpriteUV0");
        gameTime = shader.safeGetUniform("GameTime");

    }


}
