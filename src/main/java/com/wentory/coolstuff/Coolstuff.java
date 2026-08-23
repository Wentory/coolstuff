package com.wentory.coolstuff;

import com.wentory.coolstuff.network.ParryEffectPayload;
import com.wentory.coolstuff.network.BlackHoleImpactPayload;
import com.wentory.coolstuff.network.SnowballHitPayload;
import com.wentory.coolstuff.registry.ModEntities;
import com.wentory.coolstuff.registry.ModItems;
import com.wentory.coolstuff.registry.ModSounds;
import com.wentory.coolstuff.registry.ModCreativeTabs;
import com.wentory.coolstuff.registry.ModConditions;
import com.wentory.coolstuff.server.CakeDispenserBehavior;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.CoolstuffClientConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import com.wentory.coolstuff.client.ClientConfigScreenRegistration;

@Mod(Coolstuff.MODID)
public final class Coolstuff {
    public static final String MODID = "coolstuff";

    public Coolstuff(IEventBus modEventBus, ModContainer modContainer) {
        ModConditions.CONDITIONS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, CoolstuffConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CoolstuffClientConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientConfigScreenRegistration.register(modContainer);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CakeDispenserBehavior::register);
    }
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                ParryEffectPayload.TYPE,
                ParryEffectPayload.STREAM_CODEC,
                ParryEffectPayload::handle
        ).playToClient(
                BlackHoleImpactPayload.TYPE,
                BlackHoleImpactPayload.STREAM_CODEC,
                BlackHoleImpactPayload::handle
        ).playToClient(
                SnowballHitPayload.TYPE,
                SnowballHitPayload.STREAM_CODEC,
                SnowballHitPayload::handle
        );
    }

}
