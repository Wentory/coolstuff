package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.entity.EnchantedBoatAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoatItem.class)
public abstract class BoatItemMixin {
    @Inject(method = "getBoat", at = @At("RETURN"))
    private void coolstuff$transferEnchantments(Level level, HitResult hit, ItemStack stack, Player player,
                                               CallbackInfoReturnable<Boat> cir) {
        ((EnchantedBoatAccess) cir.getReturnValue()).coolstuff$setBoatStack(stack);
    }
}
