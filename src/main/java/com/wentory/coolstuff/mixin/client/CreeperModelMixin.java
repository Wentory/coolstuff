package com.wentory.coolstuff.mixin.client;

import com.wentory.coolstuff.entity.LeapingCreeperEntity;
import com.wentory.coolstuff.entity.LeapingCreeperProjectileEntity;
import com.wentory.coolstuff.model.animation.SporeCreeperAnimations;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperModel.class)
public abstract class CreeperModelMixin {
    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void coolstuff$resetSporeCreeperPose(Entity entity, float limbSwing, float limbSwingAmount,
                                                  float ageInTicks, float netHeadYaw, float headPitch,
                                                  CallbackInfo ci) {
        if (entity instanceof LeapingCreeperEntity || entity instanceof LeapingCreeperProjectileEntity) {
            ((CreeperModel<?>) (Object) this).root().getAllParts().forEach(ModelPart::resetPose);
        }
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void coolstuff$animateLeapPreparation(Entity entity, float limbSwing, float limbSwingAmount,
                                                   float ageInTicks, float netHeadYaw, float headPitch,
                                                   CallbackInfo ci) {
        CreeperModel<?> model = (CreeperModel<?>) (Object) this;
        HierarchicalModelAccessor animationAccess = (HierarchicalModelAccessor) (Object) model;
        if (entity instanceof LeapingCreeperEntity creeper && creeper.getAttackState() == 1) {
            animationAccess.coolstuff$animate(
                    creeper.chargeAnimationState, SporeCreeperAnimations.charge, ageInTicks);
        } else if (entity instanceof LeapingCreeperProjectileEntity) {
            animationAccess.coolstuff$applyStatic(SporeCreeperAnimations.flying);
        }
    }
}
