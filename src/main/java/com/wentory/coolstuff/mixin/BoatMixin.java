package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.entity.EnchantedBoatAccess;
import com.wentory.coolstuff.server.BoatEnchantments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Boat.class)
public abstract class BoatMixin implements EnchantedBoatAccess {
    @Shadow private float invFriction;
    @Shadow private float deltaRotation;

    @Inject(method = "floatBoat", at = @At("HEAD"), cancellable = true)
    private void coolstuff$glideOnWater(CallbackInfo ci) {
        Boat boat = (Boat) (Object) this;
        double surface = BoatEnchantments.supportHeight(boat);
        if (Double.isNaN(surface)) return;
        invFriction = 0.98F;
        deltaRotation *= invFriction;
        var velocity = boat.getDeltaMovement();
        boat.setDeltaMovement(velocity.x * invFriction, surface - boat.getY(), velocity.z * invFriction);
        boat.setOnGround(true);
        boat.fallDistance = 0;
        ci.cancel();
    }
    @Unique
    private static final EntityDataAccessor<ItemStack> COOLSTUFF_STACK =
            SynchedEntityData.defineId(Boat.class, EntityDataSerializers.ITEM_STACK);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void coolstuff$defineStack(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(COOLSTUFF_STACK, ItemStack.EMPTY);
    }

    @Override
    public ItemStack coolstuff$getBoatStack() {
        return ((Boat) (Object) this).getEntityData().get(COOLSTUFF_STACK);
    }

    @Override
    public void coolstuff$setBoatStack(ItemStack stack) {
        ((Boat) (Object) this).getEntityData().set(COOLSTUFF_STACK, stack.copyWithCount(1));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void coolstuff$saveStack(CompoundTag tag, CallbackInfo ci) {
        if (!coolstuff$getBoatStack().isEmpty()) {
            tag.put("CoolstuffBoatItem", coolstuff$getBoatStack().save(((Boat) (Object) this).registryAccess()));
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void coolstuff$loadStack(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("CoolstuffBoatItem", 10)) {
            coolstuff$setBoatStack(ItemStack.parseOptional(((Boat) (Object) this).registryAccess(), tag.getCompound("CoolstuffBoatItem")));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void coolstuff$icewake(CallbackInfo ci) {
        BoatEnchantments.tick((Boat) (Object) this);
    }

    @Inject(method = "getPickResult", at = @At("RETURN"), cancellable = true)
    private void coolstuff$pickStack(CallbackInfoReturnable<ItemStack> cir) {
        BoatEnchantments.copyEnchantments((Boat) (Object) this, cir.getReturnValue());
    }
}
