package com.z227.akatzumatool.render.finalRender.miaoOutline;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Optional;

// MiaoOutlineRenderTypeFilter 判断实体捕获阶段哪些 RenderType 不应参与描边 mask。
public class MiaoOutlineRenderTypeFilter {
    // 判断当前 RenderType 是否应该捕获到 Miao 描边 mask。
    public static boolean shouldCapture(RenderType renderType, Optional<ResourceLocation> texture) {
        if (renderType == null) return false;
        if (isGlintRenderType(renderType)) return false;
        return texture.isEmpty() || !isGlintTexture(texture.get());
    }

    // 通过 RenderType 名称过滤附魔 glint / foil 相关批次。
    public static boolean isGlintRenderType(RenderType renderType) {
        if (renderType == null) return false;
        String renderTypeName = renderType.toString().toLowerCase(Locale.ROOT);
        return renderTypeName.contains("glint") || renderTypeName.contains("foil");
    }

    // 通过纹理路径过滤附魔流光纹理，作为名称判断的兜底。
    public static boolean isGlintTexture(ResourceLocation texture) {
        if (texture == null) return false;
        String namespace = texture.getNamespace().toLowerCase(Locale.ROOT);
        String path = texture.getPath().toLowerCase(Locale.ROOT);
        if (!"minecraft".equals(namespace)) return path.contains("enchanted_glint") || path.contains("glint");
        return path.contains("textures/misc/enchanted_glint") || path.contains("glint");
    }
}
