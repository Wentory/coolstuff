package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.registry.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class GhastCoreDropHandler {
    private static final float DROP_CHANCE = 0.25F;
    private static final String PARRIED_FIREBALL_HIT_TIME = "coolstuff_parried_fireball_hit_time";
    private static final long HIT_GRACE_TICKS = 2L;

    private GhastCoreDropHandler() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof LargeFireball fireball
                && FireballCombo.getCombo(fireball) > 0
                && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof Ghast ghast
                && !ghast.level().isClientSide()) {
            ghast.getPersistentData().putLong(PARRIED_FIREBALL_HIT_TIME, ghast.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Ghast ghast)) return;

        boolean directParriedFireball = event.getSource().getDirectEntity() instanceof LargeFireball fireball
                && FireballCombo.getCombo(fireball) > 0;
        long markedAt = ghast.getPersistentData().getLong(PARRIED_FIREBALL_HIT_TIME);
        boolean justHitByParriedFireball = markedAt > 0L
                && ghast.level().getGameTime() - markedAt <= HIT_GRACE_TICKS;
        if ((!directParriedFireball && !justHitByParriedFireball)
                || ghast.getRandom().nextFloat() >= DROP_CHANCE) return;

        event.getDrops().add(new ItemEntity(ghast.level(), ghast.getX(), ghast.getY(), ghast.getZ(),
                new ItemStack(ModItems.GHAST_CORE.get())));
    }
}