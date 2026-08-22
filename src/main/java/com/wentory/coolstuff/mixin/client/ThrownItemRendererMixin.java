package com.wentory.coolstuff.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wentory.coolstuff.fireball.FireballPhase;
import com.wentory.coolstuff.fireball.FireballPhaseAccess;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownItemRenderer.class)
public abstract class ThrownItemRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void coolstuff$beginPhaseTransform(Entity entity, float entityYaw, float partialTick,
                                                PoseStack poseStack, MultiBufferSource buffers,
                                                int packedLight, CallbackInfo ci) {
        poseStack.pushPose();
        if (!(entity instanceof LargeFireball fireball)) return;

        int combo = ((FireballPhaseAccess) fireball).coolstuff$getParryCombo();
        FireballPhase phase = FireballPhase.fromCombo(combo);
        if (phase == FireballPhase.DIVINE || phase == FireballPhase.BLACK_HOLE) {
            RenderSystem.setShaderColor(0.62F, 0.88F, 1.0F, 1.0F);
        }
        float time = fireball.tickCount + partialTick;
        float scale = switch (phase) {
            case IGNITED -> 1.35F + (float) Math.sin(time * 0.35F) * 0.04F;
            case OVERCHARGED -> 1.75F + (float) Math.sin(time * 0.55F) * 0.10F;
            case DIVINE -> 2.50F + (float) Math.sin(time * 0.85F) * 0.25F;
            case BLACK_HOLE -> 2.65F + (float) Math.sin(time * 1.35F) * 0.28F;
            default -> 1.0F;
        };
        if (phase == FireballPhase.BLACK_HOLE) {
            float shake = 0.055F + Math.min(0.16F, fireball.tickCount * 0.0008F);
            poseStack.translate(Math.sin(time * 3.7F) * shake,
                    Math.cos(time * 4.9F) * shake,
                    Math.sin(time * 5.8F) * shake);
        }
        poseStack.scale(scale, scale, scale);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void coolstuff$endPhaseTransform(Entity entity, float entityYaw, float partialTick,
                                              PoseStack poseStack, MultiBufferSource buffers,
                                              int packedLight, CallbackInfo ci) {
        poseStack.popPose();
        if (entity instanceof LargeFireball fireball
                && FireballPhase.fromCombo(((FireballPhaseAccess) fireball).coolstuff$getParryCombo())
                .ordinal() >= FireballPhase.DIVINE.ordinal()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
