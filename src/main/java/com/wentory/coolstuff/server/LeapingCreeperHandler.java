package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.LeapingCreeperEntity;
import com.wentory.coolstuff.registry.ModEntities;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class LeapingCreeperHandler {
    private LeapingCreeperHandler() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!RestartRequiredConfig.sporeCreeper()
                || event.getSpawnType() != MobSpawnType.NATURAL
                || event.getEntity().getClass() != Creeper.class
                || event.getLevel().getRandom().nextDouble()
                >= CoolstuffConfig.SPORE_CREEPER_SPAWN_CHANCE.get()) return;

        var type = ModEntities.LEAPING_CREEPER.orElse(null);
        if (type == null) return;
        Creeper original = (Creeper) event.getEntity();
        LeapingCreeperEntity leaper = new LeapingCreeperEntity(type.get(),
                event.getLevel().getLevel());
        leaper.moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        leaper.finalizeSpawn(event.getLevel(), event.getDifficulty(), event.getSpawnType(), null);
        event.setSpawnCancelled(true);
        event.getLevel().addFreshEntity(leaper);
        DebugMode.markAndAnnounce(leaper, "Spore Creeper");
    }
}
