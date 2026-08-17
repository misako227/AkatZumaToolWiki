package com.z227.akatzumatool.mixin;

import com.z227.akatzumatool.item.MagicBowItem;
import com.z227.akatzumatool.event.client.KeyChargeHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// MagicBowMovementMixin 管理魔法弓移动恢复和通用按键蓄力减速。
@Mixin(LocalPlayer.class)
public class MagicBowMovementMixin {
    private static final float USING_ITEM_SLOWDOWN_RECOVER_MULTIPLIER = 5.0F; // 原版使用物品把移动输入乘 0.2，这里乘回 5。

    // 在 Player.aiStep 读取移动输入前恢复魔法弓移动输入。
    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V", shift = At.Shift.BEFORE))
    public void akatzumatool$restoreMagicBowMovementInput(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (KeyChargeHandler.shouldBlockMovement() && !player.isPassenger()) {
            // 手动蓄力类技能需要完全禁止移动输入，但保留转头瞄准。
            player.input.leftImpulse = 0.0F;
            player.input.forwardImpulse = 0.0F;
            player.input.jumping = false;
            return;
        }
        if (KeyChargeHandler.shouldSlowMovement() && !player.isPassenger()) {
            // 通用按键蓄力不进入原版 use 链路，这里补上与原版一致的 0.2 移动倍率。
            player.input.leftImpulse *= 0.2F;
            player.input.forwardImpulse *= 0.2F;
            return;
        }
        if (!player.isUsingItem()) return;
        if (player.isPassenger()) return;
        if (!(player.getUseItem().getItem() instanceof MagicBowItem)) return;

        // 只恢复原版使用物品减速，不改变潜行、爬行等更早阶段已经应用的移动修正。
        player.input.leftImpulse *= USING_ITEM_SLOWDOWN_RECOVER_MULTIPLIER;
        player.input.forwardImpulse *= USING_ITEM_SLOWDOWN_RECOVER_MULTIPLIER;
    }
}
