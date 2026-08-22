package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.entity.CreeperSugarTransformationAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperSugarTransformationMixin implements CreeperSugarTransformationAccess {
    @Unique
    private static final EntityDataAccessor<Boolean> COOLSTUFF_SUGAR_TRANSFORMING =
            SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void coolstuff$defineSugarTransformation(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(COOLSTUFF_SUGAR_TRANSFORMING, false);
    }

    @Override
    public boolean coolstuff$isSugarTransforming() {
        return ((Creeper) (Object) this).getEntityData().get(COOLSTUFF_SUGAR_TRANSFORMING);
    }

    @Override
    public void coolstuff$setSugarTransforming(boolean transforming) {
        ((Creeper) (Object) this).getEntityData().set(COOLSTUFF_SUGAR_TRANSFORMING, transforming);
    }
}
