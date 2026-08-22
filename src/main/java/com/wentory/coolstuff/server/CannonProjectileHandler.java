package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class CannonProjectileHandler {
    private static final String LOBBED_TAG = "coolstuff_cannon_lobbed";

    private CannonProjectileHandler() {
    }

    public static void markAsLobbed(LargeFireball fireball) {
        fireball.getPersistentData().putBoolean(LOBBED_TAG, true);
    }

    public static void releaseFromArc(LargeFireball fireball) {
        if (!fireball.getPersistentData().getBoolean(LOBBED_TAG)) return;
        fireball.getPersistentData().remove(LOBBED_TAG);
        fireball.accelerationPower = 0.1;
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LargeFireball fireball)
                || fireball.level().isClientSide()
                || !fireball.getPersistentData().getBoolean(LOBBED_TAG)) return;
        Vec3 movement = fireball.getDeltaMovement();
        fireball.setDeltaMovement(movement.x * 0.995, movement.y - 0.035, movement.z * 0.995);
        fireball.hasImpulse = true;
    }
}
