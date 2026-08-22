package com.wentory.coolstuff.client;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = Coolstuff.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE_EXPLOSION.get(), BlackHoleExplosionRenderer::new);
        ModEntities.LEAPING_CREEPER.ifPresent(type ->
                event.registerEntityRenderer(type.get(), LeapingCreeperRenderer::new));
        ModEntities.LEAPING_CREEPER_PROJECTILE.ifPresent(type ->
                event.registerEntityRenderer(type.get(), LeapingCreeperProjectileRenderer::new));
        event.registerEntityRenderer(ModEntities.THROWN_CAKE.get(), ThrownCakeRenderer::new);
        ModEntities.FROSTLING.ifPresent(type ->
                event.registerEntityRenderer(type.get(), FrostlingRenderer::new));
        ModEntities.ZOMBIE_WOLF.ifPresent(type ->
                event.registerEntityRenderer(type.get(), ZombieWolfRenderer::new));
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "entity/splat_cake")));
    }
}
