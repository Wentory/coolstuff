package com.wentory.coolstuff.mixin.client;

import net.minecraft.client.model.SkeletonModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonModel.class)
public abstract class SkeletonModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void coolstuff$poseShieldArm(Mob entity, float limbSwing, float limbSwingAmount,
                                         float ageInTicks, float netHeadYaw, float headPitch,
                                         CallbackInfo ci) {
        if (!(entity instanceof AbstractSkeleton skeleton)
                || !skeleton.isUsingItem()
                || skeleton.getUsedItemHand() != InteractionHand.OFF_HAND
                || !skeleton.getUseItem().is(Items.SHIELD)) return;

        SkeletonModel<?> model = (SkeletonModel<?>) (Object) this;
        // Same BLOCK pose used by HumanoidModel for a left/off-hand shield.
        model.leftArm.xRot = model.leftArm.xRot * 0.5F - 0.9424779F;
        model.leftArm.yRot = 0.5235988F;
        model.leftArm.zRot = 0.0F;
    }
}
