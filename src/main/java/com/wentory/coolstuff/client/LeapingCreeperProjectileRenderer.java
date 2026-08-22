package com.wentory.coolstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wentory.coolstuff.entity.LeapingCreeperProjectileEntity;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LeapingCreeperProjectileRenderer extends EntityRenderer<LeapingCreeperProjectileEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "coolstuff", "textures/entity/spore_creeper.png");
    private final CreeperModel<LeapingCreeperProjectileEntity> model;

    public LeapingCreeperProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LeapingCreeperProjectileEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() < 0.0001) velocity = new Vec3(0.0, 1.0, 0.0);
        Vector3f direction = new Vector3f((float) velocity.x, (float) velocity.y, (float) velocity.z).normalize();
        Quaternionf orientation = new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), direction);

        poseStack.pushPose();
        poseStack.mulPose(orientation);
        // The entity origin and its 0.72 cube hitbox are the head. The rest of
        // the creeper model extends backwards along the flight direction.
        poseStack.translate(0.0F, -1.45F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        float swelling = Mth.clamp((entity.tickCount + partialTick) / 30.0F, 0.0F, 1.0F);
        float pulse = 1.0F + Mth.sin(swelling * 100.0F) * swelling * 0.025F;
        float cubic = swelling * swelling * swelling;
        poseStack.scale((1.0F + cubic * 0.18F) * pulse,
                (1.0F + cubic * 0.06F) / pulse,
                (1.0F + cubic * 0.18F) * pulse);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        float white = (int) (swelling * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(swelling, 0.35F, 1.0F);
        int overlay = OverlayTexture.pack(OverlayTexture.u(white), OverlayTexture.v(false));
        model.renderToBuffer(poseStack, vertices, packedLight, overlay,
                entity.isReflected() ? 0xFFFF9090 : 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LeapingCreeperProjectileEntity entity) {
        return TEXTURE;
    }
}
