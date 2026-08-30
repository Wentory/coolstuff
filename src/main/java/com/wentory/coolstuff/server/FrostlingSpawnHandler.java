package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import com.wentory.coolstuff.entity.FrostlingEntity;
import com.wentory.coolstuff.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class FrostlingSpawnHandler {
    private FrostlingSpawnHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onNaturalZombieSpawn(FinalizeSpawnEvent event) {
        if (!RestartRequiredConfig.frostling()
                || event.getSpawnType() != MobSpawnType.NATURAL
                || !(event.getEntity() instanceof Zombie original)
                || original.getType() != EntityType.ZOMBIE
                || !event.getLevel().getBiome(original.blockPosition()).value().coldEnoughToSnow(original.blockPosition())
                || event.getLevel().getRandom().nextDouble() >= CoolstuffConfig.FROSTLING_SPAWN_CHANCE.get()) {
            return;
        }

        var type = ModEntities.FROSTLING.orElse(null);
        if (type == null) return;
        FrostlingEntity frostling = new FrostlingEntity(type.get(), event.getLevel().getLevel());
        if (original.getVehicle() != null) {
            original.getVehicle().discard();
            original.stopRiding();
        }
        frostling.moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        frostling.setBaby(original.isBaby());
        frostling.finalizeSpawn(event.getLevel(), event.getDifficulty(), event.getSpawnType(), null);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY}) {
            frostling.setItemSlot(slot, original.getItemBySlot(slot).copy());
        }
        event.setSpawnCancelled(true);
        event.getLevel().addFreshEntity(frostling);
        DebugMode.markAndAnnounce(frostling, "Frosted");
    }
}
