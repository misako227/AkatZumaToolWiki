package com.z227.akatzumatool.render.shader.post;

import com.z227.akatzumatool.AkatZumaTool;
import com.z227.akatzumatool.entity.sword.DimensionSlashConfig;
import com.z227.akatzumatool.render.finalRender.DimensionSlashScreenEffect;
import com.z227.akatzumatool.render.frameBuffer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;

public class FinalShader extends ShaderProgram {

    private static final ResourceLocation VERTEX_FILE = new ResourceLocation(AkatZumaTool.MODID, "shaders/post/final_shader.vsh");
    private static final ResourceLocation FRAGMENT_FILE = new ResourceLocation(AkatZumaTool.MODID, "shaders/post/final_shader.fsh");

    private int location_colourTexture;
    private int location_mainTexture;
    private int location_bloomTexture;
    private int location_bloomStrength;
    private int location_dimensionEffect;
    private int location_dimensionGlass;
    private int location_dimensionField;

    public FinalShader(ResourceLocation vertexFile, ResourceLocation fragmentFile) {
        super(vertexFile, fragmentFile);
    }

    public FinalShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }

    @Override
    protected void getAllUniformLocations() {
        location_colourTexture = super.getUniformLocation("colourTexture");
        location_mainTexture = super.getUniformLocation("mainTexture");
        location_bloomTexture = super.getUniformLocation("bloomTexture");
        location_bloomStrength = super.getUniformLocation("bloomStrength");
        location_dimensionEffect = super.getUniformLocation("DimensionEffect");
        location_dimensionGlass = super.getUniformLocation("DimensionGlass");
        location_dimensionField = super.getUniformLocation("DimensionField");
    }

    public void loadUniforms(){
        super.loadInt(location_colourTexture, 0);
        super.loadInt(location_mainTexture, 1);
        super.loadInt(location_bloomTexture, 2);
        super.loadFloat(location_bloomStrength, 1.5f);
        super.loadVector(location_dimensionEffect, new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
        super.loadVector(location_dimensionGlass, new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
        super.loadVector(location_dimensionField, new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
    }

    // 写入次元斩屏幕效果参数：蓝色重影、灰化、Voronoi 碎片错位和爆发参数。
    public void loadDimensionSlashEffect(DimensionSlashScreenEffect effect) {
        if (effect == null) {
            super.loadVector(location_dimensionEffect, new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
            super.loadVector(location_dimensionGlass, new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
            super.loadVector(location_dimensionField, new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
            return;
        }
        super.loadVector(location_dimensionEffect, new org.joml.Vector4f(effect.blueIntensity, effect.grayIntensity, effect.cutIntensity, effect.cutProgress));
        super.loadVector(location_dimensionGlass, new org.joml.Vector4f(effect.seed, effect.flashIntensity, effect.zoomBlurStrength, effect.contrastBoost));
        super.loadVector(location_dimensionField, new org.joml.Vector4f(effect.chromaticStrength, effect.vignetteStrength, effect.wallStrength, DimensionSlashConfig.CUT_STRENGTH));
    }

    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
    }
}
