package com.wentory.coolstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.BlackHoleExplosionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class BlackHoleExplosionRenderer extends EntityRenderer<BlackHoleExplosionEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Coolstuff.MODID, "textures/entity/black_hole.png");
    private static final int LATITUDES = 16;
    private static final int LONGITUDES = 24;

    public BlackHoleExplosionRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(BlackHoleExplosionEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        float age = entity.tickCount + partialTick;
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        renderFlashSphere(age, poseStack, vertices);
        renderImpactRings(age, poseStack, vertices);
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void renderFlashSphere(float age, PoseStack poseStack, VertexConsumer vertices) {
        float progress = clamp(age / 18.0F);
        if (progress >= 1.0F) return;
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0F);
        float radius = 0.25F + eased * 20.0F;
        int alpha = (int) (245.0F * (1.0F - progress) * (1.0F - progress));
        poseStack.pushPose();
        poseStack.scale(radius, radius, radius);
        Matrix4f matrix = poseStack.last().pose();
        for (int lat = 0; lat < LATITUDES; lat++) {
            float phi0 = (float) (-Math.PI / 2.0 + Math.PI * lat / LATITUDES);
            float phi1 = (float) (-Math.PI / 2.0 + Math.PI * (lat + 1) / LATITUDES);
            for (int lon = 0; lon < LONGITUDES; lon++) {
                float theta0 = (float) (Math.PI * 2.0 * lon / LONGITUDES);
                float theta1 = (float) (Math.PI * 2.0 * (lon + 1) / LONGITUDES);
                sphereVertex(vertices, matrix, phi0, theta0, alpha);
                sphereVertex(vertices, matrix, phi1, theta0, alpha);
                sphereVertex(vertices, matrix, phi1, theta1, alpha);
                sphereVertex(vertices, matrix, phi0, theta1, alpha);
                sphereVertex(vertices, matrix, phi0, theta1, alpha);
                sphereVertex(vertices, matrix, phi1, theta1, alpha);
                sphereVertex(vertices, matrix, phi1, theta0, alpha);
                sphereVertex(vertices, matrix, phi0, theta0, alpha);
            }
        }
        poseStack.popPose();
    }

    private static void renderImpactRings(float age, PoseStack poseStack, VertexConsumer vertices) {
        for (int ring = 0; ring < 3; ring++) {
            float progress = clamp((age - 2.0F - ring * 4.0F) / (28.0F + ring * 3.0F));
            if (progress <= 0.0F || progress >= 1.0F) continue;
            float radius = 2.0F + (1.0F - (float) Math.pow(1.0F - progress, 2.0F)) * (43.0F + ring * 6.0F);
            float width = 2.4F * (1.0F - progress) + 0.35F;
            int alpha = (int) (210.0F * (1.0F - progress) * (1.0F - progress));
            int red = ring == 1 ? 255 : 180;
            int green = ring == 1 ? 190 : 225;
            int blue = ring == 1 ? 105 : 255;

            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees((ring - 1) * 7.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees((ring - 1) * 3.5F));
            Matrix4f matrix = poseStack.last().pose();
            final int segments = 96;
            for (int i = 0; i < segments; i++) {
                float a0 = (float) (Math.PI * 2.0 * i / segments);
                float a1 = (float) (Math.PI * 2.0 * (i + 1) / segments);
                ringVertex(vertices, matrix, radius - width, a0, red, green, blue, alpha);
                ringVertex(vertices, matrix, radius + width, a0, red, green, blue, alpha);
                ringVertex(vertices, matrix, radius + width, a1, red, green, blue, alpha);
                ringVertex(vertices, matrix, radius - width, a1, red, green, blue, alpha);
                ringVertex(vertices, matrix, radius - width, a1, red, green, blue, alpha);
                ringVertex(vertices, matrix, radius + width, a1, red, green, blue, alpha);
                ringVertex(vertices, matrix, radius + width, a0, red, green, blue, alpha);
                ringVertex(vertices, matrix, radius - width, a0, red, green, blue, alpha);
            }
            poseStack.popPose();
        }
    }

    private static void sphereVertex(VertexConsumer vertices, Matrix4f matrix, float phi, float theta, int alpha) {
        float cosPhi = (float) Math.cos(phi);
        vertices.addVertex(matrix, cosPhi * (float) Math.cos(theta), (float) Math.sin(phi),
                cosPhi * (float) Math.sin(theta)).setColor(245, 252, 255, alpha);
    }

    private static void ringVertex(VertexConsumer vertices, Matrix4f matrix, float radius, float angle,
                                   int red, int green, int blue, int alpha) {
        vertices.addVertex(matrix, (float) Math.cos(angle) * radius, 0.0F,
                (float) Math.sin(angle) * radius).setColor(red, green, blue, alpha);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Override
    public boolean shouldRender(BlackHoleExplosionEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return frustum.isVisible(new AABB(entity.position(), entity.position()).inflate(64.0));
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleExplosionEntity entity) {
        return TEXTURE;
    }
}
