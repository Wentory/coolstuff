package com.wentory.coolstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.BlackHoleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "textures/entity/black_hole.png");
    private static final int LATITUDE_SEGMENTS = 20;
    private static final int LONGITUDE_SEGMENTS = 32;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(BlackHoleEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        float remaining = entity.getRemainingTicks() - partialTick;
        float age = entity.tickCount + partialTick;
        float birth = Math.max(0.0F, Math.min(1.0F, age / 18.0F));
        float birthScale = 1.0F - (float) Math.pow(1.0F - birth, 3.0F);
        float radius = remaining <= 30.0F ? 2.25F * Math.max(0.02F, remaining / 30.0F)
                : 2.25F * Math.max(0.01F, birthScale);
        poseStack.scale(radius, radius, radius);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        VertexConsumer vertices = buffers.getBuffer(RenderType.debugQuads());
        for (int lat = 0; lat < LATITUDE_SEGMENTS; lat++) {
            float phi0 = (float) (-Math.PI / 2.0 + Math.PI * lat / LATITUDE_SEGMENTS);
            float phi1 = (float) (-Math.PI / 2.0 + Math.PI * (lat + 1) / LATITUDE_SEGMENTS);
            for (int lon = 0; lon < LONGITUDE_SEGMENTS; lon++) {
                float theta0 = (float) (Math.PI * 2.0 * lon / LONGITUDE_SEGMENTS);
                float theta1 = (float) (Math.PI * 2.0 * (lon + 1) / LONGITUDE_SEGMENTS);
                vertex(vertices, matrix, phi0, theta0);
                vertex(vertices, matrix, phi1, theta0);
                vertex(vertices, matrix, phi1, theta1);
                vertex(vertices, matrix, phi0, theta1);
            }
        }
        poseStack.popPose();

        renderAccretionDisk(entity, partialTick, poseStack, buffers);
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void renderAccretionDisk(BlackHoleEntity entity, float partialTick,
                                             PoseStack poseStack, MultiBufferSource buffers) {
        poseStack.pushPose();
        float age = entity.tickCount + partialTick;
        float birth = Math.max(0.0F, Math.min(1.0F, age / 24.0F));
        float growth = 1.0F - (float) Math.pow(1.0F - birth, 3.0F);
        if (entity.isPacified()) {
            growth *= Math.max(0.0F, (entity.getRemainingTicks() - partialTick) / 30.0F);
        }
        poseStack.scale(growth, growth, growth);
        poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));

        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        for (int layer = 0; layer < 7; layer++) {
            poseStack.pushPose();
            float layerOffset = layer - 3.0F;
            poseStack.translate(0.0F, layerOffset * 0.34F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(layerOffset * 1.7F));
            renderSpiralLayer(vertices, poseStack.last().pose(), age, layer);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderSpiralLayer(VertexConsumer vertices, Matrix4f matrix, float time, int layer) {
        final int arms = 5;
        final int segments = 72;
        final float outerRadius = 30.5F;
        final float innerRadius = 3.0F;
        for (int arm = 0; arm < arms; arm++) {
            float armOffset = (float) (Math.PI * 2.0 * arm / arms);
            for (int segment = 0; segment < segments; segment++) {
                float p0 = segment / (float) segments;
                float p1 = (segment + 1) / (float) segments;
                float r0 = outerRadius + (innerRadius - outerRadius) * p0;
                float r1 = outerRadius + (innerRadius - outerRadius) * p1;
                float flow = time * (0.075F + layer * 0.0025F);
                float a0 = armOffset + p0 * 10.5F + flow;
                float a1 = armOffset + p1 * 10.5F + flow;
                float width0 = 0.22F + (1.0F - p0) * 0.50F;
                float width1 = 0.22F + (1.0F - p1) * 0.50F;
                float wave = 0.55F + 0.45F * (float) Math.sin(p0 * 34.0F - time * 0.34F + arm);
                int alpha = (int) ((24.0F + 34.0F * wave) * (1.0F - Math.abs(layer - 3.0F) * 0.10F));
                int red = layer % 2 == 0 ? 150 : 92;
                int green = layer % 2 == 0 ? 225 : 170;
                int blue = 255;

                spiralVertex(vertices, matrix, r0, a0, -width0, red, green, blue, alpha);
                spiralVertex(vertices, matrix, r0, a0, width0, red, green, blue, alpha);
                spiralVertex(vertices, matrix, r1, a1, width1, red, green, blue, alpha);
                spiralVertex(vertices, matrix, r1, a1, -width1, red, green, blue, alpha);

                // Render the reverse winding as well. RenderType.lightning culls back faces,
                // otherwise the accretion disk disappears when viewed from above.
                spiralVertex(vertices, matrix, r1, a1, -width1, red, green, blue, alpha);
                spiralVertex(vertices, matrix, r1, a1, width1, red, green, blue, alpha);
                spiralVertex(vertices, matrix, r0, a0, width0, red, green, blue, alpha);
                spiralVertex(vertices, matrix, r0, a0, -width0, red, green, blue, alpha);
            }
        }
    }

    private static void spiralVertex(VertexConsumer vertices, Matrix4f matrix, float radius, float angle,
                                     float width, int red, int green, int blue, int alpha) {
        float adjustedRadius = radius + width;
        float x = (float) Math.cos(angle) * adjustedRadius;
        float z = (float) Math.sin(angle) * adjustedRadius;
        float y = (float) Math.sin(angle * 2.0F) * 0.18F;
        vertices.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float phi, float theta) {
        float cosPhi = (float) Math.cos(phi);
        float x = cosPhi * (float) Math.cos(theta);
        float y = (float) Math.sin(phi);
        float z = cosPhi * (float) Math.sin(theta);
        vertices.addVertex(matrix, x, y, z).setColor(0, 0, 0, 255);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return TEXTURE;
    }
}
