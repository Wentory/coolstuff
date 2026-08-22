package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.registry.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class GhastCoreDropHandler {
    private static final float DROP_CHANCE = 0.25F;

    private GhastCoreDropHandler() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Ghast ghast)
                || !(event.getSource().getDirectEntity() instanceof LargeFireball fireball)
                || FireballCombo.getCombo(fireball) <= 0
                || ghast.getRandom().nextFloat() >= DROP_CHANCE) return;

        event.getDrops().add(new ItemEntity(ghast.level(), ghast.getX(), ghast.getY(), ghast.getZ(),
                new ItemStack(ModItems.GHAST_CORE.get())));
    }
}
