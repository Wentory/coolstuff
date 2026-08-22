package com.wentory.coolstuff.mixin;

import net.minecraft.world.entity.projectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LargeFireball.class)
public interface LargeFireballAccessor {
    @Accessor("explosionPower")
    void coolstuff$setExplosionPower(int explosionPower);
}
