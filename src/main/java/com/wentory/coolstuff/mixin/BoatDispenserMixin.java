package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.entity.EnchantedBoatAccess;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BoatDispenseItemBehavior.class)
public abstract class BoatDispenserMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean coolstuff$transferEnchantments(ServerLevel level, Entity entity, BlockSource source, ItemStack stack) {
        ((EnchantedBoatAccess) entity).coolstuff$setBoatStack(stack);
        return level.addFreshEntity(entity);
    }
}
