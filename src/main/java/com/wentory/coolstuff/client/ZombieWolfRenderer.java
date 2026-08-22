package com.wentory.coolstuff.client;

import com.wentory.coolstuff.Coolstuff;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public final class ZombieWolfRenderer extends WolfRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Coolstuff.MODID, "textures/entity/zombie_wolf.png");

    public ZombieWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Wolf wolf) {
        return TEXTURE;
    }
}
