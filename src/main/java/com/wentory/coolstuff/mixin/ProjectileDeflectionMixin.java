package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.server.ProjectileDeflectionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Projectile.class)
public abstract class ProjectileDeflectionMixin {
    @Redirect(
            method = "onHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;deflect(Lnet/minecraft/world/entity/projectile/ProjectileDeflection;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Z)Z"
            )
    )
    private boolean coolstuff$markProjectileCollisionDeflection(Projectile target,
                                                                 ProjectileDeflection deflection,
                                                                 Entity deflector,
                                                                 Entity newOwner,
                                                                 boolean deflectedByAttack) {
        ProjectileDeflectionContext.enterProjectileCollision();
        try {
            return target.deflect(deflection, deflector, newOwner, deflectedByAttack);
        } finally {
            ProjectileDeflectionContext.leaveProjectileCollision();
        }
    }
}