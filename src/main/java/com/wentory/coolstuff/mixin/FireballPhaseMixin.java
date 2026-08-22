package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.fireball.FireballPhaseAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.Fireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Fireball.class)
public abstract class FireballPhaseMixin implements FireballPhaseAccess {
    @Unique
    private static final EntityDataAccessor<Integer> COOLSTUFF_PARRY_COMBO =
            SynchedEntityData.defineId(Fireball.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void coolstuff$defineParryCombo(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(COOLSTUFF_PARRY_COMBO, 0);
    }

    @Override
    public int coolstuff$getParryCombo() {
        return ((Fireball) (Object) this).getEntityData().get(COOLSTUFF_PARRY_COMBO);
    }

    @Override
    public void coolstuff$setParryCombo(int combo) {
        ((Fireball) (Object) this).getEntityData().set(COOLSTUFF_PARRY_COMBO, combo);
    }
}
