package com.wentory.coolstuff.mixin.client;

import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.item.EmissiveTrims;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Unique
    private boolean coolstuff$emissiveTrim;

    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("HEAD"))
    private void coolstuff$rememberEmissiveTrim(PoseStack poseStack, MultiBufferSource buffer,
                                                LivingEntity entity, EquipmentSlot slot, int light,
                                                HumanoidModel<?> model, float limbSwing, float limbSwingAmount,
                                                float partialTick, float ageInTicks, float netHeadYaw,
                                                float headPitch, CallbackInfo ci) {
        coolstuff$emissiveTrim = CoolstuffConfig.ENABLE_EMISSIVE_TRIMS.get()
                && EmissiveTrims.isEmissive(entity.getItemBySlot(slot));
    }

    @ModifyArg(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderTrim(Lnet/minecraft/core/Holder;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/Model;Z)V"),
            index = 3)
    private int coolstuff$makeTrimFullBright(int originalLight) {
        return coolstuff$emissiveTrim ? LightTexture.FULL_BRIGHT : originalLight;
    }
}
