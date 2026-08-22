package com.wentory.coolstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.ThrownCakeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class ThrownCakeRenderer extends EntityRenderer<ThrownCakeEntity> {
    private static final ModelResourceLocation SPLAT_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "entity/splat_cake"));
    private static final ResourceLocation SMEAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Coolstuff.MODID, "textures/entity/cake_smear.png");

    public ThrownCakeRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.25F;
    }

    @Override
    public void render(ThrownCakeEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        if (entity.getSmearProgress(partialTick) > 0.0F) {
            renderSmear(entity, partialTick, poseStack, buffers, packedLight);
        }
        if (entity.getCakeState() == ThrownCakeEntity.FLYING) {
            renderFlyingCake(entity, poseStack, buffers, packedLight);
        } else {
            renderSplatCake(entity, partialTick, poseStack, buffers, packedLight);
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void renderFlyingCake(ThrownCakeEntity entity, PoseStack poseStack,
                                         MultiBufferSource buffers, int packedLight) {
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() < 0.0001) velocity = new Vec3(0.0, -1.0, 0.0);
        Vector3f flightDirection = new Vector3f(
                (float) velocity.x, (float) velocity.y, (float) velocity.z).normalize();
        Quaternionf faceFlightDirection = new Quaternionf().rotationTo(
                new Vector3f(0.0F, 1.0F, 0.0F), flightDirection);

        poseStack.pushPose();
        poseStack.mulPose(faceFlightDirection);
        // The cake's frosted top (+Y) is its leading face. Rotate around the
        // cake's actual centre instead of around a full block cube.
        poseStack.translate(-0.5F, -0.25F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.CAKE.defaultBlockState(), poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderSplatCake(ThrownCakeEntity entity, float partialTick, PoseStack poseStack,
                                        MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        if (entity.getCakeState() == ThrownCakeEntity.ATTACHED) {
            Vec3 outward = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
            Vector3f intoFace = new Vector3f(
                    (float) -outward.x, (float) -outward.y, (float) -outward.z).normalize();
            poseStack.mulPose(faceDirectionWithoutRoll(intoFace));
        } else {
            orientToSurface(poseStack, entity.getSurfaceFace());
        }
        if (entity.getCakeState() == ThrownCakeEntity.SPLATTED && entity.getStateAge(partialTick) > 150.0F) {
            float animationTime = entity.getStateAge(partialTick) - 150.0F;
            float shrink;
            if (animationTime < 10.0F) {
                // 0.5 s fade-out: a small anticipatory scale-up from 1.0 to 1.15.
                float t = animationTime / 10.0F;
                float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
                shrink = 1.0F + 0.15F * eased;
            } else {
                // 1 s fade-in: shrink the enlarged cake all the way to zero.
                float t = Math.min(1.0F, (animationTime - 10.0F) / 20.0F);
                shrink = 1.15F * (1.0F - t * t * t);
            }
            poseStack.scale(shrink, shrink, shrink);
        }
        // +Y points into the struck surface. Move the full splat back by its
        // maximum height so none of it is buried in the block.
        poseStack.translate(-0.5F, -0.375F, -0.5F);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(SPLAT_MODEL);
        VertexConsumer modelVertices = buffers.getBuffer(RenderType.cutout());
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(), modelVertices, Blocks.CAKE.defaultBlockState(), model,
                1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void orientToSurface(PoseStack poseStack, Direction face) {
        // The frosted top hit the surface first, therefore it points INTO the
        // block, opposite to the outward face normal.
        Vector3f intoSurface = new Vector3f(-face.getStepX(), -face.getStepY(), -face.getStepZ());
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), intoSurface));
    }

    private static Quaternionf faceDirectionWithoutRoll(Vector3f normal) {
        Vector3f localY = new Vector3f(normal).normalize();
        Vector3f localZ = new Vector3f(0.0F, 1.0F, 0.0F);
        localZ.sub(new Vector3f(localY).mul(localZ.dot(localY)));
        if (localZ.lengthSquared() < 0.0001F) {
            localZ.set(0.0F, 0.0F, 1.0F);
            localZ.sub(new Vector3f(localY).mul(localZ.dot(localY)));
        }
        localZ.normalize();
        Vector3f localX = new Vector3f(localY).cross(localZ).normalize();

        Matrix3f orientation = new Matrix3f();
        orientation.setColumn(0, localX);
        orientation.setColumn(1, localY);
        orientation.setColumn(2, localZ);
        return orientation.getUnnormalizedRotation(new Quaternionf()).normalize();
    }

    private static void renderSmear(ThrownCakeEntity entity, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffers, int packedLight) {
        float progress = entity.getSmearProgress(partialTick);
        float wallAge = entity.getWallAge(partialTick);
        float alphaFade = wallAge <= 150.0F ? 1.0F : Math.max(0.0F, 1.0F - (wallAge - 150.0F) / 70.0F);
        int alpha = Math.max(0, Math.min(255, Math.round(alphaFade * 255.0F)));
        if (alpha == 0) return;

        Vec3 relative = entity.getWallPosition().subtract(entity.getPosition(partialTick));
        poseStack.pushPose();
        poseStack.translate(relative.x, relative.y, relative.z);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(SMEAR_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        float halfWidth = 0.48F;
        float height = 3.0F * progress;
        Direction face = entity.getWallFace();
        float offset = 0.012F;
        if (face == Direction.NORTH || face == Direction.SOUTH) {
            float z = face == Direction.NORTH ? -offset : offset;
            smearVertex(vertices, pose, -halfWidth, 0.0F, z, 0.0F, 0.0F, alpha, packedLight, face);
            smearVertex(vertices, pose, -halfWidth, -height, z, 0.0F, progress, alpha, packedLight, face);
            smearVertex(vertices, pose, halfWidth, -height, z, 1.0F, progress, alpha, packedLight, face);
            smearVertex(vertices, pose, halfWidth, 0.0F, z, 1.0F, 0.0F, alpha, packedLight, face);
        } else {
            float x = face == Direction.WEST ? -offset : offset;
            smearVertex(vertices, pose, x, 0.0F, -halfWidth, 0.0F, 0.0F, alpha, packedLight, face);
            smearVertex(vertices, pose, x, -height, -halfWidth, 0.0F, progress, alpha, packedLight, face);
            smearVertex(vertices, pose, x, -height, halfWidth, 1.0F, progress, alpha, packedLight, face);
            smearVertex(vertices, pose, x, 0.0F, halfWidth, 1.0F, 0.0F, alpha, packedLight, face);
        }
        poseStack.popPose();
    }

    private static void smearVertex(VertexConsumer vertices, PoseStack.Pose pose,
                                    float x, float y, float z, float u, float v,
                                    int alpha, int packedLight, Direction normal) {
        vertices.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownCakeEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
