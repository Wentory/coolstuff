package com.wentory.coolstuff.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class EmissiveTrims {
    private static final String TAG = "coolstuff_emissive_trim";

    private EmissiveTrims() {
    }

    public static boolean canApply(ItemStack stack) {
        return stack.has(DataComponents.TRIM) && !isEmissive(stack);
    }

    public static boolean isEmissive(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.contains(TAG) && data.copyTag().getBoolean(TAG);
    }

    public static ItemStack apply(ItemStack stack) {
        ItemStack result = stack.copyWithCount(1);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putBoolean(TAG, true));
        return result;
    }
}
