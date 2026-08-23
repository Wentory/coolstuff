package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.fireball.FireballPhase;
import com.wentory.coolstuff.fireball.FireballPhaseAccess;
import com.wentory.coolstuff.server.FireballCombo;
import com.wentory.coolstuff.server.ProjectileDeflectionContext;
import com.wentory.coolstuff.registry.ModEnchantments;
import com.wentory.coolstuff.registry.ModItems;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.Registries;
import com.wentory.coolstuff.server.CannonProjectileHandler;
import com.wentory.coolstuff.config.CoolstuffConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void coolstuff$freezeBlackHoleFormation(CallbackInfo ci) {
        if ((Object) this instanceof LargeFireball fireball
                && FireballPhase.fromCombo(((FireballPhaseAccess) fireball).coolstuff$getParryCombo())
                == FireballPhase.BLACK_HOLE) {
            fireball.setDeltaMovement(0.0, 0.0, 0.0);
            fireball.accelerationPower = 0.0;
        }
    }

    @Inject(method = "onDeflection", at = @At("TAIL"))
    private void coolstuff$powerUpAfterPlayerParry(Entity deflector, boolean deflectedByAttack, CallbackInfo ci) {
        if (!CoolstuffConfig.ENABLE_PARRY.get() || ProjectileDeflectionContext.isProjectileCollision()) return;
        if ((Object) this instanceof LargeFireball fireball
                && deflector instanceof Player
                && deflectedByAttack
                && !fireball.level().isClientSide()) {
            CannonProjectileHandler.releaseFromArc(fireball);
            int amount = 1;
            Player badmintonPlayer = null;
            net.minecraft.world.item.ItemStack badminton = net.minecraft.world.item.ItemStack.EMPTY;
            if (deflector instanceof Player player && player.getMainHandItem().is(ModItems.BADMINTON.get())) {
                badmintonPlayer = player;
                badminton = player.getMainHandItem();
                int level = EnchantmentHelper.getItemEnchantmentLevel(
                        player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.SERVE),
                        badminton);
                amount = level >= 2 ? 20 : level == 1 ? 10 : 5;
                badminton.hurtAndBreak(5, player, EquipmentSlot.MAINHAND);
            }
            int combo = FireballCombo.parry(fireball, amount);
            if (badmintonPlayer != null && !badminton.isEmpty()
                    && FireballPhase.fromCombo(combo) == FireballPhase.BLACK_HOLE) {
                badminton.hurtAndBreak(badminton.getMaxDamage(), badmintonPlayer, EquipmentSlot.MAINHAND);
            }
        }
    }

    @Inject(method = "getTrailParticle", at = @At("HEAD"), cancellable = true)
    private void coolstuff$changeTrailWithPhase(CallbackInfoReturnable<ParticleOptions> cir) {
        if (!((Object) this instanceof LargeFireball fireball)) return;

        FireballPhase phase = FireballPhase.fromCombo(((FireballPhaseAccess) fireball).coolstuff$getParryCombo());
        switch (phase) {
            case IGNITED -> cir.setReturnValue(ParticleTypes.FLAME);
            case OVERCHARGED -> cir.setReturnValue(ParticleTypes.SOUL_FIRE_FLAME);
            case DIVINE -> cir.setReturnValue(ParticleTypes.END_ROD);
            case BLACK_HOLE -> cir.setReturnValue(ParticleTypes.END_ROD);
            default -> {
            }
        }
    }
}
