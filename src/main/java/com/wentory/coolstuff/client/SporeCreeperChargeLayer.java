package com.wentory.coolstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.LeapingCreeperEntity;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;

public final class SporeCreeperChargeLayer extends RenderLayer<Creeper, CreeperModel<Creeper>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Coolstuff.MODID, "textures/entity/spore_creeper_emessive_charge.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public SporeCreeperChargeLayer(RenderLayerParent<Creeper, CreeperModel<Creeper>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, Creeper entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!(entity instanceof LeapingCreeperEntity creeper)
                || creeper.getAttackState() != 1 || creeper.isInvisible()) return;

        float progress = creeper.getPreparationProgress(partialTick);
        float alpha = progress * progress * (3.0F - 2.0F * progress);
        int packedColor = (Math.round(alpha * 255.0F) << 24) | 0xFFFFFF;
        getParentModel().renderToBuffer(
                poseStack,
                buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                packedColor
        );
    }
}