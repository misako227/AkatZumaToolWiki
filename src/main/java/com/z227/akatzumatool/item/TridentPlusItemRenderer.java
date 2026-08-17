package com.z227.akatzumatool.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.z227.akatzumatool.event.client.ClientKeyChargeRegistry;
import com.z227.akatzumatool.render.renderType.TridentPlusType.TridentPlusGlowRenderType;
import com.z227.akatzumatool.render.renderType.TridentPlusType.TridentPlusGlowShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import com.mojang.math.Axis;

// TridentPlusItemRenderer 使用原版三叉戟实体模型渲染玩家手中的天雷战戟。
public class TridentPlusItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static ItemDisplayContext currentDisplayContext = ItemDisplayContext.NONE; // 当前物品渲染上下文，由 BakedModel.applyTransform 写入。
    private static final float TRIDENT_GLOW_CHARGE_TICKS = 20.0F; // 战戟蓄力蓝光达到满强度的视觉 tick。
    private static final float TRIDENT_GLOW_STRENGTH = 0.72F; // 战戟蓝光覆盖层基础强度。
    private static final int FULL_BRIGHT_LIGHT = 15728880; // 蓝光覆盖层使用满亮度，避免环境光压暗。
    private TridentModel model; // 原版三叉戟实体模型。

    public TridentPlusItemRenderer() {
        this(Minecraft.getInstance().getBlockEntityRenderDispatcher());
    }

    public TridentPlusItemRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher, Minecraft.getInstance().getEntityModels());
    }

    // 记录当前渲染上下文，具体方向由物品 JSON 的 display transform 控制。
    public static void prepareRenderContext(ItemDisplayContext displayContext) {
        currentDisplayContext = displayContext;
    }

    // 渲染自定义物品模型，Java 只绘制原版三叉戟模型，手持方向交给 JSON transform。
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        TridentModel tridentModel = getModel();
        poseStack.pushPose();
        // 原版三叉戟 BEWLR 需要做 Y/Z 翻转，普通手持和蓄力姿态由不同 JSON 模型控制。
        poseStack.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(bufferSource, tridentModel.renderType(TridentModel.TEXTURE), false, stack.hasFoil());
        tridentModel.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
//        renderChargeGlow(stack, displayContext, poseStack, bufferSource, packedOverlay, tridentModel);
        poseStack.popPose();
    }

    // 蓄力时追加一层蓝色自发光模型覆盖层，让整把天雷战戟发蓝光。
    public void renderChargeGlow(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedOverlay, TridentModel tridentModel) {
        if (!shouldRenderChargeGlow(stack, displayContext)) return;
        if (!TridentPlusGlowShader.isLoaded()) return;
        float chargeProgress = getChargeProgress(stack);
        boolean fullyCharged = chargeProgress >= 1.0F;
        Minecraft minecraft = Minecraft.getInstance();
        float time = minecraft.level == null ? 0.0F : (minecraft.level.getGameTime() + minecraft.getFrameTime()) / 20.0F;
        TridentPlusGlowShader.setGlowParams(time, chargeProgress, TRIDENT_GLOW_STRENGTH, fullyCharged ? 1.0F : 0.0F);
        VertexConsumer glowConsumer = bufferSource.getBuffer(TridentPlusGlowRenderType.getRenderType());
        tridentModel.renderToBuffer(poseStack, glowConsumer, FULL_BRIGHT_LIGHT, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    // 判断当前物品渲染是否属于玩家正在蓄力的天雷战戟手持模型。
    public boolean shouldRenderChargeGlow(ItemStack stack, ItemDisplayContext displayContext) {
        if (displayContext == ItemDisplayContext.GUI) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isUsingItem() && player.getUseItem() == stack;
    }

    // 计算天雷战戟视觉蓄力进度，首版保持客户端视觉计算，不改服务端投掷逻辑。
    public float getChargeProgress(ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || player.getUseItem() != stack) return 0.0F;
        float usedTicks = stack.getUseDuration() - player.getUseItemRemainingTicks();
        return Mth.clamp(usedTicks / TRIDENT_GLOW_CHARGE_TICKS, 0.0F, 1.0F);
    }

    // 懒加载模型，避免客户端初始化早期模型层还没准备好时直接烘焙。
    public TridentModel getModel() {
        if (this.model == null) {
            this.model = new TridentModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.TRIDENT));
        }
        return this.model;
    }

    public static ItemDisplayContext getCurrentDisplayContext() {
        return currentDisplayContext;
    }

    // 第一人称复用原版拉弓布局，并用通用按键蓄力进度拉长手中战戟。
    public void applyBowChargeTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                        float partialTick, float equipProgress) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float progress = ClientKeyChargeRegistry.getProgress(player, partialTick);

        // 先应用原版手持基础偏移，再使用 BOW 分支的旋转和蓄力拉伸参数。
        poseStack.translate(direction * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
        poseStack.translate(direction * -0.2785682F, 0.18344387F, 0.15731531F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-13.935F));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * 35.3F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * -9.785F));
        poseStack.translate(0.0F, 0.0F, progress * 0.04F);
        poseStack.scale(1.0F, 1.0F, 1.0F + progress * 0.2F);
        poseStack.mulPose(Axis.YN.rotationDegrees(direction * 45.0F));
    }
}
