package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.item.EmissiveTrims;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {
    @Inject(method = "createInputSlotDefinitions", at = @At("RETURN"), cancellable = true)
    private void coolstuff$allowGlowInk(CallbackInfoReturnable<ItemCombinerMenuSlotDefinition> cir) {
        ItemCombinerMenuSlotDefinition original = cir.getReturnValue();
        ItemCombinerMenuSlotDefinition.Builder builder = ItemCombinerMenuSlotDefinition.create();
        for (ItemCombinerMenuSlotDefinition.SlotDefinition slot : original.getSlots()) {
            Predicate<ItemStack> predicate = slot.mayPlace();
            if (slot.slotIndex() == SmithingMenu.ADDITIONAL_SLOT) {
                predicate = predicate.or(stack -> CoolstuffConfig.ENABLE_EMISSIVE_TRIMS.get()
                        && stack.is(Items.GLOW_INK_SAC));
            }
            builder.withSlot(slot.slotIndex(), slot.x(), slot.y(), predicate);
        }
        ItemCombinerMenuSlotDefinition.SlotDefinition result = original.getResultSlot();
        builder.withResultSlot(result.slotIndex(), result.x(), result.y());
        cir.setReturnValue(builder.build());
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void coolstuff$createEmissiveTrimResult(CallbackInfo ci) {
        ItemCombinerMenuAccessor accessor = (ItemCombinerMenuAccessor) this;
        Container inputs = accessor.coolstuff$getInputSlots();
        if (!isEmissiveRecipe(inputs)) return;
        ResultContainer result = accessor.coolstuff$getResultSlots();
        result.setRecipeUsed(null);
        result.setItem(0, EmissiveTrims.apply(inputs.getItem(SmithingMenu.BASE_SLOT)));
        ci.cancel();
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void coolstuff$mayPickupEmissiveTrim(Player player, boolean hasStack,
                                                  CallbackInfoReturnable<Boolean> cir) {
        Container inputs = ((ItemCombinerMenuAccessor) this).coolstuff$getInputSlots();
        if (isEmissiveRecipe(inputs)) cir.setReturnValue(true);
    }

    @Inject(method = "getSlotToQuickMoveTo", at = @At("HEAD"), cancellable = true)
    private void coolstuff$quickMoveGlowInk(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (CoolstuffConfig.ENABLE_EMISSIVE_TRIMS.get() && stack.is(Items.GLOW_INK_SAC)) {
            cir.setReturnValue(SmithingMenu.ADDITIONAL_SLOT);
        }
    }

    @Inject(method = "canMoveIntoInputSlots", at = @At("HEAD"), cancellable = true)
    private void coolstuff$allowQuickMoveGlowInk(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CoolstuffConfig.ENABLE_EMISSIVE_TRIMS.get() && stack.is(Items.GLOW_INK_SAC)) {
            cir.setReturnValue(true);
        }
    }

    private static boolean isEmissiveRecipe(Container inputs) {
        return CoolstuffConfig.ENABLE_EMISSIVE_TRIMS.get()
                && inputs.getItem(SmithingMenu.TEMPLATE_SLOT).isEmpty()
                && EmissiveTrims.canApply(inputs.getItem(SmithingMenu.BASE_SLOT))
                && inputs.getItem(SmithingMenu.ADDITIONAL_SLOT).is(Items.GLOW_INK_SAC);
    }
}
