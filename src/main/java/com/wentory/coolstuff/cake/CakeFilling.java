package com.wentory.coolstuff.cake;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;

public enum CakeFilling {
    NONE("", ChatFormatting.GRAY),
    GUNPOWDER("gunpowder", ChatFormatting.DARK_GRAY),
    STONE("stone", ChatFormatting.GRAY),
    ICE("ice", ChatFormatting.AQUA),
    SNOW("snow", ChatFormatting.WHITE),
    CHORUS("chorus", ChatFormatting.LIGHT_PURPLE),
    END_ROD("end_rod", ChatFormatting.LIGHT_PURPLE),
    CANDLE("candle", ChatFormatting.YELLOW),
    LIGHTNING_ROD("lightning_rod", ChatFormatting.GOLD),
    REDSTONE("redstone", ChatFormatting.RED),
    SLIME("slime", ChatFormatting.GREEN),
    HONEY("honey", ChatFormatting.GOLD),
    GLOW_INK("glow_ink", ChatFormatting.AQUA),
    INK("ink", ChatFormatting.DARK_GRAY),
    PUFFERFISH("pufferfish", ChatFormatting.YELLOW),
    PHANTOM_MEMBRANE("phantom_membrane", ChatFormatting.LIGHT_PURPLE),
    ECHO("echo", ChatFormatting.DARK_AQUA);

    private static final String TAG = "coolstuff:cake_filling";
    private final String id;
    private final ChatFormatting color;

    CakeFilling(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public ChatFormatting color() {
        return color;
    }

    public void applyTo(ItemStack stack) {
        if (this == NONE) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TAG, id));
    }

    public static CakeFilling fromStack(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return fromId(data.copyTag().getString(TAG));
    }

    public static CakeFilling fromId(String id) {
        if (id == null || id.isEmpty()) return NONE;
        try {
            return valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
