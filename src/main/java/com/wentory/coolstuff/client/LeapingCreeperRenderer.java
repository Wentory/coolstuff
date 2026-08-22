package com.wentory.coolstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wentory.coolstuff.entity.LeapingCreeperEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class LeapingCreeperRenderer extends CreeperRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "coolstuff", "textures/entity/spore_creeper.png");

    public LeapingCreeperRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(net.minecraft.world.entity.monster.Creeper entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        if (entity instanceof LeapingCreeperEntity leaper) {
            if (leaper.getAttackState() >= 2) {
                Vec3 velocity = leaper.getDeltaMovement();
                double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                float pitch = (float) Math.toDegrees(Math.atan2(velocity.y, Math.max(0.001, horizontal)));
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - pitch));
                poseStack.scale(0.88F, 1.28F, 0.88F);
            }
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Creeper entity) {
        return TEXTURE;
    }
}
