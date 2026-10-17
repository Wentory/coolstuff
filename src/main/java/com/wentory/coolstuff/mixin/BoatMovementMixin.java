package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.server.BoatEnchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class BoatMovementMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 coolstuff$step(Vec3 motion, MoverType type, Vec3 originalMotion) {
        if ((Object) this instanceof Boat boat && (type == MoverType.SELF || type == MoverType.PLAYER)) {
            return BoatEnchantments.stepMotion(boat, motion);
        }
        return motion;
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"))
    private void coolstuff$preserveDrop(ItemStack stack, float offset, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.entity.item.ItemEntity> cir) {
        if ((Object) this instanceof Boat boat && stack.is(boat.getDropItem())) {
            BoatEnchantments.copyEnchantments(boat, stack);
        }
    }
}
