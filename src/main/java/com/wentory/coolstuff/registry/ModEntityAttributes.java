package com.wentory.coolstuff.registry;

import com.wentory.coolstuff.Coolstuff;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = Coolstuff.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {
    private ModEntityAttributes() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        ModEntities.LEAPING_CREEPER.ifPresent(type -> event.put(type.get(), Creeper.createAttributes().build()));
        ModEntities.FROSTLING.ifPresent(type -> event.put(type.get(), Zombie.createAttributes().build()));
        ModEntities.ZOMBIE_WOLF.ifPresent(type -> event.put(type.get(), Wolf.createAttributes()
                .add(Attributes.ARMOR, 2.0).build()));
    }
}
