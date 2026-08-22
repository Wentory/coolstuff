package com.wentory.coolstuff.server;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Creeper;

public final class CreeperTransformation {
    private CreeperTransformation() {
    }

    public static void copyState(Creeper source, Creeper target) {
        target.getAttributes().assignAllValues(source.getAttributes());
        target.absMoveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        target.setOldPosAndRot();
        target.setDeltaMovement(source.getDeltaMovement());
        target.hasImpulse = true;
        target.fallDistance = source.fallDistance;
        target.setHealth(Math.min(target.getMaxHealth(), source.getHealth()));
        target.setAbsorptionAmount(source.getAbsorptionAmount());
        target.setNoAi(source.isNoAi());
        target.setInvulnerable(source.isInvulnerable());
        target.setSilent(source.isSilent());
        target.setGlowingTag(source.isCurrentlyGlowing());
        target.setRemainingFireTicks(source.getRemainingFireTicks());
        target.setAirSupply(source.getAirSupply());
        for (MobEffectInstance effect : source.getActiveEffects()) {
            target.addEffect(new MobEffectInstance(effect));
        }
        if (source.getTarget() != null && source.getTarget().isAlive()) target.setTarget(source.getTarget());
        if (source.hasCustomName()) {
            target.setCustomName(source.getCustomName());
            target.setCustomNameVisible(source.isCustomNameVisible());
        }
    }
}
