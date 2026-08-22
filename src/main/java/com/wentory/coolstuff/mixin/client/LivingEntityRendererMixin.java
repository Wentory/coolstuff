package com.wentory.coolstuff.mixin.client;

import com.wentory.coolstuff.entity.CreeperSugarTransformationAccess;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "isShaking", at = @At("RETURN"), cancellable = true)
    private void coolstuff$shakeSugarTransformingCreeper(LivingEntity entity,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof CreeperSugarTransformationAccess transforming
                && transforming.coolstuff$isSugarTransforming()) {
            cir.setReturnValue(true);
        }
    }
}
