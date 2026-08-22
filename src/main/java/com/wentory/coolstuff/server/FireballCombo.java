package com.wentory.coolstuff.server;

import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.fireball.FireballPhaseAccess;
import com.wentory.coolstuff.mixin.LargeFireballAccessor;
import com.wentory.coolstuff.network.ParryEffectPayload;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FireballCombo {
    private static final String COMBO_TAG = "coolstuff_parry_combo";

    private FireballCombo() {
    }

    public static int parry(LargeFireball fireball) {
        return parry(fireball, 1);
    }

    public static int parry(LargeFireball fireball, int amount) {
        if (!CoolstuffConfig.ENABLE_PARRY.get()) return getCombo(fireball);
        if (BlackHole.isActive(fireball)) return 100;

        int combo = fireball.getPersistentData().getInt(COMBO_TAG) + Math.max(1, amount);
        combo = Math.min(combo, 100);
        fireball.getPersistentData().putInt(COMBO_TAG, combo);
        ((FireballPhaseAccess) fireball).coolstuff$setParryCombo(combo);

        Vec3 movement = fireball.getDeltaMovement();
        if (movement.lengthSqr() > 1.0E-6) {
            double speedMultiplier = 1.18 + Math.min(combo, 10) * 0.025;
            double boostedSpeed = movement.length() * speedMultiplier;
            double newSpeed = Math.min(3.5, boostedSpeed);
            fireball.setDeltaMovement(movement.normalize().scale(newSpeed));
        }
        double boostedAcceleration = 0.1 + combo * 0.015;
        fireball.accelerationPower = Math.min(0.28, boostedAcceleration);
        fireball.hasImpulse = true;

        int boostedExplosionPower = 1 + combo / 2;
        int explosionPower = Math.min(6, boostedExplosionPower);
        ((LargeFireballAccessor) fireball).coolstuff$setExplosionPower(explosionPower);

        PacketDistributor.sendToPlayersTrackingEntity(fireball,
                new ParryEffectPayload(fireball.getX(), fireball.getY(), fireball.getZ(), combo));
        if (combo >= 100 && CoolstuffConfig.ENABLE_BLACK_HOLES.get()) BlackHole.activate(fireball);
        return combo;
    }

    public static int getCombo(LargeFireball fireball) {
        return fireball.getPersistentData().getInt(COMBO_TAG);
    }
}
