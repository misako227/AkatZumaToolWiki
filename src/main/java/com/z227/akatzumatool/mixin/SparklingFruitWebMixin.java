package com.z227.akatzumatool.mixin;

import com.z227.akatzumatool.event.EffectRegister;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// SparklingFruitWebMixin 让拥有闪闪果实 Buff 的生物免疫蜘蛛网减速。
@Mixin(Entity.class)
public class SparklingFruitWebMixin {
    // 拦截蜘蛛网对实体速度的 makeStuckInBlock 乘法。
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    public void akatzumatool$skipSparklingFruitCobweb(BlockState state, Vec3 motionMultiplier, CallbackInfo callbackInfo) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof LivingEntity livingEntity)) return;
        if (!state.is(Blocks.COBWEB)) return;
        if (!livingEntity.hasEffect(EffectRegister.SPARKLING_FRUIT_EFFECT.get())) return;

        callbackInfo.cancel();
    }
}
